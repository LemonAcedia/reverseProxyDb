/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.proxycom.reverseproxydb.Db;

import com.datastax.oss.driver.api.core.CqlSession;
import com.proxycom.reverseproxydb.Db.repository.KeyspaceRepository;
import com.proxycom.reverseproxydb.Db.repository.CountriesRepository;
import java.util.List;


public class DbController {
    
    DbConnector connector = null;
    KeyspaceRepository keysStore = null;
    CountriesRepository countriesRepository = null;
    
    final String KEYSPACE = "countries";
    
    public DbController(){
    
        connector = new DbConnector();
        connector.connect("127.0.0.1", 9042, "datacenter1");
        CqlSession session = connector.getSession();
        
        keysStore = new KeyspaceRepository(session);
        keysStore.createKeyspace(KEYSPACE, "SimpleStrategy", 1);
        keysStore.useKeyspace(KEYSPACE);
        
        countriesRepository = new CountriesRepository(session, KEYSPACE);
    }
    
    public boolean deleteTable(){
        return countriesRepository.deleteTable();
    }
    
    public boolean insertNewCountry(Country country){
        return countriesRepository.insertCountry(country);
    }
    
    public boolean deleteCountryByCode(int countryCode){
        return countriesRepository.deleteCountryByCode(countryCode, KEYSPACE);
    }
    
    public Country getCountryByName(String name){
        return countriesRepository.selectByCountryName(name, KEYSPACE);
    }
    
    public Country getCountryByCode(int code){
        return countriesRepository.selectByCountryCode(code, KEYSPACE);
    } 
     
    public List<Country> getAllCountries(){
        return countriesRepository.selectAll(KEYSPACE);
    }
}
