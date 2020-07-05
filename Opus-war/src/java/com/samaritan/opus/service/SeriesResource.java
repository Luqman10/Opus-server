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
        listOfSeries = setPosterImageForSeries(listOfSeries) ;
            
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
     * get all the series in the DB
     * @return 
     */
    @GET
    @Produces("application/json")
    public Response getAllSeries(){
        
        //get the list of series
        List<Series> listOfSeries = selectAllSeriesFromDB() ;
        
        logger.log(Level.INFO,  "There are " + listOfSeries.size() + " series on the server") ;
                    
        //set the base64 representation of each series' poster image
        listOfSeries = setPosterImageForSeries(listOfSeries) ;
            
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
     * get a list of series recommendations based on a user's series download history
     * @param profileAccountId the user's profile account id
     * @return 
     */
    public List<Series> getSeriesRecommendations(ServletContext servletContext, int profileAccountId){
        
        //init the servletContext field with the one coming from the caller
        this.servletContext = servletContext ;
        //list of series recommendations to return
        List<Series> listOfSeriesRecommendations ;
        //find producer id whose series the user has downloaded most
        int producerDownloadedMostByUser = getProducerDownloadedMostByUser(profileAccountId) ;
        //find category id of series the user has downloaded most
        int categoryDownloadedMostByUser = getCategoryDownloadedMostByUser(profileAccountId) ;
        
        //if the user has not made any downloads, depend on all downloads to suggest series to user
        if(producerDownloadedMostByUser == -1 || categoryDownloadedMostByUser == -1){
            
            //find producer id whose series have been downloaded most
            int producerDownloadedMostByAllUsers = getProducerDownloadedMostByAllUsers() ;
            //find category id of series that have been downloaded most
            int categoryDownloadedMostByAllUsers = getCategoryDownloadedMostByAllUsers() ;
            //use the producer and category downloaded most by all users to find recommendations for user
            //in case there are no downloads in the system, then no recommendations will be made to the user
            listOfSeriesRecommendations = getSeriesThatMatchProducerOrCategory(producerDownloadedMostByAllUsers, categoryDownloadedMostByAllUsers) ;
            
        }
        //use producer and category to find recommendations for user
        else
            listOfSeriesRecommendations = getSeriesThatMatchProducerOrCategory(producerDownloadedMostByUser, categoryDownloadedMostByUser) ;
        
        //set details for each series in the list
        listOfSeriesRecommendations = setPosterImageForSeries(listOfSeriesRecommendations) ;
        
        return listOfSeriesRecommendations ;
    }
    
    /**
     * get the id of the producer whose series the user has downloaded most
     * @param profileAccountId the user's id
     * @return the id of the producer
     */
    private int getProducerDownloadedMostByUser(int profileAccountId){
        
        SessionFactory sessionFactory = (SessionFactory)servletContext.getAttribute(OpusApplication.HIBERNATE_SESSION_FACTORY) ;
        Session session = sessionFactory.openSession() ;
        Query query = session.createQuery("SELECT sd, sd.series.videoProducer.id, count(*) FROM SeriesDownload sd JOIN FETCH sd.series WHERE sd.profileAccount.id = :profileAccountId GROUP BY sd.series.videoProducer.id ORDER BY count(*) DESC") ;
        query.setParameter("profileAccountId", profileAccountId) ;
        query.setMaxResults(1) ;
        List resultSet = query.list() ;
        session.close() ;
        //if there is no result set, it means the user hasn't made any downloads so return -1
        if(resultSet.isEmpty())
            return -1 ;
        //else return the producer id field of the first tuple since the result set has been sorted in desc order
        Object[] tupleFields = (Object[])resultSet.get(0) ;
        return (int)tupleFields[1] ;
    }
    
    /**
     * get the id of the category whose series the user has downloaded most
     * @param profileAccountId the user's id
     * @return the id of the category
     */
    private int getCategoryDownloadedMostByUser(int profileAccountId){
        
        SessionFactory sessionFactory = (SessionFactory)servletContext.getAttribute(OpusApplication.HIBERNATE_SESSION_FACTORY) ;
        Session session = sessionFactory.openSession() ;
        Query query = session.createQuery("SELECT sd, sd.series.videoCategory.id, count(*) FROM SeriesDownload sd JOIN FETCH sd.series WHERE sd.profileAccount.id = :profileAccountId GROUP BY sd.series.videoCategory.id ORDER BY count(*) DESC") ;
        query.setParameter("profileAccountId", profileAccountId) ;
        query.setMaxResults(1) ;
        List resultSet = query.list() ;
        session.close() ;
        //if there is no result set, it means the user hasn't made any downloads so return -1
        if(resultSet.isEmpty())
            return -1 ;
        //else return the category id field of the first tuple since the result set has been sorted in desc order
        Object[] tupleFields = (Object[])resultSet.get(0) ;
        return (int)tupleFields[1] ;
    }
    
    /**
     * get the id of the producer whose series has been downloaded most by all users
     * @param profileAccountId the user's id
     * @return the id of the producer
     */
    private int getProducerDownloadedMostByAllUsers(){
        
        SessionFactory sessionFactory = (SessionFactory)servletContext.getAttribute(OpusApplication.HIBERNATE_SESSION_FACTORY) ;
        Session session = sessionFactory.openSession() ;
        Query query = session.createQuery("SELECT sd, sd.series.videoProducer.id, count(*) FROM SeriesDownload sd JOIN FETCH sd.series GROUP BY sd.series.videoProducer.id ORDER BY count(*) DESC") ;
        query.setMaxResults(1) ;
        List resultSet = query.list() ;
        session.close() ;
        //if there is no result set, it means there are no downloads so return -1
        if(resultSet.isEmpty())
            return -1 ;
        //else return the producer id field of the first tuple since the result set has been sorted in desc order
        Object[] tupleFields = (Object[])resultSet.get(0) ;
        return (int)tupleFields[1] ;
    }
    
    /**
     * get the id of the category whose series has been downloaded most by all users
     * @param profileAccountId the user's id
     * @return the id of the category
     */
    private int getCategoryDownloadedMostByAllUsers(){
        
        SessionFactory sessionFactory = (SessionFactory)servletContext.getAttribute(OpusApplication.HIBERNATE_SESSION_FACTORY) ;
        Session session = sessionFactory.openSession() ;
        Query query = session.createQuery("SELECT sd, sd.series.videoCategory.id, count(*) FROM SeriesDownload sd JOIN FETCH sd.series GROUP BY sd.series.videoCategory.id ORDER BY count(*) DESC") ;
        query.setMaxResults(1) ;
        List resultSet = query.list() ;
        session.close() ;
        //if there is no result set, it means there are no downloads so return -1
        if(resultSet.isEmpty())
            return -1 ;
        //else return the category id field of the first tuple since the result set has been sorted in desc order
        Object[] tupleFields = (Object[])resultSet.get(0) ;
        return (int)tupleFields[1] ;
    }
    
    /**
     * set the base 64 representation of each series poster image
     */
    private List<Series> setPosterImageForSeries(List<Series> listOfSeries){
        
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
        
        return listOfSeries ;
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
     
    
    /**
     * get a list of series whose producer / category match the args passed
     * @param producerId the producer id
     * @param categoryId the category id 
     * @return the result set
     */
    private List<Series> getSeriesThatMatchProducerOrCategory(int producerId, int categoryId){
        
        SessionFactory sessionFactory = (SessionFactory)servletContext.getAttribute(OpusApplication.HIBERNATE_SESSION_FACTORY) ;
        Session session = sessionFactory.openSession() ;
        Query<Series> query = session.createQuery("FROM Series s JOIN FETCH s.videoProducer JOIN FETCH s.videoCategory WHERE s.videoProducer.id = :producerId OR s.videoCategory.id = :categoryId", Series.class) ;
        query.setParameter("producerId", producerId) ;
        query.setParameter("categoryId", categoryId) ;
        List<Series> listOfSeries = query.getResultList() ;
        session.close() ;
        return listOfSeries ;
    }
    
    /**
     * select all the series in the DB
     * @return the result set
     */
    private List<Series> selectAllSeriesFromDB(){
        
        SessionFactory sessionFactory = (SessionFactory)servletContext.getAttribute(OpusApplication.HIBERNATE_SESSION_FACTORY) ;
        Session session = sessionFactory.openSession() ;
        Query<Series> query = session.createQuery("FROM Series s JOIN FETCH s.videoProducer JOIN FETCH s.videoCategory", Series.class) ;
        List<Series> listOfSeries = query.getResultList() ;
        session.close() ;
        return listOfSeries ;
    }
    
}
