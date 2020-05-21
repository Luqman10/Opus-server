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
import javax.ws.rs.core.Response;
import org.hibernate.Session;
import com.samaritan.opus.model.Artiste ;
import com.samaritan.opus.model.Following;
import com.samaritan.opus.util.Base64Util;
import java.io.File;
import java.io.IOException;
import java.util.logging.Level;
import javax.persistence.NoResultException;
import javax.servlet.ServletContext;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import org.hibernate.SessionFactory;
import org.hibernate.query.Query;

/**
 *service for the Artiste model
 * @author electron
 */
@Path("/artiste")
public class ArtisteResource {
    
    //send messages to the log
    static final Logger LOGGER = Logger.getLogger("com.samaritan.opus.service.ArtisteResource") ;
    
    //servlet context
    @Context
    protected ServletContext servletContext ;
    
    /**
     * get a list of artists whose first,last or stage name matches the search query
     * @param query the query term to match the first,last and stage names against
     * @param userEmail the user's email address
     * @return response to the client
     */
    @Path("/search")
    @GET
    @Produces("application/json")
    public Response getArtistes(@QueryParam("q") String query, @QueryParam("userEmail") String userEmail){
        
        //change name and user email to lowercase so the search becomes case in-sensitive
        query = query.toLowerCase() ;
        userEmail = userEmail.toLowerCase() ;
        
        //get list of artists macthing search query from DB
        List<Artiste> listOfArtistes = selectArtistesFromDBMatchingQuery(query) ;
        
        //log the number of artists
        LOGGER.log(Level.INFO, listOfArtistes.size() + " artistes found matching \'" + 
            query + "\'") ;
        
        //for every artiste, set the profile picture and set isUserFollowingArtiste
        for(Artiste artiste : listOfArtistes){
            
            try {
                
                //generate the Base64 string of each artiste's profile picture and set it to the
                //profilePicture field of each artiste object
                String profilePictureSource = artiste.getProfilePicture() ;
                if(profilePictureSource != null){
                        
                    File file = new File(profilePictureSource) ;
                    String base64 = Base64Util.convertFileToBase64(file) ;
                    artiste.setProfilePicture(base64) ; 
                }
                
                //set whether the user is following the artiste or not
                artiste.setIsUserFollowingArtiste(isUserFollowingArtiste(artiste.getId(), userEmail)) ;
                
            } 
            catch (IOException ex) {
                
                LOGGER.log(Level.SEVERE, "An IO exception occured when converting the image file "
                            + "to base64") ;
            }
        }
            
        //send status code 200
        //parse list of artistes to JSON and set JSON as entity of response
        Gson gson = new GsonBuilder()
        .excludeFieldsWithoutExposeAnnotation()
        .serializeNulls()
        .create();
        String jsonString = gson.toJson(listOfArtistes) ;
        Response.ResponseBuilder responseBuilder = Response.ok() ;
        responseBuilder.entity(jsonString) ;
        
        return responseBuilder.build() ;
        
    }
    
    
    /**
     * select all the artists from DB whose first, last or stage name matches the query
     * @param query the search query
     * @return list of artists meeting the condition
     */
    private List<Artiste> selectArtistesFromDBMatchingQuery(String queryString){
        
        
        SessionFactory sessionFactory = (SessionFactory)servletContext.getAttribute(OpusApplication.HIBERNATE_SESSION_FACTORY) ;
        Session session = sessionFactory.openSession() ;
        Query<Artiste> query = session.createQuery("FROM Artiste WHERE firstName LIKE :firstName OR lastName LIKE :lastName OR stageName LIKE :stageName", Artiste.class) ;
        queryString = "%" + queryString + "%" ;
        query.setParameter("firstName", queryString) ;
        query.setParameter("lastName", queryString) ;
        query.setParameter("stageName", queryString) ;
        List<Artiste> listOfArtistes = query.getResultList() ;
        session.close() ;
        return listOfArtistes ;
    }
    
    /**
     * checks if there's a following in which the given artiste id is followed by a user with the given id
     * @param artisteId
     * @param userEmail
     * @return true if the user is following the artiste
     */
    private boolean isUserFollowingArtiste(int artisteId, String userEmail){
        
        SessionFactory sessionFactory = (SessionFactory)servletContext.getAttribute(OpusApplication.HIBERNATE_SESSION_FACTORY) ;
        Session session = sessionFactory.openSession() ;
        
        try{
        
            Query<Following> query = session.createQuery("FROM Following f JOIN FETCH f.profileAccount JOIN FETCH f.artiste WHERE f.profileAccount.email =:email AND f.artiste.id =:id", Following.class) ;
            query.setParameter("email",userEmail) ;
            query.setParameter("id", artisteId) ;
            query.getSingleResult() ;
            session.close() ;
            return true ;
        }
        catch(NoResultException ex){
            
            session.close() ;
            return false ;
        }
    }
    
}
