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
 * A 16 char token assigned to a profile account and expected as the Authentication request header with every incoming
 * request apart from login, verify user email request.
 * @author electron
 */
@Entity
@Table(name = "bearer_token", schema = "opus")
public class BearerToken {
    
    @Expose(serialize = true, deserialize = true)
    @Id
    @Column(name = "id", nullable = false, unique = true)
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id ;
    
    @Expose(serialize = true, deserialize = true)
    @OneToOne
    @JoinColumn(name="profile_account", unique = true,nullable = true)
    private ProfileAccount profileAccount ;
    
    @Expose(serialize = true, deserialize = true)
    @Column(name = "token", length = 16)
    private String token ;

    
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

    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }
    
    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        BearerToken that = (BearerToken) o;
        return  Objects.equals(id, that.id) &&
                Objects.equals(profileAccount, that.profileAccount) &&
                Objects.equals(token, that.token) ;
    }

    @Override
    public int hashCode() {
        return Objects.hash(id,profileAccount, token);
    }
}
