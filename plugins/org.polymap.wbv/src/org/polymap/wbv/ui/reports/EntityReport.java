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

import java.util.Collection;
import java.util.Date;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStreamWriter;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;
import java.text.DateFormat;
import java.text.SimpleDateFormat;

import org.json.JSONArray;
import org.json.JSONObject;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.eclipse.core.runtime.IProgressMonitor;

import org.polymap.core.model2.CollectionProperty;
import org.polymap.core.model2.Composite;
import org.polymap.core.model2.Entity;
import org.polymap.core.model2.Property;
import org.polymap.core.model2.PropertyBase;
import org.polymap.core.model2.runtime.PropertyInfo;
import org.polymap.core.runtime.UIJob;

/**
 * 
 *
 * @author <a href="http://www.polymap.de">Falko Bräutigam</a>
 */
public abstract class EntityReport
        extends DownloadableReport {

    private static Log log = LogFactory.getLog( EntityReport.class );
    
    public static final DateFormat          df = new SimpleDateFormat( "dd.MM.yyyy" );

    protected Iterable<? extends Entity>    entities;
    

    public EntityReport setEntities( Iterable<? extends Entity> entities ) {
        this.entities = entities;
        return this;
    }

    
    /**
     * 
     */
    protected static class JsonBuilderJob
            extends UIJob {
        
        private Iterable<? extends Entity>  entities;
        
        private PipedOutputStream           out;

        private OutputStreamWriter          writer;
        

        public JsonBuilderJob( Iterable<? extends Entity> entities ) {
            super( "JsonBuilder" );
            this.entities = entities;
        }

        
        public InputStream run() throws IOException {
            assert out == null;
            out = new PipedOutputStream();
            writer = new OutputStreamWriter( out, "UTF8" );
            PipedInputStream result = new PipedInputStream( out, 4*1024 );
            schedule();
            return result;
        }
        
        
        @Override
        protected void runWithException( IProgressMonitor monitor ) throws Exception {
            try {
                monitor.beginTask( "Daten lesen", IProgressMonitor.UNKNOWN );
                // prefix
                writeln( "[" );
                
                //
                int count = 0;
                for (Entity entity : entities) {
                    JSONObject json = ((JSONObject)buildJson( entity ));
                    writeln( count++ > 0 ? "," : "", json.toString( 4 ) );
                    
                    monitor.worked( 1 );
                    monitor.subTask( "Objekte: " + count );
                    if (monitor.isCanceled()) {
                        break;
                    }
                }

                // suffix
                writeln( "\n]" );
                monitor.done();
            }
            catch (Throwable e) {
                out.close();
                throw e;
            }
        }

        
        /**
         * 
         * <p/>
         * Override this method in order to change, manipulate or complement the
         * created JSON value.
         * 
         * @param value
         * @return
         */
        protected Object buildJson( Object value ) {
            if (value == null) {
                return "null";
            }
            else if (value instanceof String) {
                return value;
            }
            else if (value instanceof Number) {
                return value;
            }
            else if (value instanceof Boolean) {
                return value;
            }
            else if (value instanceof Enum) {
                return ((Enum)value).name();
            }
            else if (value instanceof Date) {
                return df.format( ((Date)value) );
            }
            else if (value instanceof Composite) {
                JSONObject result = new JSONObject();
                Collection<PropertyInfo> props = ((Composite)value).info().getProperties();
                for (PropertyInfo propInfo : props) {
                    PropertyBase prop = propInfo.get( (Composite)value );
                    // Property
                    if (prop instanceof Property) {
                        result.put( propInfo.getName(), buildJson( ((Property)prop).get() ) );
                    }
                    // Collection
                    else if (prop instanceof CollectionProperty) {
                        JSONArray array = new JSONArray();
                        for (Object propValue : ((CollectionProperty)prop)) {
                            array.put( buildJson( propValue ) );                            
                        }
                        result.put( propInfo.getName(), array );
                    }
                    // Association
                    else {
                        // ignore
                    }
                }
                return result;
            }
            else {
                throw new IllegalStateException( "Unknown value type: " + value );
            }
        }
        
        
        protected void writeln( String... strings ) {
            try {
                for (String s : strings) {
                    writer.write( s );
                    if (log.isDebugEnabled()) {
                        System.out.print( s );
                    }
                }
                writer.write( "\n" );
                writer.flush();
                if (log.isDebugEnabled()) {
                    System.out.print( "\n" );
                }
            }
            catch (IOException e) {
                throw new RuntimeException( e );
            }
        }
        
    }    

}
