/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.samaritan.opus.model;

import com.google.gson.annotations.Expose;
import java.io.Serializable;
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
import org.hibernate.annotations.Type;

/**
 * represents a notification to the user about a new album release from an artiste he's following
 * @author electron
 */
@Entity
@Table(name = "album_release_notification", schema = "opus")
public class AlbumReleaseNotification implements Serializable{
    
    
    //data fields
    @Expose(serialize = true)
    @Id
    @Column(name = "id", nullable = false, unique = true)
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id ;
    
    
    @OneToOne
    @JoinColumn(name="profile_account", nullable = false)
    private ProfileAccount profileAccount ;
    
    @Expose(serialize = true)
    @OneToOne(cascade = CascadeType.ALL)
    @JoinColumn(name="artiste", unique = true)
    private Artiste artiste ;
    
    @Expose(serialize = true)
    @OneToOne
    @JoinColumn(name="album", nullable = false)
    private Album album ;
    
    
    @Type(type = "org.hibernate.type.NumericBooleanType")
    @Column(name = "is_user_notified", nullable = false, columnDefinition = "TINYINT")
    private boolean userNotified ;
    
    
    @Column(name = "date_created")
    private Long dateCreated ;

    
    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public ProfileAccount getProfileAccount() {
        return profileAccount;
    }

    public void setProfileAccount(ProfileAccount profileAccount) {
        this.profileAccount = profileAccount;
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

    public boolean isUserNotified() {
        return userNotified;
    }

    public void setUserNotified(boolean userNotified) {
        this.userNotified = userNotified;
    }

    public Long getDateCreated() {
        return dateCreated;
    }

    public void setDateCreated(Long dateCreated) {
        this.dateCreated = dateCreated;
    }
    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        AlbumReleaseNotification that = (AlbumReleaseNotification) o;
        return Objects.equals(id, that.id) &&
                Objects.equals(profileAccount, that.profileAccount) &&
                Objects.equals(artiste, that.artiste) &&
                Objects.equals(album, that.album) &&
                Objects.equals(userNotified, that.userNotified) &&
                Objects.equals(dateCreated, that.dateCreated) ;
    }

    @Override
    public int hashCode() {
        return Objects.hash(id,profileAccount,artiste,album,userNotified,dateCreated) ;
    }

}
