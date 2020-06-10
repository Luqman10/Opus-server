/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.samaritan.opus.model;

import com.google.gson.annotations.Expose;
import java.util.List;

/**
 * represents video(music videos, movies, series and docs) recommendations
 * @author electron
 */
public class VideoRecommendation {
    
    @Expose(serialize = true, deserialize = true)
    private List<MusicVideo> musicVideos ;
    
    @Expose(serialize = true, deserialize = true)
    private List<Movie> movies ;
    
    @Expose(serialize = true, deserialize = true)
    private List<Series> series ;
    
    @Expose(serialize = true, deserialize = true)
    private List<Documentary> documentaries ;

    //constructor
    public VideoRecommendation(List<MusicVideo> musicVideos, List<Movie> movies, List<Series> series, List<Documentary> documentaries) {
        
        this.musicVideos = musicVideos;
        this.movies = movies;
        this.series = series;
        this.documentaries = documentaries;
    }

    public List<MusicVideo> getMusicVideos() {
        return musicVideos;
    }

    public void setMusicVideos(List<MusicVideo> musicVideos) {
        this.musicVideos = musicVideos;
    }

    public List<Movie> getMovies() {
        return movies;
    }

    public void setMovies(List<Movie> movies) {
        this.movies = movies;
    }

    public List<Series> getSeries() {
        return series;
    }

    public void setSeries(List<Series> series) {
        this.series = series;
    }

    public List<Documentary> getDocumentaries() {
        return documentaries;
    }

    public void setDocumentaries(List<Documentary> documentaries) {
        this.documentaries = documentaries;
    }
    
}
