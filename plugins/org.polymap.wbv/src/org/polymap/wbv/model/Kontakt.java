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

import javax.annotation.Nullable;

import com.google.common.base.Joiner;

import org.polymap.core.model2.Property;
import org.polymap.core.model2.Queryable;

/**
 * 
 * 
 * @author <a href="http://www.polymap.de">Falko Bräutigam</a>
 */
public class Kontakt
        extends Adresse {

    public enum Anrede {
        Herr, Frau;
    }
    
    /**
     * Der Familienname einer natürlichen Person. Freitext für Namen einschließlich
     * Adelstitel 'Wagner', 'von Lüttichau', usw. )
     */
    @Nullable
    @Queryable
    public Property<String> name;

    /**
     * Die Anrede inklusive Titel: "Herr", "Frau", "Frau Dr." (defaults:
     * {@link Anrede}).
     */
    @Nullable
    @Queryable
    public Property<String> anrede;

    /**
     * Der Vorname bei einer natürlichen Person.
     */
    @Nullable
    @Queryable
    public Property<String> vorname;

    @Nullable
    @Queryable
    public Property<String> organisation;

    @Nullable
    public Property<String> telefon;

    /** Mobilfunknummer */
    @Nullable
    public Property<String> mobil;

    @Nullable
    public Property<String> fax;

    @Nullable
    public Property<String> email;

    /** Zusätzliche Bemerkungen zu diesem Kontakt. */
    @Nullable
    public Property<String> bemerkung;

    
    /**
     * Berechneter Anzeigename. Berechnungsvorschrift: (Organisation des Kontakts,
     * falls die nicht vorhanden ist: Titel+Vorname+Familienname )
     */
    public String anzeigename() {
        return organisation.get() != null
                ? organisation.get()
                : Joiner.on( ' ' ).skipNulls().join( anrede.get(), vorname.get(), name.get() );
    }
    
}
