package com.samaritan.opus.model;

import com.google.gson.annotations.Expose;
import java.util.Objects;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Table;

/**
 *an artiste who has signed up on Opus
 * @author electron
 */
@Entity
@Table(name = "artiste", schema = "opus")
public class Artiste {
    
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
    
    @Column(name = "phone_number", nullable = false, length = 13)
    private String phoneNumber ;
    
    @Expose(serialize = true, deserialize = true)
    @Column(name = "stage_name", nullable = false, length = 20)
    private String stageName ;
    
    @Expose(serialize = true, deserialize = true)
    @Column(name = "profile_picture", nullable = true, length = 200)
    private String profilePicture ;
    
    @Expose(serialize = true, deserialize = true)
    private boolean isUserFollowingArtiste ;
    
    @Expose(serialize = true, deserialize = true)
    @Column(name = "number_of_songs", nullable = false)
    private long numberOfSongs ;

    
    @Expose(serialize = true, deserialize = true)
    @Column(name = "number_of_albums", nullable = false)
    private long numberOfAlbums ;

    //constructor
    public Artiste(){}
    
    public Artiste(Integer id, String firstName, String lastName, String phoneNumber,
            String stageName, String profilePicture, long numberOfSongs, long numberOfAlbums) {
        
        this.id = id;
        this.firstName = firstName;
        this.lastName = lastName;
        this.phoneNumber = phoneNumber;
        this.stageName = stageName;
        this.profilePicture = profilePicture;
        this.numberOfSongs = numberOfSongs;
        this.numberOfAlbums = numberOfAlbums ;
    }

    
    
    //getters
    public Integer getId(){
        
        return id ;
    }
    
    public String getFirstName(){
        
        return firstName ;
    }
    
    public String getLastName(){
        
        return lastName ;
    }
    
    public String getPhoneNumber(){
        
        return phoneNumber ;
    }
    
    public String getStageName(){
        
        return stageName ;
    }
    
    public String getProfilePicture(){
        
        return profilePicture ;
    }
    
    public boolean isIsUserFollowingArtiste() {
        return isUserFollowingArtiste;
    }
    
    public long getNumberOfSongs() {
        return numberOfSongs;
    }


    public long getNumberOfAlbums() {
        return numberOfAlbums;
    }
    
    //setters
    public void setId(int id){
        
        this.id = id ;
    }
    
    public void setFirstName(String firstName){
        
        this.firstName = firstName ;
    }
    
    public void setLastName(String lastName){
        
        this.lastName = lastName ;
    }
    
    public void setPhoneNumber(String phoneNumber){
        
        this.phoneNumber = phoneNumber ;
    }
    
    public void setStageName(String stageName){
        
        this.stageName = stageName ;
    }
    
    public void setProfilePicture(String profilePicture){
        
        this.profilePicture = profilePicture ;
    }
    
    public void setIsUserFollowingArtiste(boolean isUserFollowingArtiste) {
        this.isUserFollowingArtiste = isUserFollowingArtiste;
    }
    
    public void setNumberOfSongs(long numberOfSongs) {
        this.numberOfSongs = numberOfSongs;
    }
    
    public void setNumberOfAlbums(long numberOfAlbums) {
        this.numberOfAlbums = numberOfAlbums;
    }
   
 
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Artiste that = (Artiste) o;
        return Objects.equals(id, that.id) &&
                Objects.equals(firstName, that.firstName) &&
                Objects.equals(lastName, that.lastName) &&
                Objects.equals(phoneNumber, that.phoneNumber) &&
                Objects.equals(stageName, that.stageName) &&
                Objects.equals(profilePicture, that.profilePicture) &&
                Objects.equals(isUserFollowingArtiste, that.isUserFollowingArtiste) &&
                Objects.equals(numberOfSongs, that.numberOfSongs) &&
                Objects.equals(numberOfAlbums, that.numberOfAlbums)
                ;
    }

    @Override
    public int hashCode() {
        return Objects.hash(id,firstName,lastName,phoneNumber,stageName,profilePicture,isUserFollowingArtiste,
                numberOfSongs,numberOfAlbums) ;
    }
    
}
