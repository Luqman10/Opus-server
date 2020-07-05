/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.samaritan.opus.model;

import java.io.Serializable;
import javax.persistence.Entity;
import javax.persistence.Table;

/**
 * represents a notification to the user about a new album release from an artiste he's following
 * @author electron
 */
@Entity
@Table(name = "album_release_notification", schema = "opus")
public class AlbumReleaseNotification implements Serializable{
    
    
    
}
