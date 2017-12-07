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
import static net.sf.dynamicreports.report.builder.DynamicReports.cmp;
import static net.sf.dynamicreports.report.builder.DynamicReports.col;
import static net.sf.dynamicreports.report.builder.DynamicReports.report;
import static net.sf.dynamicreports.report.builder.DynamicReports.sbt;
import static net.sf.dynamicreports.report.builder.DynamicReports.type;

import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.HashSet;
import java.util.Map.Entry;
import java.util.TreeSet;

import java.io.IOException;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Iterables;
import com.google.common.collect.Multimap;
import com.google.common.collect.Sets;

import org.polymap.wbv.model.Flurstueck;
import net.sf.dynamicreports.jasper.builder.JasperReportBuilder;
import net.sf.dynamicreports.report.base.expression.AbstractValueFormatter;
import net.sf.dynamicreports.report.builder.column.TextColumnBuilder;
import net.sf.dynamicreports.report.constant.HorizontalAlignment;
import net.sf.dynamicreports.report.constant.PageOrientation;
import net.sf.dynamicreports.report.constant.PageType;
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
        extends WbvReport {

    private static final Log log = LogFactory.getLog( Report106.class );

    private static final TreeSet<Double> intervalle = Sets.newTreeSet( Arrays.asList( 10000d, 1000d, 500d, 200d, 100d, 50d, 20d, 10d, 5d, 1d, 0d ) );
    
    private enum Column {
        Flaechengruppe, Anzahl, Gesamt, Durchschnitt
    }

    // instance *******************************************

    /**
     * Flurstücksgruppen: obere Intervallgrenze -> Liste von Flurstücken
     */
    private Multimap<Double,Flurstueck>     gruppen = ArrayListMultimap.create();
    
    @Override
    public String getName() {
        return "WBV 1.06 - Anzahl und Fläche der WBS nach Größengruppe";
    }


    @Override
    public JasperReportBuilder build() throws DRException, JRException, IOException {
        // akkumulieren pro intervall
        for (Flurstueck flurstueck : revierFlurstuecke()) {
            Double flaecheWald = flurstueck.flaecheWald.opt().orElse( 0d );
            Double gruppe = intervalle.higher( flaecheWald - 0.00001d );  // flaeche kleiner oder *gleich* intervallgrenze
            gruppen.put( gruppe, flurstueck );
            // XXX if (!fs.contains( flurstueck )) {
        }
        
        // zeilen berechnen -> data source 
        SimpleDataSource ds = new SimpleDataSource(
                Column.Flaechengruppe.name(), Column.Anzahl.name(), Column.Gesamt.name(), Column.Durchschnitt.name() );

        int rowIndex = 0;
        for (Entry<Double,Collection<Flurstueck>> entry : gruppen.asMap().entrySet()) {
            double gesamt = 0.0d;
            HashSet<String> wbIds = new HashSet();
            for (Flurstueck flurstueck : entry.getValue()) {
                gesamt += flurstueck.flaecheWald.opt().orElse( 0d );
                wbIds.add( (String)flurstueck.waldbesitzer().id() );
            }
            
            ds.put( Column.Flaechengruppe, rowIndex, entry.getKey() );
            ds.put( Column.Anzahl, rowIndex, wbIds.size() );
            ds.put( Column.Gesamt, rowIndex, gesamt );
            ds.put( Column.Durchschnitt, rowIndex, gesamt / wbIds.size() );
            
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

        return report()
                .setTemplate( reportTemplate )
                .setPageFormat( PageType.A4, PageOrientation.PORTRAIT )
                .setDataSource( ds )
                .title( cmp.text( "Meldeliste Anzahl Waldbesitzer nach Größengruppen" ).setStyle( titleStyle ),
                        cmp.text( "Basis: Waldfläche der Waldbesitzer" ).setStyle( title2Style ),
                        cmp.text( "Forstbezirk: Mittelsachsen" ).setStyle( headerStyle ),
                        cmp.text( "Revier: " + getRevier() /*+ " / Abfrage: \"" + getQuery() + "\""*/ ).setStyle( headerStyle ), 
                        cmp.text( df.format( new Date() ) ).setStyle( headerStyle ),
                        cmp.text( "" ).setStyle( headerStyle ) )
                .pageFooter( cmp.pageXofY().setStyle( footerStyle ) )
                .setDetailOddRowStyle( highlightRowStyle )
                .setColumnTitleStyle( columnTitleStyle )
                .columns( gruppeColumn, anzahlColumn, gesamtColumn, durchschnittColumn )
                .columnGrid( gruppeColumn, anzahlColumn, gesamtColumn, durchschnittColumn )
                .subtotalsAtSummary().sortBy( asc( gruppeColumn ) )
                .subtotalsAtSummary( sbt.text( "("+Iterables.size( revierWaldbesitzer() )+")", anzahlColumn ) )
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
