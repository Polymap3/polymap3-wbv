/* 
 * polymap.org
 * Copyright (C) 2015-2017, Falko Bräutigam. All rights reserved.
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
package org.polymap.wbv.ui.reports;

import static net.sf.dynamicreports.report.builder.DynamicReports.asc;
import static net.sf.dynamicreports.report.builder.DynamicReports.col;
import static net.sf.dynamicreports.report.builder.DynamicReports.sbt;
import static net.sf.dynamicreports.report.builder.DynamicReports.type;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.TreeSet;

import java.io.IOException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.google.common.collect.Sets;

import org.polymap.wbv.model.Waldbesitzer;

import net.sf.dynamicreports.jasper.builder.JasperReportBuilder;
import net.sf.dynamicreports.report.base.expression.AbstractValueFormatter;
import net.sf.dynamicreports.report.builder.column.TextColumnBuilder;
import net.sf.dynamicreports.report.constant.HorizontalAlignment;
import net.sf.dynamicreports.report.definition.ReportParameters;
import net.sf.dynamicreports.report.exception.DRException;
import net.sf.jasperreports.engine.JRException;

/**
 * Waldflächen aller Waldbesitzer.
 *
 * @author Joerg Reichert <joerg@mapzone.io>
 * @author <a href="http://www.polymap.de">Falko Bräutigam</a>
 */
public class Report106
        extends WbvReport<JasperReportBuilder> {

    private static final Log log = LogFactory.getLog( Report106.class );

    private static final TreeSet<Double> INTERVALL_GRENZEN = Sets.newTreeSet( Arrays.asList( 10000d, 1000d, 500d, 200d, 100d, 50d, 20d, 10d, 5d, 1d, 0d ) );
    
    private enum Column {
        Flaechengruppe, Anzahl, Gesamt, Durchschnitt
    }

    class RowAccu {
        public int      anzahl;
        public double   gesamt;
        
        public RowAccu add( double _gesamt ) {
            this.gesamt += _gesamt;
            this.anzahl ++;
            return this;
        }
    }
    
    // instance *******************************************

    /**
     * Flurstücksgruppen: obere Intervallgrenze -> Liste von Flurstücken
     */
    private Map<Double,RowAccu>     gruppen = new HashMap();
    
    @Override
    public String getName() {
        return "WBV 1.06 - Anzahl und Fläche der WBS nach Größengruppe";
    }


    @Override
    public JasperReportBuilder build() throws DRException, JRException, IOException {
        // akkumulieren pro intervall
        for (Waldbesitzer wb : revierWaldbesitzer()) {
            double flaecheWald = wb.flurstuecke( revier.get() ).stream()
                    .mapToDouble( fst -> fst.flaecheWald.opt().orElse( 0d ) )
                    .sum();
            Double gruppe = INTERVALL_GRENZEN.higher( flaecheWald - 0.00001d );  // flaeche kleiner oder *gleich* intervallgrenze
            gruppen.computeIfAbsent( gruppe, key -> new RowAccu() ).add( flaecheWald );
        }
        
        // zeilen berechnen -> data source 
        SimpleDataSource ds = new SimpleDataSource(
                Column.Flaechengruppe.name(), Column.Anzahl.name(), Column.Gesamt.name(), Column.Durchschnitt.name() );

        int rowIndex = 0;
        for (Entry<Double,RowAccu> entry : gruppen.entrySet()) {
            ds.put( Column.Flaechengruppe, rowIndex, entry.getKey() );
            ds.put( Column.Anzahl, rowIndex, entry.getValue().anzahl );
            ds.put( Column.Gesamt, rowIndex, entry.getValue().gesamt );
            ds.put( Column.Durchschnitt, rowIndex, entry.getValue().gesamt / entry.getValue().anzahl );
            
            rowIndex ++;
        }

        // report
        HaNumberFormatter hanf = new HaNumberFormatter();
        NumberFormatter anzahlFormatter = new NumberFormatter( 1, 0, 100000, 0 );
        TextColumnBuilder<Double> gruppeColumn = col
                .column( "Flächengruppe", Column.Flaechengruppe.name(), type.doubleType() )
                .setValueFormatter( new IntervallFormatter() );
        TextColumnBuilder<Integer> anzahlColumn = col
                .column( "Anzahl der Waldbesitzer", Column.Anzahl.name(), type.integerType() )
                .setHorizontalAlignment( HorizontalAlignment.RIGHT )
                .setValueFormatter( anzahlFormatter );
        TextColumnBuilder<Double> gesamtColumn = col
                .column( "Gesamtfläche pro Flächengruppe", Column.Gesamt.name(), type.doubleType() )
                .setValueFormatter( hanf );
        TextColumnBuilder<Double> durchschnittColumn = col
                .column( "Durchschnittl. Waldfläche je WBS", Column.Durchschnitt.name(), type.doubleType() )
                .setValueFormatter( hanf );

        return newReport( 
                "Meldeliste Anzahl Waldbesitzer nach Größengruppen",
                "Basis: Waldfläche der Waldbesitzer" )
                .setDataSource( ds )
                .columns( gruppeColumn, anzahlColumn, gesamtColumn, durchschnittColumn )
                .columnGrid( gruppeColumn, anzahlColumn, gesamtColumn, durchschnittColumn )
                .subtotalsAtSummary().sortBy( asc( gruppeColumn ) )
                .subtotalsAtSummary( sbt.sum( anzahlColumn ).setValueFormatter( anzahlFormatter ) )
                .subtotalsAtSummary( sbt.sum( gesamtColumn ).setValueFormatter( hanf ) )
                .subtotalsAtSummary( sbt.text( "", durchschnittColumn ) );
    }

 
    public class IntervallFormatter
            extends AbstractValueFormatter<String,Double> {

        @Override
        public String format( Double value, ReportParameters params ) {
            if (value == 0d) {
                return "Waldfläche unbekannt";
            }
            return "bis " + value.intValue() + " ha";
        }

    }
}
