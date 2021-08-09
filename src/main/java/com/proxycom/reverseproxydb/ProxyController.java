/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.proxycom.reverseproxydb;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.message.BasicNameValuePair;
import redis.clients.jedis.Jedis;
import sun.misc.IOUtils;

public class ProxyController {
    
    private static Jedis redisCache = null;
    private static int brokerPort = 0;
    
    private static final String SendMessage_ENDPOINT = "/sendMessage";
    private static final String GetMessage_ENDPOINT = "/getMessage";
    private static final String SetDataFormat_ENDPOINT = "/setDataFormat";
    private static final String InsertCountry_ENDPOINT = "/insertCountry";
    private static final String GetAll_ENDPOINT = "/getAll";
    private static final String GetCountryByCode_ENDPOINT = "/getCountryByCode";
    private static final String GetCountryByName_ENDPOINT = "/getCountryByName";
    private static final String DeleteCountryByCode_ENDPOINT = "/deleteCountryByCode";

    ProxyController(){
        
        redisCache = new Jedis();
        redisCache.flushDB(); // Чистим кеш перед каждым запуском приложения
    }
    
    public void initContexts(HttpServer server){
    
        server.createContext("/test", new TestHandler());
        server.createContext(SendMessage_ENDPOINT, new SendMessageHandler());
        server.createContext(GetMessage_ENDPOINT, new GetMessageHandler());
        server.createContext(SetDataFormat_ENDPOINT, new SetDataTypeHandler());

        // Работа с БД
        server.createContext(InsertCountry_ENDPOINT, new InsertNewCountryHandler());
        server.createContext(GetAll_ENDPOINT, new GetAllCountriesHandler());
        server.createContext(GetCountryByCode_ENDPOINT, new GetCountryByCodeHandler());
        server.createContext(GetCountryByName_ENDPOINT, new GetCountryByNameHandler());
        server.createContext(DeleteCountryByCode_ENDPOINT, new DeleteCountryByCodeHandler());
    }
    
    public void setBrokerPort(int port){
        brokerPort = port;
    }

    private static Map<String, Object> getRequestParams(URI requestedUri, InputStream requestBody, String method){
    
        try{
        
            Map<String, Object> parameters = new HashMap<String, Object>();
            
            String query = "";
            
            if(method.equals("GET"))
                query = requestedUri.getRawQuery();
            else if(method.equals("POST")){
            
                 InputStreamReader isr = new InputStreamReader(requestBody, "utf-8");
                 BufferedReader br = new BufferedReader(isr);
                 query = br.readLine();
            }
               
            parseQuery(query, parameters);
            
            return parameters;
            
        }catch(Exception e){
        
            System.out.println(e.getMessage());
        }
    
        return null;
    }
    
    private static void parseQuery(String query, Map<String, Object> parameters) throws UnsupportedEncodingException {

         if (query != null) {
                 String pairs[] = query.split("[&]");
                 for (String pair : pairs) {
                          String param[] = pair.split("[=]");
                          String key = null;
                          String value = null;
                          if (param.length > 0) {
                            key = URLDecoder.decode(param[0], 
                          	System.getProperty("file.encoding"));
                          }

                          if (param.length > 1) {
                            value = URLDecoder.decode(param[1], 
                            System.getProperty("file.encoding"));
                          }

                          if (parameters.containsKey(key)) {
                                   Object obj = parameters.get(key);
                                   if (obj instanceof List<?>) {
                                            List<String> values = (List<String>) obj;
                                            values.add(value);

                                   } else if (obj instanceof String) {
                                            List<String> values = new ArrayList<String>();
                                            values.add((String) obj);
                                            values.add(value);
                                            parameters.put(key, values);
                                   }
                          } else {
                                   parameters.put(key, value);
                          }
                    }
             }
      }
    
    private static String getResponseFromBroker(String endPoint, Map<String, Object> parameters){
    
        String result = "";
        
        try {
            
            String countryName = "";
            String countryCode = "";
            
            if(parameters.containsKey("countryName"))
                countryName = parameters.get("countryName").toString();
            
            if(parameters.containsKey("countryCode"))
                countryCode = parameters.get("countryCode").toString();
                        
            boolean isNeedRequest = true;

            if(redisCache.exists(endPoint) && redisCache.type(endPoint).equals("hash"))
                isNeedRequest = redisCache.exists(endPoint) ? 
                    (redisCache.hexists(endPoint, countryName) 
                    || redisCache.hexists(endPoint, countryCode) ? false: true) : true;
            else isNeedRequest = !redisCache.exists(endPoint);

            if(isNeedRequest){
            
                HttpClient httpclient = HttpClients.createDefault();
                HttpPost httppost = new HttpPost("http://localhost:" + String.valueOf(brokerPort) + endPoint);

                List<NameValuePair> params = new ArrayList<NameValuePair>();

                for (String key : parameters.keySet())
                    params.add(new BasicNameValuePair(key, parameters.get(key).toString()));            

                if(params.size() > 0)
                    httppost.setEntity(new UrlEncodedFormEntity(params, "UTF-8"));

                HttpResponse response = httpclient.execute(httppost);
                HttpEntity entity = response.getEntity();

                if (entity != null) {
                    InputStream instream = entity.getContent();

                    result = convertStreamToString(instream);

                    setRedisCache(endPoint, result, parameters);
                }
            }else{
                
                if(redisCache.type(endPoint).equals("hash")){
                
                    if(redisCache.hexists(endPoint, countryName))
                            result = redisCache.hget(endPoint, countryName);
                    else if(redisCache.hexists(endPoint, countryCode))
                        result = redisCache.hget(endPoint, countryCode);
                }
                else
                    result = redisCache.get(endPoint);
            }
        } catch (IOException ex) {
            
            System.out.println(ex.getMessage());
        }
        
        return result;
    }
    
    private static void setRedisCache(String endPoint, String response, Map<String, Object> parameters){
    
        switch(endPoint){

            case SendMessage_ENDPOINT:
                if(redisCache.exists(GetMessage_ENDPOINT))
                    redisCache.del(GetMessage_ENDPOINT);
                break;

            case GetMessage_ENDPOINT:        
                redisCache.set(endPoint, response);
                break;

            case InsertCountry_ENDPOINT:
                if(redisCache.exists(GetAll_ENDPOINT))
                    redisCache.del(GetAll_ENDPOINT);
                break;

            case GetAll_ENDPOINT:
                redisCache.set(endPoint, response);
                break;

            case GetCountryByCode_ENDPOINT:
                if(parameters.containsKey("countryCode"))
                    redisCache.hset(GetCountryByCode_ENDPOINT, parameters.get("countryCode").toString(), response);
                break;

            case GetCountryByName_ENDPOINT:
                if(parameters.containsKey("countryName"))
                    redisCache.hset(GetCountryByName_ENDPOINT, parameters.get("countryName").toString(), response);
                break;

            case DeleteCountryByCode_ENDPOINT:
                if(redisCache.exists(GetAll_ENDPOINT))
                    redisCache.del(GetAll_ENDPOINT);
                
                if(redisCache.exists(GetCountryByCode_ENDPOINT))
                    redisCache.del(GetCountryByCode_ENDPOINT);
                         
                if(redisCache.exists(GetCountryByName_ENDPOINT))
                    redisCache.del(GetCountryByName_ENDPOINT);
                break;
                
            case SetDataFormat_ENDPOINT:
                // Чтобы наглядно увидеть работу кеша redis, можно закомментировать этот метод
                // Если его закомментировать, то кеш не будет реагировать на смену формата данных - при смене формата, redis будет возвращать данные в старом формате, пока не перезапишет их
                // Если оставить этот метод рабочим, то кеш будет чиститься и перезаписываться с новым форматом данных
                redisCache.flushDB();
                break;

            default:
                break;
        }
    }
    
    static String convertStreamToString(java.io.InputStream is) {
        java.util.Scanner s = new java.util.Scanner(is).useDelimiter("\\A");
        return s.hasNext() ? s.next() : "";
    }
    
    public static class TestHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange he) throws IOException{

            String response = "REUEST URL";
            System.out.println(response);
            System.out.println(he.getRequestURI());
            OutputStream os = he.getResponseBody();
            
            he.sendResponseHeaders(200, response.length());
            os.write(response.getBytes());
            os.close();      
        }
    }
    
    private static void HandleRequests(HttpExchange he, String endPoint) throws IOException{
    
        int statusCode = 200;
        String response = "";
        OutputStream os = he.getResponseBody();

        try{

            Map<String, Object> parameters = getRequestParams(he.getRequestURI(), he.getRequestBody(), he.getRequestMethod());

            if(parameters != null){                    
                response = getResponseFromBroker(endPoint, parameters);

                if(response.equals("")){

                    response = "<h1>404 NOT FOUND</h1>";
                    statusCode = 404;
                }

            }else{

                response = "<h1>404 NOT FOUND</h1>";
                statusCode = 404;
            }

        }catch(Exception ex){

            System.out.println(ex.getMessage());

            statusCode = 500;
            response = "<h1>Internal Proxy Error</h1>";
        }

        he.sendResponseHeaders(statusCode, response.length());
        os.write(response.getBytes());
        os.close();       
    }

    public static class SendMessageHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange he) throws IOException{
            HandleRequests(he, SendMessage_ENDPOINT);
        }
    }

    public static class GetMessageHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange he) throws IOException {
            HandleRequests(he, GetMessage_ENDPOINT);
        }
    }

    public static class SetDataTypeHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange he) throws IOException {
            HandleRequests(he, SetDataFormat_ENDPOINT);
        }
    }

    public static class InsertNewCountryHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange he) throws IOException {
            HandleRequests(he, InsertCountry_ENDPOINT);
        }
    }

    public static class GetAllCountriesHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange he) throws IOException {
            HandleRequests(he, GetAll_ENDPOINT);
        }
    }

    public static class GetCountryByCodeHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange he) throws IOException {
            HandleRequests(he, GetCountryByCode_ENDPOINT);
        }
    }

    public static class GetCountryByNameHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange he) throws IOException {
            HandleRequests(he, GetCountryByName_ENDPOINT);
        }
    }

    public static class DeleteCountryByCodeHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange he) throws IOException {
            HandleRequests(he, DeleteCountryByCode_ENDPOINT);
        }
    }
}