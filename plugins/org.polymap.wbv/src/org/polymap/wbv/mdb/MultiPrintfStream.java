package org.polymap.wbv.mdb;

import java.io.PrintStream;
import java.util.Locale;

public class MultiPrintfStream {

    private PrintStream outs[];


    public MultiPrintfStream( PrintStream... printStreams ) {
        outs = printStreams;
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
