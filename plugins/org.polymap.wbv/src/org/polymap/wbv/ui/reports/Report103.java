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
import static net.sf.dynamicreports.report.builder.DynamicReports.grid;
import static net.sf.dynamicreports.report.builder.DynamicReports.report;
import static net.sf.dynamicreports.report.builder.DynamicReports.sbt;
import static net.sf.dynamicreports.report.builder.DynamicReports.stl;
import static net.sf.dynamicreports.report.builder.DynamicReports.template;
import static net.sf.dynamicreports.report.builder.DynamicReports.type;

import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.List;

import java.io.IOException;

import net.sf.dynamicreports.jasper.builder.JasperReportBuilder;
import net.sf.dynamicreports.report.builder.ReportTemplateBuilder;
import net.sf.dynamicreports.report.builder.column.TextColumnBuilder;
import net.sf.dynamicreports.report.builder.grid.ColumnTitleGroupBuilder;
import net.sf.dynamicreports.report.builder.group.ColumnGroupBuilder;
import net.sf.dynamicreports.report.builder.group.Groups;
import net.sf.dynamicreports.report.constant.GroupHeaderLayout;
import net.sf.dynamicreports.report.constant.HorizontalAlignment;
import net.sf.dynamicreports.report.constant.PageOrientation;
import net.sf.dynamicreports.report.constant.PageType;
import net.sf.dynamicreports.report.constant.Position;
import net.sf.dynamicreports.report.exception.DRException;
import net.sf.jasperreports.engine.JRException;
import net.sf.jasperreports.engine.data.JsonDataSource;

import org.json.JSONObject;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.polymap.core.model2.Composite;

import org.polymap.rhei.batik.Context;
import org.polymap.rhei.batik.ContextProperty;

import org.polymap.wbv.model.Flurstueck;
import org.polymap.wbv.model.Revier;
import org.polymap.wbv.model.Waldbesitzer;

/**
 * Waldflächen aller Waldbesitzer.
 *
 * @author <a href="http://www.polymap.de">Falko Bräutigam</a>
 */
public class Report103
        extends WaldbesitzerReport {

    private static Log      log = LogFactory.getLog( Report103.class );

    @Context(scope="org.polymap.wbv.ui")
    private ContextProperty<Revier> revier;

    @Context(scope="org.polymap.wbv.ui")
    private ContextProperty<String> queryString;


    @Override
    public String getName() {
        return "WBV 1.03";
    }


    @Override
    public JasperReportBuilder build() throws DRException, JRException, IOException {
        super.build();

        ReportTemplateBuilder templateBuilder = template();
        templateBuilder.setGroupShowColumnHeaderAndFooter( false );
        templateBuilder.setGroupHeaderLayout( GroupHeaderLayout.VALUE );
        templateBuilder.setSubtotalLabelPosition( Position.BOTTOM );
        templateBuilder.setGroupStyle( stl.style( stl.style().bold() )
                .setHorizontalAlignment( HorizontalAlignment.LEFT ) );
        templateBuilder.setGroupTitleStyle( stl.style( stl.style().bold() ).setHorizontalAlignment(
                HorizontalAlignment.LEFT ) );

        List<Flurstueck> flurstuecke = new ArrayList<Flurstueck>();

        // nur den ersten, weil sonst Subreports noetig waeren
        Waldbesitzer firstWb = null;

        Iterator<? extends Composite> iter = entities.iterator();
        Composite entity = iter.next();
        if (entity instanceof Waldbesitzer) {
            firstWb = (Waldbesitzer)entity;
            flurstuecke.addAll( firstWb.flurstuecke );
        }

        final Waldbesitzer wb = firstWb;

        // datasource
        JsonBuilder jsonBuilder = new JsonBuilder( flurstuecke ) {

            @Override
            protected Object buildJson( Object value ) {
                Object result = super.buildJson( value );
                //
                if (value instanceof Flurstueck) {
                    JSONObject resultObj = (JSONObject)result;
                    Flurstueck flurstueck = (Flurstueck)value;
                    resultObj.put( "name", wb.besitzer().anzeigename() );
                    resultObj.put( "adresse", calculateAdresse( wb ) );
                    String gemeinde, gemarkung, flstNr;
                    double gesamtFlaeche, waldFlaeche;
                    if (flurstueck.gemarkung.get() != null) {
                        gemeinde = flurstueck.gemarkung.get().gemeinde.get();
                        resultObj.put( "gemeinde", gemeinde );
                        gemarkung = flurstueck.gemarkung.get().gemarkung.get();
                        resultObj.put( "gemarkung", gemarkung );
                    }
                    flstNr = flurstueck.zaehlerNenner.get();
                    resultObj.put( "flst_nr", flstNr );
                    gesamtFlaeche = flurstueck.flaeche.get();
                    waldFlaeche = flurstueck.flaecheWald.get();
                    resultObj.put( "gesamtFlaeche", gesamtFlaeche );
                    resultObj.put( "flaecheWaldAnteilig", waldFlaeche );
                    resultObj.put( "nutzungsart", "" );
                    resultObj.put( "forstort", "" );
                    resultObj.put( "nutzungsflaeche", "" );
                }
                return result;
            }
        };

        TextColumnBuilder<String> gemeindeColumn = col.column( "Gemeinde", "gemeinde", type.stringType() ).setStyle(
                stl.style().bold().setBottomBorder( stl.pen1Point() ) );
        TextColumnBuilder<String> gemarkungColumn = col.column( "Gemarkung", "gemarkung", type.stringType() ).setStyle(
                stl.style().bold().setBottomBorder( stl.pen1Point() ) );
        TextColumnBuilder<String> flstnrColumn = col.column( "Flst-Nr", "flst_nr", type.stringType() ).setStyle(
                stl.style().bold() );
        TextColumnBuilder<Double> gesamtFlaecheColumn = col.column( "Gesamtfläche", "gesamtFlaeche", type.doubleType() )
                .setValueFormatter( nf );
        TextColumnBuilder<Double> waldFlaecheColumn = col.column( "davon Wald", "flaecheWaldAnteilig",
                type.doubleType() ).setValueFormatter( nf );
        TextColumnBuilder<String> nutzungsartColumn = col.column( "Nutzungsart", "nutzungsart", type.stringType() )
                .setStyle( stl.style().bold() );
        TextColumnBuilder<String> forstortColumn = col.column( "Forstort", "forstort", type.stringType() ).setStyle(
                stl.style().bold() );
        TextColumnBuilder<String> nutzungsflaecheColumn = col.column( "Nutzungsfläche", "nutzungsflaeche",
                type.stringType() ).setStyle( stl.style().bold() );

        ColumnTitleGroupBuilder titleGroup1 = grid.titleGroup( "Gemeinde \n   Gemarkung", gemeindeColumn,
                gemarkungColumn );

        ColumnGroupBuilder gemeindeGroupBuilder = Groups.group( gemeindeColumn ).setPadding( 5 );
        ColumnGroupBuilder gemarkungGroupBuilder = Groups.group( gemarkungColumn ).setPadding( 5 );

        JasperReportBuilder report = report()
                .setTemplate( templateBuilder )
                .setDataSource( new JsonDataSource( jsonBuilder.run() ) )
                .setPageFormat( PageType.A4, PageOrientation.PORTRAIT )
                .title( cmp.text( "Waldflächen eines Waldbesitzers" ).setStyle( titleStyle ),
                        cmp.text( df.format( new Date() ) ).setStyle( headerStyle ),
                        cmp.text( "Forstbezirk: Mittelsachsen" ).setStyle( headerStyle ),
                        cmp.text( "Revier: " + getRevier() + " / Abfrage: \"" + getQuery() + "\"" ).setStyle(
                                headerStyle ), cmp.text( firstWb.besitzer().anzeigename() ).setStyle( headerStyle ),
                        cmp.text( calculateAdresse( firstWb ) ).setStyle( headerStyle ) )
                .pageFooter( cmp.pageXofY().setStyle( footerStyle ) )
                // number of page
                .setDetailOddRowStyle( highlightRowStyle )
                .setColumnTitleStyle( columnTitleStyle )
                .setColumnTitleStyle( columnTitleStyle )
                .addGroup( gemeindeGroupBuilder )
                .addGroup( gemarkungGroupBuilder )
                .columns( flstnrColumn, gesamtFlaecheColumn, waldFlaecheColumn, nutzungsartColumn, forstortColumn,
                        nutzungsflaecheColumn )
                .columnGrid( titleGroup1, flstnrColumn, gesamtFlaecheColumn, waldFlaecheColumn, nutzungsartColumn,
                        forstortColumn, nutzungsflaecheColumn )
                .subtotalsAtGroupHeader( gemeindeGroupBuilder, sbt.sum( gesamtFlaecheColumn ).setValueFormatter( nf ),
                        sbt.sum( waldFlaecheColumn ).setValueFormatter( nf ) )
                .subtotalsAtGroupHeader( gemarkungGroupBuilder, sbt.sum( gesamtFlaecheColumn ).setValueFormatter( nf ),
                        sbt.sum( waldFlaecheColumn ).setValueFormatter( nf ) ).subtotalsAtSummary()
                .sortBy( asc( gemeindeColumn ), asc( gemarkungColumn ) );
        return report;
    }


    protected String getQuery() {
        return queryString.get();
    }


    protected String getRevier() {
        return revier.get().name;
    }
}
