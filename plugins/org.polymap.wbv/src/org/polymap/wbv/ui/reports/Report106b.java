/* 
 * polymap.org
 * Copyright (C) 2015, Falko Bräutigam. All rights reserved.
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

import static net.sf.dynamicreports.report.builder.DynamicReports.cmp;
import static net.sf.dynamicreports.report.builder.DynamicReports.col;
import static net.sf.dynamicreports.report.builder.DynamicReports.grid;
import static net.sf.dynamicreports.report.builder.DynamicReports.report;
import static net.sf.dynamicreports.report.builder.DynamicReports.sbt;
import static net.sf.dynamicreports.report.builder.DynamicReports.stl;
import static net.sf.dynamicreports.report.builder.DynamicReports.template;
import static net.sf.dynamicreports.report.builder.DynamicReports.type;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.text.NumberFormat;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.polymap.wbv.model.Flurstueck;
import org.polymap.wbv.model.Waldbesitzer;
import org.polymap.wbv.model.Waldbesitzer.Waldeigentumsart;

import net.sf.dynamicreports.jasper.builder.JasperReportBuilder;
import net.sf.dynamicreports.report.builder.ReportTemplateBuilder;
import net.sf.dynamicreports.report.builder.column.TextColumnBuilder;
import net.sf.dynamicreports.report.builder.grid.ColumnGridComponentBuilder;
import net.sf.dynamicreports.report.builder.subtotal.AggregationSubtotalBuilder;
import net.sf.dynamicreports.report.constant.HorizontalAlignment;
import net.sf.dynamicreports.report.constant.PageOrientation;
import net.sf.dynamicreports.report.constant.PageType;
import net.sf.dynamicreports.report.constant.Position;
import net.sf.dynamicreports.report.constant.VerticalAlignment;
import net.sf.dynamicreports.report.definition.ReportParameters;
import net.sf.dynamicreports.report.exception.DRException;
import net.sf.jasperreports.engine.JRException;
import net.sf.jasperreports.engine.data.JRCsvDataSource;

/**
 * Waldflächen aller Waldbesitzer.
 *
 * @author Joerg Reichert <joerg@mapzone.io>
 */
public class Report106b
        extends WbvReport {

    private static final Log log = LogFactory.getLog( Report106b.class );

    @Override
    public String getName() {
        return "WBV 1.06b - Anzahl und Fläche nach Größengruppe (Agrarbericht)";
    }


    class Group {

        List<Flurstueck>             flurstuecke = new ArrayList<Flurstueck>();

        Map<Double,List<Flurstueck>> flaecheToFlurstuecke = new HashMap<Double,List<Flurstueck>>();

        Map<Double,Double>           flaecheToGesamtFlaeche = new HashMap<Double,Double>();

        Map<Double,Integer>          flaecheToWBS = new HashMap<Double,Integer>();
    }


    @Override
    public JasperReportBuilder build() throws DRException, JRException, IOException {
        List<String> arten = new ArrayList<String>();
        arten.add( "Privatwald" );
        arten.add( "Kirchenwald" );
        arten.add( "Körperschaftswald" );

        Map<String,Group> grouped = new HashMap<String,Group>();
        for (String art : arten) {
            grouped.put( art, new Group() );
        }

        List<Double> flaechenGruppe = new ArrayList<Double>();
        flaechenGruppe.add( 1000d );
        flaechenGruppe.add( 500d );
        flaechenGruppe.add( 200d );
        flaechenGruppe.add( 100d );
        flaechenGruppe.add( 50d );
        flaechenGruppe.add( 20d );
        flaechenGruppe.add( 10d );
        flaechenGruppe.add( 5d );
        flaechenGruppe.add( 1d );
        flaechenGruppe.add( 0d );

        for (Waldbesitzer wb : revierWaldbesitzer()) {
                for (Flurstueck flurstueck : wb.flurstuecke( revier.get() )) {
                    Group group = grouped.get( getArt( wb.eigentumsArt.get() ) );
                    group.flurstuecke.add( flurstueck );
                    List<Flurstueck> fs = null;
                    if (flurstueck.flaecheWald.get() == null) {
                        fs = getFlurstueckeForGroup( group.flaecheToFlurstuecke, -1d );
//                        if (!fs.contains( flurstueck )) {
                            fs.add( flurstueck );
//                        }
                    }
                    else {
                        for (int i = 1; i < flaechenGruppe.size(); i++) {
                            if (flurstueck.flaecheWald.get() >= flaechenGruppe.get( i )) {
                                fs = getFlurstueckeForGroup( group.flaecheToFlurstuecke, flaechenGruppe.get( i - 1 ) );
                                fs.add( flurstueck );
                            }
                        }
                    }
                }
        }

        Double sum;
        Set<Waldbesitzer> wbs;
        //Double durchschnittsFlaeche;
        for (Group group : grouped.values()) {
            for (Entry<Double,List<Flurstueck>> entry : group.flaecheToFlurstuecke.entrySet()) {
                sum = 0.0d;
                //durchschnittsFlaeche = 0.0d;
                wbs = new HashSet<Waldbesitzer>();
                for (Flurstueck flurstueck : entry.getValue()) {
                    sum += flurstueck.flaecheWald.get() != null ? flurstueck.flaecheWald.get() : 0; 
                    wbs.add( flurstueck.waldbesitzer() );
                }
                group.flaecheToGesamtFlaeche.put( entry.getKey(), sum );
                group.flaecheToWBS.put( entry.getKey(), wbs.size() );
            }
        }

        StringBuilder sb = new StringBuilder();
        int index = -1;
        Double upperBound;
        Collections.reverse( flaechenGruppe );
        for (Double entry : flaechenGruppe) {
            index = flaechenGruppe.indexOf( entry );
            if (index + 1 < flaechenGruppe.size()) {
                upperBound = flaechenGruppe.get( index + 1 );
            }
            else {
                upperBound = null;
            }
            if (upperBound != null && upperBound.intValue() >= 0) {
                sb.append( entry.intValue() + " bis " + upperBound.intValue() ).append( " ha;" );
            }
            else {
                sb.append( "über " + entry.intValue() + " ha" ).append( ";" );
            }
            Integer wbsCount;
            Double gesamtFlaeche;
            for (String art : arten) {
                wbsCount = grouped.get( art ).flaecheToWBS.get( entry );
                sb.append( wbsCount == null ? 0 : wbsCount ).append( ";" );
                gesamtFlaeche = grouped.get( art ).flaecheToGesamtFlaeche.get( entry );
                sb.append( gesamtFlaeche == null ? 0d : gesamtFlaeche ).append( ";" );
            }
            sb.append( "\n" );
        }
        ByteArrayInputStream bis = new ByteArrayInputStream( sb.toString().getBytes() );

        NumberFormatter countFormatter = new NumberFormatter( 1, 0, 10000, 0 ) {
            @Override
            public String format( Number value, ReportParameters params ) {
                if (value.intValue() <= 0) {
                    return "";
                }
                else {
                    return super.format( value, params );
                }
            }
        };

        NumberFormatter haNumberFormatter = new NumberFormatter( 1, 2, 10000, 2 ) {
            @Override
            public String format( Number value, ReportParameters params ) {
                if (value.doubleValue() <= 0) {
                    return "";
                }
                else {
                    return super.format( value, params ) + " ha";
                }
            }
        };

        List<String> columns = new ArrayList<String>();
        columns.add( "flaechengruppe" );
        for (String art : arten) {
            columns.add( art + "_anzahl_waldbesitzer" );
            columns.add( art + "_gesamtflaeche" );
        }

        JRCsvDataSource datasource = new JRCsvDataSource( bis );
        datasource.setColumnNames( columns.toArray( new String[columns.size()] ) );
        datasource.setFieldDelimiter( ';' );
        datasource.setUseFirstRowAsHeader( false );
        datasource.setNumberFormat( NumberFormat.getInstance( Locale.US ) );

        // report
        TextColumnBuilder<String> flaechengruppeColumn = col.column( "Flächengruppe", "flaechengruppe",
                type.stringType() ).setStyle(
                stl.style().setAlignment( HorizontalAlignment.CENTER, VerticalAlignment.MIDDLE )
                        .setBorder( stl.pen().setLineWidth( 0.5f ) ) );

        Map<String,List<TextColumnBuilder<? extends Number>>> columnGroups = new HashMap<String,List<TextColumnBuilder<? extends Number>>>();
        for (String art : arten) {
            List<TextColumnBuilder<? extends Number>> list = new ArrayList<TextColumnBuilder<? extends Number>>();
            TextColumnBuilder<Integer> waldbesitzerAnzahlColumn = col.column( "Anzahl", art + "_anzahl_waldbesitzer",
                    type.integerType() ).setValueFormatter( countFormatter );
            TextColumnBuilder<Double> gesamtflaecheColumn = col.column( "Hektar", art + "_gesamtflaeche",
                    type.doubleType() ).setValueFormatter( haNumberFormatter );
            list.add( waldbesitzerAnzahlColumn );
            list.add( gesamtflaecheColumn );
            columnGroups.put( art, list );
        }

        List<ColumnGridComponentBuilder> titleGroups = new ArrayList<ColumnGridComponentBuilder>();
        titleGroups.add( flaechengruppeColumn );
        for (String art : arten) {
            List<TextColumnBuilder<? extends Number>> columnGroup = columnGroups.get( art );
            titleGroups.add( grid.titleGroup( art, columnGroup.toArray( new TextColumnBuilder[columnGroup.size()] ) ) );
        }

        ReportTemplateBuilder templateBuilder = template();
        templateBuilder.setSubtotalLabelPosition( Position.BOTTOM );
        templateBuilder.setSummaryStyle( stl.style().setTopBorder( stl.pen1Point() ) );
        templateBuilder.setColumnStyle( stl.style().setBorder( stl.pen().setLineWidth( 0.5f ) ) );

        JasperReportBuilder report = report()
                .setTemplate( templateBuilder )
                .setDataSource( datasource )

                .setPageFormat( PageType.A4, PageOrientation.PORTRAIT )
                .title( cmp.text( "Meldeliste Anzahl Waldbesitzer nach Größengruppen (Agrarbericht)" ).setStyle(
                        titleStyle ),
                        cmp.text( "Basis: Waldfläche der Waldbesitzer" ).setStyle( titleStyle ),
                        cmp.text( "Forstbezirk: Mittelsachsen" ).setStyle( headerStyle ),
                        cmp.text( "Revier: " + getRevier() /*+ " / Abfrage: \"" + getQuery() + "\""*/ ).setStyle( headerStyle ), 
                        cmp.text( df.format( new Date() ) ).setStyle( headerStyle ),
                        cmp.text( "" ).setStyle( headerStyle ) ).pageFooter( cmp.pageXofY().setStyle( footerStyle ) )
                // number of page
                .setDetailOddRowStyle( highlightRowStyle ).setColumnTitleStyle( columnTitleStyle )
                .columnGrid( titleGroups.toArray( new ColumnGridComponentBuilder[titleGroups.size()] ) );

        report.addColumn( flaechengruppeColumn );
        List<TextColumnBuilder<? extends Number>> group;
        TextColumnBuilder<? extends Number> column;
        AggregationSubtotalBuilder<? extends Number> subtotal;
        for (String art : arten) {
            group = columnGroups.get( art );
            for (int i = 0; i < group.size(); i++) {
                column = group.get( i );
                report.addColumn( column );
                subtotal = sbt.sum( column );
                if (i > 0) {
                    subtotal.setValueFormatter( haNumberFormatter );
                }
                else {
                    subtotal.setValueFormatter( countFormatter );
                }
                report.addSubtotalAtSummary( subtotal );
            }
        }

        return report;
    }


    private String getArt( Waldeigentumsart art ) {
        switch (art
                ) {
            case Privat:
                return "Privatwald";
            case Kirche42:
            case Kirche43:
                return "Kirchenwald";
            default:
                return "Körperschaftswald";
        }
    }


    private List<Flurstueck> getFlurstueckeForGroup( Map<Double,List<Flurstueck>> flaecheToFlurstuecke, Double key ) {
        List<Flurstueck> fs = flaecheToFlurstuecke.get( key );
        if (fs == null) {
            fs = new ArrayList<Flurstueck>();
            flaecheToFlurstuecke.put( key, fs );
        }
        return fs;
    }
}
