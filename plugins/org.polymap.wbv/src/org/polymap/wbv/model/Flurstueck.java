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

import javax.annotation.Nullable;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.polymap.core.model2.Association;
import org.polymap.core.model2.Composite;
import org.polymap.core.model2.Property;
import org.polymap.core.model2.Queryable;

import org.polymap.wbv.mdb.ImportColumn;
import org.polymap.wbv.mdb.ImportTable;

/**
 * 
 *
 * @author <a href="http://www.polymap.de">Falko Bräutigam</a>
 */
@ImportTable("FL_Flurstück")
public class Flurstueck
        extends Composite {

    private static Log log = LogFactory.getLog( Flurstueck.class );
    
//    FL_Flurstück (18076 Datensätze)
//    0|ID_WBS                         (LONG 4)                  
//    1|ID_FL                          (LONG 4)                  
//    2|FL_Gemeinde                    (INT 2)                   
//    3|FL_Gemarkung                   (INT 2)                   
//    4|FL_Flur                        (TEXT 22)                 
//    5|FL_Flstnr                      (TEXT 20)                 
//    6|FL_Flae                        (DOUBLE 8)                
//    7|FL_davon_Wald                  (DOUBLE 8)                
//    8|FL_Datum_Eingabe               (SHORT_DATE_TIME 8)       
//    9|FL_Karte                       (TEXT 20)                 
//   10|FL_FoA                         (INT 2)                   
//   11|FL_Rev                         (INT 2)                   
//   12|FL_Bemerkung                   (MEMO 0)                  
//   13|FL_WT                          (TEXT 8)                  
//   14|FL_Feld1                       (TEXT 100)                
//   15|FL_Feld2                       (TEXT 100)                
//   16|FL_ID_Alt                      (LONG 4)                  
//   17|FL_MEAnteil                    (LONG 4)                  
//   18|FL_LK                          (TEXT 200)                
//   19|FL_GEMAFLUR                    (TEXT 200)
    
    //@ImportColumn("FL_Gemeinde")
    public Association<Gemeinde>        gemeinde;
    
    /**
     * <p/>
     * Im Import leider Flurstücke ohne Gemarkung, deshalb {@link Nullable}.
     */
    //@ImportColumn("FL_Gemarkung")
    @Nullable
    public Association<Gemarkung>       gemarkung;

    @Nullable
    @ImportColumn("FL_Flstnr")
    public Property<String>             zaehlerNenner;
    
    @Nullable
    @ImportColumn("FL_Flae")
    public Property<Double>             flaeche;

    @Nullable
    @ImportColumn("FL_davon_Wald")
    public Property<Double>             flaecheWald;

    @Queryable
    @Nullable
    @ImportColumn("FL_Bemerkung")
    public Property<String>             bemerkung;

    @Queryable
    @Nullable
    @ImportColumn("FL_Datum_Eingabe")
    public Property<String>             eingabe;

    @Nullable
    @ImportColumn("FL_LK")
    public Property<String>             landkreis;

}
