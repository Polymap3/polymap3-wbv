/*
 * Copyright (C) 2013-2014, Falko Bräutigam. All rights reserved.
 * 
 * This is free software; you can redistribute it and/or modify it under the terms of
 * the GNU Lesser General Public License as published by the Free Software
 * Foundation; either version 3.0 of the License, or (at your option) any later
 * version.
 * 
 * This software is distributed in the hope that it will be useful, but WITHOUT ANY
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A
 * PARTICULAR PURPOSE. See the GNU Lesser General Public License for more details.
 */
package org.polymap.wbv.ui;

import static com.google.common.collect.ImmutableList.copyOf;
import static com.google.common.collect.Iterables.transform;
import static java.util.Arrays.asList;

import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import java.beans.PropertyChangeEvent;

import org.geotools.feature.NameImpl;
import org.geotools.feature.type.AttributeDescriptorImpl;
import org.geotools.feature.type.AttributeTypeImpl;
import org.opengis.feature.type.AttributeType;
import org.opengis.feature.type.PropertyDescriptor;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.time.FastDateFormat;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.google.common.base.Function;
import com.google.common.base.Joiner;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;

import org.eclipse.jface.viewers.ColumnLabelProvider;

import org.polymap.core.data.ui.featuretable.DefaultFeatureTableColumn;
import org.polymap.core.data.ui.featuretable.FeatureTableViewer;
import org.polymap.core.data.ui.featuretable.IFeatureTableElement;
import org.polymap.core.model2.runtime.UnitOfWork;
import org.polymap.core.runtime.event.Event;
import org.polymap.core.runtime.event.EventFilter;
import org.polymap.core.runtime.event.EventHandler;
import org.polymap.core.runtime.event.EventManager;

import org.polymap.wbv.model.Flurstueck;
import org.polymap.wbv.model.Kontakt;
import org.polymap.wbv.model.Waldbesitzer;
import org.polymap.wbv.ui.CompositesFeatureContentProvider.FeatureTableElement;

/**
 * 
 * 
 * @author <a href="http://www.polymap.de">Falko Bräutigam</a>
 */
public class WaldbesitzerTableViewer
        extends FeatureTableViewer {

    private static Log                  log  = LogFactory.getLog( WaldbesitzerTableViewer.class );

    private static final FastDateFormat df   = FastDateFormat.getInstance( "dd.MM.yyyy" );

    private UnitOfWork                  uow;


    public WaldbesitzerTableViewer( UnitOfWork uow, Composite parent, Iterable<Waldbesitzer> rs, int style ) {
        super( parent, /* SWT.VIRTUAL | SWT.V_SCROLL | SWT.FULL_SELECTION | */SWT.NONE );
        this.uow = uow;
        try {
            NameColumn nameColumn = new NameColumn(); 
            addColumn( nameColumn );
            addColumn( new FlurstueckColumn() );
            nameColumn.sort( SWT.UP );

            // suppress deferred loading to fix "empty table" issue
            // setContent( fs.getFeatures( this.baseFilter ) );
            setContent( new CompositesFeatureContentProvider( rs ) );
            setInput( rs );

            /* Register for property change events */
            EventManager.instance().subscribe( this, new EventFilter<PropertyChangeEvent>() {
                public boolean apply( PropertyChangeEvent input ) {
                    return input.getSource() instanceof Waldbesitzer;
                }
            });
        }
        catch (Exception e) {
            throw new RuntimeException( e );
        }
    }


    @EventHandler(display=true, delay=1000, scope=Event.Scope.JVM)
    protected void someWaldBesitzerChanged( List<PropertyChangeEvent> ev ) {
        if (!getControl().isDisposed()) {
            refresh();
        }
    }


    public List<Waldbesitzer> getSelected() {
        return copyOf( transform( asList( getSelectedElements() ), new Function<IFeatureTableElement,Waldbesitzer>() {
            public Waldbesitzer apply( IFeatureTableElement input ) {
                return (Waldbesitzer)((FeatureTableElement)input).getComposite();
            }
        }));
    }

    
    /**
    *
    */
   class NameColumn
           extends DefaultFeatureTableColumn {

       public NameColumn() {
           super( createDescriptor( "besitzer", String.class ) );
           setWeight( 2, 120 );
           setHeader( "Name" );
           setAlign( SWT.LEFT );
//           setComparator( new ViewerComparator() {                
//           });
           setLabelProvider( new ColumnLabelProvider() {
               @Override
               public String getText( Object elm ) {
                   Waldbesitzer wb = (Waldbesitzer)((FeatureTableElement)elm).getComposite();
                   Kontakt besitzer = wb.besitzer();
                   return besitzer != null ? besitzer.anzeigename() : "(kein Besitzer festgelegt)";
               }
               @Override
               public String getToolTipText( Object elm ) {
                   return getText( elm );
               }
           });
       }
   }


   /**
    *
    */
   class FlurstueckColumn
           extends DefaultFeatureTableColumn {

       public FlurstueckColumn() {
           super( createDescriptor( "flurstuecke", String.class ) );
           setWeight( 2, 120 );
           setHeader( "Flurstücke" );
           setAlign( SWT.LEFT );
           //          setComparator( new ViewerComparator() {                
           //          });
           setLabelProvider( new ColumnLabelProvider() {
               @Override
               public String getText( Object elm ) {
                   Waldbesitzer wb = (Waldbesitzer)((FeatureTableElement)elm).getComposite();
                   Set<String> names = new TreeSet();
                   for (Flurstueck flurstueck : wb.flurstuecke) {
                       names.add( flurstueck.gemeinde.get().name.get() );    
                   }
                   return StringUtils.abbreviate( Joiner.on( ", " ).join( names ), 30 );
               }
               @Override
               public String getToolTipText( Object elm ) {
                   Waldbesitzer wb = (Waldbesitzer)((FeatureTableElement)elm).getComposite();
                   Set<String> names = new TreeSet();
                   for (Flurstueck flurstueck : wb.flurstuecke) {
                       String name = flurstueck.gemeinde.get().name.get() + "/" + flurstueck.gemarkung.get().name.get();
                       names.add( name );    
                   }
                   StringBuilder result = new StringBuilder( 1024 );
                   for (String name : names) {
                       result.append( name ).append( "\n" );
                   }
                   return result.toString();
               }
           });
       }
   }


    public static PropertyDescriptor createDescriptor( String _name, Class binding ) {
        NameImpl name = new NameImpl( _name );
        AttributeType type = new AttributeTypeImpl( name, binding, true, false, null, null, null );
        return new AttributeDescriptorImpl( type, name, 1, 1, false, null );
    }

}
