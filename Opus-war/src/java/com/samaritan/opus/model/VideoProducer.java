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
 * a video (movie,documentary,series) producer
 * @author electron
 */
@Entity
@Table(name = "video_producer", schema = "opus")
public class VideoProducer implements Serializable {
    
    //data fields
    @Expose(serialize = true, deserialize = true)
    @Id
    @Column(name = "id")
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id ;
    
    @Expose(serialize = true, deserialize = true)
    @Column(name = "first_name", nullable = false, length = 20)
    private String firstName ;
    
    @Expose(serialize = true, deserialize = true)
    @Column(name = "last_name", nullable = false, length = 30)
    private String lastName ;
    
    @Column(name = "bank_acc_number", nullable = false, length = 18)
    private String bankAccountNumber ;
    
    @Expose(serialize = true, deserialize = true)
    @Column(name = "company_name", nullable = true, length = 30)
    private String companyName ;
    
    
    //constructor
    public VideoProducer(){}
    
    public VideoProducer(String firstName, String lastName, String bankAccountNumber, String companyName){
        
        this.firstName = firstName ;
        this.lastName = lastName ; 
        this.bankAccountNumber = bankAccountNumber ;
        this.companyName = companyName ;
        
    }
    
    //getters and setters
    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public String getFirstName() {
        return firstName;
    }

    public void setFirstName(String firstName) {
        this.firstName = firstName;
    }

    public String getLastName() {
        return lastName;
    }

    public void setLastName(String lastName) {
        this.lastName = lastName;
    }

    public String getBankAccountNumber() {
        return bankAccountNumber;
    }

    public void setBankAccountNumber(String bankAccountNumber) {
        this.bankAccountNumber = bankAccountNumber;
    }

    public String getCompanyName() {
        return companyName;
    }

    public void setCompanyName(String companyName) {
        this.companyName = companyName;
    }
    
    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        VideoProducer that = (VideoProducer) o;
        return Objects.equals(id, that.id) &&
                Objects.equals(firstName, that.firstName) &&
                Objects.equals(lastName, that.lastName) &&
                Objects.equals(bankAccountNumber, that.bankAccountNumber) &&
                Objects.equals(companyName, that.companyName) ;
    }
    
    
    @Override
    public int hashCode() {
        return Objects.hash(id,firstName,lastName,bankAccountNumber,companyName) ;
    }
    
    
}
