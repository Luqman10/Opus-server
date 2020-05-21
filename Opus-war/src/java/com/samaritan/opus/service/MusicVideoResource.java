/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.samaritan.opus.service;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.samaritan.opus.application.OpusApplication;
import java.util.List;
import java.util.logging.Logger;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Response;
import org.hibernate.Session;

import com.samaritan.opus.model.MusicVideo ;
import com.samaritan.opus.util.Base64Util;
import java.io.File;
import java.io.IOException;
import java.util.logging.Level;
import javax.persistence.NoResultException;
import javax.servlet.ServletContext;
import javax.ws.rs.core.Context;
import org.hibernate.SessionFactory;
import org.hibernate.query.Query;

/**
 * service for MusicVideo model
 * @author electron
 */
@Path("/musicVideo")
public class MusicVideoResource {
    
    //servlet context
    @Context
    protected ServletContext servletContext ;

    //send messages to the log
    Logger logger = Logger.getLogger("com.samaritan.opus.service.MusicVideoResource") ;
    
    
    /**
     * get a list of music videos whose title matches the search query
     * @param query the query term to match the music video title against
     * @return response to the client
     */
    @Path("/search/name")
    @GET
    @Produces("application/json")
    public Response getMusicVideos(@QueryParam("q") String query){
        
        //change query to lowercase so the search becomes case in-sensitive
        query = query.toLowerCase() ;
        //get the list of music videos that match query from DB
        List<MusicVideo> listOfMusicVideos = selectMusicVideosFromDBMatchingTitle(query) ;
            
        logger.log(Level.INFO, listOfMusicVideos.size() + " music videos found matching \'" + 
            query + "\'") ;
            
            
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

                logger.log(Level.SEVERE, "An IO exception occured when converting the image file "
                                + "to base64") ;
            }
        }
            
            
        //if a match was found, send status code 200
        //parse list of music videos to JSON and set JSON as entity of response
        Gson gson = new GsonBuilder()
        .excludeFieldsWithoutExposeAnnotation()
        .serializeNulls()
        .create();
        String jsonString = gson.toJson(listOfMusicVideos) ;
        Response.ResponseBuilder responseBuilder = Response.ok() ;
        responseBuilder.entity(jsonString) ;
        
        return responseBuilder.build() ;
    }
    
    /**
     * get a list of music videos that is owned by the given artiste
     * @param artisteId the artiste whose music videos list is being requested
     * @return response
     */
    @Path("/search")
    @GET
    @Produces("application/json")
    public Response getMusicVideos(@QueryParam("artisteId") int artisteId){
        
        //get the list of music videos owned by artiste with the given ID
        List<MusicVideo> listOfMusicVideos = selectMusicVideosFromDBOwnedByArtiste(artisteId) ;
          
        //log the number of music videos artiste owns
        logger.log(Level.INFO, String.format("%s has %d music videos", "Artiste with ID: " + artisteId,listOfMusicVideos.size())) ;
        
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

                logger.log(Level.SEVERE, "An IO exception occured when converting the image file "
                                + "to base64") ;
            }
        }
         
        //if a match was found, send status code 200
        //parse list of music videos to JSON and set JSON as entity of response
        Gson gson = new GsonBuilder()
        .excludeFieldsWithoutExposeAnnotation()
        .serializeNulls()
        .create();
        String jsonString = gson.toJson(listOfMusicVideos) ;
        Response.ResponseBuilder responseBuilder = Response.ok() ;
        responseBuilder.entity(jsonString) ;
        
        return responseBuilder.build() ;
          
    }
    
    /**
     * get the sample of a music video sent to the client as a stream of bytes
     * @param id the id of the music video whose sample is requested
     * @return the music video sample file (if exists) that will be written to the client as bytes incrementally / null if the 
     * file doesn't exist
     */
    @Path("/sample")
    @GET
    @Produces("video/mp4")
    public File getMusicVideoSample(@QueryParam("id") int id){
        
            
        //get the music video with the given id from DB
        MusicVideo musicVideo = selectMusicVideoFromDB(id) ;

        //proceed if there's a music video with that id, it has a sample and the music video file exists
        if(musicVideo != null && musicVideo.getSample() != null){

            //create file for the sample
            File file = new File(musicVideo.getSample()) ;
            
            //return the file if it exists
            if(file.exists())
                return file ;

        }
        
        //return null if any of the above conditions fail
        return null ;
        
    }
    
    /**
     * select the music video from the DB with the given id
     * @param id
     * @return the music video
     */
    private MusicVideo selectMusicVideoFromDB(int id){
        
        SessionFactory sessionFactory = (SessionFactory)servletContext.getAttribute(OpusApplication.HIBERNATE_SESSION_FACTORY) ;
        Session session = sessionFactory.openSession() ;
        
        try{
        
            Query<MusicVideo> query = session.createQuery("FROM MusicVideo WHERE id =:id", MusicVideo.class) ;
            query.setParameter("id", id) ;
            MusicVideo musicVideo = query.getSingleResult() ;
            session.close() ;
            return musicVideo ;
        }
        catch(NoResultException ex){
            
            session.close() ;
            return null ;
        }
    }
    
    
    /**
     * select all music videos from the database whose title matches the query
     * @param queryString the query term to use in matching
     * @return 
     */
    private List<MusicVideo> selectMusicVideosFromDBMatchingTitle(String queryString){
        
        SessionFactory sessionFactory = (SessionFactory)servletContext.getAttribute(OpusApplication.HIBERNATE_SESSION_FACTORY) ;
        Session session = sessionFactory.openSession() ;
        Query<MusicVideo> query = session.createQuery("FROM MusicVideo mv JOIN FETCH mv.artiste JOIN FETCH mv.genre WHERE title LIKE :title", MusicVideo.class) ;
        queryString = "%" + queryString + "%" ;
        query.setParameter("title", queryString) ;
        List<MusicVideo> listOfMusicVideos = query.getResultList() ;
        session.close() ;
        return listOfMusicVideos ;
    }
    
    /**
     * select all music videos from the database owned by the given artiste
     * @param artisteId the artiste who owns the music videos
     * @return 
     */
    private List<MusicVideo> selectMusicVideosFromDBOwnedByArtiste(int artisteId){
        
        SessionFactory sessionFactory = (SessionFactory)servletContext.getAttribute(OpusApplication.HIBERNATE_SESSION_FACTORY) ;
        Session session = sessionFactory.openSession() ;
        Query<MusicVideo> query = session.createQuery("FROM MusicVideo mv JOIN FETCH mv.artiste JOIN FETCH mv.genre WHERE mv.artiste.id=:artisteId", MusicVideo.class) ;
        query.setParameter("artisteId", artisteId) ;
        List<MusicVideo> listOfMusicVideos = query.getResultList() ;
        session.close() ;
        return listOfMusicVideos ;
    }
   
}
