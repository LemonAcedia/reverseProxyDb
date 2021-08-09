/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.proxycom.reverseproxydb.Db.repository;

import com.datastax.oss.driver.api.core.CqlIdentifier;
import com.datastax.oss.driver.api.core.CqlSession;
import com.datastax.oss.driver.api.core.cql.BoundStatement;
import com.datastax.oss.driver.api.core.cql.PreparedStatement;
import com.datastax.oss.driver.api.core.cql.ResultSet;
import com.datastax.oss.driver.api.core.cql.Row;
import com.datastax.oss.driver.api.core.cql.SimpleStatement;
import com.datastax.oss.driver.api.core.type.DataTypes;
import static com.datastax.oss.driver.api.core.type.codec.TypeCodecs.UUID;
import com.datastax.oss.driver.api.querybuilder.QueryBuilder;
import static com.datastax.oss.driver.api.querybuilder.QueryBuilder.literal;
import com.datastax.oss.driver.api.querybuilder.SchemaBuilder;
import com.datastax.oss.driver.api.querybuilder.delete.Delete;
import com.datastax.oss.driver.api.querybuilder.insert.RegularInsert;
import com.datastax.oss.driver.api.querybuilder.relation.Relation;
import com.datastax.oss.driver.api.querybuilder.schema.CreateTable;
import com.datastax.oss.driver.api.querybuilder.select.Select;
import com.proxycom.reverseproxydb.Db.Country;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.function.Predicate;
import java.util.stream.Collectors;

public class CountriesRepository {
    
    private static final String TABLE_NAME = "countries";

    private CqlSession session;
    
    private int counterIDs = 0;

    public CountriesRepository(CqlSession session, String keyspace) {
        this.session = session;
        
        createTable();
        
        counterIDs = getLastId(keyspace) + 1;
    }
    
    private int getLastId(String keyspace){
        
        try{
        
            List<Country> countries = selectAll(keyspace);

            return countries.stream().max(Comparator.comparing(Country::getId)).orElseThrow(NoSuchElementException::new).getId();

        }catch(Exception e){
        
            System.out.println(e.getMessage());
        }
        
        return 0;
  }
      
    public boolean deleteTable(){
    
        try{
        
            StringBuilder sb = new StringBuilder("DROP TABLE ")
                .append(TABLE_NAME);

            String query = sb.toString();
            session.execute(query);
            
            return true;
            
        }catch(Exception e){
        
           System.out.println(e.getMessage());
        }
        
        return false;
    }
    
    public void createTable() {
        
        StringBuilder sb = new StringBuilder("CREATE TABLE  IF NOT EXISTS ")
            .append(TABLE_NAME).append("(")
            .append("Id int PRIMARY KEY, ")
            .append("countryCode int,")
            .append("countryName text);");

        String query = sb.toString();
        session.execute(query);
    }
    
    public boolean insertCountry(Country country) {
        try{
            StringBuilder sb = new StringBuilder("INSERT INTO ").append(TABLE_NAME).append("( Id, countryCode, countryName) ").append("VALUES (").append(counterIDs).append(",").append(country.getCountryCode()).append(",'").append(country.getCountryName()).append("')");
                
            final String query = sb.toString();
            session.execute(query);

            counterIDs++;
        
            return true;
        }catch(Exception e){
        
            System.out.println(e.getMessage());
        }
        
        return false;
    }
    
    public Country selectByCountryCode(int countryCode, String keyspace) {
      
        Select select = QueryBuilder.selectFrom(TABLE_NAME).all();
       
        ResultSet resultSet = executeStatement(select.build(), keyspace);
 
        List<Country> countriesList = new ArrayList<Country>();
 
        resultSet.forEach(x -> {
            if(x.getInt("countryCode") == countryCode)
                countriesList.add(new Country(x.getInt("Id"), x.getInt("countryCode"), x.getString("countryName")));
        });

        if(countriesList.size() > 0)
            return countriesList.get(0);
        else return null;
    }
    
    public Country selectByCountryName(String countryName, String keyspace) {
        Select select = QueryBuilder.selectFrom(TABLE_NAME).all();
       
        ResultSet resultSet = executeStatement(select.build(), keyspace);
 
        List<Country> countriesList = new ArrayList<Country>();
        
        resultSet.forEach(x -> {
            final String tmpStr = x.getString("countryName");
            
            if(tmpStr.equals(countryName))
                countriesList.add(new Country(x.getInt("Id"), x.getInt("countryCode"), x.getString("countryName")));
        });

        if(countriesList.size() > 0)
            return countriesList.get(0);
        else return null;
    }
        
    public List<Country> selectAll(String keyspace) {
        Select select = QueryBuilder.selectFrom(TABLE_NAME).all();
        
        ResultSet resultSet = executeStatement(select.build(), keyspace);
 
        List<Country> result = new ArrayList<Country>();
 
        resultSet.forEach(x -> result.add(
            new Country(x.getInt("Id"), x.getInt("countryCode"), x.getString("countryName"))
        ));
 
        if(result.size() > 0)
            return result;
        else return null;
    }
        
    private ResultSet executeStatement(SimpleStatement statement, String keyspace) {
  
        if (keyspace != null) 
            statement.setKeyspace(CqlIdentifier.fromCql(keyspace));

        return session.execute(statement);
    }
            
    public boolean deleteCountryByCode(int countryCode, String keyspace) {
        try{
        
            Country country = selectByCountryCode(countryCode, keyspace);
        
            StringBuilder sb = new StringBuilder("DELETE FROM ").append(TABLE_NAME).append(" WHERE Id=").append(country.getId());

            final String query = sb.toString();
            session.execute(query);
            
            return true;
        }catch(Exception e){
        
            System.out.println(e.getMessage());
        }
        
        return false;
    }
}
