package com.samaritan.opus.service;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonSyntaxException;
import com.samaritan.opus.application.OpusApplication;
import com.samaritan.opus.application.OpusProperties;
import com.samaritan.opus.model.BearerToken;
import com.samaritan.opus.model.LoginCredentials;
import com.samaritan.opus.model.PaymentAccount;
import com.samaritan.opus.model.ProfileAccount;
import com.samaritan.opus.util.EmailSender;
import org.apache.commons.codec.digest.DigestUtils;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.query.Query;
import com.samaritan.opus.response.UserAccountResponse;
import com.samaritan.opus.response.GenericResponse;

import javax.ws.rs.*;
import javax.ws.rs.core.Response;
import java.io.*;
import java.net.URI;
import java.util.Scanner;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.mail.MessagingException;
import javax.servlet.ServletContext;
import javax.ws.rs.core.Context;
import org.hibernate.HibernateException;
import org.hibernate.Transaction;
import org.hibernate.exception.ConstraintViolationException;

@Path("/profileAccounts")
public class ProfileAccountResource{
    
    //servlet context
    @Context
    protected ServletContext servletContext ;

    //send messages to the log
    Logger logger = Logger.getLogger("com.samaritan.opus.service.ProfileAccountResource") ;
    
    //sql error codes
    private final int DUPLICATE_ENTRY_ERROR_CODE = 1062 ;

    /**
     * Create a new profile account resource read from the request body in json
     * @param profileAccountInJson
     * @return response to the client
     */
    @POST
    @Consumes("application/json")
    @Produces("application/json")
    public Response createProfileAccount(String profileAccountInJson){
        
        //profile account from request body
        ProfileAccount profileAccount = null ;
        Response.ResponseBuilder responseBuilder ;

        try{
            
            //parse profileAccountInJson to ProfileAccount object
            Gson gson = new GsonBuilder()
                    .excludeFieldsWithoutExposeAnnotation()
                    .create();
            profileAccount = gson.fromJson(profileAccountInJson, ProfileAccount.class) ;
            
            //if the request body was successfully parsed, create profile account and save in DB
            if(profileAccount != null){
                
                profileAccount.setPassword(DigestUtils.shaHex(profileAccount.getPassword())) ;
                profileAccount.setAccountActive(false) ;
                String verificationKey = generateKey() ;
                profileAccount.setEmailVerificationKey(verificationKey) ;
                long currentDate = System.currentTimeMillis() ;
                profileAccount.setDateEmailVerificationKeyCreated(currentDate) ;
                profileAccount.setDateAccountCreated(currentDate) ;
                profileAccount.setEmailVerified(false) ;
                profileAccount.setUserLoggedIn(false) ;
                saveProfileAccountInDatabase(profileAccount) ;
                
                //send a verifiction email to the user
                sendVerificationEmailToUser(verificationKey,profileAccount) ;
                logger.log(Level.INFO, "A verification email has been sent to " + profileAccount.getEmail()) ;
                responseBuilder = Response.created(URI.create("/profileAccounts/" + profileAccount.getEmail())) ;
                responseBuilder.entity(createGenericJsonResponse("your profile account was created successfully and "
                        + "an email verification code has been sent to " + profileAccount.getEmail())) ;
                
                
            }
            //let the client know that the request body format is invalid
            else{
                
                responseBuilder = Response.status(Response.Status.CONFLICT) ;
                responseBuilder.entity(createGenericJsonResponse("the profile account object format is invalid")) ;
            }
        }
        catch(ConstraintViolationException ex){
            
            switch(ex.getErrorCode()){
                
                case DUPLICATE_ENTRY_ERROR_CODE:
                    //if the error was because of a duplicate entry
                    responseBuilder = Response.status(Response.Status.CONFLICT) ;
                    responseBuilder.entity(createGenericJsonResponse("a profile account already exists for your email")) ;
                    break ;
                    
                default:
                    responseBuilder = Response.serverError() ;
                    responseBuilder.entity(createGenericJsonResponse("an unexpected error occurred. please try signing up"
                            + " again")) ;
            }
        }
        catch(MessagingException ex){
            
            //when an exception occured during sending the verification email, delete the saved profile account from DB
            deleteProfileAccountFromDatabase(profileAccount.getEmail()) ;

            //send bad gateway (502) response code JSON body describing the problem
            responseBuilder = Response.status(Response.Status.BAD_GATEWAY) ;
            responseBuilder.entity(createGenericJsonResponse("an unexpected error occurred. please try" +
            " signing up again")) ;
        }
        catch(JsonSyntaxException ex){
            
            responseBuilder = Response.serverError() ;
            responseBuilder.entity(createGenericJsonResponse("an unexpected error occurred. please try again")) ;
        }
        
        return responseBuilder.build() ;
    }

    /**
     * verify a profile account's email address if the given key matches that in the database
     * @param email
     * @param key
     * @return response (id,username,email)
     */
    @Path("/verifyEmail")
    @GET
    @Produces("application/json")
    public Response verifyEmail(@QueryParam("email") String email, @QueryParam("key") String key){
         
        //to create appropriate response to send to client
        Response.ResponseBuilder responseBuilder ;
        
        try{
            
            //send a response code of 200, if the email was verified
            if(verifyProfileAccountEmailAddress(email,key)){
                
                //update the profile account's isUserLoggedIn, send a 500 if it cannot be updated
                if(!updateProfileAccountIsUserLoggedIn(email, true)){

                    responseBuilder = Response.serverError() ;
                    responseBuilder.entity(createGenericJsonResponse("an unexpected error occurred. please try again")) ;
                    return responseBuilder.build() ;
                }
                
                //retrieve profile account from DB
                ProfileAccount profileAccount = retrieveProfileAccountFromDBUsingEmail(email) ;
                
                //generate bearer token for profile account and save in DB
                String token = generateKey() ;
                BearerToken bearerToken = new BearerToken() ;
                bearerToken.setProfileAccount(profileAccount) ;
                bearerToken.setToken(token) ;
                token = saveBearerTokenForProfileAccount(bearerToken) ;
                
                responseBuilder = Response.ok() ;
                responseBuilder.entity(createUserAccountJsonResponse(profileAccount.getId(),profileAccount.getUsername(),
                        profileAccount.getEmail(),token)) ;
            }
            //if there is no profile account with the given email or verification key, send a 409
            else{

                responseBuilder = Response.status(Response.Status.CONFLICT) ;
                responseBuilder.entity(createGenericJsonResponse("there's no profile with email: " + email
                           + " and key: " + key + " to verify")) ;

            }
        }
        catch(HibernateException | NullPointerException ex){
            
            responseBuilder = Response.serverError() ;
            responseBuilder.entity(createGenericJsonResponse("an unexpected error occurred. please try again")) ;
        }
        
        return responseBuilder.build() ;
    }

    /**
     * resends a verification email to a user when he or she requests for it.
     * @param email
     */
    @Path("/resendKey")
    @GET
    @Produces("application/json")
    public Response resendEmailVerificationKey(@QueryParam("email") String email){

        //to create appropriate response to send to client
        Response.ResponseBuilder responseBuilder ;
        
        try{
            
            //use email to retrieve profile account from DB
            ProfileAccount profileAccount = retrieveProfileAccountFromDBUsingEmail(email) ;
            
            //send a 409 if profile account doesn't exist
            if(profileAccount == null){
                
                responseBuilder = Response.status(Response.Status.CONFLICT) ;
                responseBuilder.entity(createGenericJsonResponse("sorry, there's no profile account with email: " + email)) ;
            }
            else{
                
                //send a 409 if the email has already been verified
                if(profileAccount.getEmailVerified()){
                    
                    responseBuilder = Response.status(Response.Status.CONFLICT) ;
                    responseBuilder.entity(createGenericJsonResponse("your email has been verified already")) ;
                }
                //send the user a new email with the verification key and a 200 response if the email isn't verified
                else{
                    
                    sendVerificationEmailToUser(profileAccount.getEmailVerificationKey(), profileAccount) ;
                    responseBuilder = Response.ok() ;
                    responseBuilder.entity(createGenericJsonResponse("an email verification code has been sent to: " + email)) ;
                }
            }
        }
        catch(MessagingException ex){
            
            //send bad gateway (502) response code JSON body describing the problem
            responseBuilder = Response.status(Response.Status.BAD_GATEWAY) ;
            responseBuilder.entity(createGenericJsonResponse("an unexpected error occurred. please try again")) ;
        }
        
        return responseBuilder.build() ;
    }

    /**
     * log a user in if the given email and password match a profile account
     * @param loginCredentialsInJson the login credential object in json format
     * @return response
     */
    @Path("/login")
    @POST
    @Consumes("application/json")
    @Produces("application/json")
    public Response login(String loginCredentialsInJson){
            
        //login credentials from the request body
        LoginCredentials loginCredentials = null ;
        Response.ResponseBuilder responseBuilder ;
        
        try{
            
            //parse loginCredentialsInJson to LoginCredentials object
            Gson gson = new GsonBuilder()
                    .excludeFieldsWithoutExposeAnnotation()
                    .create();
            loginCredentials = gson.fromJson(loginCredentialsInJson, LoginCredentials.class) ;
            
            //if the login credentials couldn't be parsed, send a 409
            if(loginCredentials == null){
                
                responseBuilder = Response.status(Response.Status.CONFLICT) ;
                responseBuilder.entity(createGenericJsonResponse("the login credentials object format is invalid")) ;
            }
            else{
                
                //get the email
                String email = loginCredentials.getEmail() ;
                //hash the password
                String hashedPassword = DigestUtils.shaHex(loginCredentials.getPassword()) ;

                //get profile account from DB that macthes email and password
                ProfileAccount profileAccount = retrieveProfileAccountFromDBUsingEmailAndPassword(email,hashedPassword) ;
                
                //if no profile account was found, send 409
                if(profileAccount == null){
                    
                    responseBuilder = Response.status(Response.Status.CONFLICT) ;
                    responseBuilder.entity(createGenericJsonResponse("your email or password is wrong")) ;
                }
                //if a profile account was found
                else{
                    
                    //if the user has not verified his email, return a 403 (forbidden) response code
                    if(!profileAccount.getEmailVerified()){
                        
                        responseBuilder = Response.status(Response.Status.FORBIDDEN) ;
                        responseBuilder.entity(createGenericJsonResponse("sorry, you have to verify your email first before log in")) ;
                    }
                    //if user has already verified email, check if account is deactivated and activate it before logging in
                    else{
                        
                        //if the profile account is deactivated, activate it
                        if(!profileAccount.getAccountActive()){

                            //if the account couldn't be activated, send a 500 response
                            if(!updateProfileAccountActive(profileAccount.getEmail(), true)){

                                responseBuilder = Response.serverError() ;
                                responseBuilder.entity(createGenericJsonResponse("an unexpected error occurred. please try again")) ;
                                return responseBuilder.build() ;
                            }
                        }
                        
                        //if the profile account is already logged in, report to client
                        if(profileAccount.isUserLoggedIn()){
                            
                            responseBuilder = Response.status(Response.Status.CONFLICT) ;
                            responseBuilder.entity(createGenericJsonResponse("sorry, you have already logged in to your account")) ;
                            return responseBuilder.build() ;
                        }
                        else{
                            
                            //update the profile account's isUserLoggedIn, send a 500 if it cannot be updated
                            if(!updateProfileAccountIsUserLoggedIn(profileAccount.getEmail(), true)){

                                responseBuilder = Response.serverError() ;
                                responseBuilder.entity(createGenericJsonResponse("an unexpected error occurred. please try again")) ;
                                return responseBuilder.build() ;
                            }
                            
                        }
                        
                        //get the payment account whose email macthes that of the profile account
                        PaymentAccount paymentAccount = retrievePaymentAccountFromDBUsingEmail(profileAccount.getEmail()) ;
                        
                        //get the profile account's bearer token
                        String token = retrieveBearerToken(profileAccount.getId()) ;
                        
                        //send a 200 response code with the entity(profile account id, username, email, 
                        //payment account id, momo number)
                        responseBuilder = Response.ok() ;
                        responseBuilder.entity(createUserAccountJsonResponse(profileAccount, paymentAccount, token)) ;
                        
                    }
                }
            }
        }
        catch(JsonSyntaxException | HibernateException ex){
            
            responseBuilder = Response.serverError() ;
            responseBuilder.entity(createGenericJsonResponse("an unexpected error occurred. please try again")) ;
        }
        
        return responseBuilder.build() ;
    }

    /**
     * generate a password reset key, update database, and send the user an email with the reset key
     * @param email
     */
    @Path("/passwordResetKey")
    @GET
    @Produces("application/json")
    public Response generatePasswordResetKey(@QueryParam("email") String email){

        //to send response to client
        Response.ResponseBuilder responseBuilder ;
        
        try{
            
            //retrieve profile account from DB using email
            ProfileAccount profileAccount = retrieveProfileAccountFromDBUsingEmail(email) ;

            //check if account exists with given email
            if(profileAccount != null){

                //if the profile account has a password reset key that is not expired, send it to the user
                String currentPasswordResetKey = profileAccount.getPasswordResetKey() ;
                if(currentPasswordResetKey != null){
                    
                    //send the current password reset link to the user
                    if(sendPasswordResetLinkToUser(currentPasswordResetKey, profileAccount)){

                        //send an OK response to client
                        responseBuilder = Response.ok() ;
                        responseBuilder.entity(createGenericJsonResponse("Your password reset link has been sent to: " + email)) ;
                    }
                    else{

                        //if the email wasn't sent successfully, tell the user
                        responseBuilder = Response.serverError() ;
                        responseBuilder.entity(createGenericJsonResponse("we couldn't send you the password reset link."
                                    + " please try again")) ;
                    }
                }
                else{
                    
                    //if there's no password reset key, generate a new one and send to user
                    //generate password reset key
                    String passwordResetKey = generateKey() ;

                    //update DB with generated key
                    if(updateProfileAccountPasswordResetKey(email, passwordResetKey)){

                        //send the password reset link to the user
                        if(sendPasswordResetLinkToUser(passwordResetKey, profileAccount)){

                            //send an OK response to client
                            responseBuilder = Response.ok() ;
                            responseBuilder.entity(createGenericJsonResponse("Your password reset link has been sent to: " + email)) ;
                        }
                        else{

                            //if the email wasn't sent successfully, tell the user
                            responseBuilder = Response.serverError() ;
                            responseBuilder.entity(createGenericJsonResponse("we couldn't send you the password reset link."
                                    + " please try again")) ;
                        }
                    }
                    else{

                        //send an internal server error (500) response code
                        responseBuilder = Response.serverError() ;
                        responseBuilder.entity(createGenericJsonResponse("an unexpected error occured. please try again")) ;
                    }
                }
            }
            else{

                //send a 409 error
                responseBuilder = Response.status(Response.Status.CONFLICT) ;
                responseBuilder.entity(createGenericJsonResponse("sorry, there's no profile account with email: " + email)) ;
            }
        }
        catch(MessagingException | HibernateException ex){
            
            responseBuilder = Response.serverError() ;
            responseBuilder.entity(createGenericJsonResponse("an unexpected error occurred. please try again")) ;
        }
        
        return responseBuilder.build() ;
    }

    /**
     * reset a profile account's password
     * if the user provided a valid password reset key
     * @param email
     * @param key
     * @param password
     * @return response code of 200 if the password was reset successfully
     */
    @Path("/resetPassword")
    @GET
    @Produces("application/json")
    public Response resetPassword(@QueryParam("email") String email, @QueryParam("key") String key,
                                  @QueryParam("password") String password){

        //to send response to client
        Response.ResponseBuilder responseBuilder ;
        
        try{
            
            //update the user's password and send a 200 if it was successful
            if(updateProfileAccountPassword(email, key, DigestUtils.shaHex(password))){
                
                responseBuilder = Response.ok() ;
                responseBuilder.entity(createGenericJsonResponse("Your password has been reset successfully. you can " +
                        "log in with your new password")) ;
            }
            else{
                
                responseBuilder = Response.status(Response.Status.CONFLICT) ;
                responseBuilder.entity(createGenericJsonResponse("sorry, your email or password reset key may be wrong. please" +
                        " try again")) ;
            }
        }
        catch(HibernateException ex){
            
            responseBuilder = Response.serverError() ;
            responseBuilder.entity(createGenericJsonResponse("There was an error on the server")) ;
        }
        
        return responseBuilder.build() ;
    }

    /**
     * update a profile accounts email address
     * @param email the profile account's email address
     * @param newEmail the new email address
     */
    @Path("/updateEmail")
    @PUT
    @Produces("application/json")
    public Response updateEmail(@QueryParam("email") String email, @QueryParam("newEmail") String newEmail){
        
        //to send response to client
        Response.ResponseBuilder responseBuilder ;
        
        //retrieve profile account from DB using email
        ProfileAccount profileAccount = retrieveProfileAccountFromDBUsingEmail(email) ;
        
        try{
            
            //check if account exists with given email
            if(profileAccount != null){

                //generate new ver key
                String verificationKey = generateKey() ;

                //update the email address, ver key, account active, date verification key created, is_email_verified, userLoggedIn in DB
                updateProfileAccountEmail(email, newEmail, verificationKey, false, System.currentTimeMillis(), false, false) ;

                //update the retrieved profile account with the new email
                profileAccount.setEmail(newEmail) ;

                //send verification email
                if(sendVerificationEmailToUser(verificationKey, profileAccount)){

                    logger.log(Level.INFO, "A verification email has been sent to " + newEmail) ;
                    responseBuilder = Response.ok() ;
                    responseBuilder.entity(createGenericJsonResponse("a verification email has been sent to the email: "
                        + newEmail + ". please follow the link to verify your account")) ;

                }
                else{

                    logger.log(Level.INFO, "sending verification email to " + newEmail + " failed. resetting to " +
                        "previous email") ;

                    //reset the profile account's email back to the previous email if sending email verification failed
                    updateProfileAccountEmail(newEmail, email, profileAccount.getEmailVerificationKey(), true,
                        profileAccount.getDateEmailVerificationKeyCreated(), true, true) ;

                    //send bad gateway (502) response code JSON body describing the problem
                    responseBuilder = Response.status(Response.Status.BAD_GATEWAY) ;
                    responseBuilder.entity(createGenericJsonResponse("an unexpected error occurred. try" +
                        " updating your email again")) ;
                }

            }
            else{

                //send a 409 error
                responseBuilder = Response.status(Response.Status.CONFLICT) ;
                responseBuilder.entity(createGenericJsonResponse("sorry, there's no profile with email: " + email)) ;
            }
        }
        catch(HibernateException ex){
            
            responseBuilder = Response.serverError() ;
            responseBuilder.entity(createGenericJsonResponse("an unexpected error occurred. please try again")) ;
        }
        catch(MessagingException ex){
            
            logger.log(Level.INFO, "sending verification email to " + newEmail + " failed. resetting to " +
                        "previous email") ;

            //reset the profile account's email back to the previous email if sending email verification failed
            updateProfileAccountEmail(newEmail, email, profileAccount.getEmailVerificationKey(), true,
                profileAccount.getDateEmailVerificationKeyCreated(), true, true) ;

            //send bad gateway (502) response code JSON body describing the problem
            responseBuilder = Response.status(Response.Status.BAD_GATEWAY) ;
            responseBuilder.entity(createGenericJsonResponse("an unexpected error occurred. try" +
                " updating your email again")) ;
        }
        
        return responseBuilder.build() ;
    }

    /**
     * update the profile account's username
     * @param email the profile account's email address
     * @param newUsername the new username to set
     */
    @Path("/updateUsername")
    @PUT
    @Produces("application/json")
    public Response updateUsername(@QueryParam("email") String email, @QueryParam("newUsername") String newUsername){

        //to send response to client
        Response.ResponseBuilder responseBuilder ;
        
        try{
            
            if(updateProfileAccountUsername(email, newUsername)){

                logger.log(Level.INFO, "username of profile account with email: " + email + " has been " +
                        "updated to " + newUsername) ;
                responseBuilder = Response.ok() ;
                responseBuilder.entity(createGenericJsonResponse("your username has been updated successfully")) ;
            }
            else{

                //send a 409 error
                responseBuilder = Response.status(Response.Status.CONFLICT) ;
                responseBuilder.entity(createGenericJsonResponse("sorry, there's no profile with email: " + email)) ;
            }
        }
        catch(HibernateException ex){
            
            //send an internal server error (500) response code
            responseBuilder = Response.serverError() ;
            responseBuilder.entity(createGenericJsonResponse("There was an error on the server")) ;
        }
        
        return responseBuilder.build() ;
    }

    /**
     * deactivate a profile account
     * @param email the profile account's email address
     */
    @Path("/deactivate")
    @PUT
    @Produces("application/json")
    public Response deactivateProfileAccount(@QueryParam("email") String email){

        //to send response to client
        Response.ResponseBuilder responseBuilder ;
        
        try{
            
            //update the profile account's isUserLoggedIn, send a 500 if it cannot be updated
            if(!updateProfileAccountIsUserLoggedIn(email, false)){

                responseBuilder = Response.serverError() ;
                responseBuilder.entity(createGenericJsonResponse("an unexpected error occurred. please try again")) ;
                return responseBuilder.build() ;
            }
            
            if(updateProfileAccountActive(email, false)){

                logger.log(Level.INFO, "profile account with email: " + email + " has been " +
                        "deactivated") ;
                responseBuilder = Response.ok() ;
                responseBuilder.entity(createGenericJsonResponse("your account has been deactivated")) ;
                
            }
            else{

                //send a 409 error
                responseBuilder = Response.status(Response.Status.CONFLICT) ;
                responseBuilder.entity(createGenericJsonResponse("sorry, there's no profile with email: " + email)) ;
            }
        }
        catch(HibernateException ex){
            
            //send an internal server error (500) com.samaritan.opus.response code
            responseBuilder = Response.serverError() ;
            responseBuilder.entity(createGenericJsonResponse("There was an error on the server")) ;
            
        }
        
        return responseBuilder.build() ;
    }

    /**
     * delete a profile account
     * @param email the profile account's email address
     */
    @Path("/delete")
    @DELETE
    @Produces("application/json")
    public Response deleteProfileAccount(@QueryParam("email") String email){

        //to send response to client
        Response.ResponseBuilder responseBuilder ;
        
        try{
            
            if(deleteProfileAccountFromDatabase(email)){
                
                logger.log(Level.INFO, "profile account with email: " + email + " has been " +
                        "deleted") ;
                responseBuilder = Response.ok() ;
                responseBuilder.entity(createGenericJsonResponse("your account has been deleted")) ;
                
            }
            else{

                //send a 409 error
                responseBuilder = Response.status(Response.Status.CONFLICT) ;
                responseBuilder.entity(createGenericJsonResponse("sorry, there's no profile with email: " + email)) ;
                
            }
        }
        catch(HibernateException ex){
            
            //send an internal server error (500) response code
            responseBuilder = Response.serverError() ;
            responseBuilder.entity(createGenericJsonResponse("There was an error on the server")) ;
            
        }
        
        return responseBuilder.build() ;
        
    }
    
    /**
     * logout of the profile account with the given email
     * @param email the profile account's email address
     */
    @Path("/logout")
    @GET
    @Produces("application/json")
    public Response logout(@QueryParam("email") String email){
        
        //to create appropriate response to send to client
        Response.ResponseBuilder responseBuilder ;
        
        try{
            
            //update the profile account's isUserLoggedIn, send a 500 if it cannot be updated
            if(!updateProfileAccountIsUserLoggedIn(email, false)){

                responseBuilder = Response.serverError() ;
                responseBuilder.entity(createGenericJsonResponse("an unexpected error occurred. please try again")) ;
                return responseBuilder.build() ;
            }
            
            //send a 200 if the account has been logged out successfully
            responseBuilder = Response.ok() ;
            responseBuilder.entity(createGenericJsonResponse("logout successful")) ;
        }
        catch(HibernateException ex){
            
            responseBuilder = Response.serverError() ;
            responseBuilder.entity(createGenericJsonResponse("an unexpected error occurred. please try again")) ;
        }
        
        return responseBuilder.build() ;
    }

    /**
     * retrieve a profile account from the database using an email address in the where clause
     * @param email the email address
     * @return the profile account
     */
    private ProfileAccount retrieveProfileAccountFromDBUsingEmail(String email){

        SessionFactory sessionFactory = (SessionFactory)servletContext.getAttribute(OpusApplication.HIBERNATE_SESSION_FACTORY) ;
        Session session = sessionFactory.openSession() ;
        Query<ProfileAccount> query = session.createQuery("FROM ProfileAccount WHERE email=:email", ProfileAccount.class) ;
        query.setParameter("email", email) ;
        ProfileAccount profileAccount = query.uniqueResult() ;
        session.close() ;
        return profileAccount ;
    }
    
    /**
     * retrieve a payment account from db using an email address in the where clause
     * @param email
     * @return payment account
     */
    private PaymentAccount retrievePaymentAccountFromDBUsingEmail(String email){
        
        SessionFactory sessionFactory = (SessionFactory)servletContext.getAttribute(OpusApplication.HIBERNATE_SESSION_FACTORY) ;
        Session session = sessionFactory.openSession() ;
        Query<PaymentAccount> query = session.createQuery("FROM PaymentAccount pa JOIN FETCH pa.profileAccount WHERE pa.profileAccount.email=:email", PaymentAccount.class) ;
        query.setParameter("email", email) ;
        PaymentAccount paymentAccount = query.uniqueResult() ;
        session.close() ;
        return paymentAccount ;
    }

    
    /**
     * retrieve a profile account from db using email and password in the where clause
     * @param email
     * @param password
     * @return profileAccount
     */
    private ProfileAccount retrieveProfileAccountFromDBUsingEmailAndPassword(String email, String password){

        SessionFactory sessionFactory = (SessionFactory)servletContext.getAttribute(OpusApplication.HIBERNATE_SESSION_FACTORY) ;
        Session session = sessionFactory.openSession() ;
        Query<ProfileAccount> query = session.createQuery("FROM ProfileAccount WHERE email=:email AND password=:password", ProfileAccount.class) ;
        query.setParameter("email", email) ;
        query.setParameter("password", password) ;
        ProfileAccount profileAccount = query.uniqueResult() ;
        session.close() ;
        return profileAccount ;
    }


    /**
     * update a profile account's password reset key and date key was created
     * @return true if operation was successful
     */
    private boolean updateProfileAccountPasswordResetKey(String email, String passwordResetKey) throws HibernateException{

        SessionFactory sessionFactory = (SessionFactory)servletContext.getAttribute(OpusApplication.HIBERNATE_SESSION_FACTORY) ;
        Session session = sessionFactory.openSession() ;
        Transaction transaction = null ;
        int success = 0 ;
        try{
            
            transaction = session.beginTransaction() ;
            Query query = session.createQuery("UPDATE ProfileAccount SET passwordResetKey=:passwordResetKey, datePasswordResetKeyCreated=:datePasswordResetKeyCreated WHERE email=:email") ;
            query.setParameter("passwordResetKey", passwordResetKey) ;
            query.setParameter("datePasswordResetKeyCreated", System.currentTimeMillis()) ;
            query.setParameter("email",email) ;
            success = query.executeUpdate() ;
            transaction.commit() ;
        }
        catch(HibernateException ex){
            
            if(transaction != null) transaction.rollback() ;
            throw ex ;
        }
        finally{
            
            session.close() ;
        }
        
        return success > 0 ;
    }

    /**
     * update a profile account's password
     * @param email the profile account's email
     * @param key the password reset key
     * @param password the new password
     * @return true if it was successful
     */
    private boolean updateProfileAccountPassword(String email, String key, String password){

        SessionFactory sessionFactory = (SessionFactory)servletContext.getAttribute(OpusApplication.HIBERNATE_SESSION_FACTORY) ;
        Session session = sessionFactory.openSession() ;
        Transaction transaction = null ;
        int success = 0 ;
        try{
            
            transaction = session.beginTransaction() ;
            Query query = session.createQuery("UPDATE ProfileAccount SET password=:password, passwordResetKey=:passwordResetKey, datePasswordResetKeyCreated=:datePasswordResetKeyCreated  WHERE email=:email AND passwordResetKey=:key") ;
            query.setParameter("password", password) ;
            query.setParameter("passwordResetKey", null) ;
            query.setParameter("datePasswordResetKeyCreated", null) ;
            query.setParameter("email",email) ;
            query.setParameter("key",key) ;
            success = query.executeUpdate() ;
            transaction.commit() ;
        }
        catch(HibernateException ex){
            
            if(transaction != null) transaction.rollback() ;
            throw ex ;
        }
        finally{
        
            session.close() ;
        }
        
        return success > 0 ;
    }

    /**
     * update a profile account email address
     * @param email the profile account current email
     * @param newEmail the new email to set
     * @param verificationKey the new verification key
     * @param accountActive true if account is active
     * @param dateKeyCreated date key was created
     * @param isEmailVerified true if the email is verified
     * @param isUserLoggedIn true if the user is logged in
     */
    private void updateProfileAccountEmail(String email, String newEmail, String verificationKey, boolean accountActive,
                                              Long dateKeyCreated, boolean isEmailVerified, boolean isUserLoggedIn) throws HibernateException{

        SessionFactory sessionFactory = (SessionFactory)servletContext.getAttribute(OpusApplication.HIBERNATE_SESSION_FACTORY) ;
        Session session = sessionFactory.openSession() ;
        Transaction transaction = null ;
        try{
            
            transaction = session.beginTransaction() ;
            Query query = session.createQuery("UPDATE ProfileAccount SET email=:newEmail, emailVerificationKey=:emailVerificationKey, dateEmailVerificationKeyCreated=:dateEmailVerificationKeyCreated, accountActive=:accountActive, emailVerified=:emailVerified, userLoggedIn=:userLoggedIn WHERE email=:email") ;
            query.setParameter("newEmail", newEmail) ;
            query.setParameter("email",email) ;
            query.setParameter("emailVerificationKey", verificationKey) ;
            query.setParameter("accountActive", accountActive) ;
            query.setParameter("dateEmailVerificationKeyCreated", dateKeyCreated) ;
            query.setParameter("emailVerified", isEmailVerified) ;
            query.setParameter("userLoggedIn", isUserLoggedIn) ;
            query.executeUpdate() ;
            transaction.commit() ;
        }
        catch(HibernateException ex){
            
            if(transaction != null) transaction.rollback() ;
            throw ex ;
        }
        finally{
            
            session.close() ;
        }
        
    }

    /**
     * update a profile account's username
     * @param email the profile account's email address
     * @param newUsername the new username to set
     * @return true if update was successful
     */
    private boolean updateProfileAccountUsername(String email, String newUsername) throws HibernateException{

        SessionFactory sessionFactory = (SessionFactory)servletContext.getAttribute(OpusApplication.HIBERNATE_SESSION_FACTORY) ;
        Session session = sessionFactory.openSession() ;
        Transaction transaction = null ;
        int success = 0 ;
        
        try{
            
            transaction = session.beginTransaction() ;
            Query query = session.createQuery("UPDATE ProfileAccount SET username=:username WHERE email=:email") ;
            query.setParameter("username", newUsername) ;
            query.setParameter("email",email) ;
            success = query.executeUpdate() ;
            transaction.commit() ;
        }
        catch(HibernateException ex){
            
            if(transaction != null) transaction.rollback() ;
            throw ex ;
        }
        finally{
            
            session.close() ;
        }
        
        return success > 0 ;
    }

    /**
     * update a profile account's is_account_active
     * @param email the profile account's email address
     * @param isAccountActive true if account is active
     * @return true if update was successful
     */
    private boolean updateProfileAccountActive(String email, boolean isAccountActive) throws HibernateException{

        SessionFactory sessionFactory = (SessionFactory)servletContext.getAttribute(OpusApplication.HIBERNATE_SESSION_FACTORY) ;
        Session session = sessionFactory.openSession() ;
        Transaction transaction = null ;
        int success = 0 ;
        try{
            
            transaction = session.beginTransaction() ;
            Query query = session.createQuery("UPDATE ProfileAccount SET accountActive=:accountActive WHERE email=:email") ;
            query.setParameter("accountActive", isAccountActive) ;
            query.setParameter("email",email) ;
            success = query.executeUpdate() ;
            transaction.commit() ;
        }
        catch(HibernateException ex){
            
            if(transaction != null) transaction.rollback() ;
            throw ex ;
        }
        finally{
            
            session.close() ;
        }
        
        return success > 0 ;
    }

    
    /**
     * update a profile account's is_user_logged_in
     * @param email the profile account's email address
     * @param isUserLoggedIn true if the profile account has been logged in
     * @return true if update was successful
     */
    private boolean updateProfileAccountIsUserLoggedIn(String email, boolean isUserLoggedIn) throws HibernateException{

        SessionFactory sessionFactory = (SessionFactory)servletContext.getAttribute(OpusApplication.HIBERNATE_SESSION_FACTORY) ;
        Session session = sessionFactory.openSession() ;
        Transaction transaction = null ;
        int success = 0 ;
        try{
            
            transaction = session.beginTransaction() ;
            Query query = session.createQuery("UPDATE ProfileAccount SET userLoggedIn=:userLoggedIn WHERE email=:email") ;
            query.setParameter("userLoggedIn", isUserLoggedIn) ;
            query.setParameter("email",email) ;
            success = query.executeUpdate() ;
            transaction.commit() ;
        }
        catch(HibernateException ex){
            
            if(transaction != null) transaction.rollback() ;
            throw ex ;
        }
        finally{
            
            session.close() ;
        }
        
        return success > 0 ;
    }

    
    /**
     * save the given profile account in the database
     * @param profileAccount
     * @return void
     * @throws ConstraintViolationException
     */
    private void saveProfileAccountInDatabase(ProfileAccount profileAccount) throws ConstraintViolationException{

        SessionFactory sessionFactory = (SessionFactory)servletContext.getAttribute(OpusApplication.HIBERNATE_SESSION_FACTORY) ;
        Session session = sessionFactory.openSession() ;
        Transaction transaction = null ;
        try{
            transaction = session.beginTransaction() ;
            session.save(profileAccount) ;
            transaction.commit() ;
            logger.log(Level.INFO, "A new profile account has been created in database") ;
        }
        catch(ConstraintViolationException ex){
            
            if(transaction != null) transaction.rollback() ;
            throw ex ;
        }
        finally{
            
            session.close() ;
        }
    }
    
    /**
     * delete a profile account from the database using the email address
     * @param email
     * @return
     * @throws HibernateException 
     */
    private boolean deleteProfileAccountFromDatabase(String email) throws HibernateException{

        SessionFactory sessionFactory = (SessionFactory)servletContext.getAttribute(OpusApplication.HIBERNATE_SESSION_FACTORY) ;
        Session session = sessionFactory.openSession() ;
        Transaction transaction = null ;
        int success = 0 ;
        try{
            transaction = session.beginTransaction() ;
            Query query = session.createQuery("DELETE FROM ProfileAccount WHERE email=:email") ;
            query.setParameter("email", email) ;
            success = query.executeUpdate() ;
            transaction.commit() ;
        }
        catch(HibernateException ex){
            
            if(transaction != null) transaction.rollback() ;
            throw ex ;
        }
        finally{
            
            session.close() ;
        }
        
        return success > 0 ;
    }


    /**
     * generate a 16 character key
     * @return key
     */
    public static String generateKey(){

        //the length of the verification key
        final int VERIFICATION_CODE_LENGTH = 16 ;

        //generate string containing valid characters a verification key can contain
        String validCharacters = "" ;

        //digits
        for(int i = 48 ; i <= 57 ; i++){

            validCharacters += (char)i ;
        }

        //lowercase letters
        for(int i = 97 ; i <= 122 ; i++){

            validCharacters += (char)i ;
        }

        //uppercase letters
        for(int i = 65 ; i <= 90 ; i++){

            validCharacters += (char)i ;
        }

        //get length of valid chars
        int validCharactersLength = validCharacters.length() ;

        //create verification key
        String key = "" ;
        for(int i = 0 ; i < VERIFICATION_CODE_LENGTH ; i++){

            key += validCharacters.charAt((int) (Math.random() * validCharactersLength)) ;
        }

        return key ;
    }

    /**
     * send an email to the user telling him or her to verify the email provided
     * @param verificationKey
     * @param profileAccount
     * @return true if the email was sent successfully
     * @throws MessagingException
     */
    private boolean sendVerificationEmailToUser(String verificationKey, ProfileAccount profileAccount) throws MessagingException{

        try{
            
            String emailSubject = "Verify your email address" ;
            String messageBody = "" ;

            //get the email body
            InputStream inputStream = getClass().getClassLoader().getResourceAsStream("com/samaritan/opus/asset/email_verification_content.txt") ;
            Scanner scanner = new Scanner(inputStream) ;
            while(scanner.hasNext()){

                messageBody += scanner.nextLine() ;
            }

            //make changes to message body
            messageBody = messageBody.replace("{opus_logo_url}", OpusProperties.OPUS_LOGO_URL) ;
            messageBody = messageBody.replace("{username}", profileAccount.getUsername()) ;
            messageBody = messageBody.replace("{verification_key}", verificationKey) ;
            messageBody = messageBody.replace("{verification_link}", OpusProperties.BASE_URL +
                    "/Opus_war_exploded/opus/profileAccounts/verifyEmail?email=" + profileAccount.getEmail() +
                    "&key=" + verificationKey) ;

            EmailSender emailSender = new EmailSender(profileAccount.getEmail(), emailSubject, messageBody) ;
            return emailSender.sendMessage() ;
        }
        catch(MessagingException ex){
            
            throw ex ;
        }
    }

    /**
     * send a password reset link to a user' email address
     * @return true if the operation was successful
     */
    private boolean sendPasswordResetLinkToUser(String passwordResetKey,ProfileAccount profileAccount) throws MessagingException{

        try{
            
            String emailSubject = "Reset your Opus password" ;
            String messageBody = "" ;

            //get the email body
            InputStream inputStream = getClass().getClassLoader().getResourceAsStream("com/samaritan/opus/asset/reset_password_content.txt") ;
            Scanner scanner = new Scanner(inputStream) ;
            while(scanner.hasNext()){

                messageBody += scanner.nextLine() ;
            }

            //make changes to message body
            messageBody = messageBody.replace("{opus_logo_url}", OpusProperties.OPUS_LOGO_URL) ;
            messageBody = messageBody.replace("{username}", profileAccount.getUsername()) ;
            messageBody = messageBody.replace("{reset_password_link}", OpusProperties.BASE_URL +
                    "/Opus_war_exploded/opus/profileAccounts/resetPassword?email=" + profileAccount.getEmail() +
                    "&key=" + passwordResetKey) ;

            EmailSender emailSender = new EmailSender(profileAccount.getEmail(), emailSubject, messageBody) ;
            return emailSender.sendMessage() ;
        }
        catch(MessagingException ex){
            
            throw ex ;
        }
    }

    /**
     * create a generic json object from a GenericResponse object
     * @param message
     * @return the json object
     */
    private String createGenericJsonResponse(String message){

        GenericResponse genericResponse = new GenericResponse(message) ;
        String responseBody = new Gson().toJson(genericResponse, GenericResponse.class) ;
        return responseBody ;
    }

    /**
     * create a string holding user account info in json format
     * @param id
     * @param username
     * @param email
     * @param token
     * @return the json string
     */
    private String createUserAccountJsonResponse(int profileAccountId, String username, String email, String token){

        UserAccountResponse userAccountResponse = new UserAccountResponse(profileAccountId, username, email, token) ;
        String responseBody = new Gson().toJson(userAccountResponse, UserAccountResponse.class) ;
        return responseBody ;
    }
    
    
    /**
     * create a string holding user account info in json format
     * @param profileAccount
     * @param paymentAccount
     * @param token
     * @return the json object
     */
    private String createUserAccountJsonResponse(ProfileAccount profileAccount, PaymentAccount paymentAccount, 
            String token){

        UserAccountResponse userAccountResponse ;
        //create user account response based on whether payment account is null or not
        if(paymentAccount == null){
            
            userAccountResponse = new UserAccountResponse(profileAccount.getId(), profileAccount.getUsername(), 
                    profileAccount.getEmail(), token) ;
        }
        else{
            
            userAccountResponse = new UserAccountResponse(profileAccount.getId(), profileAccount.getUsername(), 
                    profileAccount.getEmail(), paymentAccount.getId(), paymentAccount.getPhoneNumber(), token) ;
        }
        String responseBody = new Gson().toJson(userAccountResponse, UserAccountResponse.class) ;
        return responseBody ;
    }
    
    
    /**
     * verify a profile account's email address by updating 
     * email_verification_key = null
     * is_account_active = true
     * date_email_verification_key_created = null
     * is_email_verified = true
     * @param email the profile account's email address
     * @param key the email verification key
     * @return true if the email was verified
     */
    private boolean verifyProfileAccountEmailAddress(String email, String key) throws HibernateException{
        
        SessionFactory sessionFactory = (SessionFactory)servletContext.getAttribute(OpusApplication.HIBERNATE_SESSION_FACTORY) ;
        Session session = sessionFactory.openSession() ;
        Transaction transaction = null ;
        int success = 0 ;
        try{
            
            transaction = session.beginTransaction() ;
            Query query = session.createQuery("UPDATE ProfileAccount SET emailVerificationKey=:emailVerificationKey, accountActive=:accountActive, dateEmailVerificationKeyCreated=:dateEmailVerificationKeyCreated, emailVerified=:emailVerified WHERE email=:email AND emailVerificationKey=:key") ;
            query.setParameter("emailVerificationKey", null) ;
            query.setParameter("accountActive", true) ;
            query.setParameter("dateEmailVerificationKeyCreated", null) ;
            query.setParameter("emailVerified", true) ;
            query.setParameter("email", email) ;
            query.setParameter("key", key) ;
            success = query.executeUpdate() ;
            transaction.commit() ;
        }
        catch(HibernateException ex){
            
            if(transaction != null) transaction.rollback() ;
            throw ex ;
        }
        finally{
        
            session.close() ;
        }
        
        return success > 0 ;
    }
    
    /**
     * save or update the passed token into the bearer_token table
     * @param bearerToken the bearer token
     */
    private String saveBearerTokenForProfileAccount(BearerToken bearerToken){
        
        SessionFactory sessionFactory = (SessionFactory)servletContext.getAttribute(OpusApplication.HIBERNATE_SESSION_FACTORY) ;
        Session session = sessionFactory.openSession() ;
        Transaction transaction = null ;
        String tokenToReturn = "" ;
        try{
            transaction = session.beginTransaction() ;
            session.save(bearerToken) ;
            transaction.commit() ;
            logger.log(Level.INFO, "A bearer token has been assigned with profile id " + 
                    bearerToken.getProfileAccount().getId()) ;
            tokenToReturn = bearerToken.getToken() ;
        }
        catch(ConstraintViolationException ex){
            
            logger.log(Level.INFO, "A bearer token already exists for profile account with id " + 
                    bearerToken.getProfileAccount().getId()) ;
            
            if(transaction != null) transaction.rollback() ;
            
            //return the bearer token that already exists
            tokenToReturn = retrieveBearerToken(bearerToken.getProfileAccount().getId()) ;
        }
        finally{
            
            session.close() ;
            return tokenToReturn ;
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
        return bearerToken.getToken() ;
    }

}
