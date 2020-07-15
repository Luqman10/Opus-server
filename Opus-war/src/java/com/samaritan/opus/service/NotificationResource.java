/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.samaritan.opus.service;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.samaritan.opus.application.OpusApplication;
import com.samaritan.opus.model.Album;
import com.samaritan.opus.model.AlbumReleaseNotification;
import com.samaritan.opus.model.MusicVideo;
import com.samaritan.opus.model.MusicVideoReleaseNotification;
import com.samaritan.opus.model.Song;
import com.samaritan.opus.model.SongReleaseNotification;
import static com.samaritan.opus.service.MusicVideoResource.LOGGER;
import com.samaritan.opus.util.Base64Util;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
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

            //create list to hold each song release notification's [song]
            List<Song> listOfSongs = new ArrayList<>() ;
            
            //set userNotified of all notifications in the list to true
            for(SongReleaseNotification songReleaseNotification : listOfNotifications){
                
                //when updating any of the notifications fail, return a server error to client
                if(!updateSongReleaseNotificationUserNotifiedToTrue(songReleaseNotification)){
                    
                    responseBuilder = Response.serverError() ;
                    return responseBuilder.build() ;
                }
                
                //add SRN's song to list
                listOfSongs.add(songReleaseNotification.getSong()) ;
                
            }
                
            //log list size
            logger.log(Level.INFO, "Profile account with ID:" + profileAccountId + " has " + listOfNotifications.size() + 
                    " unread song notifications") ;
            
            
            //set song details
            setDetailsForSongs(listOfSongs) ;

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

            //create list to hold each album release notification's [album]
            List<Album> listOfAlbums = new ArrayList<>() ;
            
            //set userNotified of all notifications in the list to true
            for(AlbumReleaseNotification albumReleaseNotification : listOfNotifications){
                
                //when updating any of the notifications fail, return a server error to client
                if(!updateAlbumReleaseNotificationUserNotifiedToTrue(albumReleaseNotification)){
                    
                    responseBuilder = Response.serverError() ;
                    return responseBuilder.build() ;
                }
                
                //add ARN's album to list
                listOfAlbums.add(albumReleaseNotification.getAlbum()) ;
                
            }
                
            //log list size
            logger.log(Level.INFO, "Profile account with ID:" + profileAccountId + " has " + listOfNotifications.size() + 
                    " unread album notifications") ;

            //set details for albums
            setDetailsForAlbums(listOfAlbums) ;
            
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
     * returns a list of MusicVideoReleaseNotifications that the user has not yet received
     * @param profileAccountId the user's profile account id
     * @return response
     */
    @Path("/musicVideo")
    @GET
    @Produces("application/json")
    public Response getNewMusicVideoReleaseNotifications(@QueryParam("profileAccountId") int profileAccountId){
        
        
        Response.ResponseBuilder responseBuilder ;
        
        try{
            
            //the list of notifications user has not read yet
            List<MusicVideoReleaseNotification> listOfNotifications = getMusicVideoNotificationsFromDB(profileAccountId) ;

            //list to hold each notification's music video
            List<MusicVideo> listOfMusicVideos = new ArrayList<>() ;
            
            //set userNotified of all notifications in the list to true
            for(MusicVideoReleaseNotification musicVideoReleaseNotification : listOfNotifications){
                
                //when updating any of the notifications fail, return a server error to client
                if(!updateMusicVideoReleaseNotificationUserNotifiedToTrue(musicVideoReleaseNotification)){
                    
                    responseBuilder = Response.serverError() ;
                    return responseBuilder.build() ;
                }
                
                //add notification's music video to list
                listOfMusicVideos.add(musicVideoReleaseNotification.getMusicVideo()) ;
            }
                
            //log list size
            logger.log(Level.INFO, "Profile account with ID:" + profileAccountId + " has " + listOfNotifications.size() + 
                    " unread music video notifications") ;

            //set poster image for music videos
            setPosterImageForMusicVideos(listOfMusicVideos) ;
            
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
     * set details (song sold as single, poster image) for each song in the passed list
     * @param listOfSongs
     * @return listOfSongs
     */
    private List<Song> setDetailsForSongs(List<Song> listOfSongs){
        
        //set song sold as single and poster image for each song in the list
        for(Song song : listOfSongs){
            
            try{
                
                //if album is null, set song can be sold as single
                if(song.getAlbum() == null)
                    song.setIsSongSoldAsSingle(true) ;
                
                else
                    //if sellAlbumOnly is true, then isSongSoldAsSingle is false
                    song.setIsSongSoldAsSingle(!song.getAlbum().getSellAlbumOnly()) ;

                
                //generate the Base64 string of each song's poster image and set it to the
                //posterImage field of each song object
                String posterImageSource = song.getPosterImage() ;
                if(posterImageSource != null){
                    
                    File file = new File(posterImageSource) ;
                    String base64 = Base64Util.convertFileToBase64(file) ;
                    song.setPosterImage(base64) ;
                }
                
            
            }
            catch(IOException ex){
                
                    logger.log(Level.SEVERE, "An IO exception occured when converting the image file "
                            + "to base64") ;
            }
              
        }
        
        return listOfSongs ;
        
    }
    
    /**
     * set the details (album cover) for each album in the given list
     * @param listOfAlbums
     * @return list of albums
     */
    private List<Album> setDetailsForAlbums(List<Album> listOfAlbums){
        
        //for each album, set the album cover
        for(Album album : listOfAlbums){
            
            try{
               
                //generate the Base64 string of each album's cover and set it to the
                //albumCover field of each album object
                String albumCover = album.getAlbumCover() ;
                if(albumCover != null){

                    File file = new File(albumCover) ;
                    String base64 = Base64Util.convertFileToBase64(file) ;
                    album.setAlbumCover(base64) ;
                         
                }
            }
            catch(IOException ex){
                
                logger.log(Level.SEVERE, "An IO exception occured when converting the image file "
                            + "to base64") ;
                
            }
        }
        
        return listOfAlbums ;
    }
    
    
    /**
     * set the base 64 representation of each music video's poster image
     * @param listOfMusicVideos the list of music videos
     * @return 
     */
    private List<MusicVideo> setPosterImageForMusicVideos(List<MusicVideo> listOfMusicVideos){
        
        //for each music video, set the base64 of the poster image
        for(MusicVideo musicVideo : listOfMusicVideos){

            try{

                //generate the Base64 string of each music video's poster image and set it to the
                //posterImage field of each music video object
                String posterImage = musicVideo.getPosterImage() ;
                if(posterImage != null){

                    File file = new File(posterImage) ;
                    String base64 = Base64Util.convertFileToBase64(file) ;
                    musicVideo.setPosterImage(base64) ;

                }

            }
            catch(IOException ex){

                LOGGER.log(Level.SEVERE, "An IO exception occured when converting the image file "
                                + "to base64") ;
            }
        }
        
        return listOfMusicVideos ;
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
     * get unread music video release notifications for the given profile account id
     * @param profileAccountId profile account id
     * @return list
     */
    private List<MusicVideoReleaseNotification> getMusicVideoNotificationsFromDB(int profileAccountId){
        
        SessionFactory sessionFactory = (SessionFactory)servletContext.getAttribute(OpusApplication.HIBERNATE_SESSION_FACTORY) ;
        Session session = sessionFactory.openSession() ;
        Query<MusicVideoReleaseNotification> query = session.createQuery("FROM MusicVideoReleaseNotification mvrn JOIN FETCH mvrn.profileAccount JOIN FETCH mvrn.artiste JOIN FETCH mvrn.musicVideo WHERE mvrn.profileAccount.id=:profileAccountId AND mvrn.userNotified = false", MusicVideoReleaseNotification.class) ;
        query.setParameter("profileAccountId", profileAccountId) ;
        List<MusicVideoReleaseNotification> listOfMusicVideoReleaseNotifications = query.getResultList() ;
        session.close() ;
        return listOfMusicVideoReleaseNotifications ;
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
    
    /**
     * update the passed musicVideoReleaseNotification's userNotified to true
     * @param musicVideoReleaseNotification 
     * @return true if update was successful
     */
    private boolean updateMusicVideoReleaseNotificationUserNotifiedToTrue(MusicVideoReleaseNotification musicVideoReleaseNotification) 
            throws HibernateException{
        
        SessionFactory sessionFactory = (SessionFactory)servletContext.getAttribute(OpusApplication.HIBERNATE_SESSION_FACTORY) ;
        Session session = sessionFactory.openSession() ;
        Transaction transaction = null ;
        int success = 0 ;
        try{
            
            transaction = session.beginTransaction() ;
            Query query = session.createQuery("UPDATE MusicVideoReleaseNotification SET userNotified=true WHERE id=:id") ;
            query.setParameter("id", musicVideoReleaseNotification.getId()) ;
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
