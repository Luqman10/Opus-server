package com.samaritan.opus.model;

import com.google.gson.annotations.Expose;

/**
 * holds details of a user's login credentials[email,password]
 */
public class LoginCredentials{

    @Expose(serialize = true, deserialize = true)
    private String email ;
    
    @Expose(serialize = true, deserialize = true)
    private String password ;

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }
}
