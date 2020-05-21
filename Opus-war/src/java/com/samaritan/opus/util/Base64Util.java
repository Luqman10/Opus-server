/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.samaritan.opus.util;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Base64;

/**
 *This class has a static method for converting a file to its corresponding Base64 representation
 * @author electron
 */
public class Base64Util {
    
    /**
     * convert file to its corresponding Base64 representation
     * @param file the file to convert
     * @return the base 64 string representation
     * @throws java.io.FileNotFoundException
     */
    public static String convertFileToBase64(File file) throws FileNotFoundException,IOException{
        
        String base64Representation = null ;
        
            try (FileInputStream fileInputStream = new FileInputStream(file)) {
                byte[] bytes = new byte[(int)file.length()] ;
                fileInputStream.read(bytes) ;
                Base64.Encoder encoder = Base64.getEncoder() ;
                base64Representation = encoder.encodeToString(bytes) ;
            }
            catch (FileNotFoundException ex) {
                // TODO Auto-generated catch block
                throw ex ;
            } 
            catch (IOException ex) {
                // TODO Auto-generated catch block
                throw ex ;
            }

            return base64Representation ;
    }
}
