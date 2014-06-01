package org.polymap.wbv.mdb;

import java.util.List;

import com.healthmarketscience.jackcess.Index;

public class PrinterPlainText {

    MultiPrintfStream out;


    public PrinterPlainText( MultiPrintfStream outPrintfStream ) {
        out = outPrintfStream;
    }


    public void printSeparator() {
        out.printf( "\n\n" );
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


    public void printReadError( String currentFileName, String message ) {
        out.printf( "Fehler beim Lesen der Datei : %s (%s)\n", currentFileName, message );
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

}
