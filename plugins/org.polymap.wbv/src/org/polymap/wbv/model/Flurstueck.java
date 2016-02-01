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

import java.util.Date;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.polymap.model2.Association;
import org.polymap.model2.Composite;
import org.polymap.model2.Defaults;
import org.polymap.model2.Nullable;
import org.polymap.model2.Property;
import org.polymap.model2.Queryable;
import org.polymap.model2.runtime.ValueInitializer;
import org.polymap.model2.runtime.config.DefaultDouble;
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
    
    public static Flurstueck            TYPE;
    
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

    public static final ValueInitializer<Flurstueck> defaults = (Flurstueck proto) -> {
        proto.landkreis.set( "Mittelsachsen" );
        Date now = new Date();
        proto.eingabe.set( now );
        proto.aenderung.set( now );
        return proto;
    };
    
    // instance *******************************************
    
    @Queryable
    @Defaults
    public Property<Boolean>            geloescht;

    /**
     * Im Import leider Flurstücke ohne Gemarkung, deshalb {@link Nullable}. Id ist
     * Gemarkungsschlüssel.
     */
    //@ImportColumn("FL_Gemarkung")
    @Nullable
    public Association<Gemarkung>       gemarkung;

    //@Queryable: wird behandelt in WaldbesitzerFulltextTransformer
    @Nullable
    @ImportColumn("FL_Flstnr")
    public Property<String>             zaehlerNenner;
    
    @Nullable
    @DefaultDouble( 0 )
    @ImportColumn("FL_Flae")
    public Property<Double>             flaeche;

    @Nullable
    @DefaultDouble( 0 )
    @ImportColumn("FL_davon_Wald")
    public Property<Double>             flaecheWald;

    @Queryable
    @Nullable
    @ImportColumn("FL_Bemerkung")
    public Property<String>             bemerkung;

    @Nullable
    @ImportColumn("FL_Datum_Eingabe")
    public Property<Date>               eingabe;

    @Nullable
    public Property<Date>               aenderung;

    @Nullable
    @ImportColumn("FL_LK")
    public Property<String>             landkreis;

    /**
     * 
     */
    public Waldbesitzer waldbesitzer() {
        return context.getEntity();
    }
}
