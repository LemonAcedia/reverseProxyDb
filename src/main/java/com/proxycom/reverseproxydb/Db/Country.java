/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.proxycom.reverseproxydb.Db;

public class Country {
    
    private int Id = 0;
    private int countryCode = 0;
    private String countryName = "";
    
    public Country(int id, int code, String name){
       this.countryName = name;
       this.countryCode = code;
       this.Id = id;
    }
    
    public int getId(){
        return Id;
    }
        
    public String getCountryName(){
        return countryName;
    }
    
    public int getCountryCode(){
        return countryCode;
    }
}
