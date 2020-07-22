package com.samaritan.opus.service;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.samaritan.opus.application.OpusApplication;
import com.samaritan.opus.model.PaymentAccount;
import com.samaritan.opus.model.ProfileAccount;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import com.samaritan.opus.response.GenericResponse;
import com.samaritan.opus.response.UserAccountResponse;
import javax.ws.rs.*;
import javax.ws.rs.core.Response;
import java.net.URI;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.servlet.ServletContext;
import javax.ws.rs.core.Context;
import org.hibernate.HibernateException;
import org.hibernate.Transaction;
import org.hibernate.exception.ConstraintViolationException;
import org.hibernate.query.Query;

@Path("/paymentAccounts")
public class PaymentAccountResource{

    //servlet context
    @Context
    protected ServletContext servletContext ;

    //send messages to the log
    Logger logger = Logger.getLogger("com.samaritan.opus.service.PaymentAccountResource") ;
    
    //sql error codes
    private final int DUPLICATE_ENTRY_ERROR_CODE = 1062 ;
    private final int CONSTRAINT_VIOLATION_ERROR_CODE = 1452 ;
    
    /**
     * create new payment account
     * @param paymentAccountInJson
     * @return response
     */
    @POST
    @Consumes("application/json")
    @Produces("application/json")
    public Response createPaymentAccount(String paymentAccountInJson){
        
        //create a new payment account which is null
        PaymentAccount paymentAccount = null ;
        Response.ResponseBuilder responseBuilder = null ;
        
        try{
            
            //parse paymentAccountInJson to PaymentAccount object
            Gson gson = new GsonBuilder()
                    .excludeFieldsWithoutExposeAnnotation()
                    .create();
            paymentAccount = gson.fromJson(paymentAccountInJson, PaymentAccount.class) ;
            
            //if the request body(payment account) was succesfully parsed, create the new payment account in DB
            if(paymentAccount != null){
                
                //save the payment account in the DB and send a response code of 201 with the payment details as response
                //body
                savePaymentAccountInDB(paymentAccount) ;
                responseBuilder = Response.created(URI.create("/paymentAccounts/" + paymentAccount.getId())) ;
                responseBuilder.entity(createUserAccountJsonResponse(paymentAccount.getId(), paymentAccount.getPhoneNumber())) ;
            }
            //let the client know the payment account had an invalid format
            else{
                
                responseBuilder = Response.status(Response.Status.CONFLICT) ;
                responseBuilder.entity(createGenericJsonResponse("the payment account object format is invalid")) ;
            }
            
        }
        catch (ConstraintViolationException ex) {
            
            switch(ex.getErrorCode()){
                
                case DUPLICATE_ENTRY_ERROR_CODE:
                    //if the error was because of a duplicate entry
                    responseBuilder = Response.status(Response.Status.CONFLICT) ;
                    responseBuilder.entity(createGenericJsonResponse("a payment account already exists for your profile")) ;
                    break ;
                    
                case CONSTRAINT_VIOLATION_ERROR_CODE:
                    //if the error was because of a constraint violation
                    responseBuilder = Response.status(Response.Status.CONFLICT) ;
                    responseBuilder.entity(createGenericJsonResponse("sorry, you do not have a profile account on opus")) ;
                    break ;
                    
                default:
                    responseBuilder = Response.serverError() ;
                    responseBuilder.entity(createGenericJsonResponse("an unexpected error occurred. please try again")) ;
            }
            
        }

        return responseBuilder.build() ;
    }
    
    /**
     * update a payment account's phone number
     * @param id the payment account (to be updated) id
     * @param updatedPhoneNumber the updated phone number
     * @param profileAccountId
     * @return response
     */
    @PUT
    @Produces("application/json")
    public Response updatePhoneNumber(@QueryParam("id") int id, @QueryParam("phoneNumber") String updatedPhoneNumber,
            @QueryParam("profileAccountId") int profileAccountId){
        
        Response.ResponseBuilder responseBuilder = null ;
        
        try{
            
            //update the payment account's phone number
            int affectedRows = updatePaymentAccountPhoneNumber(id,updatedPhoneNumber) ;
            //1 means number updated
            if(affectedRows == 1){
            
                logger.log(Level.INFO, "phone number of payment account with ID: " + id + " has been " + "updated to " 
                        + updatedPhoneNumber) ;
                responseBuilder = Response.ok() ;
                responseBuilder.entity(createUserAccountJsonResponse(id, updatedPhoneNumber)) ;
            
            }
            //0 means there's no payment account with that id, so save payment account in DB
            else if(affectedRows == 0){
                
                ProfileAccount profileAccount = new ProfileAccount() ;
                profileAccount.setId(profileAccountId) ;
                
                PaymentAccount paymentAccount = new PaymentAccount() ;
                paymentAccount.setProfileAccount(profileAccount) ;
                paymentAccount.setPhoneNumber(updatedPhoneNumber) ;
                savePaymentAccountInDB(paymentAccount) ;
                
                responseBuilder = Response.ok() ;
                responseBuilder.entity(createUserAccountJsonResponse(paymentAccount.getId(), paymentAccount.getPhoneNumber())) ;
            }
            else{
            
                //send a 409 error code
                responseBuilder = Response.status(Response.Status.CONFLICT) ;
                responseBuilder.entity(createGenericJsonResponse("updating mobile money number failed")) ;
            }
        }
        catch(HibernateException ex){
            
            responseBuilder = Response.serverError() ;
            responseBuilder.entity(createGenericJsonResponse("an unexpected error occurred. please try again")) ;
        }
        
        return responseBuilder.build() ;
    }
    
    /**
     * save a payment account object into the payment_account database table
     * @param paymentAccount the payment account to save
     * @return void
     * @throws ConstraintViolationException when an error occurred during insertion
     */
    private void savePaymentAccountInDB(PaymentAccount paymentAccount) throws ConstraintViolationException{
        
        SessionFactory sessionFactory = (SessionFactory)servletContext.getAttribute(OpusApplication.HIBERNATE_SESSION_FACTORY) ;
        Session session = sessionFactory.openSession() ;
        Transaction transaction = null ;
        try{
            transaction = session.beginTransaction() ;
            session.save(paymentAccount) ;
            transaction.commit() ;
            logger.log(Level.INFO, "A new payment account has been created in database") ;
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
     * update a payment account's phone number
     * @param id the payment account's id
     * @param updatedPhoneNumber the new phone number
     * @return the number of affected rows
     */
    private int updatePaymentAccountPhoneNumber(int id, String updatedPhoneNumber) throws HibernateException{

        SessionFactory sessionFactory = (SessionFactory)servletContext.getAttribute(OpusApplication.HIBERNATE_SESSION_FACTORY) ;
        Session session = sessionFactory.openSession() ;
        Transaction transaction = null ;
        int success = 0 ;
        try{
            
            transaction = session.beginTransaction() ;
            Query query = session.createQuery("UPDATE PaymentAccount SET phoneNumber=:phoneNumber WHERE id=:id") ;
            query.setParameter("phoneNumber", updatedPhoneNumber) ;
            query.setParameter("id",id) ;
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
        
        return success ;
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
     * @param paymentAccountId
     * @param mobileMoneyNumber
     * @return a json representation of the user account info object
     */
    private String createUserAccountJsonResponse(int paymentAccountId, String mobileMoneyNumber){

        UserAccountResponse userAccountResponse = new UserAccountResponse(paymentAccountId, mobileMoneyNumber) ;
        String responseBody = new Gson().toJson(userAccountResponse, UserAccountResponse.class) ;
        return responseBody ;
    }
    
}
