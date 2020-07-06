/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.samaritan.opus.service;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.samaritan.opus.application.OpusApplication;
import com.samaritan.opus.model.AlbumReleaseNotification;
import com.samaritan.opus.model.SongReleaseNotification;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.servlet.ServletContext;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;
import org.hibernate.HibernateException;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.Transaction;
import org.hibernate.query.Query;

/**
 * returns a list of new song,album and music video notifications(that have never been sent to user) to the client.
 * @author electron
 */
@Path("/notification")
public class NotificationResource {
    
    //servlet context
    @Context
    protected ServletContext servletContext ;

    //send messages to the log
    Logger logger = Logger.getLogger("com.samaritan.opus.service.NotificationResource") ;
    
    
    /**
     * returns a list of SongReleaseNotifications that the user has not yet received
     * @param profileAccountId the user's profile account id
     * @return response
     */
    @Path("/song")
    @GET
    @Produces("application/json")
    public Response getNewSongReleaseNotifications(@QueryParam("profileAccountId") int profileAccountId){
        
        
        Response.ResponseBuilder responseBuilder ;
        
        try{
            
            //the list of notifications user has not read yet
            List<SongReleaseNotification> listOfNotifications = getSongNotificationsFromDB(profileAccountId) ;

            //set userNotified of all notifications in the list to true
            for(SongReleaseNotification songReleaseNotification : listOfNotifications){
                
                //when updating any of the notifications fail, return a server error to client
                if(!updateSongReleaseNotificationUserNotifiedToTrue(songReleaseNotification)){
                    
                    responseBuilder = Response.serverError() ;
                    return responseBuilder.build() ;
                }
                
            }
                
            //log list size
            logger.log(Level.INFO, "Profile account with ID:" + profileAccountId + " has " + listOfNotifications.size() + 
                    " unread song notifications") ;

            Gson gson = new GsonBuilder()
            .excludeFieldsWithoutExposeAnnotation()
            .serializeNulls()
            .create();
            String jsonString = gson.toJson(listOfNotifications) ;
            responseBuilder = Response.ok() ;
            responseBuilder.entity(jsonString) ;
            return responseBuilder.build() ;
        }
        catch(HibernateException ex){
            
            responseBuilder = Response.serverError() ;
            return responseBuilder.build() ;
        }
        
    }
    
    /**
     * returns a list of AlbumReleaseNotifications that the user has not yet received
     * @param profileAccountId the user's profile account id
     * @return response
     */
    @Path("/album")
    @GET
    @Produces("application/json")
    public Response getNewAlbumReleaseNotifications(@QueryParam("profileAccountId") int profileAccountId){
        
        
        Response.ResponseBuilder responseBuilder ;
        
        try{
            
            //the list of notifications user has not read yet
            List<AlbumReleaseNotification> listOfNotifications = getAlbumNotificationsFromDB(profileAccountId) ;

            //set userNotified of all notifications in the list to true
            for(AlbumReleaseNotification albumReleaseNotification : listOfNotifications){
                
                //when updating any of the notifications fail, return a server error to client
                if(!updateAlbumReleaseNotificationUserNotifiedToTrue(albumReleaseNotification)){
                    
                    responseBuilder = Response.serverError() ;
                    return responseBuilder.build() ;
                }
                
            }
                
            //log list size
            logger.log(Level.INFO, "Profile account with ID:" + profileAccountId + " has " + listOfNotifications.size() + 
                    " unread album notifications") ;

            Gson gson = new GsonBuilder()
            .excludeFieldsWithoutExposeAnnotation()
            .serializeNulls()
            .create();
            String jsonString = gson.toJson(listOfNotifications) ;
            responseBuilder = Response.ok() ;
            responseBuilder.entity(jsonString) ;
            return responseBuilder.build() ;
        }
        catch(HibernateException ex){
            
            responseBuilder = Response.serverError() ;
            return responseBuilder.build() ;
        }
        
    }
    
    /**
     * get unread song release notifications for the given profile account id
     * @param profileAccountId profile account id
     * @return list
     */
    private List<SongReleaseNotification> getSongNotificationsFromDB(int profileAccountId){
        
        SessionFactory sessionFactory = (SessionFactory)servletContext.getAttribute(OpusApplication.HIBERNATE_SESSION_FACTORY) ;
        Session session = sessionFactory.openSession() ;
        Query<SongReleaseNotification> query = session.createQuery("FROM SongReleaseNotification srn JOIN FETCH srn.profileAccount JOIN FETCH srn.artiste JOIN FETCH srn.song WHERE srn.profileAccount.id=:profileAccountId AND srn.userNotified = false", SongReleaseNotification.class) ;
        query.setParameter("profileAccountId", profileAccountId) ;
        List<SongReleaseNotification> listOfSongReleaseNotifications = query.getResultList() ;
        session.close() ;
        return listOfSongReleaseNotifications ;
    }
    
    
    /**
     * get unread album release notifications for the given profile account id
     * @param profileAccountId profile account id
     * @return list
     */
    private List<AlbumReleaseNotification> getAlbumNotificationsFromDB(int profileAccountId){
        
        SessionFactory sessionFactory = (SessionFactory)servletContext.getAttribute(OpusApplication.HIBERNATE_SESSION_FACTORY) ;
        Session session = sessionFactory.openSession() ;
        Query<AlbumReleaseNotification> query = session.createQuery("FROM AlbumReleaseNotification arn JOIN FETCH arn.profileAccount JOIN FETCH arn.artiste JOIN FETCH arn.album WHERE arn.profileAccount.id=:profileAccountId AND arn.userNotified = false", AlbumReleaseNotification.class) ;
        query.setParameter("profileAccountId", profileAccountId) ;
        List<AlbumReleaseNotification> listOfAlbumReleaseNotifications = query.getResultList() ;
        session.close() ;
        return listOfAlbumReleaseNotifications ;
    }
    
    
    /**
     * update the passed songReleaseNotification's userNotified to true
     * @param songReleaseNotification 
     * @return true if update was successful
     */
    private boolean updateSongReleaseNotificationUserNotifiedToTrue(SongReleaseNotification songReleaseNotification) 
            throws HibernateException{
        
        SessionFactory sessionFactory = (SessionFactory)servletContext.getAttribute(OpusApplication.HIBERNATE_SESSION_FACTORY) ;
        Session session = sessionFactory.openSession() ;
        Transaction transaction = null ;
        int success = 0 ;
        try{
            
            transaction = session.beginTransaction() ;
            Query query = session.createQuery("UPDATE SongReleaseNotification SET userNotified=true WHERE id=:id") ;
            query.setParameter("id", songReleaseNotification.getId()) ;
            success = query.executeUpdate() ;
            transaction.commit() ;
        }
        catch(HibernateException ex){
            
            if(transaction != null) transaction.rollback() ;
            throw ex ;
        }
        finally{
            
            session.close() ;
        }
        
        return success > 0 ;
    }
    
    /**
     * update the passed albumReleaseNotification's userNotified to true
     * @param albumReleaseNotification 
     * @return true if update was successful
     */
    private boolean updateAlbumReleaseNotificationUserNotifiedToTrue(AlbumReleaseNotification albumReleaseNotification) 
            throws HibernateException{
        
        SessionFactory sessionFactory = (SessionFactory)servletContext.getAttribute(OpusApplication.HIBERNATE_SESSION_FACTORY) ;
        Session session = sessionFactory.openSession() ;
        Transaction transaction = null ;
        int success = 0 ;
        try{
            
            transaction = session.beginTransaction() ;
            Query query = session.createQuery("UPDATE AlbumReleaseNotification SET userNotified=true WHERE id=:id") ;
            query.setParameter("id", albumReleaseNotification.getId()) ;
            success = query.executeUpdate() ;
            transaction.commit() ;
        }
        catch(HibernateException ex){
            
            if(transaction != null) transaction.rollback() ;
            throw ex ;
        }
        finally{
            
            session.close() ;
        }
        
        return success > 0 ;
    }
}
