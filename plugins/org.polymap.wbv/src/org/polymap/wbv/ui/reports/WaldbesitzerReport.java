/*
 * polymap.org 
 * Copyright (C) 2015 individual contributors as indicated by the @authors tag. 
 * All rights reserved.
 * 
 * This is free software; you can redistribute it and/or modify it under the terms of
 * the GNU Lesser General Public License as published by the Free Software
 * Foundation; either version 3 of the License, or (at your option) any later
 * version.
 * 
 * This software is distributed in the hope that it will be useful, but WITHOUT ANY
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A
 * PARTICULAR PURPOSE. See the GNU Lesser General Public License for more details.
 */
package org.polymap.wbv.ui.reports;

import static net.sf.dynamicreports.report.builder.DynamicReports.field;

import java.util.List;

import com.google.common.base.Joiner;
import com.google.common.base.Strings;

import org.polymap.wbv.model.Kontakt;
import org.polymap.wbv.model.Waldbesitzer;

import net.sf.dynamicreports.report.builder.expression.AbstractComplexExpression;
import net.sf.dynamicreports.report.definition.ReportParameters;

/**
 * 
 * @author Joerg Reichert <joerg@mapzone.io>
 */
public abstract class WaldbesitzerReport
        extends WbvReport {
    
    protected String besitzerName( Waldbesitzer wb ) {
        Kontakt besitzer = wb.besitzer();
        return besitzer != null ? besitzer.anzeigename() : "(kein Besitzer festgelegt)";
    }

    
    protected String calculateAdresse( Waldbesitzer wb ) {
        Kontakt besitzer = wb.besitzer();
        if (besitzer == null) {
            return "(kein Besitzer festgelegt)";
        }
        else {
            String strasse = besitzer.strasse.get();
            String ortsteil = besitzer.ortsteil.get();
            String plz = besitzer.plz.get();
            String ort = besitzer.ort.get();
            StringBuilder sb = new StringBuilder();
            if (!Strings.isNullOrEmpty(ortsteil)) {
                sb.append( " OT " + ortsteil + ", " );
            }
            if (!Strings.isNullOrEmpty(strasse)) {
                sb.append( strasse + ", " );
            }
            if (!Strings.isNullOrEmpty(plz)) {
                sb.append( plz + " " );
            }
            if (!Strings.isNullOrEmpty(ort)) {
                sb.append( ort );
            }
            return sb.toString();
        }
    }


//    protected String calculateName( Waldbesitzer wb ) {
//        StringBuilder sb = new StringBuilder();
//        String name = wb.besitzer().name.get();
//        String vorname = wb.besitzer().vorname.get();
//        String organisation = wb.besitzer().organisation.get();
//        if (!Strings.isNullOrEmpty( name )) {
//            sb.append( name );
//        }
//        if (!Strings.isNullOrEmpty( vorname )) {
//            sb.append( ", " + vorname );
//        }
//        return sb.toString();
//    }


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

            // String name = (String)values.get( 0 );
            // String vorname = (String)values.get( 1 );
            return Joiner.on( ", " ).skipNulls().join( /* anrede.get(), */name, vorname );
        }
    }
}
