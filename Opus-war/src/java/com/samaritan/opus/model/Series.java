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
 * represents a series
 * @author electron
 */
@Entity
@Table(name = "series", schema = "opus")
public class Series extends OpusMedia implements Comparable<Series>{
    
    //data fields
    @Expose(serialize = true, deserialize = true)
    @Id
    @Column(name = "id", nullable = false, unique = true)
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id ;
    
    @Expose(serialize = true, deserialize = true)
    @Column(name = "title", nullable = false, length = 30)
    private String title ;
    
    @Expose(serialize = true, deserialize = true)
    @OneToOne(cascade = CascadeType.ALL)
    @JoinColumn(name="producer", unique = true)
    private VideoProducer videoProducer ;
    
    @Expose(serialize = true, deserialize = true)
    @OneToOne(cascade = CascadeType.ALL)
    @JoinColumn(name="category", unique = true)
    private VideoCategory videoCategory ;
    
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
    @Column(name = "description", nullable = true, length = 200)
    private String description ;
    
    
    //constructors
    public Series(){}
    
    public Series(String title, VideoProducer videoProducer, VideoCategory videoCategory, Double price, String posterImage,
            Date dateReleased, String description){
        
        this.title = title ;
        this.videoProducer = videoProducer ;
        this.videoCategory = videoCategory ;
        this.price = price ;
        this.posterImage = posterImage ;
        this.dateReleased = dateReleased ;
        this.description = description ;
    }

    
    //getters and setters
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

    public VideoProducer getVideoProducer() {
        return videoProducer;
    }

    public void setVideoProducer(VideoProducer videoProducer) {
        this.videoProducer = videoProducer;
    }

    public VideoCategory getVideoCategory() {
        return videoCategory;
    }

    public void setVideoCategory(VideoCategory videoCategory) {
        this.videoCategory = videoCategory;
    }

    @Override
    public Double getPrice() {
        return price;
    }

    @Override
    public void setPrice(Double price) {
        this.price = price;
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

    public Date getDateReleased() {
        return dateReleased;
    }

    public void setDateReleased(Date dateReleased) {
        this.dateReleased = dateReleased;
    }
    
    public String getDescription(){
        
        return description ;
    }
    
    public void setDescription(String description){
        
        this.description = description ;
    }
    
    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Series that = (Series) o;
        return Objects.equals(id, that.id) &&
                Objects.equals(title, that.title) &&
                Objects.equals(videoProducer, that.videoProducer) &&
                Objects.equals(videoCategory, that.videoCategory) &&
                Objects.equals(price, that.price) &&
                Objects.equals(dateReleased, that.dateReleased) &&
                Objects.equals(posterImage, that.posterImage) &&
                Objects.equals(sample, that.sample) &&
                Objects.equals(description, that.description)
                ;
                
    }
    
    @Override
    public int hashCode() {
        return Objects.hash(id,title,videoProducer,videoCategory,price,dateReleased,posterImage,sample,description) ;
    }

    @Override
    public int compareTo(Series series) {
        
        //compareTo method of Date, returns a positive value when the date param is greater than the date instance on which
        //compareTo() is called. since we want latest series to appear before older series, we reverse that behaviour by 
        //multiplying the returned value by -1.
        return -1 * dateReleased.compareTo(series.getDateReleased()) ;
    }
    
    
}
