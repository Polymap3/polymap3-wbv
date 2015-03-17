/*
 * polymap.org Copyright (C) 2014 Polymap GmbH. All rights reserved.
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

import com.vividsolutions.jts.geom.MultiPolygon;

import org.polymap.model2.Entity;
import org.polymap.model2.Property;
import org.polymap.model2.Queryable;
import org.polymap.model2.query.Expressions;
import org.polymap.model2.query.Query;

/**
 * Siehe <a href="http://polymap.org/wbv/wiki/Konzept#r4r">Spezifikation</a>.
 * 
 * @author <a href="http://www.polymap.de">Falko Bräutigam</a>
 */
//@SRS("EPSG:4326")
public class Revier
        extends Entity {

    @Queryable
    public Property<MultiPolygon>       geom;
    
    @Queryable
    public Property<String>             name;
    
    public Property<Kontakt>            revierleiter;

    
    /**
     * Andere Seite der {@link Waldstueck#revier} Assoziation.  
     */
    public Query<Waldstueck> waldstuecke() {
        Waldstueck wanted = Expressions.template( Waldstueck.class, context.getRepository() );
        return context.getUnitOfWork()
                .query( Waldstueck.class )
                .where( Expressions.is( wanted.revier, this ) );    
    }

}
