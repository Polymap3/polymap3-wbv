/* 
 * polymap.org
 * Copyright (C) 2014 Polymap GmbH. All rights reserved.
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
package org.polymap.wbv.mdb;

import java.io.PrintStream;
import java.util.Locale;

/**
 * 
 *
 * @author <a href="http://www.polymap.de">Bertram Kirsch</a>
 * @author <a href="http://www.polymap.de">Falko Bräutigam</a>
 */
public class MultiPrintfStream {

    private PrintStream outs[];


    public MultiPrintfStream( PrintStream... printStreams ) {
        outs = printStreams;
    }


    public void print( String msg ) {
        for (int i = 0; i < outs.length; i++) {
            outs[i].print( msg );
        }
    }


    public void printf( String format, Object... args ) {
        for (int i = 0; i < outs.length; i++) {
            outs[i].printf( format, args );
        }
    }


    public void printf( Locale l, String format, Object... args ) {
        for (int i = 0; i < outs.length; i++) {
            outs[i].printf( l, format, args );
        }
    }

}
