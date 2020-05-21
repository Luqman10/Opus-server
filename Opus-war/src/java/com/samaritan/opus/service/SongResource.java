/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.samaritan.opus.service;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.samaritan.opus.application.OpusApplication;
import com.samaritan.opus.model.Song;
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
 * service for Song model
 * @author electron
 */
@Path("/song")
public class SongResource {
    
    
    //servlet context
    @Context
    protected ServletContext servletContext ;

    //send messages to the log
    Logger logger = Logger.getLogger("com.samaritan.opus.service.SongResource") ;
    
    /**
     * get a list of songs that is owned by the given artiste
     * @param artisteId the artiste whose songs list is being requested
     * @return response
     */
    @Path("/search/artiste")
    @GET
    @Produces("application/json")
    public Response getSongsOwnedByArtiste(@QueryParam("artisteId") int artisteId){
        
        
        //get the list of songs owned by artiste with the given ID
        List<Song> listOfSongs = selectSongsFromDBOwnedByArtiste(artisteId) ;
          
        //log the number of songs artiste owns
        logger.log(Level.INFO, String.format("%s has %d songs", "Artiste with ID: " + artisteId,listOfSongs.size())) ;
        
        //set the details for songs in the list
        listOfSongs = setDetailsForSongs(listOfSongs) ;
        
        //parse list of songs to JSON and set JSON as entity of response
        Gson gson = new GsonBuilder()
        .excludeFieldsWithoutExposeAnnotation()
        .serializeNulls()
        .create();
        String jsonString = gson.toJson(listOfSongs) ;
            
        //send response with 200 code
        Response.ResponseBuilder responseBuilder = Response.ok() ;
        responseBuilder.entity(jsonString) ;
        return responseBuilder.build() ;
    }
    
    /**
     * get a list of songs that belong to a particular album
     * @param albumId the id of the album whose songs are being requested
     * @return response
     */
    @Path("/search/album")
    @GET
    @Produces("application/json")
    public Response getSongsBelongingToAlbum(@QueryParam("albumId") int albumId){
        
        //get the list of songs belonging to the album
        List<Song> listOfSongs = selectSongsFromDBBelongingToAlbum(albumId) ;
        
        //log the number of songs in the album
        logger.log(Level.INFO, String.format("%s has %d songs", "Album with ID: " + albumId,listOfSongs.size())) ;
        
        //set the details for songs in the list
        listOfSongs = setDetailsForSongs(listOfSongs) ;
        
        //parse list of songs to JSON and set JSON as entity of response
        Gson gson = new GsonBuilder()
        .excludeFieldsWithoutExposeAnnotation()
        .serializeNulls()
        .create();
        String jsonString = gson.toJson(listOfSongs) ;
            
        //send response with 200 code
        Response.ResponseBuilder responseBuilder = Response.ok() ;
        responseBuilder.entity(jsonString) ;
        return responseBuilder.build() ;
    }
    
    /**
     * get a list of songs whose title match the given query string
     * @param query the query string
     * @return response
     */
    @Path("/search")
    @GET
    @Produces("application/json")
    public Response getSongs(@QueryParam("q") String query){
        
        //change query to lowercase to make search case in-sensitive
        query = query.toLowerCase() ;
        
        //get the list of songs that match the query
        List<Song> listOfSongs = selectSongsFromDBMatchingTitle(query) ;
        
        //log the number of songs that match query
        logger.log(Level.INFO, String.format("%s matches %d songs", query,listOfSongs.size())) ;
        
        //set details for each song in the list
        listOfSongs = setDetailsForSongs(listOfSongs) ;
        
        //parse list of songs to JSON and set JSON as entity of response
        Gson gson = new GsonBuilder()
        .excludeFieldsWithoutExposeAnnotation()
        .serializeNulls()
        .create();
        String jsonString = gson.toJson(listOfSongs) ;
            
        //send response with 200 code
        Response.ResponseBuilder responseBuilder = Response.ok() ;
        responseBuilder.entity(jsonString) ;
        return responseBuilder.build() ;
        
    }
    
    /**
     * get the sample of a song sent to the client as a stream of bytes
     * @param id the id of the song whose sample is requested
     * @return the song sample file (if exists) that will be written to the client as bytes incrementally / null if the 
     * file doesn't exist
     */
    @Path("/sample")
    @GET
    @Produces("audio/mpeg")
    public File getSongSample(@QueryParam("id") int id){
        
            
        //get the song with the given id from DB
        Song song = selectSongFromDB(id) ;

        //proceed if there's a song with that id, it has a sample and the song file exixts
        if(song != null && song.getSample() != null){

            //create file for the sample
            File file = new File(song.getSample()) ;
            
            //return the file if it exists
            if(file.exists())
                return file ;

        }
        
        //return null if any of the above conditions fail
        return null ;
        
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
     * select songs from the database whose title matches the query
     * @param queryString the query term to use in matching
     * @return 
     */
    private List<Song> selectSongsFromDBMatchingTitle(String queryString){
        
        SessionFactory sessionFactory = (SessionFactory)servletContext.getAttribute(OpusApplication.HIBERNATE_SESSION_FACTORY) ;
        Session session = sessionFactory.openSession() ;
        Query<Song> query = session.createQuery("FROM Song s JOIN FETCH s.artiste JOIN FETCH s.genre LEFT JOIN FETCH s.album WHERE s.title LIKE :title", Song.class) ;
        queryString = "%" + queryString + "%" ;
        query.setParameter("title", queryString) ;
        List<Song> listOfSongs = query.getResultList() ;
        session.close() ;
        return listOfSongs ;
    }
    
    /**
     * select all songs from the database owned by the given artiste
     * @param artisteId the artiste who owns the songs
     * @return the list of songs
     */
    private List<Song> selectSongsFromDBOwnedByArtiste(int artisteId){
        
        SessionFactory sessionFactory = (SessionFactory)servletContext.getAttribute(OpusApplication.HIBERNATE_SESSION_FACTORY) ;
        Session session = sessionFactory.openSession() ;
        Query<Song> query = session.createQuery("FROM Song s JOIN FETCH s.artiste JOIN FETCH s.genre LEFT JOIN FETCH s.album WHERE s.artiste.id=:artisteId", Song.class) ;
        query.setParameter("artisteId", artisteId) ;
        List<Song> listOfSongs = query.getResultList() ;
        session.close() ;
        return listOfSongs ;
    }
    
    /**
     * select all songs from the database that belong to the album with the given id
     * @param albumId the id of the album whose songs we want
     * @return the list of songs
     */
    private List<Song> selectSongsFromDBBelongingToAlbum(int albumId){
        
        SessionFactory sessionFactory = (SessionFactory)servletContext.getAttribute(OpusApplication.HIBERNATE_SESSION_FACTORY) ;
        Session session = sessionFactory.openSession() ;
        Query<Song> query = session.createQuery("FROM Song s JOIN FETCH s.artiste JOIN FETCH s.genre LEFT JOIN FETCH s.album WHERE s.album.id=:albumId", Song.class) ;
        query.setParameter("albumId", albumId) ;
        List<Song> listOfSongs = query.getResultList() ;
        session.close() ;
        return listOfSongs ;
    }
    
    /**
     * select the song from the DB with the given id
     * @param id
     * @return the song
     */
    private Song selectSongFromDB(int id){
        
        SessionFactory sessionFactory = (SessionFactory)servletContext.getAttribute(OpusApplication.HIBERNATE_SESSION_FACTORY) ;
        Session session = sessionFactory.openSession() ;
        
        try{
        
            Query<Song> query = session.createQuery("FROM Song WHERE id =:id", Song.class) ;
            query.setParameter("id", id) ;
            Song song = query.getSingleResult() ;
            session.close() ;
            return song ;
        }
        catch(NoResultException ex){
            
            session.close() ;
            return null ;
        }
    }
    
}
