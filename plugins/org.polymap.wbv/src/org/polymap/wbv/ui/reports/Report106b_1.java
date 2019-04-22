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
import org.polymap.wbv.model.Waldbesitzer.Waldeigentumsart;

import net.sf.dynamicreports.jasper.builder.JasperReportBuilder;
import net.sf.dynamicreports.report.base.expression.AbstractValueFormatter;
import net.sf.dynamicreports.report.builder.column.TextColumnBuilder;
import net.sf.dynamicreports.report.builder.subtotal.Subtotals;
import net.sf.dynamicreports.report.constant.HorizontalAlignment;
import net.sf.dynamicreports.report.constant.PageOrientation;
import net.sf.dynamicreports.report.constant.PageType;
import net.sf.dynamicreports.report.definition.ReportParameters;
import net.sf.dynamicreports.report.exception.DRException;
import net.sf.jasperreports.engine.JRException;

/**
 * Waldflächen aller Waldbesitzer.
 *
 * @author <a href="http://www.polymap.de">Falko Bräutigam</a>
 */
public class Report106b_1
        extends WbvReport {

    private static final Log log = LogFactory.getLog( Report106b_1.class );

    private static final TreeSet<Double> INTERVALL_GRENZEN = Sets.newTreeSet( Arrays.asList( 10000d, 1000d, 500d, 200d, 100d, 50d, 20d, 10d, 5d, 1d, 0d ) );
    
    private enum Column {
        Flaechengruppe, PrivatAnzahl, PrivatFlaeche, KircheAnzahl, KircheFlaeche, KoerpAnzahl, KoerpFlaeche, StaatAnzahl, StaatFlaeche
    }

    
    class RowAccu {
        public int      koerpAnzahl, kircheAnzahl, privatAnzahl, staatAnzahl, bvvgAnzahl;
        public double   koerpFlaeche, kircheFlaeche, privatFlaeche, staatFlaeche, bvvgFlaeche;
        
        public RowAccu add( Waldeigentumsart eigentumsart, double flaeche ) {
            switch (eigentumsart) {
                case Staat_Bund:
                case Staat_Sachsen: {
                    staatFlaeche += flaeche;
                    staatAnzahl ++;
                    break;
                }
                case BVVG: {
                    bvvgAnzahl ++;
                    bvvgFlaeche += flaeche;
                    break;
                }
                case Kirche42:
                case Kirche43: {
                    kircheFlaeche += flaeche;
                    kircheAnzahl ++;
                    break;
                }
                case Körperschaft_Kommune:
                case Körperschaft_ZVB: {
                    koerpFlaeche += flaeche;
                    koerpAnzahl ++;
                    break;
                }
                case Privat: {
                    privatFlaeche += flaeche;
                    privatAnzahl ++;
                    break;
                }
                case Unbekannt: {
                }
            }
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
        return "WBV 1.06b - Anzahl und Fläche nach Größengruppe (Agrarbericht)";
    }


    @Override
    public JasperReportBuilder build() throws DRException, JRException, IOException {
        // akkumulieren pro intervall
        for (Waldbesitzer wb : revierWaldbesitzer()) {
            double flaecheWald = wb.flurstuecke( revier.get() ).stream()
                    .mapToDouble( fst -> fst.flaecheWald.opt().orElse( 0d ) )
                    .sum();
            Double gruppe = INTERVALL_GRENZEN.higher( flaecheWald - 0.00001d );  // flaeche kleiner oder *gleich* intervallgrenze
            RowAccu rowAccu = gruppen.computeIfAbsent( gruppe, key -> new RowAccu() );
            rowAccu.add( wb.eigentumsArt.get(), flaecheWald );
        }
        
        // data source 
        SimpleDataSource ds = new SimpleDataSource(
                Column.Flaechengruppe.name(), 
                Column.PrivatAnzahl.name(), Column.PrivatFlaeche.name(),
                Column.KircheAnzahl.name(), Column.KircheFlaeche.name(),
                Column.KoerpAnzahl.name(), Column.KoerpFlaeche.name(),
                Column.StaatAnzahl.name(), Column.StaatFlaeche.name() );

        int rowIndex = 0;
        for (Entry<Double,RowAccu> entry : gruppen.entrySet()) {
            ds.put( Column.Flaechengruppe, rowIndex, entry.getKey() );
            ds.put( Column.PrivatAnzahl, rowIndex, entry.getValue().privatAnzahl );
            ds.put( Column.PrivatFlaeche, rowIndex, entry.getValue().privatFlaeche );
            ds.put( Column.KircheAnzahl, rowIndex, entry.getValue().kircheAnzahl );
            ds.put( Column.KircheFlaeche, rowIndex, entry.getValue().kircheFlaeche );
            ds.put( Column.KoerpAnzahl, rowIndex, entry.getValue().koerpAnzahl );
            ds.put( Column.KoerpFlaeche, rowIndex, entry.getValue().koerpFlaeche );
            ds.put( Column.StaatAnzahl, rowIndex, entry.getValue().staatAnzahl );
            ds.put( Column.StaatFlaeche, rowIndex, entry.getValue().staatFlaeche );
            rowIndex ++;
        }

        // report
        HaNumberFormatter hanf = new HaNumberFormatter();
        NumberFormatter anzahlFormatter = new NumberFormatter( 1, 0, 100000, 0 );
        TextColumnBuilder<Double> gruppeColumn = col
                .column( "Fläche", Column.Flaechengruppe.name(), type.doubleType() )
                .setValueFormatter( new IntervallFormatter() );
        TextColumnBuilder<Integer> privatAnzahlCol = col
                .column( "Privat\nAnzahl", Column.PrivatAnzahl.name(), type.integerType() )
                .setHorizontalAlignment( HorizontalAlignment.RIGHT )
                .setValueFormatter( anzahlFormatter );
        TextColumnBuilder<Double> privatFlaecheCol = col
                .column( "Privat\nFläche", Column.PrivatFlaeche.name(), type.doubleType() )
                .setValueFormatter( hanf );
        TextColumnBuilder<Integer> kircheAnzahlCol = col
                .column( "Kirche\nAnzahl", Column.KircheAnzahl.name(), type.integerType() )
                .setHorizontalAlignment( HorizontalAlignment.RIGHT )
                .setValueFormatter( anzahlFormatter );
        TextColumnBuilder<Double> kircheFlaecheCol = col
                .column( "Kirche\nFläche", Column.KircheFlaeche.name(), type.doubleType() )
                .setValueFormatter( hanf );
        TextColumnBuilder<Integer> koerpAnzahlCol = col
                .column( "KdöR\nAnzahl", Column.KoerpAnzahl.name(), type.integerType() )
                .setHorizontalAlignment( HorizontalAlignment.RIGHT )
                .setValueFormatter( anzahlFormatter );
        TextColumnBuilder<Double> koerpFlaecheCol = col
                .column( "KdöR\nFläche", Column.KoerpFlaeche.name(), type.doubleType() )
                .setValueFormatter( hanf );
        TextColumnBuilder<Integer> staatAnzahlCol = col
                .column( "Staat\nAnzahl", Column.StaatAnzahl.name(), type.integerType() )
                .setHorizontalAlignment( HorizontalAlignment.RIGHT )
                .setValueFormatter( anzahlFormatter );
        TextColumnBuilder<Double> staatFlaecheCol = col
                .column( "Staat\nFläche", Column.StaatFlaeche.name(), type.doubleType() )
                .setValueFormatter( hanf );

        return newReport( 
                "Meldeliste Anzahl Waldbesitzer nach Größengruppen (Agrarbericht)",
                "Basis: Waldfläche der Waldbesitzer" )
                .setPageFormat( PageType.A4, PageOrientation.LANDSCAPE )
                .setDataSource( ds )
                .columns( gruppeColumn, privatAnzahlCol, privatFlaecheCol, kircheAnzahlCol, kircheFlaecheCol, 
                        koerpAnzahlCol, koerpFlaecheCol, staatAnzahlCol, staatFlaecheCol )
                //.columnGrid( gruppeColumn, privatAnzahlCol, privatFlaecheCol, kircheAnzahlCol, kircheFlaecheCol, koerpAnzahlCol, koerpFlaecheCol )
                //.subtotalsAtSummary()
                //.subtotalsAtSummary( DynamicReports.sbt.sum( anzahlColumn ).setValueFormatter( anzahlFormatter ) )
//                .subtotalsAtSummary( sbt.sum( gesamtColumn ).setValueFormatter( hanf ) )
                .subtotalsAtSummary( 
                        Subtotals.sum( privatAnzahlCol ), 
                        Subtotals.sum( privatFlaecheCol ).setValueFormatter( hanf ),
                        Subtotals.sum( kircheAnzahlCol ), 
                        Subtotals.sum( kircheFlaecheCol ).setValueFormatter( hanf ),
                        Subtotals.sum( koerpAnzahlCol ), 
                        Subtotals.sum( koerpFlaecheCol ).setValueFormatter( hanf ),
                        Subtotals.sum( staatAnzahlCol ), 
                        Subtotals.sum( staatFlaecheCol ).setValueFormatter( hanf ) )
                .sortBy( asc( gruppeColumn ) );
    }

 
    public class IntervallFormatter
            extends AbstractValueFormatter<String,Double> {

        @Override
        public String format( Double value, ReportParameters params ) {
            return value > 0d ? "bis " + value.intValue() + " ha" : "Unbekannt";
        }
    }
    
}
