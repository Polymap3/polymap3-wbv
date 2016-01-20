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

import static net.sf.dynamicreports.report.builder.DynamicReports.stl;

import java.awt.Color;
import java.io.IOException;
import java.text.NumberFormat;
import java.util.Locale;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.polymap.rhei.batik.Context;
import org.polymap.rhei.batik.Scope;

import org.polymap.wbv.model.Revier;

import net.sf.dynamicreports.jasper.builder.JasperReportBuilder;
import net.sf.dynamicreports.report.base.expression.AbstractValueFormatter;
import net.sf.dynamicreports.report.builder.style.SimpleStyleBuilder;
import net.sf.dynamicreports.report.builder.style.StyleBuilder;
import net.sf.dynamicreports.report.constant.HorizontalAlignment;
import net.sf.dynamicreports.report.definition.ReportParameters;
import net.sf.dynamicreports.report.exception.DRException;
import net.sf.jasperreports.engine.JRException;

/**
 * Mostly style templates for WBV.
 *
 * @author <a href="http://www.polymap.de">Falko Bräutigam</a>
 */
public abstract class WbvReport
        extends EntityReport {

    private static Log log = LogFactory.getLog( WbvReport.class );

    protected StyleBuilder          bold;

    protected StyleBuilder          titleStyle;

    protected StyleBuilder          columnTitleStyle;

    protected StyleBuilder          footerStyle;

    protected StyleBuilder          headerStyle;
    
    protected SimpleStyleBuilder    highlightRowStyle;

    @Scope( "org.polymap.wbv.ui" )
    protected Context<Revier>       revier;

    @Scope( "org.polymap.wbv.ui" )
    protected Context<String>       queryString;


    protected String getQuery() {
        return queryString.get();
    }


    protected String getRevier() {
        Revier result = revier.get();
        return result != null ? result.name : "Alle";
    }


    /**
     * Creates template styles. Sub-classes should invoke this super implementation
     * before doing their work.
     */
    @Override
    public JasperReportBuilder build() throws DRException, JRException, IOException {
        bold = stl.style().bold();

        titleStyle = stl.style()
                .setHorizontalAlignment( HorizontalAlignment.LEFT )
                .setFontSize( 20 )
                .setPadding( stl.padding().setTop( 10 ).setBottom( 10 ) );

        headerStyle = stl.style()
                .setHorizontalAlignment( HorizontalAlignment.RIGHT )
                .setPadding( stl.padding().setTop( 0 ).setBottom( 2 ) );

        footerStyle = stl.style()
                .setHorizontalAlignment( HorizontalAlignment.CENTER );

        columnTitleStyle = stl.style( bold )
                .setHorizontalAlignment( HorizontalAlignment.CENTER )
                .setPadding( 5 )
                .setBorder( stl.penThin() )
                .setBackgroundColor( new Color( 240, 240, 248 ) );

        highlightRowStyle = stl.simpleStyle()
                .setBackgroundColor( new Color( 243, 243, 248 ) );

        return null;
    }


    /**
     * 
     */
    public static class NumberFormatter<T extends Number>
            extends AbstractValueFormatter<String,T> {

        @SuppressWarnings("hiding")
        private NumberFormat nf;


        public NumberFormatter( int minInt, int minFrac, int maxInt, int maxFrac ) {
            nf = NumberFormat.getInstance( Locale.GERMAN );
            nf.setMinimumIntegerDigits( minInt );
            nf.setMinimumFractionDigits( minFrac );
            nf.setMaximumIntegerDigits( maxInt );
            nf.setMaximumFractionDigits( maxFrac );
        }


        @Override
        public String format( T value, ReportParameters params ) {
            return nf.format( value );
        }

        public String format( T value ) {
            return nf.format( value );
        }

    }
    
}
