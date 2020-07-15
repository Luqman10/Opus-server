/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.samaritan.opus.service;

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
        
        
    }
    
}
