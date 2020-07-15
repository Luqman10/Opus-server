/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.samaritan.opus.response;

import com.google.gson.annotations.SerializedName;

/**
 * holds values for opus properties that will be sent back to client
 * @author electron
 */
public class PropertiesResponse {
    
    @SerializedName("versionCode")
    private int versionCode ;

    public int getVersionCode() {
        return versionCode;
    }

    public void setVersionCode(int versionCode) {
        this.versionCode = versionCode;
    }
    
    
}
