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
 * represents an album
 * @author electron
 */
@Entity
@Table(name = "album", schema = "opus")
public class Album implements Comparable<Album>{
    
    //data fields
    @Expose(serialize = true, deserialize = true)
    @Id
    @Column(name = "id", nullable = false, unique = true)
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id ;
    
    @Expose(serialize = true, deserialize = true)
    @Column(name = "name", nullable = false, length = 20)
    private String name ;
    
    
    @Expose(serialize = true, deserialize = true)
    @OneToOne(cascade = CascadeType.ALL)
    @JoinColumn(name="artiste", unique = true)
    private Artiste artiste ;
    
    @Expose(serialize = true, deserialize = true)
    @OneToOne(cascade = CascadeType.ALL)
    @JoinColumn(name="genre", unique = true)
    private Genre genre ; 
    
    @Expose(serialize = true, deserialize = true)
    @Column(name = "price", nullable = false)
    private Double price ;
    
    @Expose(serialize = true, deserialize = true)
    @Column(name = "album_cover", nullable = true, length = 200)
    private String albumCover ;
    
    @Column(name = "sell_album_only", nullable = false)
    private Boolean sellAlbumOnly ;
    
    @Expose(serialize = true, deserialize = true)
    @Column(name = "number_of_songs", nullable = false)
    private Long numberOfSongs ;
    
    @Expose(serialize = true, deserialize = true)
    @Column(name = "date_released", nullable = false)
    @Temporal(javax.persistence.TemporalType.DATE)
    private Date dateReleased ;
 
    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Artiste getArtiste() {
        return artiste;
    }

    public void setArtiste(Artiste artiste) {
        this.artiste = artiste;
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

    public Boolean getSellAlbumOnly() {
        return sellAlbumOnly;
    }

    public void setSellAlbumOnly(Boolean sellAlbumOnly) {
        this.sellAlbumOnly = sellAlbumOnly;
    }
    
    
    public String getAlbumCover() {
        return albumCover;
    }

    public void setAlbumCover(String albumCover) {
        this.albumCover = albumCover;
    }
    
    public Long getNumberOfSongs() {
        return numberOfSongs;
    }

    public void setNumberOfSongs(Long numberOfSongs) {
        this.numberOfSongs = numberOfSongs;
    }
    
    public Date getDateReleased() {
        return dateReleased;
    }

    public void setDateReleased(Date dateReleased) {
        this.dateReleased = dateReleased;
    }
    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Album that = (Album) o;
        return Objects.equals(id, that.id) &&
                Objects.equals(name, that.name) &&
                Objects.equals(artiste, that.artiste) &&
                Objects.equals(genre, that.genre) &&
                Objects.equals(price, that.price) &&
                Objects.equals(sellAlbumOnly, that.sellAlbumOnly) &&
                Objects.equals(albumCover, that.albumCover) &&
                Objects.equals(numberOfSongs, that.numberOfSongs) &&
                Objects.equals(dateReleased, that.dateReleased)
                ;
    }

    @Override
    public int hashCode() {
        return Objects.hash(id,name,artiste,genre,price,sellAlbumOnly,albumCover,numberOfSongs,dateReleased) ;
    }

    @Override
    public int compareTo(Album album) {
        
        //compareTo method of Date, returns a positive value when the date param is greater than the date instance on which
        //compareTo() is called. since we want latest albums to appear before older albums, we reverse that behaviour by 
        //multiplying the returned value by -1.
        return -1 * dateReleased.compareTo(album.getDateReleased()) ;
    }
    
    
    
}
