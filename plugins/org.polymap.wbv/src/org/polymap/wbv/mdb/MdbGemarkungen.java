/* 
 * polymap.org
 * Copyright (C) 2015 Polymap GmbH. All rights reserved.
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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import org.supercsv.io.CsvListReader;
import org.supercsv.io.ICsvListReader;
import org.supercsv.prefs.CsvPreference;

import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang.StringUtils;

import com.healthmarketscience.jackcess.Database;
import com.healthmarketscience.jackcess.DatabaseBuilder;
import com.healthmarketscience.jackcess.Row;
import com.healthmarketscience.jackcess.Table;
import com.healthmarketscience.jackcess.util.LinkResolver;

/**
 *
 * 
 * @author <a href="http://www.polymap.de">Falko Bräutigam</a>
 */
public class MdbGemarkungen {

    public static void main( String[] args ) throws Throwable {
        MdbGemarkungen mdp = new MdbGemarkungen();
        mdp.printCSV();
    }


    // instance *******************************************

    private Database            db;

    private Map<String,String>  gmks = new HashMap();

    private Map<String,String>  gmds = new HashMap();

    /** "gemeinde/gemarkung" -> ... */
    private Map<String,String>  neueGmks = new TreeMap();
    
    /** "gemeindeId/gemarkungId" -> gmkschl */
    private Map<String,String>  gmkMapping = new TreeMap();
    

    public MdbGemarkungen() throws IOException {
        db = DatabaseBuilder.open( new File( WvkImporter.BASEDIR, "WVK_dat.mdb" ) );
        db.setLinkResolver( new LinkResolver() {
            @Override
            public Database resolveLinkedDatabase( Database linkerDb, String linkeeFileName ) throws IOException {
                return DatabaseBuilder.open( new File( FilenameUtils.getName( linkeeFileName ) ) );
            }
        });
        
        // Gemarkung: id -> name
        Table table = db.getTable( "ESt_Gemarkung" );
        for (Row row=table.getNextRow(); row != null; row=table.getNextRow()) {
            String id = row.get( "ID_Gemarkung" ).toString();
            gmks.put( id, (String)row.get( "Gemarkung_Name" ) );
            log( id, " - ", gmks.get( id ) );
        }

        // Gemeinden: id -> name
        table = db.getTable( "ESt_Gemeinde" );
        for (Row row=table.getNextRow(); row != null; row=table.getNextRow()) {
            String id = row.get( "ID_Gemeinde" ).toString();
            gmds.put( id, (String)row.get( "Gemeinde_Name" ) );
            log( id, " - ", gmds.get( id ) );
        }
        
        CsvPreference prefs = new CsvPreference( '"', ',', "\r\n" ); // quoteChar, delimiterChar, endOfLineSymbols
        
        // neue Gemarkungen aus CSV
        File f = new File( WvkImporter.BASEDIR, "Gemarkungen_Forstreviere.csv" );
        if (f.exists()) {
            InputStream in = new FileInputStream( f );
            ICsvListReader csv = new CsvListReader( new InputStreamReader( in, "UTF-8" ), prefs );

            for (List<String> l = csv.read(); l != null; l = csv.read()) {
                final String[] line = l.toArray( new String[l.size()] );
                String id = line[0];
                if (!StringUtils.isNumeric( id )) {
                    System.err.println( "Skipping header line: " + Arrays.toString( line ) );
                    continue;
                }
                String gmkSchl = line[0];
                String gemarkung = line[1];
                String gemeinde = line[2];
                String key = gemeinde + "/" + gemarkung;
                neueGmks.put( key, gmkSchl );
            }
        }
        else {
            System.err.println( "Keine Gemarkungen_Forstreviere.csv! Import Modus?" );
            neueGmks = null;
        }
        
        // Mapping
        FileInputStream in = new FileInputStream( new File( WvkImporter.BASEDIR, "Gemarkungen-wkv.csv" ) );
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
            String key = gemeinde + "/" + gemarkung;
            gmkMapping.put( key, gmkschl );
        }
    }


    public void printCSV() throws IOException {
        // Flurstücke
        Table table = db.getTable( "FL_Flurstück" );
        Set<String> keys = new TreeSet();
        for (Row row=table.getNextRow(); row != null; row=table.getNextRow()) {
            String gemeindeId = row.get( "FL_Gemeinde" ).toString();
            String gemeindeName = gmds.get( gemeindeId );
            if (gemeindeName == null) {
                log( "Keine Gemeinde für: " + gemeindeId + " -> " + gemeindeName );
                continue;
            }

            String gemarkungId = row.get( "FL_Gemarkung" ).toString();
            String gemarkungName = gmks.get( gemarkungId );
            if (gemarkungName == null) {
                log( "Keine Gemarkung für: " + gemarkungId + " -> " + gemarkungName );
                continue;
            }
            
            String key = gemeindeName + "/" + gemarkungName;
            if (!keys.contains( key )) {
                keys.add( key );
                String hint = neueGmks.get( key );
                log( gemeindeName, ",", gemarkungName, ",", gemeindeId, ",", gemarkungId, ",", hint != null ? hint : "???" );
            }
        }
    }

    
    protected void log( Object... parts) {
        StringBuilder buf = new StringBuilder( 256 );
        for (Object part : parts) {
            buf.append( part != null ? part.toString() : "[null]" );
        }
        System.out.println( buf.toString() );
    }


    /**
     * Gemarkungsschlüssel aus {@link #neueGmks} (CSV).
     *
     * @param gemeindeId Alter Primärschlüssel in WKV-Daten.
     * @param gemarkungId Alter Primärschlüssel in WKV-Daten.
     * @return Gemarkungsschlüssel oder null
     */
    public String gmkschl( String gemeindeId, String gemarkungId ) {
        String key = gemeindeId + "/" + gemarkungId;
        return gmkMapping.get( key );
    }

}
