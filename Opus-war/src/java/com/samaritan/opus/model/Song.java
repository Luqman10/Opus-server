/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.samaritan.opus.model;

import com.google.gson.annotations.Expose;
import java.util.Date;
import java.util.Objects;
import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.OneToOne;
import javax.persistence.Table;
import javax.persistence.Temporal;

/**
 * represents a song
 * @author electron
 */
@Entity
@Table(name = "song", schema = "opus")
public class Song implements Comparable<Song>{
    
    //data fields
    @Expose(serialize = true, deserialize = true)
    @Id
    @Column(name = "id", nullable = false, unique = true)
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id ;
    
    @Expose(serialize = true, deserialize = true)
    @Column(name = "title", nullable = false, length = 40)
    private String title ;
    
    @Expose(serialize = true, deserialize = true)
    @OneToOne(cascade = CascadeType.ALL)
    @JoinColumn(name="artiste", unique = true)
    private Artiste artiste ;
    
    @Expose(serialize = true, deserialize = true)
    @OneToOne(cascade = CascadeType.ALL)
    @JoinColumn(name="album", unique = true)
    private Album album ;
    
    @Expose(serialize = true, deserialize = true)
    @OneToOne(cascade = CascadeType.ALL)
    @JoinColumn(name="genre", unique = true)
    private Genre genre ; 
    
    @Expose(serialize = true, deserialize = true)
    @Column(name = "price", nullable = false)
    private Double price ;
    
    @Expose(serialize = true, deserialize = true)
    @Column(name = "poster_image", nullable = true, length = 200)
    private String posterImage ;
   
    @Column(name = "sample", nullable = false, length = 200)
    private String sample ;
    
    @Expose(serialize = true, deserialize = true)
    @Column(name = "date_released", nullable = false)
    @Temporal(javax.persistence.TemporalType.DATE)
    private Date dateReleased ;
    
    @Expose(serialize = true, deserialize = true)
    private boolean isSongSoldAsSingle ;
 
    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public Artiste getArtiste() {
        return artiste;
    }

    public void setArtiste(Artiste artiste) {
        this.artiste = artiste;
    }

    public Album getAlbum() {
        return album;
    }

    public void setAlbum(Album album) {
        this.album = album;
    }

    public Genre getGenre() {
        return genre;
    }

    public void setGenre(Genre genre) {
        this.genre = genre;
    }

    public Double getPrice() {
        return price;
    }

    public void setPrice(Double price) {
        this.price = price;
    }

    public Date getDateReleased() {
        return dateReleased;
    }

    public void setDateReleased(Date dateReleased) {
        this.dateReleased = dateReleased;
    }
    
    public boolean isSongSoldAsSingle(){
        
        return isSongSoldAsSingle ;
    }
    
    public void setIsSongSoldAsSingle(boolean isSongSoldAsSingle){
        
        this.isSongSoldAsSingle = isSongSoldAsSingle ;
    }
    
    public String getPosterImage() {
        return posterImage;
    }

    public void setPosterImage(String posterImage) {
        this.posterImage = posterImage;
    }
    
    public String getSample() {
        return sample;
    }

    public void setSample(String sample) {
        this.sample = sample;
    }
    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Song that = (Song) o;
        return Objects.equals(id, that.id) &&
                Objects.equals(title, that.title) &&
                Objects.equals(artiste, that.artiste) &&
                Objects.equals(album, that.album) &&
                Objects.equals(genre, that.genre) &&
                Objects.equals(price, that.price) &&
                Objects.equals(dateReleased, that.dateReleased) &&
                Objects.equals(isSongSoldAsSingle, that.isSongSoldAsSingle) &&
                Objects.equals(posterImage, that.posterImage) &&
                Objects.equals(sample, sample)
                ;
                
    }

    @Override
    public int hashCode() {
        return Objects.hash(id,title,artiste,album,genre,price,dateReleased,isSongSoldAsSingle,posterImage,sample) ;
    }

    @Override
    public int compareTo(Song song) {
        
        //compareTo method of Date, returns a positive value when the date param is greater than the date instance on which
        //compareTo() is called. since we want latest songs to appear before older songs, we reverse that behaviour by 
        //multiplying the returned value by -1.
        return -1 * dateReleased.compareTo(song.getDateReleased()) ;
    }
    
    
}
