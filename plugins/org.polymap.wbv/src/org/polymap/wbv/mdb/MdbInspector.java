package org.polymap.wbv.mdb;

import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.util.List;
import java.util.Set;

import com.healthmarketscience.jackcess.Column;
import com.healthmarketscience.jackcess.Database;
import com.healthmarketscience.jackcess.DatabaseBuilder;
import com.healthmarketscience.jackcess.Index;
import com.healthmarketscience.jackcess.PropertyMap;
import com.healthmarketscience.jackcess.Relationship;
import com.healthmarketscience.jackcess.Table;

public class MdbInspector {

    private Database         db;

    private Set<String>      tableNames;

    private String           currentFileName;

    private PrinterPlainText printer;


    public static void main( String[] args ) throws Throwable {
        MdbInspector mdp = new MdbInspector();
        mdp.printReport();
    }


    public void printReport() throws IOException {
        String fileNames[] = { "WVK_dat.mdb", "WVK_steu.mdb", "SN_Stamm.mdb" };

        for (int i = 0; i < fileNames.length; i++) {
            printReportForFile( fileNames[i] );
        }
    }


    private void printReportForFile( String fileName ) throws IOException {
        currentFileName = fileName;
        db = DatabaseBuilder.open( (new File( fileName )) );
        MultiPrintfStream out = new MultiPrintfStream( System.out, new PrintStream( fileName
                + ".txt" ) );
        printer = new PrinterPlainText( out );

        tableNames = db.getTableNames();

        printer.printTableNames( fileName, tableNames.size() );
        for (String tableName : tableNames) {
            printer.printTableName( tableName );
        }
        printer.printSeparator();
        printDetails();
    }


    private void printDetails() {
        for (String tableName : tableNames) {
            try {
                Table table = db.getTable( tableName );
                printer.printTableHeading( tableName, table.getRowCount() );
                printFields( table );
                printPrimaryKey( table );
                printRelationShips( table );
                printer.printSeparator();
            }
            catch (IOException e) {
                printer.printReadError( currentFileName, e.getMessage() );
            }
        }
    }


    private void printRelationShips( Table table ) {
        try {
            List<Relationship> relationShips = db.getRelationships( table );
            for (Relationship relationship : relationShips) {
                relationship.getName();
                printer.printRelationShip( relationship.getToTable().getName() );
            }
        }
        catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }


    private void printFields( Table table ) {
        List<? extends Column> columns = table.getColumns();
        for (Column column : columns) {
            try {
                PropertyMap colProps = column.getProperties();
                Object descProp = colProps.getValue( PropertyMap.DESCRIPTION_PROP );
                String printableDesc = descProp instanceof String ? (String)descProp : "";
                printer.printTableField( column.getColumnIndex(), column.getName(), column
                        .getType().name(), column.getLength(), printableDesc );
            }
            catch (IOException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }

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
