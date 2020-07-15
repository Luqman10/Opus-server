/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.samaritan.opus.service;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.samaritan.opus.application.OpusProperties;
import com.samaritan.opus.response.PropertiesResponse;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Response;

/**
 * this resource serves requests for application properties like version code
 * @author electron
 */
@Path("/properties")
public class PropertiesResource {
    
    @Path("/versionCode")
    @GET
    @Produces("application/json")
    public Response getVersionCode(){
        
        //read version code
        int versionCode = OpusProperties.OPUS_VERSION_CODE ;
        PropertiesResponse propertiesResponse = new PropertiesResponse() ;
        propertiesResponse.setVersionCode(versionCode) ;
        
        //send response
        Response.ResponseBuilder responseBuilder = Response.ok() ;
        responseBuilder.entity(createPropertiesResponseJson(propertiesResponse)) ;
        
        return responseBuilder.build() ;
    }

    /**
     * parse the given propertiesResponse object to json
     * @param propertiesResponse
     * @return the json representation
     */
    private String createPropertiesResponseJson(PropertiesResponse propertiesResponse) {
        
        Gson gson = new GsonBuilder().create() ;
        return gson.toJson(propertiesResponse, PropertiesResponse.class) ;
    }
    
    
    
}
