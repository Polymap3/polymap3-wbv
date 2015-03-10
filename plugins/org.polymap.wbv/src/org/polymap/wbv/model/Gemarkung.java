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

import com.google.common.base.Supplier;

import org.polymap.core.model2.Entity;
import org.polymap.core.model2.Property;
import org.polymap.core.model2.Queryable;
import org.polymap.core.model2.runtime.ModelRuntimeException;
import org.polymap.core.model2.runtime.UnitOfWork;
import org.polymap.core.runtime.CachedLazyInit;
import org.polymap.core.runtime.Lazy;

import org.polymap.rhei.field.PicklistFormField;

import org.polymap.wbv.mdb.ImportTable;
import org.polymap.wbv.ui.FlurstueckTableViewer;

/**
 * {@link #id()} ist der Gemarkungsschlüssel.
 *
 * @author <a href="http://www.polymap.de">Falko Bräutigam</a>
 */
@ImportTable("ESt_Gemarkung")
public class Gemarkung
        extends Entity {

    private static Log log = LogFactory.getLog( Gemarkung.class );
    
    public static Gemarkung         TYPE = null;

    /** {@link #label()} -> Gemarkung */
    public static Lazy<Map<String,Gemarkung>> all = new CachedLazyInit( 1000, new Supplier<Map<String,Gemarkung>>() {
        public Map<String,Gemarkung> get() {
            UnitOfWork uow = WbvRepository.instance.get().newUnitOfWork();
            Map<String,Gemarkung> result = new TreeMap();
            for (Gemarkung gmk : uow.query( Gemarkung.class ).execute()) {
                try {
                    result.put( gmk.label(), gmk );
                }
                catch (ModelRuntimeException e) {
                    log.warn( "Gemarkung ohne Namen: " + gmk );
                }
            }
            return result;
        }
    }); 

    
    // instance *******************************************

//  ESt_Gemarkung (433 Datensätze)
//  0|ID_Gemarkung                   (LONG 4)                  
//  1|Gemarkung_Name                 (TEXT 100)
  
    /**
     * Der Primärschlüssel - ist der Gemarkungsschlüssel.
     */
    @Override
    public Object id() {
        return super.id();
    }

    @Queryable
    public Property<String>             gemarkung;
    
    @Queryable
    public Property<String>             gemeinde;
    
    @Queryable
    public Property<String>             revier;

    public String label() {
        return gemeinde.get() + "/" + gemarkung.get();
    }

    
    /**
     * Entities sind (bis jetzt) nur gleich, wenn sie "same" sind. Dadurch würde in
     * {@link FlurstueckTableViewer} der Wert im {@link PicklistFormField} für die
     * Gemarkung nicht gefunden.
     */
    @Override
    public boolean equals( Object obj ) {
        if (obj instanceof Gemarkung) {
            return id().equals( ((Gemarkung)obj).id() );
        }
        return false;
    }

    @Override
    public int hashCode() {
        return id().hashCode();
    }
    
}
