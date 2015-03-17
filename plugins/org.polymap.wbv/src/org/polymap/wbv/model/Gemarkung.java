/* 
 * Copyright (C) 2014, Falko Bräutigam. All rights reserved.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 3.0 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 */
package org.polymap.wbv.model;

import java.util.Map;
import java.util.TreeMap;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.polymap.model2.Entity;
import org.polymap.model2.Property;
import org.polymap.model2.Queryable;
import org.polymap.model2.runtime.ModelRuntimeException;
import org.polymap.model2.runtime.UnitOfWork;
import org.polymap.wbv.mdb.ImportColumn;
import org.polymap.wbv.mdb.ImportTable;

/**
 * 
 *
 * @author <a href="http://www.polymap.de">Falko Bräutigam</a>
 */
@ImportTable("ESt_Gemarkung")
public class Gemarkung
        extends Entity {

    private static Log log = LogFactory.getLog( Gemarkung.class );
    
    public static Map<String,Gemarkung> all( UnitOfWork uow ) {
        Map<String,Gemarkung> result = new TreeMap();
        for (Gemarkung gemarkung : uow.query( Gemarkung.class ).execute()) {
            try {
                result.put( gemarkung.name.get(), gemarkung );
            }
            catch (ModelRuntimeException e) {
                log.warn( "Gemarkung ohne Namen: " + gemarkung );
            }
        }
        return result;
    }

    // instance *******************************************
    
//    ESt_Gemarkung (433 Datensätze)
//    0|ID_Gemarkung                   (LONG 4)                  
//    1|Gemarkung_Name                 (TEXT 100)
    
    @Queryable
    @ImportColumn("Gemarkung_Name")
    public Property<String>             name;
    
}
