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
import static net.sf.dynamicreports.report.builder.DynamicReports.field;
import static net.sf.dynamicreports.report.builder.DynamicReports.report;
import static net.sf.dynamicreports.report.builder.DynamicReports.type;

import java.util.Date;
import java.util.List;

import java.io.IOException;

import net.sf.dynamicreports.jasper.builder.JasperReportBuilder;
import net.sf.dynamicreports.report.builder.column.TextColumnBuilder;
import net.sf.dynamicreports.report.builder.expression.AbstractComplexExpression;
import net.sf.dynamicreports.report.constant.PageOrientation;
import net.sf.dynamicreports.report.constant.PageType;
import net.sf.dynamicreports.report.definition.ReportParameters;
import net.sf.dynamicreports.report.exception.DRException;
import net.sf.jasperreports.engine.JRException;
import net.sf.jasperreports.engine.data.JsonDataSource;

import org.json.JSONObject;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.google.common.base.Joiner;

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
public class Report105
        extends WbvReport {

    private static Log log = LogFactory.getLog( Report105.class );
    
    @Scope("org.polymap.wbv.ui")
    private Context<Revier>         revier;
    
    @Scope("org.polymap.wbv.ui")
    private Context<String>         queryString;
    

    @Override
    public String getName() {
        return "WBV 1.05";
    }


    @Override
    public JasperReportBuilder build() throws DRException, JRException, IOException {
        super.build();
        
        // datasource
        JsonBuilder jsonBuilder = new JsonBuilder( entities ) {
            @Override
            protected Object buildJson( Object value ) {
                Object result = super.buildJson( value );
                //
                if (value instanceof Waldbesitzer) {
                    Waldbesitzer wb = (Waldbesitzer)value;
                    double flaeche = 0;
                    for (Flurstueck fs : wb.flurstuecke) {
                        flaeche += fs.flaecheWald.get();
                    }
                    ((JSONObject)result).put( "gesamtWald", flaeche );
                }
                return result;
            }
        };
        
        // report
        TextColumnBuilder<String> nameColumn = col.column( "Waldbesitzer", new NameExpr() );
        TextColumnBuilder<Double> flaecheColumn = col.column( "Fläche (Wald)", "gesamtWald", type.doubleType() )
                .setValueFormatter( new NumberFormatter( 1, 4, 100, 4 ) );
        
        return report()
            .setDataSource( new JsonDataSource( jsonBuilder.run() ) )

            .setPageFormat( PageType.A4, PageOrientation.PORTRAIT )
            .title( 
                    cmp.text( "Waldflächen der Waldbesitzer" ).setStyle( titleStyle ),
                    cmp.text( "Forstbezirk: Mittelsachsen" ).setStyle( headerStyle ),
                    cmp.text( "Revier: " + revier.get().name + " / Abfrage: \"" + queryString.get() + "\"" ).setStyle( headerStyle ),
                    //cmp.text( "Abfrage: \"" + queryString.get() + "\"" ).setStyle( headerStyle ),
                    cmp.text( df.format( new Date() ) ).setStyle( headerStyle ),
                    cmp.text( "" ).setStyle( headerStyle ) )
            .pageFooter( cmp.pageXofY().setStyle( footerStyle ) ) // number of page

            .highlightDetailOddRows()
            .setDetailOddRowStyle( highlightRowStyle )
            
            .setColumnTitleStyle( columnTitleStyle )
            .columns( nameColumn, flaecheColumn )
            .sortBy( asc( nameColumn ) );
    }

    
    /**
     * 
     */
    static class NameExpr 
            extends AbstractComplexExpression<String> {

        public NameExpr() {
            addExpression( field( "besitzerIndex", Integer.class ) );
            addExpression( field( "kontakte[0].name", String.class ) );
            addExpression( field( "kontakte[0].vorname", String.class ) );
            addExpression( field( "kontakte[0].organisation", String.class ) );
            addExpression( field( "kontakte[1].name", String.class ) );
            addExpression( field( "kontakte[1].vorname", String.class ) );
            addExpression( field( "kontakte[1].organisation", String.class ) );
            addExpression( field( "kontakte[2].name", String.class ) );
            addExpression( field( "kontakte[2].vorname", String.class ) );
            addExpression( field( "kontakte[2].organisation", String.class ) );
        }
        
        @Override
        public String evaluate( List<?> values, ReportParameters params ) {
            Integer besitzerIndex = params.getFieldValue( "besitzerIndex" );
            String name = params.getFieldValue( "kontakte[" + besitzerIndex + "].name" );
            String vorname = params.getFieldValue( "kontakte[" + besitzerIndex + "].vorname" );
            
//            String name = (String)values.get( 0 );
//            String vorname = (String)values.get( 1 );
            return Joiner.on( ", " ).skipNulls().join( /*anrede.get(),*/ name, vorname );
        }
    }
}
