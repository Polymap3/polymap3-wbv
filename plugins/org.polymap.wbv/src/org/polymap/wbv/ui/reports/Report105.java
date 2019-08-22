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
import static net.sf.dynamicreports.report.builder.DynamicReports.type;

import java.io.IOException;
import java.util.Date;

import net.sf.dynamicreports.jasper.builder.JasperReportBuilder;
import net.sf.dynamicreports.report.builder.column.TextColumnBuilder;
import net.sf.dynamicreports.report.constant.PageOrientation;
import net.sf.dynamicreports.report.constant.PageType;
import net.sf.dynamicreports.report.exception.DRException;
import net.sf.jasperreports.engine.JRException;
import net.sf.jasperreports.engine.data.JsonDataSource;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.json.JSONObject;
import org.polymap.wbv.model.Flurstueck;
import org.polymap.wbv.model.Waldbesitzer;

/**
 * Waldflächen aller Waldbesitzer.
 *
 * @author Joerg Reichert <joerg@mapzone.io>
 * @author <a href="http://www.polymap.de">Falko Bräutigam</a>
 */
public class Report105
        extends WaldbesitzerReport {

    private static final Log log = LogFactory.getLog( Report105.class );

    @Override
    public String getName() {
        return "WBV 1.05 - Summenliste Waldflächen der Waldbesitzer";
    }


    @Override
    public JasperReportBuilder build() throws DRException, JRException, IOException {
        // datasource
        JsonBuilder jsonBuilder = new JsonBuilder( revierWaldbesitzer() ) {
            @Override
            protected Object buildJson( Object value ) {
                Object result = super.buildJson( value );
                //
                if (value instanceof Waldbesitzer) {
                    Waldbesitzer wb = (Waldbesitzer)value;
                    double flaeche = 0;
                    for (Flurstueck fs : wb.flurstuecke( revier.get() )) {
                        if (fs != null && fs.flaecheWald.get() != null) {
                            flaeche += fs.flaecheWald.get();
                        }
                    }
                    ((JSONObject)result).put( "gesamtWald", flaeche );
                }
                return result;
            }
        };

        // report
        TextColumnBuilder<String> nameColumn = col.column( "Waldbesitzer", new NameExpr() );
        TextColumnBuilder<Double> flaecheColumn = col.column( "Fläche (Wald) in ha", "gesamtWald", type.doubleType() )
                .setValueFormatter( new NumberFormatter( 1, 4, 100, 4 ) );

        return report()
                .setDataSource( new JsonDataSource( jsonBuilder.run() ) )

                .setPageFormat( PageType.A4, PageOrientation.PORTRAIT )
                .title( cmp.text( "Waldflächen der Waldbesitzer" ).setStyle( titleStyle ),
                        cmp.text( "Forstbezirk: Mittelsachsen" ).setStyle( headerStyle ),
                        cmp.text( "Revier: " + getRevier() /*+ " / Abfrage: \"" + getQuery() + "\""*/ ) .setStyle( headerStyle ),
                        cmp.text( df.format( new Date() ) ).setStyle( headerStyle ),
                        cmp.text( "" ).setStyle( headerStyle ) ).pageFooter( cmp.pageXofY().setStyle( footerStyle ) )
                // number of page

                .highlightDetailOddRows().setDetailOddRowStyle( highlightRowStyle )

                .setColumnTitleStyle( columnTitleStyle ).columns( nameColumn, flaecheColumn )
                .sortBy( asc( nameColumn ) );
    }
}
