/*
 * polymap.org 
 * Copyright (C) 2014-2014 Polymap GmbH. All rights reserved.
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

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.function.Supplier;

import org.polymap.core.runtime.CachedLazyInit;
import org.polymap.core.runtime.Lazy;

import org.polymap.model2.query.ResultSet;
import org.polymap.model2.runtime.UnitOfWork;

/**
 * Siehe <a href="http://polymap.org/wbv/wiki/Konzept#r4r">Spezifikation</a>.
 * <p/>
 * Revier ist keine eigenen Entity, sondern wird aus den Gemarkungen extrahiert.
 * {@link Gemarkung} wird extra importiert und enthält die Information welche
 * Gemarkungen zu einem Reviert gehören.
 * 
 * @author <a href="http://www.polymap.de">Falko Bräutigam</a>
 */
public class Revier {

    /**
     * Alle Reviere: Reviername -> {@link Revier}
     */
    public static Lazy<Map<String,Revier>> all = new CachedLazyInit( new Supplier<Map<String,Revier>>() {
        public Map<String,Revier> get() {
            UnitOfWork uow = WbvRepository.instance.get().newUnitOfWork();
            Map<String,Revier> result = new TreeMap();
            ResultSet<Gemarkung> gmks = uow.query( Gemarkung.class ).execute();
            for (Gemarkung gemarkung : gmks) {
                String name = gemarkung.revier.get();
                Revier revier = result.get( name );
                if (revier == null) {
                    result.put( name, revier = new Revier( name ) );
                }
                revier.gemarkungen.add( gemarkung );
            }
            return Collections.unmodifiableMap( result );
        }});
    
    
    public static final Revier      UNKNOWN = new Revier( "Alle" );
    
    
    // istance ********************************************

    public String                   name;
    
    public List<Gemarkung>          gemarkungen = new ArrayList( 128 );
    
    
    protected Revier( String name ) {
        assert name != null;
        this.name = name;
    }

}
