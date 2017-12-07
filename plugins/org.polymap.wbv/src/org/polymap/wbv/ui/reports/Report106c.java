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
import static net.sf.dynamicreports.report.builder.DynamicReports.report;
import static net.sf.dynamicreports.report.builder.DynamicReports.stl;
import static net.sf.dynamicreports.report.builder.DynamicReports.template;
import static net.sf.dynamicreports.report.builder.DynamicReports.type;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import java.io.IOException;

import org.json.JSONObject;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.ListMultimap;

import org.polymap.wbv.model.Flurstueck;
import org.polymap.wbv.model.Gemarkung;
import org.polymap.wbv.model.Waldbesitzer;
import org.polymap.wbv.model.Waldbesitzer.Waldeigentumsart;

import net.sf.dynamicreports.jasper.builder.JasperReportBuilder;
import net.sf.dynamicreports.report.builder.ReportTemplateBuilder;
import net.sf.dynamicreports.report.builder.column.TextColumnBuilder;
import net.sf.dynamicreports.report.constant.GroupHeaderLayout;
import net.sf.dynamicreports.report.constant.HorizontalAlignment;
import net.sf.dynamicreports.report.constant.PageOrientation;
import net.sf.dynamicreports.report.constant.PageType;
import net.sf.dynamicreports.report.constant.Position;
import net.sf.dynamicreports.report.exception.DRException;
import net.sf.jasperreports.engine.JRException;
import net.sf.jasperreports.engine.data.JsonDataSource;

/**
 * Waldflächen aller Waldbesitzer.
 *
 * @author Joerg Reichert <joerg@mapzone.io>
 */
public class Report106c
        extends WbvReport {

    private static Log      log = LogFactory.getLog( Report106c.class );


    @Override
    public String getName() {
        return "WBV 1.06c - Fläche nach Gemarkung und EA";
    }


    @Override
    public JasperReportBuilder build() throws DRException, JRException, IOException {
        List<Gemarkung> gemarkungen = new ArrayList( 256 );
        final ListMultimap<String,Flurstueck> gemarkung2Flurstuecke = ArrayListMultimap.create( 256, 100 );
        final Map<Integer,String> flurstuecke2Art = new HashMap( 4096 );

        for (Waldbesitzer wb : revierWaldbesitzer()) {
            for (Flurstueck flurstueck : wb.flurstuecke( revier.get() )) {
                Gemarkung gemarkung = flurstueck.gemarkung.get();
                if (gemarkung != null) {
                    gemarkungen.add( gemarkung );
                    gemarkung2Flurstuecke.put( (String)gemarkung.id(), flurstueck );
                }
                flurstuecke2Art.put( flurstueck.hashCode(), getArt( wb.eigentumsArt.get() ) );
            }
        }

        final List<String> arten = new ArrayList<String>();
        arten.add( "LW" );
        arten.add( "BW" );
        arten.add( "KiW4_2" );
        // TODO
        // arten.add("KiW\nnach §4/3");
        arten.add( "PW" );
        arten.add( "TW" );
        arten.add( "Kow_KoeW" );
        arten.add( "ohne_ea" );

        Map<String,String> artenLabels = new HashMap<String,String>();
        artenLabels.put( "LW", "LW" );
        artenLabels.put( "BW", "BW" );
        artenLabels.put( "KiW4_2", "KiW\nnach §4/2" );
        // TODO
        // artenLabels.put("KiW\nnach §4/3");
        artenLabels.put( "PW", "PW" );
        artenLabels.put( "TW", "TW" );
        artenLabels.put( "Kow_KoeW", "Kow/KöW" );
        artenLabels.put( "ohne_ea", "ohne EA" );

        // datasource
        JsonBuilder jsonBuilder = new JsonBuilder( gemarkungen ) {
            @Override
            protected Object buildJson( Object value ) {
                Object result = super.buildJson( value );
                //
                if (value instanceof Gemarkung) {
                    Gemarkung gemarkungObj = (Gemarkung)value;
                    JSONObject resultObj = (JSONObject)result;
                    resultObj.put( "gemeinde", gemarkungObj.gemeinde.get() );
                    resultObj.put( "gemarkung", gemarkungObj.gemarkung.get() );

                    double totalSum = 0d, sum = 0d;
                    for (Flurstueck f : gemarkung2Flurstuecke.get( (String)gemarkungObj.id() )) {
                        if (flurstuecke2Art.get( f.hashCode() ) == null) {
                            sum += f.flaecheWald.get();
                        }
                    }
                    resultObj.put( "ohne_ea", sum );
                    for (String art : arten) {
                        sum = 0d;
                        for (Flurstueck f : gemarkung2Flurstuecke.get( (String)gemarkungObj.id() )) {
                            if (flurstuecke2Art.get( f.hashCode() ).equals( art )) {
                                sum += f.flaecheWald.get();
                            }
                        }
                        resultObj.put( art, sum );
                        totalSum += sum;
                    }
                    resultObj.put( "gesamt", totalSum );
                }
                return result;
            }
        };

        // report
        TextColumnBuilder<String> gemeindeColumn = col.column( "Gemeinde", "gemeinde", type.stringType() );
        TextColumnBuilder<String> gemarkungColumn = col.column( "Gemarkung", "gemarkung", type.stringType() );

        List<TextColumnBuilder<Double>> flaecheColumns = new ArrayList<TextColumnBuilder<Double>>();
        TextColumnBuilder<Double> flaecheColumn;
        for (String art : arten) {
            flaecheColumn = col.column( artenLabels.get( art ), art, type.doubleType() ).setValueFormatter(
                    new NumberFormatter( 1, 2, 100, 2 ) );
            flaecheColumns.add( flaecheColumn );
        }
        TextColumnBuilder<Double> sumColumn = col.column( "Summe", "gesamt", type.doubleType() ).setValueFormatter(
                new NumberFormatter( 1, 2, 100, 2 ) );

        ReportTemplateBuilder templateBuilder = template();
        templateBuilder.setGroupShowColumnHeaderAndFooter( false );
        templateBuilder.setGroupHeaderLayout( GroupHeaderLayout.VALUE );
        templateBuilder.setSubtotalLabelPosition( Position.BOTTOM );
        templateBuilder.setSubtotalStyle( stl.style().setTopBorder( stl.pen1Point() ) );
        templateBuilder.setGroupStyle( stl.style( stl.style().bold() )
                .setHorizontalAlignment( HorizontalAlignment.LEFT ) );
        templateBuilder.setGroupTitleStyle( stl.style( stl.style().bold() ).setHorizontalAlignment(
                HorizontalAlignment.LEFT ) );

        JasperReportBuilder report = report()
                .setTemplate( templateBuilder )
                .setDataSource( new JsonDataSource( jsonBuilder.run() ) )

                .setPageFormat( PageType.A4, PageOrientation.LANDSCAPE )
                .title( cmp.text( "Flächenverzeichnis nach Gemarkung und EA" ).setStyle( titleStyle ),
                        cmp.text( "Basis: Waldfläche der Waldbesitzer" ).setStyle( headerStyle ),
                        cmp.text( "Forstbezirk: Mittelsachsen" ).setStyle( headerStyle ),
                        cmp.text( "Revier: " + getRevier() /*+ " / Abfrage: \"" + getQuery() + "\""*/ ).setStyle( headerStyle ), 
                        cmp.text( df.format( new Date() ) ).setStyle( headerStyle ),
                        cmp.text( "" ).setStyle( headerStyle ) ).pageFooter( cmp.pageXofY().setStyle( footerStyle ) )
                // number of page
                .setDetailOddRowStyle( highlightRowStyle ).setColumnTitleStyle( columnTitleStyle )
                .sortBy( gemeindeColumn, gemarkungColumn );

        report.addColumn( gemeindeColumn );
        report.addColumn( gemarkungColumn );
        for (@SuppressWarnings("hiding") TextColumnBuilder<Double> col : flaecheColumns) {
            report.addColumn( col );
        }
        report.addColumn( sumColumn );
        return report;
    }


    private String getArt( Waldeigentumsart art ) {
        if (art == null) {
            return "ohne_ea";
        }
        switch (art) {
            case Privat:    return "PW";
            case Kirche42:  return "KiW4_2";
            case Kirche43:  return "KiW4_3";
//            case :
//                artStr = "LW";
//                break;
//            case BV:
//                artStr = "BW";
//                break;
//            case T:
//                artStr = "TW";
//                break;
            case Unbekannt: return "ohne_ea";
            default:        return "ohne_ea";  // "Kow_KoeW";
        }
    }


//    private List<Flurstueck> getFlurstueckeForGemarkung( Map<Gemarkung,List<Flurstueck>> gemarkung2Flurstuecke,
//            Gemarkung key ) {
//        List<Flurstueck> fs = gemarkung2Flurstuecke.get( key );
//        if (fs == null) {
//            fs = new ArrayList<Flurstueck>();
//            gemarkung2Flurstuecke.put( key, fs );
//        }
//        return fs;
//    }
}
