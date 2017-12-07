/* 
 * polymap.org
 * Copyright (C) 2017, the @authors. All rights reserved.
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

import org.polymap.wbv.ui.reports.WbvReport.NumberFormatter;

import net.sf.dynamicreports.report.definition.ReportParameters;

/**
 *
 * @author Joerg Reichert <joerg@mapzone.io>
 * @author <a href="http://www.polymap.de">Falko Bräutigam</a>
 */
public class HaNumberFormatter
        extends NumberFormatter {

    public HaNumberFormatter() {
        super( 1, 2, 10000, 2 );
    }
    
    public HaNumberFormatter( int minInt, int minFrac, int maxInt, int maxFrac ) {
        super( minInt, minFrac, maxInt, maxFrac );
    }

    @Override
    public String format( Number value, ReportParameters params ) {
        return super.format( value, params ) + " ha";
    }
    
}