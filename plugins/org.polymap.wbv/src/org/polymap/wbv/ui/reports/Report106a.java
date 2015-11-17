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

import static net.sf.dynamicreports.report.builder.DynamicReports.asc;
import static net.sf.dynamicreports.report.builder.DynamicReports.cmp;
import static net.sf.dynamicreports.report.builder.DynamicReports.col;
import static net.sf.dynamicreports.report.builder.DynamicReports.report;
import static net.sf.dynamicreports.report.builder.DynamicReports.sbt;
import static net.sf.dynamicreports.report.builder.DynamicReports.stl;
import static net.sf.dynamicreports.report.builder.DynamicReports.template;
import static net.sf.dynamicreports.report.builder.DynamicReports.type;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import net.sf.dynamicreports.jasper.builder.JasperReportBuilder;
import net.sf.dynamicreports.report.builder.ReportTemplateBuilder;
import net.sf.dynamicreports.report.builder.column.TextColumnBuilder;
import net.sf.dynamicreports.report.builder.group.ColumnGroupBuilder;
import net.sf.dynamicreports.report.builder.group.Groups;
import net.sf.dynamicreports.report.constant.PageOrientation;
import net.sf.dynamicreports.report.constant.PageType;
import net.sf.dynamicreports.report.constant.Position;
import net.sf.dynamicreports.report.definition.ReportParameters;
import net.sf.dynamicreports.report.exception.DRException;
import net.sf.jasperreports.engine.JRException;
import net.sf.jasperreports.engine.data.JRCsvDataSource;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.polymap.rhei.batik.Context;
import org.polymap.rhei.batik.Scope;
import org.polymap.wbv.model.Flurstueck;
import org.polymap.wbv.model.Revier;
import org.polymap.wbv.model.Waldbesitzer;

/**
 * Waldflächen aller Waldbesitzer.
 *
 * @author <a href="http://www.polymap.de">Falko Bräutigam</a>
 */
public class Report106a
        extends WbvReport {

    private static Log      log = LogFactory.getLog( Report106a.class );

    @Scope("org.polymap.wbv.ui")
    private Context<Revier> revier;

    @Scope("org.polymap.wbv.ui")
    private Context<String> queryString;


    @Override
    public String getName() {
        return "WBV 1.06a";
    }


    class Group {

        List<Flurstueck>             flurstuecke                        = new ArrayList<Flurstueck>();

        Map<Double,List<Flurstueck>> flaecheToFlurstuecke               = new HashMap<Double,List<Flurstueck>>();

        Map<Double,Double>           flaecheToGesamtFlaeche             = new HashMap<Double,Double>();

        Map<Double,Integer>          flaecheToWBS                       = new HashMap<Double,Integer>();

        Map<Double,Double>           flaecheToDurchschnittFlaecheProWBS = new HashMap<Double,Double>();
    }


    @Override
    public JasperReportBuilder build() throws DRException, JRException, IOException {
        super.build();
        Map<String,Group> gemarkungen = new HashMap<String,Group>();

        List<Double> flaechenGruppe = new ArrayList<Double>();
        flaechenGruppe.add( 2000d );
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

        Map<Flurstueck,Waldbesitzer> flurstueck2Waldbesitzer = new HashMap<Flurstueck,Waldbesitzer>();

        entities.forEach( entity -> {
            if (entity instanceof Waldbesitzer) {
                Waldbesitzer wb = (Waldbesitzer)entity;
                wb.flurstuecke.forEach( flurstueck -> {
                    Group group = getGroupForGemarkung( gemarkungen, flurstueck );
                    group.flurstuecke.add( flurstueck );
                    List<Flurstueck> fs = null;
                    if (flurstueck.flaecheWald.get() == null) {
                        fs = getFlurstueckeForGroup( group.flaecheToFlurstuecke, -1d );
                        if (!fs.contains( flurstueck )) {
                            fs.add( flurstueck );
                        }
                    }
                    else {
                        for (int i = 1; i < flaechenGruppe.size(); i++) {
                            if (flurstueck.flaecheWald.get() >= flaechenGruppe.get( i )) {
                                fs = getFlurstueckeForGroup( group.flaecheToFlurstuecke, flaechenGruppe.get( i - 1 ) );
                                fs.add( flurstueck );
                            }
                        }
                    }
                    flurstueck2Waldbesitzer.put( flurstueck, wb );
                } );
            }
        } );
        Double sum;
        Set<Waldbesitzer> wbs;
        Double durchschnittsFlaeche;
        for (Group gemarkung : gemarkungen.values()) {
            for (Entry<Double,List<Flurstueck>> entry : gemarkung.flaecheToFlurstuecke.entrySet()) {
                sum = 0.0d;
                durchschnittsFlaeche = 0.0d;
                wbs = new HashSet<Waldbesitzer>();
                for (Flurstueck flurstueck : entry.getValue()) {
                    sum += flurstueck.flaecheWald.get();
                    wbs.add( flurstueck2Waldbesitzer.get( flurstueck ) );
                }
                gemarkung.flaecheToGesamtFlaeche.put( entry.getKey(), sum );
                gemarkung.flaecheToWBS.put( entry.getKey(), wbs.size() );
                durchschnittsFlaeche = sum / new Double( wbs.size() );
                gemarkung.flaecheToDurchschnittFlaecheProWBS.put( entry.getKey(), durchschnittsFlaeche );
            }
        }

        StringBuilder sb = new StringBuilder();
        int index = -1;
        Double lowerBound;
        for (Map.Entry<String,Group> gemarkung : gemarkungen.entrySet()) {
            for (Entry<Double,Integer> entry : gemarkung.getValue().flaecheToWBS.entrySet()) {
                sb.append( gemarkung.getKey() ).append( ";" );
                index = flaechenGruppe.indexOf( entry.getKey() );
                if (index + 1 < flaechenGruppe.size()) {
                    lowerBound = flaechenGruppe.get( index + 1 );
                }
                else {
                    lowerBound = null;
                }
                if (lowerBound.intValue() >= 0) {
                    sb.append( lowerBound.intValue() + " bis " + entry.getKey().intValue() ).append( ";" );
                }
                else {
                    sb.append( "ohne Flächenangabe" ).append( ";" );
                }
                sb.append( gemarkung.getValue().flaecheToWBS.get( entry.getKey() ) ).append( ";" );
                sb.append( gemarkung.getValue().flaecheToGesamtFlaeche.get( entry.getKey() ) ).append( ";" );
                sb.append( gemarkung.getValue().flaecheToDurchschnittFlaecheProWBS.get( entry.getKey() ) ).append( ";" );
                sb.append( "\n" );
            }
        }
        ByteArrayInputStream bis = new ByteArrayInputStream( sb.toString().getBytes() );

        NumberFormatter haNumberFormatter = new NumberFormatter( 1, 2, 10000, 2 ) {

            @Override
            public String format( Number value, ReportParameters params ) {
                return super.format( value, params ) + " ha";
            }
        };

        JRCsvDataSource datasource = new JRCsvDataSource( bis );
        datasource.setColumnNames( new String[] { "gemarkung", "flaechengruppe", "anzahl_waldbesitzer",
                "gesamtflaeche", "durchschnittsflaeche" } );
        datasource.setFieldDelimiter( ';' );
        datasource.setUseFirstRowAsHeader( false );
        datasource.setNumberFormat( NumberFormat.getInstance( Locale.US ) );

        // report
        TextColumnBuilder<String> gemarkung = col.column( "Gemarkung", "gemarkung", type.stringType() ).setStyle(
                stl.style().bold().setBottomBorder( stl.pen1Point() ) );
        ;
        TextColumnBuilder<String> flaechengruppeColumn = col.column( "Flächengruppe", "flaechengruppe",
                type.stringType() );
        TextColumnBuilder<Integer> waldbesitzerAnzahlColumn = col.column( "Anzahl der Waldbesitzer",
                "anzahl_waldbesitzer", type.integerType() );
        TextColumnBuilder<Double> gesamtflaecheColumn = col.column( "Gesamtfläche pro Flächengruppe", "gesamtflaeche",
                type.doubleType() ).setValueFormatter( haNumberFormatter );
        TextColumnBuilder<Double> durchschnittsflaecheColumn = col.column( "Durchschnittl. Waldfläche je WBS",
                "durchschnittsflaeche", type.doubleType() ).setValueFormatter( haNumberFormatter );

        ReportTemplateBuilder templateBuilder = template();
        templateBuilder.setSubtotalLabelPosition( Position.BOTTOM );
        templateBuilder.setSummaryStyle( stl.style().setTopBorder( stl.pen1Point() ) );

        ColumnGroupBuilder gemarkungGroupBuilder = Groups.group( gemarkung ).setPadding( 5 );

        return report()
                .setTemplate( templateBuilder )
                .setDataSource( datasource )
                .setPageFormat( PageType.A4, PageOrientation.PORTRAIT )
                .title( cmp.text( "Meldeliste Anzahl Waldbesitzer nach Größengruppen" ).setStyle( titleStyle ),
                        cmp.text( "Basis: Waldfläche der Waldbesitzer" ).setStyle( titleStyle ),
                        cmp.text( "Forstbezirk: Mittelsachsen" ).setStyle( headerStyle ),
                        cmp.text( "Revier: " + getRevier() + " / Abfrage: \"" + getQuery() + "\"" ).setStyle(
                                headerStyle ), cmp.text( df.format( new Date() ) ).setStyle( headerStyle ),
                        cmp.text( "" ).setStyle( headerStyle ) )
                .pageFooter( cmp.pageXofY().setStyle( footerStyle ) )
                // number of page
                .setDetailOddRowStyle( highlightRowStyle )
                .setColumnTitleStyle( columnTitleStyle )
                .addGroup( gemarkungGroupBuilder )
                .columns( gemarkung, flaechengruppeColumn, waldbesitzerAnzahlColumn, gesamtflaecheColumn,
                        durchschnittsflaecheColumn )
                .columnGrid( flaechengruppeColumn, waldbesitzerAnzahlColumn, gesamtflaecheColumn,
                        durchschnittsflaecheColumn )
                .subtotalsAtSummary()
                .sortBy( asc( flaechengruppeColumn ) )
                .subtotalsAtGroupHeader(
                        gemarkungGroupBuilder,
                        sbt.sum( waldbesitzerAnzahlColumn ).setStyle(
                                stl.style().bold().setBottomBorder( stl.pen1Point() ) ),
                        sbt.sum( gesamtflaecheColumn ).setValueFormatter( haNumberFormatter )
                                .setStyle( stl.style().bold().setBottomBorder( stl.pen1Point() ) ),
                        sbt.sum( durchschnittsflaecheColumn ).setValueFormatter( haNumberFormatter )
                                .setStyle( stl.style().bold().setBottomBorder( stl.pen1Point() ) ) );
    }


    protected String getQuery() {
        return queryString.get();
    }


    protected String getRevier() {
        return revier.get().name;
    }


    private List<Flurstueck> getFlurstueckeForGroup( Map<Double,List<Flurstueck>> flaecheToFlurstuecke, Double key ) {
        List<Flurstueck> fs = flaecheToFlurstuecke.get( key );
        if (fs == null) {
            fs = new ArrayList<Flurstueck>();
            flaecheToFlurstuecke.put( key, fs );
        }
        return fs;
    }


    private Group getGroupForGemarkung( Map<String,Group> gemarkungen, Flurstueck flurstueck ) {
        String gemarkung = flurstueck.gemarkung.get().gemarkung.get();
        Group group = gemarkungen.get( gemarkung );
        if (group == null) {
            group = new Group();
            gemarkungen.put( gemarkung, group );
        }
        return group;
    }
}
