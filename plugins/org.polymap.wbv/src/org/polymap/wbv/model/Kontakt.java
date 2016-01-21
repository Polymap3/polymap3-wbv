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

import static org.apache.commons.lang3.StringUtils.isEmpty;

import com.google.common.base.Joiner;

import org.polymap.model2.Defaults;
import org.polymap.model2.Nullable;
import org.polymap.model2.Property;
import org.polymap.model2.Queryable;
import org.polymap.wbv.mdb.ImportColumn;
import org.polymap.wbv.mdb.ImportTable;

/**
 * 
 * 
 * @author <a href="http://www.polymap.de">Falko Bräutigam</a>
 */
@ImportTable("Waldbesitzer_Adresse")
public class Kontakt
        extends Adresse {

    public enum Anrede {
        Herr, Frau;
    }
    
    /**
     * Der Familienname einer natürlichen Person. Freitext für Namen einschließlich
     * Adelstitel 'Wagner', 'von Lüttichau', usw. )
     */
    @Defaults
    @Queryable
    @ImportColumn("EWBS_Name")
    public Property<String> name;

    /**
     * Die Anrede inklusive Titel: "Herrn", "Frau", "Frau Dr." (defaults: {@link Anrede}).
     */
    @Defaults
    @Queryable
    @ImportColumn("EWBS_Anrede")
    public Property<String> anrede;

    @Defaults
    @ImportColumn("EWBS_Briefanrede")
    public Property<String> briefanrede;

    /**
     * Der Vorname bei einer natürlichen Person.
     */
    @Defaults
    @Queryable
    @ImportColumn("EWBS_Vorname")
    public Property<String> vorname;

    @Defaults
    @Queryable
    public Property<String> organisation;

    @Defaults
    @Queryable
    @ImportColumn("EWBS_Telefon1")
    public Property<String> telefon1;

    @Defaults
    @Queryable
    @ImportColumn("EWBS_Telefon2")
    public Property<String> telefon2;

    @Defaults
    @Queryable
    @ImportColumn("EWBS_Fax")
    public Property<String> fax;

    @Defaults
    @ImportColumn("EWBS_EMail")
//    @Label("E-Mail", "Die E-Mail-Adresse des kontaktes. Beispiel: info@example.com")
    public Property<String> email;

    /** Zusätzliche Bemerkungen zu diesem Kontakt. */
    @Defaults
    @Queryable
    public Property<String> bemerkung;

    /** Aus WKV-Daten, Bedeutung ist unklar. */
    @Nullable
    @ImportColumn("EWBS_BetrNr")
    public Property<String> betrNr;

    
    /**
     * Berechneter Anzeigename. Berechnungsvorschrift: (Organisation des Kontakts,
     * falls die nicht vorhanden ist: Familienname, Vorname )
     */
    public String anzeigename() {
        if (!isEmpty( organisation.get() )) {
            return organisation.get();
        }
        else if (isEmpty( name.get() ) && isEmpty( vorname.get() ) ) {
            return "[Besitzerdaten fehlen]";
        }
        else {
            return Joiner.on( "" ).skipNulls().join( /*anrede.get(),*/ name.get(), ", ", vorname.get() );
        }
    }
    
}
