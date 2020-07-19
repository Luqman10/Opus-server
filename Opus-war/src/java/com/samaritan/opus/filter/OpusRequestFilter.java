/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.samaritan.opus.filter;

import com.samaritan.opus.application.OpusApplication;
import com.samaritan.opus.model.BearerToken;
import java.io.IOException;
import javax.servlet.ServletContext;
import javax.ws.rs.NotAcceptableException;
import javax.ws.rs.NotAuthorizedException;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerRequestFilter;
import javax.ws.rs.container.PreMatching;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.HttpHeaders;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.query.Query;

/**
 * filters requests(except user email verification,login and sign up) based on whether they have a valid bearer token 
 * and profileAccountId in their request header
 * @author electron
 */
@PreMatching
public class OpusRequestFilter implements ContainerRequestFilter{

    //servlet context
    @Context
    protected ServletContext servletContext ;
    
    @Override
    public void filter(ContainerRequestContext requestContext) throws IOException {
        
        //paths for login,verify email and sign up
        String createProfileAccountRequestPath = "profileAccounts/" ;
        String verifyEmailRequestPath = "profileAccounts/verifyEmail" ;
        String loginRequestPath = "profileAccounts/login" ;
        
        //don't filter requests for login,verify email and sign up
        String path = requestContext.getUriInfo().getPath() ;
        if(!path.equals(createProfileAccountRequestPath) && !path.equals(verifyEmailRequestPath) &&
                !path.equals(loginRequestPath)){
            
            //get the authorization and profileAccountId headers
            String authHeader = requestContext.getHeaderString(HttpHeaders.AUTHORIZATION) ;
            String profileAccountIdHeader = requestContext.getHeaderString("profile_account_id") ;
            
            //if any of the headers are not present, send a 401
            if (authHeader == null || profileAccountIdHeader == null) throw new NotAuthorizedException("Bearer") ;
            
            //retrieve the bearer token from DB
            int profileAccountId = Integer.parseInt(profileAccountIdHeader.trim()) ;
            String bearerToken = retrieveBearerToken(profileAccountId) ;
            
            //strip 'Bearer' from the auth header
            String [] bearerTokenArray = authHeader.split(" ") ;
            if(bearerTokenArray.length != 2)
                throw new NotAcceptableException("invalid bearer token. format: Bearer [token]") ;
            
            //get the token from the header
            authHeader = bearerTokenArray[1] ;
            
            //if token is null or its value is not same as that of authentication header, send a 401
            if (bearerToken == null || !bearerToken.equals(authHeader.trim())) 
                throw new NotAuthorizedException("Bearer") ;
            
        }
    }
    
    /**
     * get the bearer token assigned to the profile account with given id
     * @param profileAccountId
     * @return the token
     */
    private String retrieveBearerToken(int profileAccountId){
        
        SessionFactory sessionFactory = (SessionFactory)servletContext.getAttribute(OpusApplication.HIBERNATE_SESSION_FACTORY) ;
        Session session = sessionFactory.openSession() ;
        Query<BearerToken> query = session.createQuery("FROM BearerToken bt JOIN FETCH bt.profileAccount WHERE bt.profileAccount.id=:profileAccountId", BearerToken.class) ;
        query.setParameter("profileAccountId", profileAccountId) ;
        BearerToken bearerToken = query.uniqueResult() ;
        session.close() ;
        if(bearerToken != null)
            return bearerToken.getToken() ;
        else
            return null ;
    }
    
}
