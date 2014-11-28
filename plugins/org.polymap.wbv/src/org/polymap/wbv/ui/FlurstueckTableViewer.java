/*
 * Copyright (C) 2014, Falko Bräutigam. All rights reserved.
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
import com.google.common.base.Objects;

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
import org.polymap.wbv.model.Gemarkung;
import org.polymap.wbv.model.Gemeinde;
import org.polymap.wbv.model.Waldbesitzer;
import org.polymap.wbv.ui.CompositesFeatureContentProvider.FeatureTableElement;

/**
 * 
 * 
 * @author <a href="http://www.polymap.de">Falko Bräutigam</a>
 */
public class FlurstueckTableViewer
        extends FeatureTableViewer {

    private static Log                  log  = LogFactory.getLog( FlurstueckTableViewer.class );

    private static final FastDateFormat df   = FastDateFormat.getInstance( "dd.MM.yyyy" );

    private UnitOfWork                  uow;


    public FlurstueckTableViewer( UnitOfWork uow, Composite parent, Iterable<Flurstueck> rs ) {
        super( parent, /* SWT.VIRTUAL | SWT.V_SCROLL | SWT.FULL_SELECTION | */SWT.FULL_SELECTION );
        this.uow = uow;
        try {
            // Gemeinde
            String propName = Flurstueck.TYPE.gemeinde.getInfo().getName();
            addColumn( new DefaultFeatureTableColumn( createDescriptor( propName, String.class ) )
                .setWeight( 2, 120 )
                .setLabelProvider( new ColumnLabelProvider() {
                    @Override
                    public String getText( Object elm ) {
                        Flurstueck entity = FeatureTableElement.entity( elm );
                        Gemeinde gemeinde = entity.gemeinde.get();
                        return gemeinde != null ? gemeinde.name.get() : "(kein Gemeinde)";
                    }
                })
                /*.setEditing( true )*/ )
                .sort( SWT.UP );

            // Gemarkung
            propName = Flurstueck.TYPE.gemarkung.getInfo().getName();
            addColumn( new DefaultFeatureTableColumn( createDescriptor( propName, String.class ) )
                .setWeight( 2, 120 )
                .setLabelProvider( new ColumnLabelProvider() {
                    @Override
                    public String getText( Object elm ) {
                        Flurstueck entity = FeatureTableElement.entity( elm );
                        Gemarkung gemarkung = entity.gemarkung.get();
                        return gemarkung != null ? gemarkung.name.get() : "(kein Gemarkung)";
                    }
                }));
            
            // Flurstücksnummer
            propName = Flurstueck.TYPE.zaehlerNenner.getInfo().getName();
            addColumn( new DefaultFeatureTableColumn( createDescriptor( propName, String.class ) )
                .setWeight( 1, 60 )
                .setHeader( "Nummer" )
                .setLabelProvider( new ColumnLabelProvider() {
                    @Override
                    public String getText( Object elm ) {
                        Flurstueck entity = FeatureTableElement.entity( elm );
                        return StringUtils.defaultIfEmpty( entity.zaehlerNenner.get(), "-" );
                    }
                })
                .setEditing( true ));
            
            // Fläche
            propName = Flurstueck.TYPE.flaeche.getInfo().getName();
            addColumn( new DefaultFeatureTableColumn( createDescriptor( propName, Double.class ) )
                .setWeight( 1, 60 )
                .setHeader( "Fläche\n(in ha)" )
                .setLabelProvider( new ColumnLabelProvider() {
                    @Override
                    public String getText( Object elm ) {
                        Flurstueck entity = FeatureTableElement.entity( elm );
                        return Objects.firstNonNull( entity.flaeche.get(), -1d ).toString();
                    }
                }));
            
            // davon Wald
            addColumn( new DefaultFeatureTableColumn( createDescriptor( "Wald\n(in ha)", Double.class ) )
                .setWeight( 1, 60 )
                .setLabelProvider( new ColumnLabelProvider() {
                    @Override
                    public String getText( Object elm ) {
                        Flurstueck entity = FeatureTableElement.entity( elm );
                        return Objects.firstNonNull( entity.flaecheWald.get(), -1d ).toString();
                    }
                }));

            // Bemerkung
            propName = Flurstueck.TYPE.bemerkung.getInfo().getName();
            addColumn( new DefaultFeatureTableColumn( createDescriptor( propName, String.class ) )
                .setWeight( 2, 120 )
                .setLabelProvider( new ColumnLabelProvider() {
                    @Override
                    public String getText( Object elm ) {
                        Flurstueck entity = FeatureTableElement.entity( elm );
                        return Objects.firstNonNull( entity.bemerkung.get(), "" ).toString();
                    }
                    @Override
                    public String getToolTipText( Object elm ) {
                        Flurstueck entity = FeatureTableElement.entity( elm );
                        return Objects.firstNonNull( entity.bemerkung.get(), "" ).toString();
                    }
                }));

            // suppress deferred loading to fix "empty table" issue
            // setContent( fs.getFeatures( this.baseFilter ) );
            setContent( new CompositesFeatureContentProvider( rs ) );
            setInput( rs );

            /* Register for property change events */
            EventManager.instance().subscribe( this, new EventFilter<PropertyChangeEvent>() {
                public boolean apply( PropertyChangeEvent input ) {
                    return input.getSource() instanceof Flurstueck;
                }
            });
        }
        catch (Exception e) {
            throw new RuntimeException( e );
        }
    }


    @EventHandler(display=true, delay=1000, scope=Event.Scope.JVM)
    protected void entityChanged( List<PropertyChangeEvent> ev ) {
        if (!getControl().isDisposed()) {
            refresh( true );
        }
    }


    public List<Waldbesitzer> getSelected() {
        return copyOf( transform( asList( getSelectedElements() ), new Function<IFeatureTableElement,Waldbesitzer>() {
            public Waldbesitzer apply( IFeatureTableElement input ) {
                return (Waldbesitzer)((FeatureTableElement)input).getComposite();
            }
        }));
    }

    
    public static PropertyDescriptor createDescriptor( String _name, Class binding ) {
        NameImpl name = new NameImpl( _name );
        AttributeType type = new AttributeTypeImpl( name, binding, true, false, null, null, null );
        return new AttributeDescriptorImpl( type, name, 1, 1, false, null );
    }

}
