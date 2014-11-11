/* 
 * polymap.org
 * Copyright (C) 2014 Polymap GmbH. All rights reserved.
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

import java.util.List;
import java.util.Map;
import java.util.Set;

import java.io.File;
import java.io.IOException;
import java.io.PrintStream;

import org.apache.commons.io.FilenameUtils;

import com.healthmarketscience.jackcess.Column;
import com.healthmarketscience.jackcess.Database;
import com.healthmarketscience.jackcess.DatabaseBuilder;
import com.healthmarketscience.jackcess.Index;
import com.healthmarketscience.jackcess.PropertyMap;
import com.healthmarketscience.jackcess.Relationship;
import com.healthmarketscience.jackcess.Table;
import com.healthmarketscience.jackcess.util.LinkResolver;

/**
 * Imhalte der WKV_xxx.mdb Tabellen anzeigen. Starten in Eclipse asl
 * "WBV - MdbInspactor" Launch-Target.
 * 
 * @author <a href="http://www.polymap.de">Bertram Kirsch</a>
 * @author <a href="http://www.polymap.de">Falko Bräutigam</a>
 */
public class MdbInspector {

    public static void main( String[] args ) throws Throwable {
        MdbInspector mdp = new MdbInspector( new File( "." ) );  // current working dir
        mdp.printReport();
    }


    // instance *******************************************

    private File                baseDir;
    
    private String              fileNames[] = { "WVK_dat.mdb"/*, "WVK_steu.mdb"/*, "SN_Stamm.mdb"*/ };
    
    private Database            db;

    private Set<String>         tableNames;

    private String              currentFileName;

    private PrinterPlainText    printer;


    public MdbInspector( File baseDir ) {
        this.baseDir = baseDir;
    }


    public void printReport() throws IOException {
        for (String fileName : fileNames) {
            printReportForFile( new File( baseDir, fileName ).getAbsolutePath() );
        }
    }


    private void printReportForFile( String fileName ) throws IOException {
        currentFileName = fileName;
        db = DatabaseBuilder.open( new File( fileName ) );
        db.setLinkResolver( new LinkResolver() {
            @Override
            public Database resolveLinkedDatabase( Database linkerDb, String linkeeFileName ) throws IOException {
                return DatabaseBuilder.open( new File( FilenameUtils.getName( linkeeFileName ) ) );
            }
        });
        
        MultiPrintfStream out = new MultiPrintfStream( System.out, new PrintStream( fileName + ".txt" ) );
        printer = new PrinterPlainText( out );

        tableNames = db.getTableNames();

        printer.printTableNames( fileName, tableNames.size() );
//        for (String tableName : tableNames) {
//            printer.printTableName( tableName );
//        }
//        printer.printSeparator();
        printDetails();
    }


    private void printDetails() {
        for (String tableName : tableNames) {
            try {
                Table table = db.getTable( tableName );
                if (table.getRowCount() > 0) {
                    printer.printTableHeading( tableName, table.getRowCount() );
                    printFields( table );
                    printRows( table );
//                    printPrimaryKey( table );
//                    printRelationShips( table );
                    printer.printSeparator();
                }
            }
            catch (IOException e) {
//                e.printStackTrace();
                printer.printReadError( FilenameUtils.getName( currentFileName ), tableName, e.getMessage() );
            }
        }
    }


    private void printRelationShips( Table table ) throws IOException {
        List<Relationship> relationShips = db.getRelationships( table );
        for (Relationship relationship : relationShips) {
            relationship.getName();
            String tableNameA = relationship.getFromTable().getName();
            String tableNameB = relationship.getToTable().getName();
            if (table.getName().equals( tableNameB )) {
                printer.printForeignKey( tableNameA, tableNameB );
            }
            else {
                printer.printReference( tableNameA, tableNameB );
            }
        }
    }


    private void printFields( Table table ) throws IOException {
        for (Column column : table.getColumns()) {
            PropertyMap colProps = column.getProperties();
            Object descProp = colProps.getValue( PropertyMap.DESCRIPTION_PROP );
            String printableDesc = descProp instanceof String ? (String)descProp : "";
            printer.printTableField( column.getColumnIndex(), column.getName(), column.getType()
                    .name(), column.getLength(), printableDesc );
        }
    }


    private void printRows( Table table ) throws IOException {
        int maxRows = table.getName().contains( "Flurstück" ) ? 300 : 5;
        int rowCount = 0;
        Map<String,Object> row = null;
        while ((row = table.getNextRow()) != null && rowCount++ < maxRows) {
            printer.printTableRow( row );
        }
    }


    private void printPrimaryKey( Table table ) {
        try {
            Index pki = table.getPrimaryKeyIndex();
            printer.printPrimaryKey( pki );
        }
        catch (IllegalArgumentException e) {
            printer.printPrimaryKey( null );
        }
    }

}
