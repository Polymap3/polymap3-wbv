/* 
 * polymap.org
 * Copyright (C) 2020, the @authors. All rights reserved.
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

import java.util.List;
import java.util.ListIterator;
import java.util.function.Supplier;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import org.supercsv.io.CsvListWriter;
import org.supercsv.io.ICsvListWriter;
import org.supercsv.prefs.CsvPreference;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * 
 * @author falko
 */
public class CsvBuilder {

    private static final Log log = LogFactory.getLog( CsvBuilder.class );

    
    public static void toExcelCsv( Supplier<List<?>> lines, OutputStream out ) throws IOException {
        // quoteChar, delimiterChar, endOfLineSymbols
        CsvPreference prefs = CsvPreference.EXCEL_NORTH_EUROPE_PREFERENCE; //  // new CsvPreference( '"', ',', "\r\n" );
        ICsvListWriter csv = new CsvListWriter( new OutputStreamWriter( out, "ISO-8859-1" ), prefs );
        
        for (List<?> line = lines.get(); line != null; line = lines.get()) {
            for (@SuppressWarnings( "rawtypes" )
            ListIterator it = line.listIterator(); it.hasNext(); ) {
                if (it.next() == null) {
                    it.set( "" );
                }
            }
            //line = line.stream().map( elm -> ObjectUtils.defaultIfNull( elm, "" ) ).collect( Collectors.toList() );
            csv.write( line );
        }
        csv.close();
    }
}
