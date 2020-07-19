package com.samaritan.opus.response;

import com.google.gson.annotations.SerializedName;

/**
 * represents the details of a user's account (profile + payment) sent back to the client
 */
public class UserAccountResponse {
    
    @SerializedName("profileAccountId")
    private int profileAccountId = -1 ;

    @SerializedName("username")
    private String username ;

    @SerializedName("email")
    private String email ;
    
    @SerializedName("token")
    private String token ;
    
    @SerializedName("paymentAccountId")
    private int paymentAccountId = -1 ;

    @SerializedName("mobileMoneyNumber")
    private String mobileMoneyNumber ;

    /**
     * to be used when only profile account details needs to be sent
     * @param profileAccountId
     * @param username
     * @param email
     * @param token
     */
    public UserAccountResponse(int profileAccountId, String username, String email, String token){
        
        this.profileAccountId = profileAccountId ;
        this.username = username ;
        this.email = email ;
        this.token = token ;

    }

    /**
     * to be used when profile account + payment account details needs to be sent
     * @param profileAccountId
     * @param username
     * @param email
     * @param paymentAccountId
     * @param mobileMoneyNumber
     * @param token
     */
    public UserAccountResponse(int profileAccountId, String username, String email, int paymentAccountId, String mobileMoneyNumber,
            String token){
        
        this.profileAccountId = profileAccountId ;
        this.username = username ;
        this.email = email ;
        this.paymentAccountId = paymentAccountId ;
        this.mobileMoneyNumber = mobileMoneyNumber ;
        this.token = token ;

    }
    
    /**
     * to be used when only payment account details need to be sent
     * @param paymentAccountId
     * @param mobileMoneyNumber 
     */
    public UserAccountResponse(int paymentAccountId, String mobileMoneyNumber){
        
        this.paymentAccountId = paymentAccountId ;
        this.mobileMoneyNumber = mobileMoneyNumber ;
    }

    public int getProfileAccountId() {
        return profileAccountId;
    }

    public void setProfileAccountId(int profileAccountId) {
        this.profileAccountId = profileAccountId;
    }
    
    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }
    
    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }
    
    public int getPaymentAccountId() {
        return paymentAccountId;
    }

    public void setPaymentAccountId(int paymentAccountId) {
        this.paymentAccountId = paymentAccountId;
    }
    
    public String getMobileMoneyNumber() {
        return mobileMoneyNumber;
    }

    public void setMobileMoneyNumber(String mobileMoneyNumber) {
        this.mobileMoneyNumber = mobileMoneyNumber;
    }
}
