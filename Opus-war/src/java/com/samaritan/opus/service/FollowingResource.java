/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.samaritan.opus.service;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.samaritan.opus.application.OpusApplication;
import com.samaritan.opus.model.*;
import com.samaritan.opus.response.GenericResponse;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.servlet.ServletContext;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;
import org.hibernate.HibernateException;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.Transaction;
import org.hibernate.exception.ConstraintViolationException;
import org.hibernate.query.NativeQuery;

/**
 *
 * @author electron
 */
@Path("/following")
public class FollowingResource {

    //servlet context
    @Context
    protected ServletContext servletContext ;

    //send messages to the log
    Logger logger = Logger.getLogger("com.samaritan.opus.service.FollowingResource") ;
    
    //sql error codes
    private final int DUPLICATE_ENTRY_ERROR_CODE = 1062 ;
    private final int CONSTRAINT_VIOLATION_ERROR_CODE = 1452 ;
    
    /**
     * create a new following (user following artiste)
     * @param requestBodyInJson
     * @return response
     */
    @POST
    @Consumes("application/json")
    @Produces("application/json")
    public Response createFollowing(String requestBodyInJson){
         
        Response.ResponseBuilder responseBuilder = null ;
        Following followingFromRequest = null ;
        
        try {
            //parse requestBodyInJson to Following object
            Gson gson = new GsonBuilder()
                    .excludeFieldsWithoutExposeAnnotation()
                    .create();
            followingFromRequest = gson.fromJson(requestBodyInJson, Following.class) ;
            //if the request body(following) was succesfully parsed, create the new following in DB
            if(followingFromRequest != null){
                
                //save the following in the DB and send a response code of 200
                saveFollowingInDB(followingFromRequest) ;
                responseBuilder = Response.status(Response.Status.CREATED) ;
                String prefix = getPrefixMessage(followingFromRequest) ;
                //set the response entity and send response
                responseBuilder.entity(createGenericJsonResponse(prefix + " is now following artiste with ID: " +
                        followingFromRequest.getArtiste().getId())) ;
            }
            //if the following wasn't parsed, send a 409 error
            else{
                
                responseBuilder = Response.status(Response.Status.CONFLICT) ;
                responseBuilder.entity(createGenericJsonResponse("the following object format is invalid")) ;
                
            }
            
        } 
        catch (ConstraintViolationException ex) {
            
            //get the prefix string
            String prefix = getPrefixMessage(followingFromRequest) ;
            
            switch(ex.getErrorCode()){
                
                case DUPLICATE_ENTRY_ERROR_CODE:
                    //if the error was because of a duplicate entry
                    responseBuilder = Response.status(Response.Status.CONFLICT) ;
                    responseBuilder.entity(createGenericJsonResponse(prefix + " is already following artiste with ID: " + 
                    followingFromRequest.getArtiste().getId())) ;
                    break ;
                    
                case CONSTRAINT_VIOLATION_ERROR_CODE:
                    //if the error was because of a constraint violation
                    responseBuilder = Response.status(Response.Status.CONFLICT) ;
                    responseBuilder.entity(createGenericJsonResponse(prefix + " does not have a profile account on Opus or an artiste with ID: " + 
                            followingFromRequest.getArtiste().getId() + " does not exist on Opus")) ;
                    break ;
                    
                default:
                    responseBuilder = Response.serverError() ;
                    responseBuilder.entity(createGenericJsonResponse("an unexpected error occurred. please try again")) ;
            }
            
        }
        
        return responseBuilder.build() ;
        
    }
    
    /**
     * construct a prefix string to use in log messages
     * @param followingFromRequest the following object
     * @return the prefix message
     */
    private String getPrefixMessage(Following followingFromRequest){
        
        ProfileAccount profileAccount = followingFromRequest.getProfileAccount() ; 
        return "User with ID: " + profileAccount.getId() ;
    }
    
    /**
     * delete a following from the database (a normal user stops following artiste)
     * @param profileAccount
     * @param artisteId
     * @return response
     */
    @DELETE
    @Produces("application/json")
    public Response deleteFollowing(@QueryParam("profileAccount") int profileAccount, @QueryParam("artisteId") int artisteId){
        
        Response.ResponseBuilder responseBuilder = null ;
        
        try{
            
            //delete the following from the database
            int affectedRows = deleteFollowingFromDB(profileAccount,artisteId) ;

            //if affected rows > 0, then following was deleted
            if(affectedRows > 0){

                String logString = "User with ID: " + profileAccount + " has stopped following artiste with ID: " + artisteId ;
                logger.log(Level.INFO, logString) ;
                responseBuilder = Response.ok() ;
                responseBuilder.entity(createGenericJsonResponse(logString)) ;
                
            }
            //if no following was deleted, let the user know
            else{
                //send a 409 error
                responseBuilder = Response.status(Response.Status.CONFLICT) ;
                responseBuilder.entity(createGenericJsonResponse("sorry, user with ID: " + profileAccount + " is not following artiste with ID: " + artisteId)) ;
                
            }
        }
        catch(HibernateException ex){
            
            responseBuilder = Response.serverError() ;
            responseBuilder.entity(createGenericJsonResponse("an unexpected error occurred. please try again")) ;
        }
        
        return responseBuilder.build() ;
    }
    
    
    /**
     * delete a following from the database
     * @param profileAccount
     * @param artisteId
     * @return affectedRows the number of affected rows
     * @throws HibernateException
     */
    private int deleteFollowingFromDB(int profileAccount, int artisteId) throws HibernateException{
        
        SessionFactory sessionFactory = (SessionFactory)servletContext.getAttribute(OpusApplication.HIBERNATE_SESSION_FACTORY) ;
        Session session = sessionFactory.openSession() ;
        Transaction transaction = null ;
        int affectedRows = 0 ;
        try{
            transaction = session.beginTransaction() ;
            NativeQuery query = session.createSQLQuery("DELETE FROM following WHERE profile_account =:profileAccount AND artiste =:artiste") ;
            query.setParameter("profileAccount", profileAccount) ;
            query.setParameter("artiste", artisteId) ;
            affectedRows = query.executeUpdate() ;
            transaction.commit() ;
        }
        catch(HibernateException ex){
            
            if(transaction != null) transaction.rollback() ;
            throw ex ;
        }
        finally{
            
            session.close() ;
        }
        
        return affectedRows ;
    }
    
    
    /**
     * save a following object into the following database table
     * @param followingFromRequest the following to save
     * @return void
     * @throws ConstraintViolationException when an error occurred during insertion
     */
    private void saveFollowingInDB(Following followingFromRequest) throws ConstraintViolationException{
        
        SessionFactory sessionFactory = (SessionFactory)servletContext.getAttribute(OpusApplication.HIBERNATE_SESSION_FACTORY) ;
        Session session = sessionFactory.openSession() ;
        Transaction transaction = null ;
        try{
            transaction = session.beginTransaction() ;
            session.save(followingFromRequest) ;
            transaction.commit() ;
            logger.log(Level.INFO, "A new following has been created in database") ;
        }
        catch(ConstraintViolationException ex){
            
            if(transaction != null) transaction.rollback() ;
            throw ex ;
        }
        finally{
            
            session.close() ;
        }
    }
    
    
    /**
     * create a generic json string from a GenericResponse object
     * @param message
     * @return the json object
     */
    private String createGenericJsonResponse(String message){

        GenericResponse genericResponse = new GenericResponse(message) ;
        String responseBody = new Gson().toJson(genericResponse, GenericResponse.class) ;
        return responseBody ;
    }
    
    
}
