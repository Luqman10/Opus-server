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
    static final Logger LOGGER = Logger.getLogger("com.samaritan.opus.service.MusicVideoResource") ;
    
    
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
            
        LOGGER.log(Level.INFO, listOfMusicVideos.size() + " music videos found matching \'" + 
            query + "\'") ;
               
        //set poster image for each music video
        listOfMusicVideos = setPosterImageForMusicVideos(listOfMusicVideos) ;
            
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
        LOGGER.log(Level.INFO, String.format("%s has %d music videos", "Artiste with ID: " + artisteId,listOfMusicVideos.size())) ;
        
        //set poster image for each music video
        listOfMusicVideos = setPosterImageForMusicVideos(listOfMusicVideos) ;
         
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
     * get all music videos
     * @return response with music video list as entity
     */
    @GET
    @Produces("application/json")
    public Response getAllMusicVideos(){
        
        //get all music videos from the DB
        List<MusicVideo> listOfMusicVideos = selectAllMusicVideosFromDB() ;
        
        //log the number of music videos
        LOGGER.log(Level.INFO, String.format("There are %d music videos on the server", listOfMusicVideos.size())) ;
        
        //set poster image for each music video
        listOfMusicVideos = setPosterImageForMusicVideos(listOfMusicVideos) ;
         
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
     * get a list of music video recommendations based on a user's music video download history
     * @param profileAccountId the user's profile account id
     * @return 
     */
    public List<MusicVideo> getMusicVideoRecommendations(ServletContext servletContext, int profileAccountId){
        
        //init the servletContext field with the one coming from the caller
        this.servletContext = servletContext ;
        //list of music video recommendations to return
        List<MusicVideo> listOfMusicVideoRecommendations ;
        //find artiste id whose music videos the user has downloaded most
        int artisteDownloadedMostByUser = getArtisteDownloadedMostByUser(profileAccountId) ;
        //find genre id of music videos the user has downloaded most
        int genreDownloadedMostByUser = getGenreDownloadedMostByUser(profileAccountId) ;
        
        //if the user has not made any downloads, depend on all downloads to suggest music videos to user
        if(artisteDownloadedMostByUser == -1 || genreDownloadedMostByUser == -1){
            
            //find artiste id whose music videos have been downloaded most
            int artisteDownloadedMostByAllUsers = getArtisteDownloadedMostByAllUsers() ;
            //find genre id of music videos that have been downloaded most
            int genreDownloadedMostByAllUsers = getGenreDownloadedMostByAllUsers() ;
            //use the artiste and genre downloaded most by all users to find recommendations for user
            //in case there are no downloads in the system, then no recommendations will be made to the user
            listOfMusicVideoRecommendations = getMusicVideosThatMatchArtisteOrGenre(artisteDownloadedMostByAllUsers, genreDownloadedMostByAllUsers) ;
            
        }
        //use artiste and genre to find recommendations for user
        else
            listOfMusicVideoRecommendations = getMusicVideosThatMatchArtisteOrGenre(artisteDownloadedMostByUser, genreDownloadedMostByUser) ;
        
        //set details for each music video in the list
        listOfMusicVideoRecommendations = setPosterImageForMusicVideos(listOfMusicVideoRecommendations) ;
        
        return listOfMusicVideoRecommendations ;
    }
    
    /**
     * get the id of the artiste whose music videos the user has downloaded most
     * @param profileAccountId the user's id
     * @return the id of the artiste
     */
    private int getArtisteDownloadedMostByUser(int profileAccountId){
        
        SessionFactory sessionFactory = (SessionFactory)servletContext.getAttribute(OpusApplication.HIBERNATE_SESSION_FACTORY) ;
        Session session = sessionFactory.openSession() ;
        Query query = session.createQuery("SELECT mvd, mvd.musicVideo.artiste.id, count(*) FROM MusicVideoDownload mvd JOIN FETCH mvd.musicVideo WHERE mvd.profileAccount.id = :profileAccountId GROUP BY mvd.musicVideo.artiste.id ORDER BY count(*) DESC") ;
        query.setParameter("profileAccountId", profileAccountId) ;
        query.setMaxResults(1) ;
        List resultSet = query.list() ;
        session.close() ;
        //if there is no result set, it means the user hasn't made any downloads so return -1
        if(resultSet.isEmpty())
            return -1 ;
        //else return the artiste id field of the first tuple since the result set has been sorted in desc order
        Object[] tupleFields = (Object[])resultSet.get(0) ;
        return (int)tupleFields[1] ;
    }
    
    /**
     * get the id of the artiste whose music videos have been downloaded most by all users
     * @return the id of the artiste
     */
    private int getArtisteDownloadedMostByAllUsers(){
        
        SessionFactory sessionFactory = (SessionFactory)servletContext.getAttribute(OpusApplication.HIBERNATE_SESSION_FACTORY) ;
        Session session = sessionFactory.openSession() ;
        Query query = session.createQuery("SELECT mvd, mvd.musicVideo.artiste.id, count(*) FROM MusicVideoDownload mvd JOIN FETCH mvd.musicVideo GROUP BY mvd.musicVideo.artiste.id ORDER BY count(*) DESC") ;
        query.setMaxResults(1) ;
        List resultSet = query.list() ;
        session.close() ;
        //if there is no result set, it means there are no downloads so return -1
        if(resultSet.isEmpty())
            return -1 ;
        //else return the artiste id field of the first tuple since the result set has been sorted in desc order
        Object[] tupleFields = (Object[])resultSet.get(0) ;
        return (int)tupleFields[1] ;
    }
    
    /**
     * get the id of the genre whose music videos the user has downloaded most
     * @param profileAccountId the user's id
     * @return the id of the genre
     */
    private int getGenreDownloadedMostByUser(int profileAccountId){
        
        SessionFactory sessionFactory = (SessionFactory)servletContext.getAttribute(OpusApplication.HIBERNATE_SESSION_FACTORY) ;
        Session session = sessionFactory.openSession() ;
        Query query = session.createQuery("SELECT mvd, mvd.musicVideo.genre.id, count(*) FROM MusicVideoDownload mvd JOIN FETCH mvd.musicVideo WHERE mvd.profileAccount.id = :profileAccountId GROUP BY mvd.musicVideo.genre.id ORDER BY count(*) DESC") ;
        query.setParameter("profileAccountId", profileAccountId) ;
        query.setMaxResults(1) ;
        List resultSet = query.list() ;
        session.close() ;
        //if there is no result set, it means the user hasn't made any downloads so return -1
        if(resultSet.isEmpty())
            return -1 ;
        //else return the genre id field of the first tuple since the result set has been sorted in desc order
        Object[] tupleFields = (Object[])resultSet.get(0) ;
        return (int)tupleFields[1] ;
    }
    
    /**
     * get the id of the genre whose music videos have been downloaded most by all users
     * @return the id of the genre
     */
    private int getGenreDownloadedMostByAllUsers(){
        
        SessionFactory sessionFactory = (SessionFactory)servletContext.getAttribute(OpusApplication.HIBERNATE_SESSION_FACTORY) ;
        Session session = sessionFactory.openSession() ;
        Query query = session.createQuery("SELECT mvd, mvd.musicVideo.genre.id, count(*) FROM MusicVideoDownload mvd JOIN FETCH mvd.musicVideo GROUP BY mvd.musicVideo.genre.id ORDER BY count(*) DESC") ;
        query.setMaxResults(1) ;
        List resultSet = query.list() ;
        session.close() ;
        //if there is no result set, it means there are no downloads so return -1
        if(resultSet.isEmpty())
            return -1 ;
        //else return the genre id field of the first tuple since the result set has been sorted in desc order
        Object[] tupleFields = (Object[])resultSet.get(0) ;
        return (int)tupleFields[1] ;
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
    
    /**
     * get a list of music videos whose artiste / genre match the args passed
     * @param artisteId the artiste id
     * @param genreId the genre id 
     * @return the result set
     */
    private List<MusicVideo> getMusicVideosThatMatchArtisteOrGenre(int artisteId, int genreId){
        
        SessionFactory sessionFactory = (SessionFactory)servletContext.getAttribute(OpusApplication.HIBERNATE_SESSION_FACTORY) ;
        Session session = sessionFactory.openSession() ;
        Query<MusicVideo> query = session.createQuery("FROM MusicVideo mv JOIN FETCH mv.artiste JOIN FETCH mv.genre WHERE mv.artiste.id = :artisteId OR mv.genre.id = :genreId", MusicVideo.class) ;
        query.setParameter("artisteId", artisteId) ;
        query.setParameter("genreId", genreId) ;
        List<MusicVideo> listOfMusicVideos = query.getResultList() ;
        session.close() ;
        return listOfMusicVideos ;
    }
    
    /**
     * get all the music videos in the DB
     * @return the result set
     */
    private List<MusicVideo> selectAllMusicVideosFromDB(){
        
        SessionFactory sessionFactory = (SessionFactory)servletContext.getAttribute(OpusApplication.HIBERNATE_SESSION_FACTORY) ;
        Session session = sessionFactory.openSession() ;
        Query<MusicVideo> query = session.createQuery("FROM MusicVideo mv JOIN FETCH mv.artiste JOIN FETCH mv.genre", MusicVideo.class) ;
        List<MusicVideo> listOfMusicVideos = query.getResultList() ;
        session.close() ;
        return listOfMusicVideos ;
    }
   
}
