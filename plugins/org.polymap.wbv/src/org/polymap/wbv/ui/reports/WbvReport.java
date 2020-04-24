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
import static net.sf.dynamicreports.report.builder.DynamicReports.cmp;
import static net.sf.dynamicreports.report.builder.DynamicReports.report;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.function.Supplier;

import java.awt.Color;
import java.text.NumberFormat;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.google.common.collect.FluentIterable;
import com.google.common.collect.Lists;

import org.polymap.rhei.batik.BatikApplication;
import org.polymap.rhei.batik.Context;
import org.polymap.rhei.batik.Scope;

import org.polymap.model2.query.Expressions;
import org.polymap.model2.runtime.UnitOfWork;
import org.polymap.wbv.model.Flurstueck;
import org.polymap.wbv.model.Revier;
import org.polymap.wbv.model.Waldbesitzer;
import org.polymap.wbv.model.WbvRepository;

import net.sf.dynamicreports.jasper.builder.JasperReportBuilder;
import net.sf.dynamicreports.report.base.expression.AbstractValueFormatter;
import net.sf.dynamicreports.report.builder.DynamicReports;
import net.sf.dynamicreports.report.builder.ReportTemplateBuilder;
import net.sf.dynamicreports.report.builder.style.SimpleStyleBuilder;
import net.sf.dynamicreports.report.builder.style.StyleBuilder;
import net.sf.dynamicreports.report.constant.HorizontalAlignment;
import net.sf.dynamicreports.report.constant.PageOrientation;
import net.sf.dynamicreports.report.constant.PageType;
import net.sf.dynamicreports.report.constant.Position;
import net.sf.dynamicreports.report.definition.ReportParameters;

/**
 * Mostly style templates for WBV.
 *
 * @author <a href="http://www.polymap.de">Falko Bräutigam</a>
 */
public abstract class WbvReport<R>
        extends EntityReport<R> {

    private static final Log log = LogFactory.getLog( WbvReport.class );

    private static final List<Class<? extends WbvReport<?>>> reports = Arrays.asList( 
            Report102.class, 
            Report103.class,
            Report105.class,
            Report106.class,
            Report106b_1.class,
            Report106c.class,
            AddressExport.class );
    
    public static final List<Supplier<WbvReport<?>>> factories = Lists.transform( reports, cl -> () -> {
            try {
                WbvReport<?> report = cl.newInstance();
                return BatikApplication.instance().getContext().propagate( report );
            }
            catch (Exception e) {
                throw new RuntimeException( e );
            }
    });
    
    // instance ******************************************
    
    protected StyleBuilder          bold;

    protected StyleBuilder          titleStyle;

    protected StyleBuilder          title2Style;

    protected StyleBuilder          columnTitleStyle;

    protected StyleBuilder          footerStyle;

    protected StyleBuilder          headerStyle;
    
    protected SimpleStyleBuilder    highlightRowStyle;

    protected ReportTemplateBuilder reportTemplate;

    @Scope( "org.polymap.wbv.ui" )
    protected Context<Revier>       revier;

    @Scope( "org.polymap.wbv.ui" )
    protected Context<String>       queryString;

    /** Die {@link Waldbesitzer}, die gerade in der Liste angezeigt werden. */
    private Iterable<Waldbesitzer>  viewerEntities;

    
    protected WbvReport() {
        bold = stl.style().bold();

        titleStyle = stl.style()
                .setHorizontalAlignment( HorizontalAlignment.LEFT )
                .setFontSize( 18 )
                .setPadding( stl.padding().setTop( 10 ).setBottom( 5 ) );

        title2Style = stl.style()
                .setHorizontalAlignment( HorizontalAlignment.LEFT )
                .setFontSize( 14 )
                .setPadding( stl.padding().setTop( 0 ).setBottom( 5 ) );

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

        reportTemplate = DynamicReports.template();
        reportTemplate.setSubtotalLabelPosition( Position.BOTTOM );
        reportTemplate.setSummaryStyle( stl.style().setTopBorder( stl.pen1Point() ) );
        reportTemplate.setPageMargin( DynamicReports.margin().setTop( 50 ).setLeft( 45 ).setBottom( 50 ).setRight( 55 ) );
    }

    
    /**
     * Creates a new report with defaults styles and default header.
     */
    protected JasperReportBuilder newReport( String title, String title2 ) {
        return report()
                .setTemplate( reportTemplate )
                .setPageFormat( PageType.A4, PageOrientation.PORTRAIT )
                .title( cmp.text( title ).setStyle( titleStyle ),
                        cmp.text( title2 ).setStyle( title2Style ),
                        cmp.text( "Forstbezirk: Mittelsachsen" ).setStyle( headerStyle ),
                        cmp.text( "Revier: " + getRevier() /*+ " / Abfrage: \"" + getQuery() + "\""*/ ).setStyle( headerStyle ), 
                        cmp.text( df.format( new Date() ) ).setStyle( headerStyle ),
                        cmp.text( "" ).setStyle( headerStyle ) )
                .pageFooter( cmp.pageXofY().setStyle( footerStyle ) )
                .setDetailOddRowStyle( highlightRowStyle )
                .setColumnTitleStyle( columnTitleStyle );
    }
    
    
    public EntityReport<R> setViewerEntities( Iterable<Waldbesitzer> entities ) {
        this.viewerEntities = entities;
        return this;
    }

    
    /** 
     * Die {@link Waldbesitzer}, die gerade in der Liste angezeigt werden. 
     */
    public Iterable<Waldbesitzer> gesuchteWaldbesitzer() {
        return viewerEntities;
    }

    
    /** 
     * Alle {@link Waldbesitzer} im aktuellen {@link Revier}. 
     */
    public Iterable<Waldbesitzer> revierWaldbesitzer() {
        UnitOfWork uow = WbvRepository.unitOfWork();
        return uow.query( Waldbesitzer.class )
                .where( revier.isPresent() ? revier.get().waldbesitzerFilter.get() : Expressions.TRUE )
                .execute();
    }

    
    /** 
     * Alle {@link Flurstueck}e im aktuellen {@link Revier}. 
     */
    public Iterable<Flurstueck> revierFlurstuecke() {
        return FluentIterable.from( revierWaldbesitzer() )
                .transformAndConcat( wb -> wb.flurstuecke( revier.get() ) );
    }


    protected String getQuery() {
        return queryString.get();
    }


    protected String getRevier() {
        Revier result = revier.get();
        return result != null ? result.name : "Alle";
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
