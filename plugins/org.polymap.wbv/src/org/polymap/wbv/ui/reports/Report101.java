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
import static net.sf.dynamicreports.report.builder.DynamicReports.type;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import net.sf.dynamicreports.jasper.builder.JasperReportBuilder;
import net.sf.dynamicreports.report.builder.column.TextColumnBuilder;
import net.sf.dynamicreports.report.constant.PageOrientation;
import net.sf.dynamicreports.report.constant.PageType;
import net.sf.dynamicreports.report.exception.DRException;
import net.sf.jasperreports.engine.JRException;
import net.sf.jasperreports.engine.data.JsonDataSource;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.json.JSONArray;
import org.json.JSONObject;
import org.polymap.wbv.model.Flurstueck;
import org.polymap.wbv.model.Waldbesitzer;

/**
 * Waldflächen aller Waldbesitzer.
 *
 * @author <a href="http://www.polymap.de">Falko Bräutigam</a>
 */
public class Report101
        extends WbvReport {

    private static Log log = LogFactory.getLog( Report101.class );
    
    
    @Override
    public String getName() {
        return "WBV 1.01";
    }
    
    


    @Override
    public JasperReportBuilder build() throws DRException, JRException, IOException {
        super.build();
        
        List<Flurstueck> flurstuecke = new ArrayList<Flurstueck>();  
        Map<Flurstueck, Waldbesitzer> flurstueck2Waldbesitzer = new HashMap<Flurstueck, Waldbesitzer>();
        
        entities.forEach( entity -> {
            if(entity instanceof Waldbesitzer) {
                Waldbesitzer wb = (Waldbesitzer) entity;
                wb.flurstuecke.forEach( flurstueck -> {
                    flurstuecke.add( flurstueck );
                    flurstueck2Waldbesitzer.put( flurstueck, wb );
                } );
            }
        } );
        
        // datasource
        JsonBuilder jsonBuilder = new JsonBuilder( flurstuecke ) {
            @Override
            protected Object buildJson( Object value ) {
                Object result = super.buildJson( value );
                //
                if (value instanceof Flurstueck) {
                    JSONObject resultObj = (JSONObject) result;
                    Flurstueck flurstueck = (Flurstueck)value;
                    Waldbesitzer wb = flurstueck2Waldbesitzer.get( flurstueck );
                    resultObj.put( "name", wb.besitzer().name.get() + ", " + wb.besitzer().vorname.get() );
                    String gemeinde, gemarkung, flstNr;
                    double gesamtFlaeche, waldFlaeche;
                    if(flurstueck.gemarkung.isPresent()) {
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
                }
                return result;
            }
        };        
        
        // report
        TextColumnBuilder<String> nameColumn = col.column( "Waldbesitzer", "name", type.stringType() );
        TextColumnBuilder<String> gemeindeColumn = col.column( "Gemeinde", "gemeinde", type.stringType() );
        TextColumnBuilder<String> gemarkungColumn = col.column( "Waldbesitzer Gemeinde \n               Gemarkung", "gemarkung", type.stringType() ); // TODO should be right below Gemeinde
        TextColumnBuilder<String> flstnrColumn = col.column( "Flst-Nr", "flst_nr", type.stringType() );
        TextColumnBuilder<Double> gesamtFlaecheColumn = col.column( "Gesamtfläche", "gesamtFlaeche", type.doubleType() )
                .setValueFormatter( new NumberFormatter( 1, 4, 100, 4 ) );
        TextColumnBuilder<Double> waldFlaecheColumn = col.column( "davon Wald", "flaecheWaldAnteilig", type.doubleType() )
                .setValueFormatter( new NumberFormatter( 1, 4, 100, 4 ) );
        
        return report()
                .setDataSource( new JsonDataSource( jsonBuilder.run() ) )
        
            .setPageFormat( PageType.A4, PageOrientation.PORTRAIT )
            .title( 
                    cmp.text( "Waldflächen der Waldbesitzer" ).setStyle( titleStyle ),
                    cmp.text( df.format( new Date() ) ).setStyle( headerStyle ),
                    cmp.text( "" ).setStyle( headerStyle ) )
            .pageFooter( cmp.pageXofY().setStyle( footerStyle ) ) // number of page
            .setDetailOddRowStyle( highlightRowStyle )
            .setColumnTitleStyle( columnTitleStyle )
            .columns( nameColumn, gemeindeColumn, gemarkungColumn, flstnrColumn, gesamtFlaecheColumn, waldFlaecheColumn )
            .columnGrid( nameColumn, grid.verticalColumnGridList(gemeindeColumn, gemarkungColumn), flstnrColumn, gesamtFlaecheColumn, waldFlaecheColumn )
            .subtotalsAtSummary(sbt.sum(gesamtFlaecheColumn), sbt.sum(waldFlaecheColumn)) 
            .groupBy( nameColumn, gemeindeColumn )
            .sortBy( asc( nameColumn ) );
    }
}
