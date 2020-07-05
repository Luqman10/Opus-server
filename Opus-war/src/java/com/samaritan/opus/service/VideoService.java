/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.samaritan.opus.service;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.samaritan.opus.model.Documentary;
import com.samaritan.opus.model.Movie;
import com.samaritan.opus.model.MusicVideo;
import com.samaritan.opus.model.Series;
import com.samaritan.opus.model.VideoRecommendation;
import java.util.List;
import javax.servlet.ServletContext;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;

/**
 * serves requests that pertain to all video content(music video,series,docs and movies)
 * @author electron
 */
@Path("/video")
public class VideoService {
    
    //servlet context
    @Context
    protected ServletContext servletContext ;
    
    /**
     * returns a response whose entity is a list of video recommendations for a user
     * @param profileAccountId the user's profile account id
     * @return response
     */
    @Path("/recommendations")
    @GET
    @Produces("application/json")
    public Response getVideoRecommendations(@QueryParam("profileAccountId") int profileAccountId){
        
        //get music video recommendations
        MusicVideoResource musicVideoResource = new MusicVideoResource() ;
        List<MusicVideo> musicVideos = musicVideoResource.getMusicVideoRecommendations(servletContext,profileAccountId) ;
        
        //get movie recommendations
        MovieResource movieResource = new MovieResource() ;
        List<Movie> movies = movieResource.getMovieRecommendations(servletContext, profileAccountId) ;
        
        //get series recommendations
        SeriesResource seriesResource = new SeriesResource() ;
        List<Series> series = seriesResource.getSeriesRecommendations(servletContext, profileAccountId) ;
        
        //get docs recommendations
        DocumentaryResource documentaryResource = new DocumentaryResource() ;
        List<Documentary> documentaries = documentaryResource.getDocumentaryRecommendations(servletContext, profileAccountId) ;
        
        //create video recommendation
        VideoRecommendation videoRecommendation = new VideoRecommendation(musicVideos,movies,series,documentaries) ;
        
        //parse videoRecommendation to json and set as response entity
        Gson gson = new GsonBuilder()
        .excludeFieldsWithoutExposeAnnotation()
        .serializeNulls()
        .create();
        String jsonString = gson.toJson(videoRecommendation) ;
        Response.ResponseBuilder responseBuilder = Response.ok() ;
        responseBuilder.entity(jsonString) ;
        
        return responseBuilder.build() ;
    }
}
