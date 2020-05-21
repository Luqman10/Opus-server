package com.samaritan.opus.response;

import com.google.gson.annotations.SerializedName;

/**
 * represents a generic com.samaritan.opus.response sent back to the http client
 */
public class GenericResponse {

    @SerializedName("message")
    private String message ;

    public GenericResponse(String message){

        this.message = message ;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }
}
