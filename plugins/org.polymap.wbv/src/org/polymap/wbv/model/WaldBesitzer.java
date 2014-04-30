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

import javax.annotation.Nullable;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.polymap.core.model2.Entity;
import org.polymap.core.model2.Property;

/**
 * 
 * 
 * @author <a href="http://www.polymap.de">Falko Bräutigam</a>
 */
//@SRS("EPSG:31468")
public class WaldBesitzer
        extends Entity {

    private static Log      log = LogFactory.getLog( WaldBesitzer.class );

    /**
     * Der Vorname einer natürlichen Person.
     */
    @Nullable
    public Property<String> vorname;

    /**
     * Der Familienname einer natürlichen Person.
     */
    @Nullable
    public Property<String> name;

}
