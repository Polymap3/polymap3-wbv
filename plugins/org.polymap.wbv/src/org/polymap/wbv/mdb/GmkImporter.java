/* 
 * polymap.org
 * Copyright (C) 2015-2016 Polymap GmbH. All rights reserved.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 3.0 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 */
package org.polymap.wbv.mdb;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import org.supercsv.io.CsvListReader;
import org.supercsv.prefs.CsvPreference;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;

import org.polymap.wbv.model.Flurstueck;
import org.polymap.wbv.model.Gemarkung;

/**
 * Gemarkungsschlüssel aus {@link #FILE_GEMARKUNGEN}.
 *
 * @author <a href="http://www.polymap.de">Falko Bräutigam</a>
 */
public class GmkImporter {

    /**
     * Nur beim Import erforderlich. Enthält die Gemarkungs- und Gemeindeschlüssel,
     * im WKV an den Flurstücken sind. Es ermöglicht also beim Import die Zuordnung
     * von {@link Flurstueck} zu {@link Gemarkung}.
     */
    public static final String  FILE_GEMARKUNGEN = "Gemarkungen-wkv.csv";
    

    // instance *******************************************

    /** "gemeindeId/gemarkungId" -> gmkschl, siehe {@link #FILE_GEMARKUNGEN} */
    private Map<Pair<String,String>,String>  gmkMapping = new TreeMap();
    

    public GmkImporter() throws IOException {
        CsvPreference prefs = new CsvPreference( '"', ',', "\r\n" ); // quoteChar, delimiterChar, endOfLineSymbols
        
        // gmkMapping
        try (
            FileInputStream in = new FileInputStream( new File( WvkImporter.BASEDIR, FILE_GEMARKUNGEN ) )
        ){
            CsvListReader csv = new CsvListReader( new InputStreamReader( in, "UTF-8" ), prefs );

            for (List<String> l = csv.read(); l != null; l = csv.read()) {
                final String[] line = l.toArray( new String[l.size()] );
                String id = line[2];
                if (!StringUtils.isNumeric( id )) {
                    System.err.println( "Skipping header line: " + Arrays.toString( line ) );
                    continue;
                }
                String gemeinde = line[2];
                String gemarkung = line[3];
                String gmkschl = line[4];
                Pair key = Pair.of( gemeinde, gemarkung );
                gmkMapping.put( key, gmkschl );
            }
        }
    }


    /**
     * Gemarkungsschlüssel aus {@link #FILE_GEMARKUNGEN}.
     *
     * @param gemeindeId Alter Primärschlüssel in WKV-Daten.
     * @param gemarkungId Alter Primärschlüssel in WKV-Daten.
     * @return Gemarkungsschlüssel oder null
     */
    public String gmkschl( String gemeindeId, String gemarkungId ) {
        return gmkMapping.get( Pair.of( gemeindeId, gemarkungId ) );
    }

}
