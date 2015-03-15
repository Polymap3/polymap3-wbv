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

import org.polymap.core.model2.Composite;
import org.polymap.core.model2.DefaultValue;
import org.polymap.core.model2.Defaults;
import org.polymap.core.model2.Property;
import org.polymap.core.model2.Queryable;

import org.polymap.wbv.mdb.ImportColumn;

/**
 * 
 * 
 * @author <a href="http://www.polymap.de">Falko Bräutigam</a>
 */
public class Adresse
        extends Composite {

    @Defaults
    @Queryable
    @ImportColumn("EWBS_ORT")
    public Property<String> ort;

    @Defaults
    @Queryable
    @ImportColumn("EWBS_Ortsteil")
    public Property<String> ortsteil;

    @Queryable
    @DefaultValue("BRD")
    public Property<String> land;

    /** Strasse, inklusive Hausnummer. */
    @Defaults
    @Queryable
    @ImportColumn("EWBS_Straße")
    public Property<String> strasse;

    @Defaults
    @Queryable
    @ImportColumn("EWBS_PLZ")
    public Property<String> plz;

}
