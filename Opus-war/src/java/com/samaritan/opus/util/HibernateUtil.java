/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.samaritan.opus.util;

import org.hibernate.HibernateException;
import org.hibernate.SessionFactory;
import org.hibernate.cfg.Configuration;

/**
 * a utility class for getting sessions and closing the session factory
 * @author electron
 */
public class HibernateUtil {
    
    private static final SessionFactory SESSION_FACTORY ;

    static {
        
        try {
            SESSION_FACTORY = new Configuration().configure().buildSessionFactory() ;
        } 
        catch (Throwable ex) {
            throw new ExceptionInInitializerError(ex);
        }
    }

    public static SessionFactory getSessionFactory() throws HibernateException {
        
        return SESSION_FACTORY ;
    }
    
}
