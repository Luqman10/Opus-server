/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.samaritan.opus.service;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.samaritan.opus.application.OpusApplication;
import com.samaritan.opus.model.Documentary;
import com.samaritan.opus.util.Base64Util;
import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.persistence.NoResultException;
import javax.servlet.ServletContext;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.query.Query;

/**
 * service for documentary model
 * @author electron
 */
@Path("/documentary")
public class DocumentaryResource {
    
    //send messages to the log
    Logger logger = Logger.getLogger("com.samaritan.opus.service.DocumentaryResource") ;
    
    //servlet context
    @Context
    protected ServletContext servletContext ;
    
    /**
     * get a list of docs whose title matches the search query
     * @param query the query term to match the doc title against
     * @return response to the client
     */
    @Path("/search/name")
    @GET
    @Produces("application/json")
    public Response getDocumentaries(@QueryParam("q") String query){
        
        //change query to lowercase so the search becomes case in-sensitive
        query = query.toLowerCase() ;
        
        //get the list of docs that match query from DB
        List<Documentary> listOfDocumentaries = selectDocumentariesFromDBMatchingTitle(query) ;
            
        logger.log(Level.INFO, listOfDocumentaries.size() + " docs found matching \'" + 
            query + "\'") ;
            
        //set the base64 representation of each doc's poster image
        for(Documentary documentary: listOfDocumentaries){
                
            try{
                    
                //generate the Base64 string of each doc's poster image and set it to the
                //posterImage field of each doc object
                String posterImage = documentary.getPosterImage() ;
                if(posterImage != null){

                    File file = new File(posterImage) ;
                    String base64 = Base64Util.convertFileToBase64(file) ;
                    documentary.setPosterImage(base64) ;
                         
                }
            }
            catch(IOException ex){
                    
                logger.log(Level.SEVERE, "An IO exception occured when converting the image file "
                    + "to base64") ;
            }
        }
            
        //send http status code 200
        //parse list of docs to JSON and set JSON as entity of response
        Gson gson = new GsonBuilder()
        .excludeFieldsWithoutExposeAnnotation()
        .serializeNulls()
        .create();
        String jsonString = gson.toJson(listOfDocumentaries) ;
        Response.ResponseBuilder responseBuilder = Response.ok() ;
        responseBuilder.entity(jsonString) ;
        return responseBuilder.build() ;
    }
    
    /**
     * get the sample of a documentary sent to the client as a stream of bytes
     * @param id the id of the documentary whose sample is requested
     * @return the documentary sample file (if exists) that will be written to the client as bytes incrementally / null if the 
     * file doesn't exist
     */
    @Path("/sample")
    @GET
    @Produces("video/mp4")
    public File getDocumentarySample(@QueryParam("id") int id){
        
            
        //get the documentary with the given id from DB
        Documentary documentary = selectDocumentaryFromDB(id) ;

        //proceed if there's a documentary with that id, it has a sample and the documentary file exists
        if(documentary != null && documentary.getSample() != null){

            //create file for the sample
            File file = new File(documentary.getSample()) ;
            
            //return the file if it exists
            if(file.exists())
                return file ;

        }
        
        //return null if any of the above conditions fail
        return null ;
        
    }
    
    /**
     * select the documentary from the DB with the given id
     * @param id
     * @return the documentary
     */
    private Documentary selectDocumentaryFromDB(int id){
        
        SessionFactory sessionFactory = (SessionFactory)servletContext.getAttribute(OpusApplication.HIBERNATE_SESSION_FACTORY) ;
        Session session = sessionFactory.openSession() ;
        
        try{
        
            Query<Documentary> query = session.createQuery("FROM Documentary WHERE id =:id", Documentary.class) ;
            query.setParameter("id", id) ;
            Documentary documentary = query.getSingleResult() ;
            session.close() ;
            return documentary ;
        }
        catch(NoResultException ex){
            
            session.close() ;
            return null ;
        }
    }
    
    
    /**
     * select all docs from the database whose title matches the query
     * @param queryString the query term to use in matching
     * @return 
     */
    private List<Documentary> selectDocumentariesFromDBMatchingTitle(String queryString){
        
        SessionFactory sessionFactory = (SessionFactory)servletContext.getAttribute(OpusApplication.HIBERNATE_SESSION_FACTORY) ;
        Session session = sessionFactory.openSession() ;
        Query<Documentary> query = session.createQuery("FROM Documentary d JOIN FETCH d.videoProducer JOIN FETCH d.videoCategory WHERE title LIKE :title", Documentary.class) ;
        queryString = "%" + queryString + "%" ;
        query.setParameter("title", queryString) ;
        List<Documentary> listOfDocumentaries = query.getResultList() ;
        session.close() ;
        return listOfDocumentaries ;
    }
    
}
