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
        listOfDocumentaries = setPosterImageForDocumentaries(listOfDocumentaries) ;
            
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
     * set the base 64 representation of each doc's poster image in the list
     */
    private List<Documentary> setPosterImageForDocumentaries(List<Documentary> listOfDocumentaries){
        
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
        
        return listOfDocumentaries ;
    }
    
    /**
     * get a list of doc recommendations based on a user's doc download history
     * @param profileAccountId the user's profile account id
     * @return 
     */
    public List<Documentary> getDocumentaryRecommendations(ServletContext servletContext, int profileAccountId){
        
        //init the servletContext field with the one coming from the caller
        this.servletContext = servletContext ;
        //list of documentary recommendations to return
        List<Documentary> listOfDocumentaryRecommendations ;
        //find producer id whose documentary the user has downloaded most
        int producerDownloadedMostByUser = getProducerDownloadedMostByUser(profileAccountId) ;
        //find category id of documentary the user has downloaded most
        int categoryDownloadedMostByUser = getCategoryDownloadedMostByUser(profileAccountId) ;
        
        //if the user has not made any downloads, depend on all downloads to suggest documentaries to user
        if(producerDownloadedMostByUser == -1 || categoryDownloadedMostByUser == -1){
            
            //find producer id whose documentaries have been downloaded most
            int producerDownloadedMostByAllUsers = getProducerDownloadedMostByAllUsers() ;
            //find category id of documentaries that have been downloaded most
            int categoryDownloadedMostByAllUsers = getCategoryDownloadedMostByAllUsers() ;
            //use the producer and category downloaded most by all users to find recommendations for user
            //in case there are no downloads in the system, then no recommendations will be made to the user
            listOfDocumentaryRecommendations = getDocumentariesThatMatchProducerOrCategory(producerDownloadedMostByAllUsers, categoryDownloadedMostByAllUsers) ;
            
        }
        //use producer and category to find recommendations for user
        else
            listOfDocumentaryRecommendations = getDocumentariesThatMatchProducerOrCategory(producerDownloadedMostByUser, categoryDownloadedMostByUser) ;
        
        //set details for each movie in the list
        listOfDocumentaryRecommendations = setPosterImageForDocumentaries(listOfDocumentaryRecommendations) ;
        
        return listOfDocumentaryRecommendations ;
    }
    
    /**
     * get the id of the producer whose docs the user has downloaded most
     * @param profileAccountId the user's id
     * @return the id of the producer
     */
    private int getProducerDownloadedMostByUser(int profileAccountId){
        
        SessionFactory sessionFactory = (SessionFactory)servletContext.getAttribute(OpusApplication.HIBERNATE_SESSION_FACTORY) ;
        Session session = sessionFactory.openSession() ;
        Query query = session.createQuery("SELECT dd, dd.documentary.videoProducer.id, count(*) FROM DocumentaryDownload dd JOIN FETCH dd.documentary WHERE dd.profileAccount.id = :profileAccountId GROUP BY dd.documentary.videoProducer.id ORDER BY count(*) DESC") ;
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
     * get the id of the category whose doc the user has downloaded most
     * @param profileAccountId the user's id
     * @return the id of the category
     */
    private int getCategoryDownloadedMostByUser(int profileAccountId){
        
        SessionFactory sessionFactory = (SessionFactory)servletContext.getAttribute(OpusApplication.HIBERNATE_SESSION_FACTORY) ;
        Session session = sessionFactory.openSession() ;
        Query query = session.createQuery("SELECT dd, dd.documentary.videoCategory.id, count(*) FROM DocumentaryDownload dd JOIN FETCH dd.documentary WHERE dd.profileAccount.id = :profileAccountId GROUP BY dd.documentary.videoCategory.id ORDER BY count(*) DESC") ;
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
     * get the id of the producer whose doc has been downloaded most by all users
     * @param profileAccountId the user's id
     * @return the id of the producer
     */
    private int getProducerDownloadedMostByAllUsers(){
        
        SessionFactory sessionFactory = (SessionFactory)servletContext.getAttribute(OpusApplication.HIBERNATE_SESSION_FACTORY) ;
        Session session = sessionFactory.openSession() ;
        Query query = session.createQuery("SELECT dd, dd.documentary.videoProducer.id, count(*) FROM DocumentaryDownload dd JOIN FETCH dd.documentary GROUP BY dd.documentary.videoProducer.id ORDER BY count(*) DESC") ;
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
     * get the id of the category whose doc has been downloaded most by all users
     * @param profileAccountId the user's id
     * @return the id of the category
     */
    private int getCategoryDownloadedMostByAllUsers(){
        
        SessionFactory sessionFactory = (SessionFactory)servletContext.getAttribute(OpusApplication.HIBERNATE_SESSION_FACTORY) ;
        Session session = sessionFactory.openSession() ;
        Query query = session.createQuery("SELECT dd, dd.documentary.videoCategory.id, count(*) FROM DocumentaryDownload dd JOIN FETCH dd.documentary GROUP BY dd.documentary.videoCategory.id ORDER BY count(*) DESC") ;
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
    
    /**
     * get a list of docs whose producer / category match the args passed
     * @param producerId the producer id
     * @param categoryId the category id 
     * @return the result set
     */
    private List<Documentary> getDocumentariesThatMatchProducerOrCategory(int producerId, int categoryId){
        
        SessionFactory sessionFactory = (SessionFactory)servletContext.getAttribute(OpusApplication.HIBERNATE_SESSION_FACTORY) ;
        Session session = sessionFactory.openSession() ;
        Query<Documentary> query = session.createQuery("FROM Documentary d JOIN FETCH d.videoProducer JOIN FETCH d.videoCategory WHERE d.videoProducer.id = :producerId OR d.videoCategory.id = :categoryId", Documentary.class) ;
        query.setParameter("producerId", producerId) ;
        query.setParameter("categoryId", categoryId) ;
        List<Documentary> listOfDocumentaries = query.getResultList() ;
        session.close() ;
        return listOfDocumentaries ;
    }
    
    
}
