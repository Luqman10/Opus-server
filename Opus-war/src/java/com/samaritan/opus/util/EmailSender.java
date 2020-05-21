package com.samaritan.opus.util ;

import javax.mail.*;
import javax.mail.internet.*;
import java.util.Properties;
import java.util.logging.Logger;

/**
 * This class is used for sending an email message to a receipient address
 */
public class EmailSender{

    //for logging exceptions and errors
    private Logger logger = null ;

    //the email params
    private String to ;
    private String subject ;
    private String messageBody ;

    //classes from javax.mail to help send email
    private Session session ;
    private Message message ;

    //properties of the SMTP server
    private final String SMTP_HOST = "smtp.gmail.com" ;
    private final String SMTP_PORT = "587" ;

    //GMAIL credentials
    private final String USER = "labdul_qadir@st.ug.edu.gh" ;
    private final String PASSWORD = "elnhmzxvagofamks" ;

    //for holding properties of the SMTP server
    private Properties properties ;

    //constructor
    public EmailSender(String to, String subject, String messageBody){

        logger = Logger.getLogger("com.findr.util.EmailSender") ;

        //initialize email params
        this.to = to ;
        this.subject = subject ;
        this.messageBody = messageBody ;

        //put the properties of the smtp server into the properties object
        properties = new Properties() ;
        properties.put("mail.smtp.host",SMTP_HOST) ;
        properties.put("mail.smtp.port",SMTP_PORT) ;
        properties.put("mail.smtp.starttls.enable", "true");
        properties.put("mail.smtp.auth", "true");

        //create the session object
        session = Session.getInstance(properties, new javax.mail.Authenticator(){

            protected PasswordAuthentication getPasswordAuthentication() {
                return new PasswordAuthentication(USER, PASSWORD) ;
            }
        });
        session.setDebug(true) ;

    }

    public void setTo(String to){

        this.to = to ;
    }

    public String getTo(){

        return to ;
    }

    public void setSubject(String subject){

        this.subject = subject ;
    }

    public String getSubject(){

        return subject ;
    }

    public void setMessageBody(String messageBody){

        this.messageBody = messageBody ;
    }

    public String getMessageBody(){

        return messageBody ;
    }


    /**
     * send the message to the email address in to
     * @return true if message was successfully sent
     * @throws MessagingException
     */
    public boolean sendMessage() throws MessagingException{

        try {
            // create a message
            message = new MimeMessage(session) ;
            // From Address
            message.setFrom(new InternetAddress(USER)) ;
            // TO Address
            InternetAddress toAddress = new InternetAddress(to) ;
            message.addRecipient(Message.RecipientType.TO, toAddress) ;
            // The Subject
            message.setSubject(subject) ;
            // Now the message body.
            message.setContent(messageBody, "text/html") ;
            // Finally, send the message!
            Transport.send(message) ;
        }
        catch(MessagingException ex){

            throw ex ;
        }

        return true ;
    }

}
