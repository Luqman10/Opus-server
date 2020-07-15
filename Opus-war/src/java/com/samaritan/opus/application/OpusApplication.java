package com.samaritan.opus.application;

import com.samaritan.opus.service.AlbumResource;
import com.samaritan.opus.service.ArtisteResource;
import com.samaritan.opus.service.DocumentaryResource;
import com.samaritan.opus.service.FollowingResource;
import com.samaritan.opus.service.MovieResource;
import com.samaritan.opus.service.MusicVideoResource;
import com.samaritan.opus.service.NotificationResource;
import com.samaritan.opus.service.PaymentAccountResource;
import com.samaritan.opus.service.ProfileAccountResource;
import com.samaritan.opus.service.PropertiesResource;
import com.samaritan.opus.service.SongResource;
import com.samaritan.opus.service.SeriesResource ;
import com.samaritan.opus.service.VideoService;
import com.samaritan.opus.util.HibernateUtil ;

import javax.ws.rs.ApplicationPath;
import javax.ws.rs.core.Application;
import java.util.HashSet;
import java.util.Set;
import javax.servlet.ServletContext;
import javax.ws.rs.core.Context;


@ApplicationPath("/opus")
public class OpusApplication extends Application{
    
    //name for the hibernet session factory attribute
    public static final String HIBERNATE_SESSION_FACTORY = "hibernate_session_factory" ;
    
    //servlet context
    @Context
    protected ServletContext servletContext ;

    @Override
    public Set<Class<?>> getClasses(){

        HashSet<Class<?>> hashSet = new HashSet<>() ;
        hashSet.add(ProfileAccountResource.class) ;
        hashSet.add(PaymentAccountResource.class) ;
        hashSet.add(ArtisteResource.class) ;
        hashSet.add(FollowingResource.class) ;
        hashSet.add(SongResource.class) ;
        hashSet.add(AlbumResource.class) ;
        hashSet.add(MusicVideoResource.class) ;
        hashSet.add(MovieResource.class) ;
        hashSet.add(DocumentaryResource.class) ;
        hashSet.add(SeriesResource.class) ;
        hashSet.add(VideoService.class) ;
        hashSet.add(NotificationResource.class) ;
        hashSet.add(PropertiesResource.class) ;
        return hashSet ;
    }
    
    @Override
    public Set<Object> getSingletons(){
        
        //add a hibernate session factory to the servlet context as an attribute
        servletContext.setAttribute(HIBERNATE_SESSION_FACTORY, HibernateUtil.getSessionFactory()) ;
        
        return new HashSet<>() ;
    }
}
