/*
 * Copyright (C) 2014-2015 Polymap GmbH. All rights reserved.
 * 
 * This is free software; you can redistribute it and/or modify it under the terms of
 * the GNU Lesser General Public License as published by the Free Software
 * Foundation; either version 3.0 of the License, or (at your option) any later
 * version.
 * 
 * This software is distributed in the hope that it will be useful, but WITHOUT ANY
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A
 * PARTICULAR PURPOSE. See the GNU Lesser General Public License for more details.
 */
package org.polymap.wbv.model;

import static org.polymap.core.model2.query.Expressions.is;
import static org.polymap.core.model2.query.Expressions.template;

import java.util.HashMap;
import java.util.Map;

import com.google.common.collect.Iterables;

import org.polymap.core.model2.CollectionProperty;
import org.polymap.core.model2.Defaults;
import org.polymap.core.model2.Entity;
import org.polymap.core.model2.MinOccurs;
import org.polymap.core.model2.Property;
import org.polymap.core.model2.Queryable;
import org.polymap.core.model2.query.Query;
import org.polymap.core.model2.store.feature.SRS;

import org.polymap.wbv.mdb.ImportColumn;
import org.polymap.wbv.mdb.ImportTable;

/**
 * 
 * 
 * @author <a href="http://www.polymap.de">Falko Bräutigam</a>
 */
@SRS("EPSG:4326")
@ImportTable("Waldbesitzer")
public class Waldbesitzer
        extends Entity {
    
    public static Waldbesitzer          TYPE;
    
//    0|ID_WBS                         (LONG 4)                  
//    1|WBS_Name                       (TEXT 100)                
//    2|WBS_Vorname                    (TEXT 100)                
//    3|WBS_Waldbesitzer               (BOOLEAN 1)               
//    4|WBS_Pächter                    (BOOLEAN 1)               
//    5|WBS_FBG                        (BOOLEAN 1)               
//    6|WBS_FBG_Nr                     (LONG 4)                  
//    7|WBS_EA                         (TEXT 6)                  
//    8|WBS_Bemerkung                  (MEMO 0)                  
//    9|WBS_Feld1                      (TEXT 100)                
//   10|WBS_Feld2                      (TEXT 100)                
//   11|WBS_ID_Alt                     (LONG 4)                  
//   12|WBS_Papierkorb                 (BOOLEAN 1)               
//   13|WBS_Betreuung_ständig          (BOOLEAN 1)               
//   14|WBS_FBNr                       (LONG 4)
   
    public enum Waldeigentumsart {
        Staat_Sachsen( "Staatswald", "Freistaat Sachsen" ),
        Staat_Bund( "Staatswald", "BRD" ),
        Körperschaft_Kommune( "Körperschaftswald", "Kommunen" ),
        Körperschaft_ZVB( "Körperschaftswald", "Zweckverbände" ),
        Kirche( "Kirchenwald", null ),
        Privat( "Privatwald", null ),
        /** Nach dem Import kein bekannter Wert */
        T( "Nicht bestimmt (Code: T)", null ),
        B( "Nicht bestimmt (Code: B)", null ),
        A( "Nicht bestimmt (Code: A)", null ),
        L( "Nicht bestimmt (Code: L)", null ),
        Unbekannt( "Unbekannt", null );
        
        private String      name;
        private String      zusatz;
        
        private Waldeigentumsart( String name, String zusatz ) {
            this.name = name;
            this.zusatz = zusatz;
        }
        
        public String label() {
            return name + (zusatz != null ? " / " + zusatz : "");
        }

        public static Map<String,Waldeigentumsart> map() {
            Map<String,Waldeigentumsart> result = new HashMap();
            for (Waldeigentumsart elm : Waldeigentumsart.values()) {
                result.put( elm.label(), elm );
            }
            return result;
        }
    }

    @Queryable
    public Property<Waldeigentumsart>   eigentumsArt;

    @Defaults
    @Queryable
    @ImportColumn("WBS_Bemerkung")
    public Property<String>             bemerkung;

    @Defaults
    @Queryable
    @ImportColumn("WBS_Pächter")
    public Property<Boolean>            pächter;

    @Defaults
    @Queryable
    @ImportColumn("WBS_Papierkorb")
    public Property<Boolean>            gelöscht;
    
    public CollectionProperty<Flurstueck> flurstuecke;

    /**
     * Alle Ansprechpartner, inklusive des {@link #besitzer()}s auf Index
     * {@link #besitzerIndex}.
     */
    @MinOccurs(1)
    public CollectionProperty<Kontakt>  kontakte;

    @Defaults
    public Property<Integer>            besitzerIndex;

    public CollectionProperty<Ereignis> ereignisse;
    

    public Kontakt besitzer() {
        return Iterables.get( kontakte, besitzerIndex.get(), null );
    }

    /**
     * Andere Seite der {@link Waldstueck#waldbesitzer} Assoziation.
     */
    public Query<Waldstueck> waldstuecke() {
        Waldstueck wanted = template( Waldstueck.class, context.getRepository() );
        return context.getUnitOfWork().query( Waldstueck.class )
                .where( is( wanted.waldbesitzer, this ) );
    }

}
