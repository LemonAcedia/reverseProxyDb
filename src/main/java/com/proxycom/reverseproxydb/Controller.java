/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.proxycom.reverseproxydb;

import com.proxycom.reverseproxydb.Db.Country;
import com.proxycom.reverseproxydb.Db.DbController;
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
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

public class Controller {
    
    DbController db = null;

    Controller(){
    
        db = new DbController();
    }
    
    private static String ServerMessage = "";
    private static int messageCounter = 0;
    private final String ServerKey = "serverMessage";
 
    private static final int DATA_FORMAT_TYPE_JSON = 0;  
    private static final int DATA_FORMAT_TYPE_XML = 1;  
    
    private int dataFormatType = DATA_FORMAT_TYPE_XML;

    public void initContexts(HttpServer server){
    
        server.createContext("/test", new TestHandler());
        server.createContext("/sendMessage", new SendMessageHandler());
        server.createContext("/getMessage", new GetMessageHandler());
        server.createContext("/setDataFormat", new SetDataFormatHandler());

        // Работа с БД
        server.createContext("/insertCountry", new InsertNewCountryHandler());
        server.createContext("/getAll", new GetAllCountriesHandler());
        server.createContext("/getCountryByCode", new GetCountryByCodeHandler());
        server.createContext("/getCountryByName", new GetCountryByNameHandler());
        server.createContext("/deleteCountryByCode", new DeleteCountryByCodeHandler());
    }
    
    private String listToCurrentFormat(List<Country> countries){
    
        String result = "";
        
        if(dataFormatType == DATA_FORMAT_TYPE_JSON){
                  
            JSONObject rootJson = new JSONObject();
            rootJson.put("id", messageCounter);

            JSONArray innerObject = new JSONArray();
            
            for(Country country: countries){
                JSONObject objectCountry = new JSONObject();

                objectCountry.put("countryCode", String.valueOf(country.getCountryCode()));
                objectCountry.put("countryName", country.getCountryName());
                
                innerObject.add(objectCountry);
            }
            
            rootJson.put("content", innerObject.toJSONString());
            
            result = rootJson.toJSONString();
        
        }else if(dataFormatType == DATA_FORMAT_TYPE_XML){
        
            result = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>";  
            result += "     <message id=\"" + messageCounter +"\">";
            result += "         <countryList>";

            for(Country country: countries){
            
                result += "             <country>";
                result += "                 " + StringToXMLString("countryCode", String.valueOf(country.getCountryCode()));
                result += "                 " + StringToXMLString("countryName", country.getCountryName());
                result += "             </country>";
            }
            
            result += "         </countryList>";
            result += "     </message>";
        }
        
        messageCounter++;
        
        return result;
    }
    
    private String convertToCurrentFormat(String key, String message){
    
        String result = "";
        
        if(dataFormatType == DATA_FORMAT_TYPE_JSON){
        
            result = StringToJSONString(key, message);
          
            JSONObject rootJson = new JSONObject();
            rootJson.put("id", messageCounter);

            rootJson.put("content", result);
            
            result = rootJson.toJSONString();
        }
        else if(dataFormatType == DATA_FORMAT_TYPE_XML){
        
            result = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>";  
            result += "     <message id=\"" + messageCounter +"\">";
            result += StringToXMLString(key, message);
            result += "     </message>";
        }
        
        messageCounter++;

        return result;
    }
    
    private String StringToXMLString(String key, String message){

        String result = "          <" + key + " content=\"" + message +"\"/>";
        
        return result;
    }
    
    
    private String StringToJSONString(String key, String message){
    
        JSONObject json = new JSONObject();
        json.put(key, message);
        
        return json.toJSONString();
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
    
    // Тест
    // localhost:32467/test
    private class TestHandler implements HttpHandler  {
        @Override
        public void handle(HttpExchange he) throws IOException {

            String response = "<h1>Если видишь сообщение, значит все ОК</h1>";

            he.sendResponseHeaders(200, response.length());

            OutputStream os = he.getResponseBody();
            os.write(response.getBytes());
            os.close();
        }
    }

    // Конечная точка для отправки сообщения на сервер брокера (GET и POST запрос) 
    // localhost:32467/sendMessage?message=bla-bla-bla (для GET запроса)
    private class SendMessageHandler implements HttpHandler  {
        @Override
        public void handle(HttpExchange he) throws IOException {

            int statusCode = 200;
            String response = "";
            OutputStream os = he.getResponseBody();

            try{
                    Map<String, Object> parameters = getRequestParams(he.getRequestURI(), he.getRequestBody(), he.getRequestMethod());
                    
                    if(parameters != null){
                    
                        ServerMessage = "";
                        
                        for (String key : parameters.keySet())
                            if(key.equals("message"))
                                ServerMessage = parameters.get(key).toString();
                        
                        if(!ServerMessage.equals(""))
                            response = convertToCurrentFormat(ServerKey, "The message sent successfully");
                        else response = convertToCurrentFormat(ServerKey, "The message was not sent because it is missing");
                        
                    }else{
                    
                        response = "<h1>404 NOT FOUND</h1>";
                        statusCode = 404;
                    }
                    
            }catch(Exception e){
            
                System.out.println(e.getMessage());
                
                statusCode = 500;
                response = "<h1>Internal Server Error</h1>";
            }
            
            he.sendResponseHeaders(statusCode, response.length());
            os.write(response.getBytes());
            os.close();       
        }
    }
    
    // Конечная точка для получения сообщения от брокера (GET и POST запрос) 
    // localhost:32467/getMessage (для GET запроса)
    private class GetMessageHandler implements HttpHandler  {
        @Override
        public void handle(HttpExchange he) throws IOException {
            
            OutputStream os = he.getResponseBody();
            String response = "";
            int statusCode = 200;
            
            try{
                                
                response = convertToCurrentFormat(ServerKey, ServerMessage);
                
            }catch(Exception e){
            
                System.out.println(e.getMessage());
                
                statusCode = 500;
                response = "<h1>Internal Server Error</h1>";
            }
            
            he.sendResponseHeaders(statusCode, response.length());
            os.write(response.getBytes());
            os.close();
        }
    }
      
     // Конечная точка для добавления новой страны в БД (GET и POST запрос) 
    // localhost:32467/insertCountry?countryCode=xx...&countryName=name... (для GET запроса)
    private class InsertNewCountryHandler implements HttpHandler  {
        @Override
        public void handle(HttpExchange he) throws IOException {
            
              OutputStream os = he.getResponseBody();
              int statusCode = 200;
              String response = "The message sent successfully";
                                                      
              try{
                    Map<String, Object> parameters = getRequestParams(he.getRequestURI(), he.getRequestBody(), he.getRequestMethod());

                    if(parameters != null){
                    
                        int countryCode = -1;
                        String countryName = "";
                        
                        for (String key : parameters.keySet()){
                        
                            if(key.equals("countryCode"))
                                countryCode = Integer.valueOf(parameters.get(key).toString());
                            else if(key.equals("countryName"))
                                countryName = parameters.get(key).toString();
                        }
                        
                        if(countryCode != -1 && !countryName.equals("")){
                        
                            Country newCountry = new Country(0, countryCode, countryName);

                            boolean isSuccess = db.insertNewCountry(newCountry);
                            
                            if(isSuccess){
                           
                                response = convertToCurrentFormat(ServerKey, "The new country was added to db successfully");
                            }
                            else{
                            
                                response = convertToCurrentFormat(ServerKey, "An error occurred while trying to add a record to the database");
                                statusCode = 500;
                            }
                        
                        }else response = convertToCurrentFormat(ServerKey, "Not all required parameters have been specified");
                    }else{
                    
                        statusCode = 404;
                        response = "<h1>404 NOT FOUND</h1>";
                    }
                    
            }catch(Exception e){
            
                System.out.println(e.getMessage());
                
                statusCode = 500;
                response = "<h1>Internal Server Error</h1>";
            }
              
            he.sendResponseHeaders(statusCode, response.length());
            os.write(response.getBytes());
            os.close();      
        }
    }

    // Конечная точка для  запроса всего, что есть в бд (GET и POST запрос) 
    // localhost:32467/getAll  (для GET запроса)
    private class GetAllCountriesHandler implements HttpHandler  {
        @Override
        public void handle(HttpExchange he) throws IOException {
            
              OutputStream os = he.getResponseBody();
              String response = "";
              int statusCode = 200;
              
              try{
                  
                  List<Country> countries = db.getAllCountries();
                  
                  if(countries != null){
                  
                      response = listToCurrentFormat(countries);
                  
                  }else response = convertToCurrentFormat(ServerKey, "Db Table with countries is empty");
                  
            }catch(Exception e){
            
                System.out.println(e.getMessage());
                
                statusCode = 500;
                response = "<h1>Internal Server Error</h1>";
            }
              
            he.sendResponseHeaders(statusCode, response.length());
            os.write(response.getBytes());
            os.close(); 
        }
    }
    
     // Конечная точка для  запроса страны по ее коду(GET и POST запрос) 
    // localhost:32467/getCountryByCode?countryCode=24  (для GET запроса)
    private class GetCountryByCodeHandler implements HttpHandler  {
        @Override
        public void handle(HttpExchange he) throws IOException {
            
              OutputStream os = he.getResponseBody();
              String response = "";
              int statusCode = 200;
              
              try{
                  
                  Map<String, Object> parameters = getRequestParams(he.getRequestURI(), he.getRequestBody(), he.getRequestMethod());

                  if(parameters != null){
                  
                        int countryCode = -1;
                        
                        for (String key : parameters.keySet()){
                        
                            if(key.equals("countryCode"))
                                countryCode = Integer.valueOf(parameters.get(key).toString());
                        }
                        
                        if(countryCode != -1){
                                                    
                            Country country = db.getCountryByCode(countryCode);
                            
                            if(country != null){

                                List<Country> countries = new ArrayList<>();

                                countries.add(country);
                                response = listToCurrentFormat(countries);
                            }else{
                                
                                statusCode = 404;
                                response = convertToCurrentFormat(ServerKey, "Country with this countryCode is not exists");
                            }

                        }else response = convertToCurrentFormat(ServerKey, "Not all required parameters have been specified");
                  
                  }else{
                  
                        statusCode = 404;
                        response = "<h1>404 NOT FOUND</h1>";
                  }
                  
            }catch(Exception e){
            
                System.out.println(e.getMessage());
                
                statusCode = 500;
                response = "<h1>Internal Server Error</h1>";
            }
              
            he.sendResponseHeaders(statusCode, response.length());
            os.write(response.getBytes());
            os.close(); 
        }
    }
    
     // Конечная точка для  запроса страны по ее названию(GET и POST запрос) 
    // localhost:32467/getCountryByName?countryName=Russia (для GET запроса)
    private class GetCountryByNameHandler implements HttpHandler  {
        @Override
        public void handle(HttpExchange he) throws IOException {
            
              OutputStream os = he.getResponseBody();
              String response = "";
              int statusCode = 200;
              
              try{
                  
                  Map<String, Object> parameters = getRequestParams(he.getRequestURI(), he.getRequestBody(), he.getRequestMethod());

                  if(parameters != null){
                  
                        String countryName = "";
                        
                        for (String key : parameters.keySet()){
                        
                            if(key.equals("countryName"))
                                countryName = parameters.get(key).toString();
                        }
                        
                        if(!countryName.equals("")){
                                                    
                            Country country = db.getCountryByName(countryName);
                            
                            if(country != null){

                                List<Country> countries = new ArrayList<>();

                                countries.add(country);
                                response = listToCurrentFormat(countries);
                            }else{
                                
                                statusCode = 404;
                                response = convertToCurrentFormat(ServerKey, "Country with this countryName is not exists");
                            }

                        }else response = convertToCurrentFormat(ServerKey, "Not all required parameters have been specified");
                  
                  }else{
                  
                        statusCode = 404;
                        response = "<h1>404 NOT FOUND</h1>";
                  }
                  
            }catch(Exception e){
            
                System.out.println(e.getMessage());
                
                statusCode = 500;
                response = "<h1>Internal Server Error</h1>";
            }
              
            he.sendResponseHeaders(statusCode, response.length());
            os.write(response.getBytes());
            os.close(); 
        }
    }
    
     // Конечная точка для удаления страны из БД (GET и POST запрос) 
    // localhost:32467/deleteCountryByCode?countryCode=xx... (для GET запроса)
    private class DeleteCountryByCodeHandler implements HttpHandler  {
        @Override
        public void handle(HttpExchange he) throws IOException {
            
              OutputStream os = he.getResponseBody();
              int statusCode = 200;
              String response = "";
                                                      
              try{
                    Map<String, Object> parameters = getRequestParams(he.getRequestURI(), he.getRequestBody(), he.getRequestMethod());

                    if(parameters != null){
                    
                        int countryCode = -1;
                        
                        for (String key : parameters.keySet()){
                        
                            if(key.equals("countryCode"))
                                countryCode = Integer.valueOf(parameters.get(key).toString());
                        }
                        
                        if(countryCode != -1){
                        
                            boolean isSuccess = db.deleteCountryByCode(countryCode);
                            
                            if(isSuccess){
                           
                                response = convertToCurrentFormat(ServerKey, "The country was deleted successfully");
                            }
                            else{
                            
                                response = convertToCurrentFormat(ServerKey, "An error occurred while trying to delete a record from the database");
                                statusCode = 500;
                            }
                        
                        }else response = convertToCurrentFormat(ServerKey, "Not all required parameters have been specified");
                    }else{
                    
                        statusCode = 404;
                        response = "<h1>404 NOT FOUND</h1>";
                    }
                    
            }catch(Exception e){
            
                System.out.println(e.getMessage());
                
                statusCode = 500;
                response = "<h1>Internal Server Error</h1>";
            }
              
            he.sendResponseHeaders(statusCode, response.length());
            os.write(response.getBytes());
            os.close();      
        }
    }
    
     // Конечная точка для  очистки всего, что есть в бд (GET и POST запрос) 
    // localhost:32467/clearAll  (для GET запроса)
    private class ClearAllDb implements HttpHandler  {
        @Override
        public void handle(HttpExchange he) throws IOException {
            
              OutputStream os = he.getResponseBody();
              String response = "";
              int statusCode = 200;
              
              try{
                                    
                  boolean isSuccess = db.deleteTable();

                  if(isSuccess){

                      response = convertToCurrentFormat(ServerKey, "The table 'countries' was deleted successfully");
                  }
                  else{

                      response = convertToCurrentFormat(ServerKey, "An error occurred while trying to delete the database");
                      statusCode = 500;
                  }

            }catch(Exception e){
            
                System.out.println(e.getMessage());
                
                statusCode = 500;
                response = "<h1>Internal Server Error</h1>";
            }
              
            he.sendResponseHeaders(statusCode, response.length());
            os.write(response.getBytes());
            os.close(); 
        }
    }

     // Конечная точка для установки формата данных (GET и POST запрос) 
    // localhost:32467/setDataFormat?type=(xml|json)      (для GET запроса)
    private class SetDataFormatHandler implements HttpHandler  {
        @Override
        public void handle(HttpExchange he) throws IOException {

            int statusCode = 200;
            String response = "";
            OutputStream os = he.getResponseBody();

            try{
                    Map<String, Object> parameters = getRequestParams(he.getRequestURI(), he.getRequestBody(), he.getRequestMethod());
                    
                    if(parameters != null){
                                            
                        String dataFormat = "";
                        
                        for (String key : parameters.keySet()){
                                                    
                            if(key.equals("type"))
                                dataFormat = parameters.get(key).toString();
                        }

                        
                        if(!dataFormat.equals("")){
                        
                            if(dataFormat.equals("xml")){
                            
                                dataFormatType = DATA_FORMAT_TYPE_XML;
                                response = convertToCurrentFormat(ServerKey, "The data format set successfully");
                            }
                            else if(dataFormat.equals("json")){
                            
                               dataFormatType = DATA_FORMAT_TYPE_JSON;
                               response = convertToCurrentFormat(ServerKey, "The data format set successfully");
                            }
                            else response = convertToCurrentFormat(ServerKey, "Unknown data format, please select between xml and json");
                        }
                        else response = convertToCurrentFormat(ServerKey, "The data format was not set because type parameter is missing");
                        
                    }else{
                    
                        response = "<h1>404 NOT FOUND</h1>";
                        statusCode = 404;
                    }
                    
            }catch(Exception e){
            
                System.out.println(e.getMessage());
                
                statusCode = 500;
                response = "<h1>Internal Server Error</h1>";
            }
            
            he.sendResponseHeaders(statusCode, response.length());
            os.write(response.getBytes());
            os.close();       
        }
    }
}
