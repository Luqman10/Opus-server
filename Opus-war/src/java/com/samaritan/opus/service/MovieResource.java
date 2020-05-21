/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.samaritan.opus.service;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.samaritan.opus.application.OpusApplication;
import com.samaritan.opus.model.Movie;
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
 * service for Movie model
 * @author electron
 */
@Path("/movie")
public class MovieResource {
    
    //servlet context
    @Context
    protected ServletContext servletContext ;

    //send messages to the log
    Logger logger = Logger.getLogger("com.samaritan.opus.service.MovieResource") ;
    
    
    /**
     * get a list of movies whose title matches the search query
     * @param query the query term to match the movie title against
     * @return response to the client
     */
    @Path("/search/name")
    @GET
    @Produces("application/json")
    public Response getMovies(@QueryParam("q") String query){
        
        //change query to lowercase so the search becomes case in-sensitive
        query = query.toLowerCase() ;
        
        //get the list of movies that match query from DB
        List<Movie> listOfMovies = selectMoviesFromDBMatchingTitle(query) ;
             
        logger.log(Level.INFO, listOfMovies.size() + " movies found matching \'" + 
                    query + "\'") ;
            
        //set the base64 representation of each movie's poster image
        for(Movie movie: listOfMovies){
                
            try{
                    
                //generate the Base64 string of each movie's poster image and set it to the
                //posterImage field of each movie object
                String posterImage = movie.getPosterImage() ;
                if(posterImage != null){

                    File file = new File(posterImage) ;
                    String base64 = Base64Util.convertFileToBase64(file) ;
                    movie.setPosterImage(base64) ;
                         
                }
            }
            catch(IOException ex){
                    
                logger.log(Level.SEVERE, "An IO exception occured when converting the image file "
                            + "to base64") ;
            }
        }
            
        //send status code 200
        //parse list of movies to JSON and set JSON as entity of response
        Gson gson = new GsonBuilder()
        .excludeFieldsWithoutExposeAnnotation()
        .serializeNulls()
        .create();
        String jsonString = gson.toJson(listOfMovies) ;
        Response.ResponseBuilder responseBuilder = Response.ok() ;
        responseBuilder.entity(jsonString) ;
            
        return responseBuilder.build() ;
    }
    
    /**
     * get the sample of a movie sent to the client as a stream of bytes
     * @param id the id of the movie whose sample is requested
     * @return the movie sample file (if exists) that will be written to the client as bytes incrementally / null if the 
     * file doesn't exist
     */
    @Path("/sample")
    @GET
    @Produces("video/mp4")
    public File getMovieSample(@QueryParam("id") int id){
        
            
        //get the movie with the given id from DB
        Movie movie = selectMovieFromDB(id) ;

        //proceed if there's a movie with that id, it has a sample and the movie file exists
        if(movie != null && movie.getSample() != null){

            //create file for the sample
            File file = new File(movie.getSample()) ;
            
            //return the file if it exists
            if(file.exists())
                return file ;

        }
        
        //return null if any of the above conditions fail
        return null ;
        
    }
    
    /**
     * select the movie from the DB with the given id
     * @param id
     * @return the movie
     */
    private Movie selectMovieFromDB(int id){
        
        SessionFactory sessionFactory = (SessionFactory)servletContext.getAttribute(OpusApplication.HIBERNATE_SESSION_FACTORY) ;
        Session session = sessionFactory.openSession() ;
        
        try{
        
            Query<Movie> query = session.createQuery("FROM Movie WHERE id =:id", Movie.class) ;
            query.setParameter("id", id) ;
            Movie movie = query.getSingleResult() ;
            session.close() ;
            return movie ;
        }
        catch(NoResultException ex){
            
            session.close() ;
            return null ;
        }
    }
    
    /**
     * select all movies from the database whose title matches the query
     * @param queryString the query term to use in matching
     * @return 
     */
    private List<Movie> selectMoviesFromDBMatchingTitle(String queryString){
        
        SessionFactory sessionFactory = (SessionFactory)servletContext.getAttribute(OpusApplication.HIBERNATE_SESSION_FACTORY) ;
        Session session = sessionFactory.openSession() ;
        Query<Movie> query = session.createQuery("FROM Movie m JOIN FETCH m.videoProducer JOIN FETCH m.videoCategory WHERE title LIKE :title", Movie.class) ;
        queryString = "%" + queryString + "%" ;
        query.setParameter("title", queryString) ;
        List<Movie> listOfMovies = query.getResultList() ;
        session.close() ;
        return listOfMovies ;
    }
    
}
