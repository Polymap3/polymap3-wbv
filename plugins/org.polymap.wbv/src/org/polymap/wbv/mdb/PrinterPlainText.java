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

import java.util.Date;
import java.util.List;
import java.util.Map;

import com.healthmarketscience.jackcess.Index;

/**
 * 
 *
 * @author <a href="http://www.polymap.de">Bertram Kirsch</a>
 * @author <a href="http://www.polymap.de">Falko Bräutigam</a>
 */
public class PrinterPlainText {

    MultiPrintfStream out;


    public PrinterPlainText( MultiPrintfStream outPrintfStream ) {
        out = outPrintfStream;
    }


    public void printSeparator() {
        out.printf( "\n" );
    }


    public void printTableNames( String fileName, int size ) {
        out.printf( "%s (%d Tabellen)\n", fileName, size );
    }


    public void printTableName( String tableName ) {
        out.printf( "  %s\n", tableName );
    }


    public void printTableHeading( String tableName, int rowCount ) {
        out.printf( "\n%s (%d Datensätze)\n", tableName, rowCount );
    }


    public void printTableField( int colIdx, String colName, String colTypeName, short typeLength,
            String desc ) {
        String dataType = String.format( "(%s %d)", colTypeName, typeLength );
        out.printf( "% 3d|%-30s %-20s      %s\n", colIdx, colName, dataType, desc );
    }


    public void printReadError( String fileName, String tableName, String msg ) {
        out.printf( "Fehler : %s::%s (%s)\n", fileName, tableName, msg );
    }


    public void printPrimaryKey( Index pki ) {
        if (pki == null) {
            out.printf( "Kein Primärschlüssel\n" );
        }
        else {
            List<? extends com.healthmarketscience.jackcess.Index.Column> pkiCols = pki
                    .getColumns();
            out.printf( "Primärschlüssel:" );
            for (com.healthmarketscience.jackcess.Index.Column column : pkiCols) {
                out.printf( "%2d ", column.getColumnIndex() );
            }
            out.printf( "\n" );
        }
    }


    public void printForeignKey( String from, String to ) {
        out.printf( " FK: %s -> %s\n", to, from );
    }


    public void printReference( String from, String to ) {
        out.printf( "Ref: %s -> %s\n", to, from );
    }


    public void printTableRow( Map<String,Object> row ) {
        for (Map.Entry<String,Object> entry : row.entrySet()) {
            String value = "???";
            if (entry.getValue() == null) {
                value = "null";
            }
            else if (entry.getValue() instanceof Date) {
                value = "[datum]";
            }
            else {
                value = entry.getValue().toString();
            }
            out.printf( "%-12s", value );
        }
        out.print( "\n" );
    }

}
