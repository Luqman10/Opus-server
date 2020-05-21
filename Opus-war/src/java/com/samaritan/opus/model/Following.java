/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.samaritan.opus.model;

import com.google.gson.annotations.Expose;
import java.util.Objects;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.OneToOne;
import javax.persistence.Table;

/**
 * A user following an artiste
 * @author electron
 */
@Entity
@Table(name = "following", schema = "opus")
public class Following {
    
    
    @Expose(serialize = true, deserialize = true)
    @Id
    @Column(name = "id", nullable = false, unique = true)
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id ;
    
    @Expose(serialize = true, deserialize = true)
    @OneToOne
    @JoinColumn(name="profile_account", unique = true, nullable = false)
    private ProfileAccount profileAccount ;
    
    @Expose(serialize = true, deserialize = true)
    @OneToOne
    @JoinColumn(name="artiste", unique = true)
    private Artiste artiste ;
    

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
    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Following that = (Following) o;
        return Objects.equals(id, that.id) &&
                Objects.equals(profileAccount, that.profileAccount) &&
                Objects.equals(artiste, that.artiste) ;
    }

    @Override
    public int hashCode() {
        return Objects.hash(id,artiste,profileAccount) ;
    }
    
   
}
