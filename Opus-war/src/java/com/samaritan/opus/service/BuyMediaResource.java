/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.samaritan.opus.service;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonSyntaxException;
import com.google.gson.reflect.TypeToken;
import com.samaritan.opus.application.OpusApplication;
import com.samaritan.opus.model.Album;
import com.samaritan.opus.model.AlbumDownload;
import com.samaritan.opus.model.Documentary;
import com.samaritan.opus.model.DocumentaryDownload;
import com.samaritan.opus.model.Movie;
import com.samaritan.opus.model.MovieDownload;
import com.samaritan.opus.model.MusicVideo;
import com.samaritan.opus.model.MusicVideoDownload;
import com.samaritan.opus.model.OpusMedia;
import com.samaritan.opus.model.OpusMediaDownload;
import com.samaritan.opus.model.ProfileAccount;
import com.samaritan.opus.model.Series;
import com.samaritan.opus.model.SeriesDownload;
import com.samaritan.opus.model.Song;
import com.samaritan.opus.model.SongDownload;
import com.samaritan.opus.response.GenericResponse;
import java.lang.reflect.Type;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.servlet.ServletContext;
import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.Transaction;
import org.hibernate.exception.ConstraintViolationException;
import org.hibernate.query.Query;

/**
 * this resource allows a client to request to buy songs,albums,music videos,movies,series and docs. 
 * @author electron
 */
@Path("/buy")
public class BuyMediaResource {
    
    //send messages to the log
    Logger logger = Logger.getLogger("com.samaritan.opus.service.AlbumResource") ;
    
    //servlet context
    @Context
    protected ServletContext servletContext ;
    
    //media types
    private final String SONG_MEDIA_TYPE = "song" ;
    private final String ALBUM_MEDIA_TYPE = "album" ;
    private final String MUSIC_VIDEO_MEDIA_TYPE = "music_video" ;
    private final String MOVIE_MEDIA_TYPE = "movie" ;
    private final String SERIES_MEDIA_TYPE = "series" ;
    private final String DOCS_MEDIA_TYPE = "docs" ;
    
    
    
    @Path("/{mediaType}")
    @POST
    @Consumes("application/json")
    @Produces("application/json")
    public Response buyMedia(@PathParam("mediaType") String mediaType, @QueryParam("profileAccountId") int profileAccountId,
            @QueryParam("paymentAccountId") int paymentAccountId ,String listOfMediaIdInJson){
        
        //to build response
        Response.ResponseBuilder responseBuilder ;
        
        //parse request body to list
        List<Integer> mediaIds = parseRequestBody(listOfMediaIdInJson) ;
        
        //if the list wasn't parsed successfully
        if(mediaIds == null){
            
            responseBuilder = Response.status(Response.Status.CONFLICT) ;
            responseBuilder.entity(createGenericJsonResponse("the list of media ids format is invalid")) ;
        }
        else{
            
            //calculate total cost for media
            double totalCost = calculateTotalCostForMedia(mediaType, profileAccountId, mediaIds) ;
            
            logger.log(Level.INFO, "User with ID: " + profileAccountId + " is making purchase that costs: " + totalCost) ;
            
            //if any of the media in the list is being purchased for the first time
            if(totalCost > 0){
                
                //make request to momo server for purchase and proceed if it was successful
                if(makePurchaseRequestToMomoServer(totalCost, paymentAccountId)){
                    
                    //add all the media download in the request body to the appropriate download table. this will make sure
                    //the user doesn't pay the next time he/she decides to buy the same media
                    if(addAllMediaToDownloadTable(mediaType, profileAccountId, mediaIds)){
                        //send a 200
                        responseBuilder = Response.ok() ;
                        responseBuilder.entity(createGenericJsonResponse("your purchase was successful")) ;
                    }
                    //if all the media download couldn't be added to the download table, 
                    //send the user's money back to him.
                    else{
                        makePaymentRequestToMomoServer(totalCost, paymentAccountId) ;
                        //send a 500 server error
                        responseBuilder = Response.serverError() ;
                        responseBuilder.entity(createGenericJsonResponse("an unexpected error occured. please try again.")) ;
                    }
                    
                }
                //send a 402(payment required)
                else{
                    
                    responseBuilder = Response.status(Response.Status.PAYMENT_REQUIRED) ;
                    responseBuilder.entity(createGenericJsonResponse("your purchase was unsuccessful. "
                            + "you may have insufficient funds in your mobile money wallet.")) ;
                
                }
            }
            //if total cost is 0, it means all the media in the request body has been bought before so return a 200
            else{
                
                responseBuilder = Response.ok() ;
                responseBuilder.entity(createGenericJsonResponse("your purchase was successful")) ;
            }
        }
        
        return responseBuilder.build() ;
    }
    
    /**
     * parses the string arg from json to list of integer ids
     * @param listOfMediaIdInJson the request body
     * @return list
     */
    private List<Integer> parseRequestBody(String listOfMediaIdInJson){
        
        try{
            Gson gson = new GsonBuilder()
                        .excludeFieldsWithoutExposeAnnotation()
                        .create();

            Type listType = new TypeToken<List<Integer>>(){}.getType();
            List<Integer> listOfIds = gson.fromJson(listOfMediaIdInJson, listType) ;
            return listOfIds ;
        }
        catch(JsonSyntaxException ex){
            
            return null ;
        }
    }
    
    /**
     * create a generic json object from a GenericResponse object
     * @param message
     * @return the json object
     */
    private String createGenericJsonResponse(String message){

        GenericResponse genericResponse = new GenericResponse(message) ;
        String responseBody = new Gson().toJson(genericResponse, GenericResponse.class) ;
        return responseBody ;
    }
    
    /**
     * calculate the total cost of all the media of media type with the ids in mediaIds
     * @param mediaType
     * @param mediaIds
     * @return 
     */
    private double calculateTotalCostForMedia(String mediaType, int profileAccountId, List<Integer> mediaIds){
        
        //total cost
        double totalCost = 0 ;
        
        for(Integer mediaId : mediaIds){
            
            //retrive media download from its download table
            OpusMediaDownload opusMediaDownload = retrieveMediaFromDownloadTable(mediaId, profileAccountId, mediaType) ;
            
            //if the user has bought the media before, don't charge him again
            if(opusMediaDownload != null)
                totalCost += 0 ;
            else
                totalCost += retrieveMediaPrice(mediaId, mediaType) ;
        }
        
        return totalCost ;
    }
    
    /**
     * get the media with id from the appropriate table/entity based on the media type
     * @param id
     * @param profileAccountId
     * @param mediaType
     * @return 
     */
    private OpusMediaDownload retrieveMediaFromDownloadTable(int mediaId, int profileAccountId, String mediaType){
        
        SessionFactory sessionFactory = (SessionFactory)servletContext.getAttribute(OpusApplication.HIBERNATE_SESSION_FACTORY) ;
        Session session = sessionFactory.openSession() ;
        
        //the sql query
        String sql = "" ;
        
        //set sql based on the media type
        switch(mediaType){
            
            case SONG_MEDIA_TYPE:
                sql = "FROM SongDownload sd JOIN FETCH sd.song JOIN FETCH sd.profileAccount WHERE sd.song.id=:mediaId AND sd.profileAccount.id=:profileAccountId" ;
                break ;
                
            case ALBUM_MEDIA_TYPE:
                sql = "FROM AlbumDownload ad JOIN FETCH ad.album JOIN FETCH ad.profileAccount WHERE ad.album.id=:mediaId AND ad.profileAccount.id=:profileAccountId" ;
                break ;
                
            case MUSIC_VIDEO_MEDIA_TYPE:
                sql = "FROM MusicVideoDownload mvd JOIN FETCH mvd.musicVideo JOIN FETCH mvd.profileAccount WHERE mvd.musicVideo.id=:mediaId AND mvd.profileAccount.id=:profileAccountId" ;
                break ;
                
            case MOVIE_MEDIA_TYPE:
                sql = "FROM MovieDownload md JOIN FETCH md.movie JOIN FETCH md.profileAccount WHERE md.movie.id=:mediaId AND md.profileAccount.id=:profileAccountId" ;
                break ;
                
            case SERIES_MEDIA_TYPE:
                sql = "FROM SeriesDownload sd JOIN FETCH sd.series JOIN FETCH sd.profileAccount WHERE sd.series.id=:mediaId AND sd.profileAccount.id=:profileAccountId" ;
                break ;
                
            case DOCS_MEDIA_TYPE:
                sql = "FROM DocumentaryDownload dd JOIN FETCH dd.documentary JOIN FETCH dd.profileAccount WHERE dd.documentary.id=:mediaId AND dd.profileAccount.id=:profileAccountId" ;
                break ;
                
                
        }
        
        
        Query<OpusMediaDownload> query = session.createQuery(sql, OpusMediaDownload.class) ;
        query.setParameter("mediaId", mediaId) ;
        query.setParameter("profileAccountId", profileAccountId) ;
        OpusMediaDownload opusMediaDownload = (OpusMediaDownload)(query.uniqueResult()) ;
        session.close() ;
        return opusMediaDownload ;
    }
    
    /**
     * get the 
     * @param mediaId
     * @param mediaType
     * @return 
     */
    private double retrieveMediaPrice(int mediaId, String mediaType){
        
        SessionFactory sessionFactory = (SessionFactory)servletContext.getAttribute(OpusApplication.HIBERNATE_SESSION_FACTORY) ;
        Session session = sessionFactory.openSession() ;
        
        //the sql query
        String sql = "" ;
        
        //set sql based on the media type
        switch(mediaType){
            
            case SONG_MEDIA_TYPE:
                sql = "FROM Song s WHERE s.id=:mediaId" ;
                break ;
                
            case ALBUM_MEDIA_TYPE:
                sql = "FROM Album a WHERE a.id=:mediaId" ;
                break ;
                
            case MUSIC_VIDEO_MEDIA_TYPE:
                sql = "FROM MusicVideo mv WHERE mv.id=:mediaId" ;
                break ;
                
            case MOVIE_MEDIA_TYPE:
                sql = "FROM Movie m WHERE m.id=:mediaId" ;
                break ;
                
            case SERIES_MEDIA_TYPE:
                sql = "FROM Series s WHERE s.id=:mediaId" ;
                break ;
                
            case DOCS_MEDIA_TYPE:
                sql = "FROM Documentary d WHERE d.id=:mediaId" ;
                break ;
                
                
        }
        
        
        Query<OpusMedia> query = session.createQuery(sql, OpusMedia.class) ;
        query.setParameter("mediaId", mediaId) ;
        OpusMedia opusMedia = (OpusMedia)(query.uniqueResult()) ;
        session.close() ;
        
        if(opusMedia != null)
            return opusMedia.getPrice() ;
        else
            return 0 ;
    }
    
    /**
     * make an HTTP request to the momo server to receive payment from user for the media purchase
     * @param totalCost
     * @param paymentAccountId
     * @return 
     */
    private boolean makePurchaseRequestToMomoServer(double totalCost, int paymentAccountId){
        
        return true ;
    }
    
    /**
     * insert into the appropriate [***]download table all the media with the ids in the mediaIds list
     * @param mediaType
     * @param profileAccountId
     * @param mediaIds 
     */
    private boolean addAllMediaToDownloadTable(String mediaType, int profileAccountId, List<Integer> mediaIds){
        
        try{
            for(Integer mediaId : mediaIds)  
                saveMediaDownloadToDownloadTable(mediaType, profileAccountId, mediaId) ;
        }
        catch(ConstraintViolationException ex){
            
            return false ;
        }
        
        return true ;
        
    }
    
    /**
     * insert the media download with the given id into the appropriate [***]download table
     * @param mediaType
     * @param profileAccountId
     * @param mediaId 
     */
    private void saveMediaDownloadToDownloadTable(String mediaType, int profileAccountId, int mediaId) 
            throws ConstraintViolationException{
        
        SessionFactory sessionFactory = (SessionFactory)servletContext.getAttribute(OpusApplication.HIBERNATE_SESSION_FACTORY) ;
        Session session = sessionFactory.openSession() ;
        Transaction transaction = null ;
        try{
            transaction = session.beginTransaction() ;
            
            //save the appropriate entity based on media type
            OpusMediaDownload opusMediaDownload = null ;
            ProfileAccount profileAccount = new ProfileAccount() ;
            profileAccount.setId(profileAccountId) ;
            
            switch(mediaType){

                case SONG_MEDIA_TYPE:
                    SongDownload songDownload = new SongDownload() ;
                    songDownload.setProfileAccount(profileAccount) ;
                    
                    Song song = new Song() ;
                    song.setId(mediaId) ;
                    songDownload.setSong(song) ;
                    
                    opusMediaDownload = songDownload ;
                    
                    break ;

                case ALBUM_MEDIA_TYPE:
                    AlbumDownload albumDownload = new AlbumDownload() ;
                    albumDownload.setProfileAccount(profileAccount) ;
                    
                    Album album = new Album() ;
                    album.setId(mediaId) ;
                    albumDownload.setAlbum(album) ;
                    
                    opusMediaDownload = albumDownload ;
                    break ;

                case MUSIC_VIDEO_MEDIA_TYPE:
                    MusicVideoDownload musicVideoDownload = new MusicVideoDownload() ;
                    musicVideoDownload.setProfileAccount(profileAccount) ;
                    
                    MusicVideo musicVideo = new MusicVideo() ;
                    musicVideo.setId(mediaId) ;
                    musicVideoDownload.setMusicVideo(musicVideo) ;
                    
                    opusMediaDownload = musicVideoDownload ;
                    break ;

                case MOVIE_MEDIA_TYPE:
                    MovieDownload movieDownload = new MovieDownload() ;
                    movieDownload.setProfileAccount(profileAccount) ;
                    
                    Movie movie = new Movie() ;
                    movie.setId(mediaId) ;
                    movieDownload.setMovie(movie) ;
                    
                    opusMediaDownload = movieDownload ;
                    break ;

                case SERIES_MEDIA_TYPE:
                    SeriesDownload seriesDownload = new SeriesDownload() ;
                    seriesDownload.setProfileAccount(profileAccount) ;
                    
                    Series series = new Series() ;
                    series.setId(mediaId) ;
                    seriesDownload.setSeries(series) ;
                    
                    opusMediaDownload = seriesDownload ;
                    break ;
                   
                case DOCS_MEDIA_TYPE:
                    DocumentaryDownload documentaryDownload = new DocumentaryDownload() ;
                    documentaryDownload.setProfileAccount(profileAccount) ;
                    
                    Documentary documentary = new Documentary() ;
                    documentary.setId(mediaId) ;
                    documentaryDownload.setDocumentary(documentary) ;
                    
                    opusMediaDownload = documentaryDownload ;
                    break ;


            }
            
            session.save(opusMediaDownload) ;
            transaction.commit() ;
            logger.log(Level.INFO, "User with ID: " + profileAccountId + " has bought media with ID: " + mediaId) ;
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
     * make payment to the user's momo number. this will be called in case the media the user bought couldn't be added
     * to the download table
     * @param totalCost
     * @param paymentAccountId
     */
    private void makePaymentRequestToMomoServer(double totalCost, int paymentAccountId){
        
        
    }
    
}
