/*
 * Copyright (C) 2014 Polymap GmbH. All rights reserved.
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

import org.polymap.model2.Association;
import org.polymap.model2.CollectionProperty;
import org.polymap.model2.Entity;
import org.polymap.model2.Property;
import org.polymap.model2.Queryable;

/**
 * Siehe <a href="http://polymap.org/wbv/wiki/Konzept#wk7">Spezifikation</a>.
 * 
 * @author <a href="http://www.polymap.de">Falko Bräutigam</a>
 */
//@SRS("EPSG:4326")
public class Waldstueck
        extends Entity {

    @Queryable
    public Property<MultiPolygon>               geom;
    
    @Queryable
    public Association<Waldbesitzer>            waldbesitzer;
    
    @Queryable
    public Association<Revier>                  revier;
    
    @Queryable
    public CollectionProperty<BaumartEintrag>   baumarten;
    
    /**
     * 
     */
    public class BaumartEintrag
            extends Entity {
        
        public Property<Integer>        anteil;
        
        @Queryable
        public Association<Baumart>     baumart;
    }

}
