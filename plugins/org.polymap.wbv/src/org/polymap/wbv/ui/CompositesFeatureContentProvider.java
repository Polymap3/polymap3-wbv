/*
 * Copyright (C) 2014, Falko Bräutigam. All rights reserved.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 */
package org.polymap.wbv.ui;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.eclipse.jface.viewers.Viewer;

import org.polymap.rhei.table.DefaultFeatureTableElement;
import org.polymap.rhei.table.IFeatureContentProvider;
import org.polymap.rhei.table.IFeatureTableElement;

import org.polymap.model2.Association;
import org.polymap.model2.Composite;
import org.polymap.model2.Entity;
import org.polymap.model2.Property;
import org.polymap.model2.PropertyBase;
import org.polymap.model2.query.Query;
import org.polymap.model2.runtime.PropertyInfo;

/**
 * Used to display {@link Entity} collections as result of a {@link Query}, or the
 * contents of a collection property that contains {@link Composite} instances.
 *
 * @author <a href="http://www.polymap.de">Falko Bräutigam</a>
 */
class CompositesFeatureContentProvider
        implements IFeatureContentProvider {

    private static Log log = LogFactory.getLog( CompositesFeatureContentProvider.class );

    private Iterable<? extends Composite>   composites;


    public CompositesFeatureContentProvider() {
    }


    public CompositesFeatureContentProvider( Iterable<? extends Composite> composites ) {
        this.composites = composites;
    }


    public void inputChanged( Viewer viewer, Object oldInput, Object newInput ) {
        this.composites = (Iterable<? extends Composite>)newInput;
    }


    public Object[] getElements( Object input ) {
        log.info( "getElements(): input=" + input.getClass().getName() );

        List<IFeatureTableElement> result = new ArrayList( 1024 );
        for (final Composite composite : composites) {
            result.add( new FeatureTableElement( composite ) );
        }
        return result.toArray();
    }

    
    public void dispose() {
    }


    /**
     *
     */
    public static class FeatureTableElement
            extends DefaultFeatureTableElement {

        public static <T extends Composite> T entity( Object elm ) {
            return (T)((FeatureTableElement)elm).getComposite();
        }

        
        // instance ***************************************
        
        private Composite       composite;


        protected FeatureTableElement( Composite composite ) {
            this.composite = composite;
        }
        
        public Composite getComposite() {
            return composite;
        }

        @Override
        public Object getValue( String name ) {
            try {
                PropertyInfo propInfo = composite.info().getProperty( name );
                if (propInfo == null) {
                    return null;
                }
                PropertyBase prop = propInfo.get( composite );
                if (prop instanceof Property) {
                    return ((Property)prop).get();
                }
                else if (prop instanceof Association) {
                    // XXX sorting???
                    return ((Association)prop).get();
                }
                else {
                    return null;
                }
            }
            catch (Exception e) {
                throw new RuntimeException( e );
            }
        }

        @Override
        public void setValue( String name, Object value ) {
            try {
                PropertyInfo propInfo = composite.info().getProperty( name );
                PropertyBase prop = propInfo.get( composite );
                if (prop instanceof Property) {
                    ((Property)prop).set( value );
                }
                else if (prop instanceof Association) {
                    ((Association)prop).set( (Entity)value );
                }
                else {
                    throw new RuntimeException( "Unknown Property type: " + prop );
                }
            }
            catch (Exception e) {
                throw new RuntimeException( e );
            }
        }

        @Override
        public String fid() {
            if (composite instanceof Entity) {
                return (String)((Entity)composite).id();
            }
            else if (composite instanceof Composite) {
                log.debug( "Using hashCode as fid for Composite: " + composite.getClass().getSimpleName() );
                return String.valueOf( composite.hashCode() );
            }
            else {
                throw new RuntimeException( "Don't know how to build fid out of type: " + composite.getClass() );
            }
        }

    }

}
