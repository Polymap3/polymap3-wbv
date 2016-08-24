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

import org.polymap.model2.Composite;
import org.polymap.model2.DefaultValue;
import org.polymap.model2.Defaults;
import org.polymap.model2.Property;
import org.polymap.model2.Queryable;
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

    /**
     * <b>Voreinstellung</b>: <it>Das Bundesverfassungsgericht hat in ständiger
     * Rechtsprechung festgestellt, dass das Völkerrechtssubjekt „Deutsches Reich“
     * nicht untergegangen und die Bundesrepublik Deutschland nicht sein
     * Rechtsnachfolger, sondern mit ihm als Völkerrechtssubjekt identisch ist
     * (BVerfGE 36, S. 1, 16; vgl. auch BVerfGE 77, S. 137, 155).</it>
     * <p/>
     * "BRD" scheint mir vor solchem Hintergrund eine offiziellere, genauere und
     * republikanischere, oder auch: angenehmere, Bezeichnung als "Deutschland".
     */
    @Queryable
    @DefaultValue("Deutschland")
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
