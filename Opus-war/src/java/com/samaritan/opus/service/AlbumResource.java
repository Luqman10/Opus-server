/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.samaritan.opus.service;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.samaritan.opus.model.Album;
import com.samaritan.opus.util.Base64Util;
import com.samaritan.opus.application.OpusApplication ;
import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Response;
import org.hibernate.query.Query;
import javax.servlet.ServletContext;
import javax.ws.rs.core.Context;
import org.hibernate.Session;
import org.hibernate.SessionFactory;

/**
 * service for Album model
 * @author electron
 */
@Path("/album")
public class AlbumResource {

    //send messages to the log
    Logger logger = Logger.getLogger("com.samaritan.opus.service.AlbumResource") ;
    
    //servlet context
    @Context
    protected ServletContext servletContext ;
    
    /**
     * get the list of albums owned by the given artiste
     * @param artisteId the artiste whose albums list is being requested
     * @return response
     */
    @Path("/search")
    @GET
    @Produces("application/json")
    public Response getAlbums(@QueryParam("artisteId") int artisteId){
        
        //get the list of albums owned by artiste with the given ID
        List<Album> listOfAlbums = selectAlbumsFromDBOwnedByArtiste(artisteId) ;
        
        //log the number of albums artiste owns
        logger.log(Level.INFO, String.format("%s has %d albums", "Artiste with ID: " + artisteId,listOfAlbums.size())) ;
        
        //set the details for each album in the list
        listOfAlbums = setDetailsForAlbums(listOfAlbums) ;
        
        //parse list of albums to JSON and set JSON as entity of response
            Gson gson = new GsonBuilder()
            .excludeFieldsWithoutExposeAnnotation()
            .serializeNulls()
            .create();
            String jsonString = gson.toJson(listOfAlbums) ;
            
            //send response with 200 code
            Response.ResponseBuilder responseBuilder = Response.ok() ;
            responseBuilder.entity(jsonString) ;
            return responseBuilder.build() ;
    }
    
    /**
     * get a list of albums whose name matches the q query param
     * @param query the query term to match the album name against
     * @return response to the client
     */
    @Path("/search/name")
    @GET
    @Produces("application/json")
    public Response getAlbums(@QueryParam("q") String query){
        
        //change query to lowercase so the search becomes case in-sensitive
        query = query.toLowerCase() ;
        
        //get the list of albums that match query from DB
        List<Album> listOfAlbums = selectAlbumsFromDBMatchingName(query) ;
        
            
        logger.log(Level.INFO, listOfAlbums.size() + " albums found matching \'" + 
        query + "\'") ;
            
        //set the details of each album in the list
        listOfAlbums = setDetailsForAlbums(listOfAlbums) ;
            
        //if a match was found, send status code 200
        //parse list of artistes to JSON and set JSON as entity of response
        Gson gson = new GsonBuilder()
        .excludeFieldsWithoutExposeAnnotation()
        .serializeNulls()
        .create();
        String jsonString = gson.toJson(listOfAlbums) ;
        
        //send response with 200
        Response.ResponseBuilder responseBuilder ;
        responseBuilder = Response.ok() ;
        responseBuilder.entity(jsonString) ;
            
        return responseBuilder.build() ;
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
     * select all albums from the database whose name matches the query
     * @param queryString the query term to use in matching
     * @return 
     */
    private List<Album> selectAlbumsFromDBMatchingName(String queryString){
        
        SessionFactory sessionFactory = (SessionFactory)servletContext.getAttribute(OpusApplication.HIBERNATE_SESSION_FACTORY) ;
        Session session = sessionFactory.openSession() ;
        Query<Album> query = session.createQuery("FROM Album a JOIN FETCH a.artiste JOIN FETCH a.genre WHERE a.name LIKE :name", Album.class) ;
        queryString = "%" + queryString + "%" ;
        query.setParameter("name", queryString) ;
        List<Album> listOfAlbums = query.getResultList() ;
        session.close() ;
        return listOfAlbums ;
    }
    
    /**
     * select all albums from the database owned by the given artiste
     * @param artisteId the artiste who owns the albums
     * @return 
     */
    private List<Album> selectAlbumsFromDBOwnedByArtiste(int artisteId){
        
        SessionFactory sessionFactory = (SessionFactory)servletContext.getAttribute(OpusApplication.HIBERNATE_SESSION_FACTORY) ;
        Session session = sessionFactory.openSession() ;
        Query<Album> query = session.createQuery("FROM Album a JOIN FETCH a.artiste JOIN FETCH a.genre WHERE a.artiste.id=:artisteId", Album.class) ;
        query.setParameter("artisteId", artisteId) ;
        List<Album> listOfAlbums = query.getResultList() ;
        session.close() ;
        return listOfAlbums ;
    }
    
}
