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
import org.polymap.model2.runtime.UnitOfWork;
import org.polymap.wbv.mdb.ImportColumn;
import org.polymap.wbv.mdb.ImportTable;

/**
 * 
 *
 * @author <a href="http://www.polymap.de">Falko Bräutigam</a>
 */
@ImportTable("ESt_Gemeinde")
public class Gemeinde
        extends Entity {

    private static Log log = LogFactory.getLog( Gemeinde.class );
    
    public static Map<String,Gemeinde> all( UnitOfWork uow ) {
        Map<String,Gemeinde> result = new TreeMap();
        for (Gemeinde gemeinde : uow.query( Gemeinde.class ).execute()) {
            result.put( gemeinde.name.get(), gemeinde );
        }
        return result;
    }

    
    // instance *******************************************
    
//    ESt_Gemeinde (60 Datensätze)
//    0|ID_Gemeinde                    (LONG 4)                  
//    1|Gemeinde_Name                  (TEXT 100)

    @Queryable
    @ImportColumn("Gemeinde_Name")
    public Property<String>             name;
    
}
