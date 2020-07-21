/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.samaritan.opus.model;

/**
 * any media that can be bought on Opus should extend this class
 * @author electron
 */
public abstract class OpusMedia {
    
    Double price ;
    public abstract Double getPrice() ;
    public abstract void setPrice(Double price) ;
}
