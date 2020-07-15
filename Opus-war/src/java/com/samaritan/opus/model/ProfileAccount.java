package com.samaritan.opus.model;

import com.google.gson.annotations.Expose;
import org.hibernate.annotations.Type;
import javax.persistence.*;
import java.util.Objects;

@Entity
@Table(name = "profile_account", schema = "opus")
public class ProfileAccount {

    @Expose(serialize = true, deserialize = true)
    @Id
    @Column(name = "id", nullable = false, unique = true)
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id ;
    
    @Expose(serialize = true, deserialize = true)
    @Column(name = "email", nullable = false, length = 50)
    private String email;
    
    @Expose(serialize = true, deserialize = true)
    @Column(name = "username", nullable = false, length = 50)
    private String username;

    @Expose(serialize = true, deserialize = true)
    @Column(name = "password", nullable = false, length = 50)
    private String password;

    @Expose(serialize = true, deserialize = true)
    @Column(name = "email_verification_key", length = 16)
    private String emailVerificationKey;

    @Expose(serialize = true, deserialize = true)
    @Type(type = "org.hibernate.type.NumericBooleanType")
    @Column(name = "is_account_active", nullable = false, columnDefinition = "TINYINT")
    private boolean accountActive;

    @Expose(serialize = true, deserialize = true)
    @Column(name = "date_email_verification_key_created")
    private Long dateEmailVerificationKeyCreated;
    
    @Expose(serialize = true, deserialize = true)
    @Column(name = "date_account_created")
    private Long dateAccountCreated;
    
    @Expose(serialize = true, deserialize = true)
    @Type(type = "org.hibernate.type.NumericBooleanType")
    @Column(name = "is_email_verified", nullable = false, columnDefinition = "TINYINT")
    private boolean emailVerified;

    @Expose(serialize = true, deserialize = true)
    @Column(name = "password_reset_key", length = 16)
    private String passwordResetKey ;

    @Expose(serialize = true, deserialize = true)
    @Column(name = "date_password_reset_key_created")
    private Long datePasswordResetKeyCreated;
    
    @Expose(serialize = true, deserialize = true)
    @Type(type = "org.hibernate.type.NumericBooleanType")
    @Column(name = "is_user_logged_in", nullable = false, columnDefinition = "TINYINT")
    private boolean userLoggedIn ;
    
    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }
    
    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }



    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }


    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }


    public String getEmailVerificationKey() {
        return emailVerificationKey;
    }

    public void setEmailVerificationKey(String emailVerificationKey) {
        this.emailVerificationKey = emailVerificationKey;
    }


    public boolean getAccountActive() {
        return accountActive;
    }

    public void setAccountActive(boolean accountActive) {
        this.accountActive = accountActive;
    }

    public Long getDateEmailVerificationKeyCreated() {
        return dateEmailVerificationKeyCreated;
    }

    public void setDateEmailVerificationKeyCreated(Long dateCreated) {
        this.dateEmailVerificationKeyCreated = dateCreated;
    }
    
    public Long getDateAccountCreated() {
        return dateAccountCreated;
    }

    public void setDateAccountCreated(Long dateAccountCreated) {
        this.dateAccountCreated = dateAccountCreated;
    }

    public boolean getEmailVerified() {
        return emailVerified;
    }

    public void setEmailVerified(boolean emailVerified) {
        this.emailVerified = emailVerified;
    }
    
    
    public String getPasswordResetKey() {
        return passwordResetKey;
    }

    public void setPasswordResetKey(String passwordResetKey) {
        this.passwordResetKey = passwordResetKey;
    }

    public Long getDatePasswordResetKeyCreated() {
        return datePasswordResetKeyCreated ;
    }

    public void setDatePasswordResetKeyCreated(Long datePasswordResetKeyCreated) {
        this.datePasswordResetKeyCreated = datePasswordResetKeyCreated;
    }
    
    public boolean isUserLoggedIn() {
        return userLoggedIn;
    }

    public void setUserLoggedIn(boolean userLoggedIn) {
        this.userLoggedIn = userLoggedIn;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ProfileAccount that = (ProfileAccount) o;
        return  Objects.equals(id, that.id) &&
                Objects.equals(email, that.email) &&
                Objects.equals(username, that.username) &&
                Objects.equals(password, that.password) &&
                Objects.equals(emailVerificationKey, that.emailVerificationKey) &&
                Objects.equals(accountActive, that.accountActive) &&
                Objects.equals(dateEmailVerificationKeyCreated, that.dateEmailVerificationKeyCreated) &&
                Objects.equals(dateAccountCreated, that.dateAccountCreated) &&
                Objects.equals(emailVerified, that.emailVerified) &&
                Objects.equals(passwordResetKey, that.passwordResetKey) &&
                Objects.equals(datePasswordResetKeyCreated, that.datePasswordResetKeyCreated) &&
                Objects.equals(userLoggedIn, that.userLoggedIn)
                ;
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, email, username, password, emailVerificationKey, accountActive, dateEmailVerificationKeyCreated, dateAccountCreated, emailVerified, passwordResetKey,datePasswordResetKeyCreated,userLoggedIn);
    }
    
}
