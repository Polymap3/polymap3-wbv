/* 
 * Copyright (C) 2014, Falko Bräutigam. All rights reserved.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 */
package org.polymap.wbv;

import java.util.Locale;
import java.util.ResourceBundle;

import org.polymap.core.runtime.i18n.IMessages;
import org.polymap.core.runtime.i18n.MessagesImpl;

/**
 * 
 * 
 * @author <a href="http://www.polymap.de">Falko Bräutigam</a> 
 */
public class Messages {

    private static final String BUNDLE_NAME = "org.polymap.wbv.messages";
    
    private static final MessagesImpl   instance = new MessagesImpl( BUNDLE_NAME, Messages.class.getClassLoader() );

    
    private Messages() {
        // prevent instantiation
    }

    public static String get( String key, Object... args ) {
        return instance.get( key, args );
    }

    public static String get2( Object caller, String key, Object... args ) {
        return instance.get( caller, key, args );
    }

//    public static Messages get() {
//        Class clazz = Messages.class;
//        return (Messages)RWT.NLS.getISO8859_1Encoded( BUNDLE_NAME, clazz );
//    }

    public static IMessages forPrefix( String prefix ) {
        return instance.forPrefix( prefix );
    }

    public static ResourceBundle resourceBundle() {
        return instance.resourceBundle( Locale.GERMAN );
    }

}
