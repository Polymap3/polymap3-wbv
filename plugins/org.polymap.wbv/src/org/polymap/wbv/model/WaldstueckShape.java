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

import org.polymap.core.model2.Entity;
import org.polymap.core.model2.NameInStore;
import org.polymap.core.model2.Property;
import org.polymap.core.model2.Queryable;
import org.polymap.core.model2.store.feature.SRS;

/**
 * Importierte Daten aus fgd_we_f.shp.
 * 
 * @author <a href="http://www.polymap.de">Falko Bräutigam</a>
 */
@SRS("EPSG:31468")
@NameInStore("fgd_we_f")
public class WaldstueckShape
        extends Entity {

    @Queryable
    public Property<MultiPolygon>       geom;
    
    /** Boolean? */
    @Queryable
    @NameInStore("WALD")
    public Property<Integer>            wald;

    /** Boolean? */
    @Queryable
    @NameInStore("EIG")
    public Property<Integer>            eigentum;

    @Queryable
    @NameInStore("EIG_ART")
    public Property<String>             eigentumsart;

    @Queryable
    @NameInStore("WT")
    public Property<String>             waldteil;

    @Queryable
    @NameInStore("ABT")
    public Property<Integer>            abteilung;

    @Queryable
    @NameInStore("UABT")
    public Property<String>             unterabteilung;

    @Queryable
    @NameInStore("TFL")
    public Property<Integer>            teilflaeche;

}
