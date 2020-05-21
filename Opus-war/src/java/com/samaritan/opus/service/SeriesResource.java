/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.samaritan.opus.service;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.samaritan.opus.application.OpusApplication;
import com.samaritan.opus.model.Series;
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
 * service for series model
 * @author electron
 */
@Path("/series")
public class SeriesResource {
    
    //servlet context
    @Context
    protected ServletContext servletContext ;

    //send messages to the log
    Logger logger = Logger.getLogger("com.samaritan.opus.service.SeriesResource") ;
    
    /**
     * get a list of series whose title matches the search query
     * @param query the search query to match the series title against
     * @return response to the client
     */
    @Path("/search/name")
    @GET
    @Produces("application/json")
    public Response getSeries(@QueryParam("q") String query){
        
        //change query to lowercase so the search becomes case in-sensitive
        query = query.toLowerCase() ;
        
        //get the list of series that match query from DB
        List<Series> listOfSeries = selectSeriesFromDBMatchingTitle(query) ;
        
        logger.log(Level.INFO, listOfSeries.size() + " series found matching \'" + 
                    query + "\'") ;
                    
        //set the base64 representation of each series' poster image
        for(Series series: listOfSeries){
                
            try{
                    
                //generate the Base64 string of each series' poster image and set it to the
                //posterImage field of each series object
                String posterImage = series.getPosterImage() ;
                if(posterImage != null){

                    File file = new File(posterImage) ;
                    String base64 = Base64Util.convertFileToBase64(file) ;
                    series.setPosterImage(base64) ;
                         
                }
            }
            catch(IOException ex){
                    
                logger.log(Level.SEVERE, "An IO exception occured when converting the image file "
                    + "to base64") ;
            }
        }
            
        //if a match was found, send status code 200
        //parse list of docs to JSON and set JSON as entity of response
        Gson gson = new GsonBuilder()
        .excludeFieldsWithoutExposeAnnotation()
        .serializeNulls()
        .create();
        String jsonString = gson.toJson(listOfSeries) ;
        Response.ResponseBuilder responseBuilder = Response.ok() ;
        responseBuilder.entity(jsonString) ;
        
        return responseBuilder.build() ;
    }
    
    /**
     * get the sample of a series sent to the client as a stream of bytes
     * @param id the id of the series whose sample is requested
     * @return the series sample file (if exists) that will be written to the client as bytes incrementally / null if the 
     * file doesn't exist
     */
    @Path("/sample")
    @GET
    @Produces("video/mp4")
    public File getSeriesSample(@QueryParam("id") int id){
        
            
        //get the series with the given id from DB
        Series series = selectSeriesFromDB(id) ;

        //proceed if there's a series with that id, it has a sample and the series file exists
        if(series != null && series.getSample() != null){

            //create file for the sample
            File file = new File(series.getSample()) ;
            
            //return the file if it exists
            if(file.exists())
                return file ;

        }
        
        //return null if any of the above conditions fail
        return null ;
        
    }
    
    /**
     * select the series from the DB with the given id
     * @param id
     * @return the series
     */
    private Series selectSeriesFromDB(int id){
        
        SessionFactory sessionFactory = (SessionFactory)servletContext.getAttribute(OpusApplication.HIBERNATE_SESSION_FACTORY) ;
        Session session = sessionFactory.openSession() ;
        
        try{
        
            Query<Series> query = session.createQuery("FROM Series WHERE id =:id", Series.class) ;
            query.setParameter("id", id) ;
            Series series = query.getSingleResult() ;
            session.close() ;
            return series ;
        }
        catch(NoResultException ex){
            
            session.close() ;
            return null ;
        }
    }
    
    
    /**
     * select all series from the database whose title matches the query
     * @param queryString the query term to use in matching
     * @return 
     */
    private List<Series> selectSeriesFromDBMatchingTitle(String queryString){
        
        SessionFactory sessionFactory = (SessionFactory)servletContext.getAttribute(OpusApplication.HIBERNATE_SESSION_FACTORY) ;
        Session session = sessionFactory.openSession() ;
        Query<Series> query = session.createQuery("FROM Series s JOIN FETCH s.videoProducer JOIN FETCH s.videoCategory WHERE title LIKE :title", Series.class) ;
        queryString = "%" + queryString + "%" ;
        query.setParameter("title", queryString) ;
        List<Series> listOfSeries = query.getResultList() ;
        session.close() ;
        return listOfSeries ;
    }
     
    
}
