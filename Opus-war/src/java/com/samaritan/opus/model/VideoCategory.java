/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.samaritan.opus.model;

import com.google.gson.annotations.Expose;
import java.io.Serializable;
import java.util.Objects;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Table;

/**
 * a category of video content
 * @author electron
 */
@Entity
@Table(name = "video_category", schema = "opus")
public class VideoCategory implements Serializable {
    
    //data fields
    @Expose(serialize = true, deserialize = true)
    @Id
    @Column(name = "id")
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id ;
    
    @Expose(serialize = true, deserialize = true)
    @Column(name = "name", nullable = false, length = 20)
    private String name ;
    
    //constructors
    public VideoCategory(){}
    
    public VideoCategory(String name){
        
        this.name = name ;
    }
    
    //getters and setters
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
    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        VideoCategory that = (VideoCategory) o;
        return Objects.equals(id, that.id) &&
                Objects.equals(name, that.name) ;
    }

    @Override
    public int hashCode() {
        return Objects.hash(id,name) ;
    }
}
