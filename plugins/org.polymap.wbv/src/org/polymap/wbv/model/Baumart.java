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

import org.polymap.core.model2.Entity;
import org.polymap.core.model2.Property;
import org.polymap.core.model2.Queryable;
import org.polymap.core.model2.store.feature.SRS;

/**
 * Siehe <a href="http://polymap.org/wbv/wiki/Konzept#b5t">Spezifikation</a>.
 * 
 * @author <a href="http://www.polymap.de">Falko Bräutigam</a>
 */
@SRS("EPSG:4326")
/*
 * See: http://polymap.org/wbv/ticket/4
 */
public class Baumart
        extends Entity {

    public enum Kategorie {
        Nadel, Laub;
    }


    /**
     * Die Gruppe lässt sich aus der Gruppe laut Waldfeststellung ableiten. z.B.: SN
     * steht für "Sonstige Nadelbaumarten"
     */
    public enum Gruppe_lt_Waldfeststellung {
        FI, KI, LÄ, SN, EI, BU, SH, BI, SW;
    }

    @Queryable
    /** Kennung (Kürzel mit drei Buchstaben, z.B.: "ELA" für "Europäische Lärche") */
    public Property<String>    kennung;

    @Queryable
    /** Nr (Ganzzahl im Intervall 0-99) */
    public Property<Integer>   nr;

    @Queryable
    /** Name deutsch (z.B.: "Europäische Lärche") */
    public Property<String>    nameDeutsch;

    @Queryable
    /** Name lateinisch (z.B.: "Larix decidua Mill.") */
    public Property<String>    nameLateinisch;

    @Queryable
    /** Kategorie (Nadelbaumarten, Laubbaumarten) */
    public Property<Kategorie> kategorie;

}
