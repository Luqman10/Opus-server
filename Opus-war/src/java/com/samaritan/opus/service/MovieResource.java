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
        listOfMovies = setPosterImageForMovies(listOfMovies) ;
            
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
     * set the base 64 representation of each movie in the list
     */
    private List<Movie> setPosterImageForMovies(List<Movie> listOfMovies){
        
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
        
        return listOfMovies ;
    }
    
    /**
     * get a list of movie recommendations based on a user's movie download history
     * @param profileAccountId the user's profile account id
     * @return 
     */
    public List<Movie> getMovieRecommendations(ServletContext servletContext, int profileAccountId){
        
        //init the servletContext field with the one coming from the caller
        this.servletContext = servletContext ;
        //list of movie recommendations to return
        List<Movie> listOfMovieRecommendations ;
        //find producer id whose movies the user has downloaded most
        int producerDownloadedMostByUser = getProducerDownloadedMostByUser(profileAccountId) ;
        //find category id of movies the user has downloaded most
        int categoryDownloadedMostByUser = getCategoryDownloadedMostByUser(profileAccountId) ;
        
        //if the user has not made any downloads, depend on all downloads to suggest movies to user
        if(producerDownloadedMostByUser == -1 || categoryDownloadedMostByUser == -1){
            
            //find producer id whose movies have been downloaded most
            int producerDownloadedMostByAllUsers = getProducerDownloadedMostByAllUsers() ;
            //find category id of movies that have been downloaded most
            int categoryDownloadedMostByAllUsers = getCategoryDownloadedMostByAllUsers() ;
            //use the producer and category downloaded most by all users to find recommendations for user
            //in case there are no downloads in the system, then no recommendations will be made to the user
            listOfMovieRecommendations = getMoviesThatMatchProducerOrCategory(producerDownloadedMostByAllUsers, categoryDownloadedMostByAllUsers) ;
            
        }
        //use producer and category to find recommendations for user
        else
            listOfMovieRecommendations = getMoviesThatMatchProducerOrCategory(producerDownloadedMostByUser, categoryDownloadedMostByUser) ;
        
        //set details for each movie in the list
        listOfMovieRecommendations = setPosterImageForMovies(listOfMovieRecommendations) ;
        
        return listOfMovieRecommendations ;
    }
    
    /**
     * get the id of the producer whose movies the user has downloaded most
     * @param profileAccountId the user's id
     * @return the id of the producer
     */
    private int getProducerDownloadedMostByUser(int profileAccountId){
        
        SessionFactory sessionFactory = (SessionFactory)servletContext.getAttribute(OpusApplication.HIBERNATE_SESSION_FACTORY) ;
        Session session = sessionFactory.openSession() ;
        Query query = session.createQuery("SELECT md, md.movie.videoProducer.id, count(*) FROM MovieDownload md JOIN FETCH md.movie WHERE md.profileAccount.id = :profileAccountId GROUP BY md.movie.videoProducer.id ORDER BY count(*) DESC") ;
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
     * get the id of the producer whose movies has been downloaded most by all users
     * @param profileAccountId the user's id
     * @return the id of the producer
     */
    private int getProducerDownloadedMostByAllUsers(){
        
        SessionFactory sessionFactory = (SessionFactory)servletContext.getAttribute(OpusApplication.HIBERNATE_SESSION_FACTORY) ;
        Session session = sessionFactory.openSession() ;
        Query query = session.createQuery("SELECT md, md.movie.videoProducer.id, count(*) FROM MovieDownload md JOIN FETCH md.movie GROUP BY md.movie.videoProducer.id ORDER BY count(*) DESC") ;
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
     * get the id of the category whose movies the user has downloaded most
     * @param profileAccountId the user's id
     * @return the id of the category
     */
    private int getCategoryDownloadedMostByUser(int profileAccountId){
        
        SessionFactory sessionFactory = (SessionFactory)servletContext.getAttribute(OpusApplication.HIBERNATE_SESSION_FACTORY) ;
        Session session = sessionFactory.openSession() ;
        Query query = session.createQuery("SELECT md, md.movie.videoCategory.id, count(*) FROM MovieDownload md JOIN FETCH md.movie WHERE md.profileAccount.id = :profileAccountId GROUP BY md.movie.videoCategory.id ORDER BY count(*) DESC") ;
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
     * get the id of the category whose movies has been downloaded most by all users
     * @param profileAccountId the user's id
     * @return the id of the category
     */
    private int getCategoryDownloadedMostByAllUsers(){
        
        SessionFactory sessionFactory = (SessionFactory)servletContext.getAttribute(OpusApplication.HIBERNATE_SESSION_FACTORY) ;
        Session session = sessionFactory.openSession() ;
        Query query = session.createQuery("SELECT md, md.movie.videoCategory.id, count(*) FROM MovieDownload md JOIN FETCH md.movie GROUP BY md.movie.videoCategory.id ORDER BY count(*) DESC") ;
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
    
    /**
     * get a list of movies whose producer / category match the args passed
     * @param producerId the producer id
     * @param categoryId the category id 
     * @return the result set
     */
    private List<Movie> getMoviesThatMatchProducerOrCategory(int producerId, int categoryId){
        
        SessionFactory sessionFactory = (SessionFactory)servletContext.getAttribute(OpusApplication.HIBERNATE_SESSION_FACTORY) ;
        Session session = sessionFactory.openSession() ;
        Query<Movie> query = session.createQuery("FROM Movie m JOIN FETCH m.videoProducer JOIN FETCH m.videoCategory WHERE m.videoProducer.id = :producerId OR m.videoCategory.id = :categoryId", Movie.class) ;
        query.setParameter("producerId", producerId) ;
        query.setParameter("categoryId", categoryId) ;
        List<Movie> listOfMovies = query.getResultList() ;
        session.close() ;
        return listOfMovies ;
    }
    
}
