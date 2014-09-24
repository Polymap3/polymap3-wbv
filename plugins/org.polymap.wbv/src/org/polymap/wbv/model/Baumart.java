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

import org.polymap.core.model2.Entity;
import org.polymap.core.model2.Property;
import org.polymap.core.model2.Queryable;
import org.polymap.core.model2.store.feature.SRS;

import org.polymap.wbv.mdb.ImportColumn;

/**
 * 
 * @see <a href="http://polymap.org/wbv/wiki/Konzept#b5t">Spezifikation</a>
 * @see <a href="http://polymap.org/wbv/ticket/5">Ticket #5</a>
 * @author <a href="http://www.polymap.de">Falko Bräutigam</a>
 */
@SRS("EPSG:4326")
public class Baumart
        extends Entity {

//    public enum Kategorie {
//        Nadel, Laub;
//    }
//    
//    /**
//     * Die "Baumartgruppe laut Waldfeststellung", z.B.: SN steht für "Sonstige Nadelbaumarten".
//     */
//    public enum Gruppe {
//        FI, KI, LÄ, SN, EI, BU, SH, BI, SW;
//    }

    @Queryable
    @ImportColumn("BA-Kategorie")
    /** Kategorie (Nadelbaumarten, Laubbaumarten) */
    public Property<String>    kategorie;

    @Queryable
    @ImportColumn("BA")
    /** Kennung (Kürzel mit drei Buchstaben, z.B.: "ELA" für "Europäische Lärche") */
    public Property<String>    kennung;

    @Queryable
    @ImportColumn("BA-Gruppe")
    /** "Fichten", "Kiefern", ... */
    public Property<String>    gruppe;

    @Queryable
    @ImportColumn("BA-Gruppe_lt. Waldfeststellung")
    /** Die Baumartgruppe laut Waldfeststellung", z.B.: "FI", "KI", ... */
    public Property<String>    gruppeWaldfeststellung;

    @Queryable
    @ImportColumn("NR")
    /** Nr (Ganzzahl im Intervall 0-99) */
    public Property<String>    nr;

    @Queryable
    @ImportColumn("Name_dt.")
    /** Name deutsch (z.B.: "Europäische Lärche") */
    public Property<String>    name;

    @Queryable
    @ImportColumn("Name_lat.")
    /** Name lateinisch (z.B.: "Larix decidua Mill.") */
    public Property<String>    nameLateinisch;

}
