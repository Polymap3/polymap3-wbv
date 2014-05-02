/* 
 * polymap.org
 * Copyright (C) 2013, Falko Bräutigam. All rights reserved.
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
package org.polymap.wbv.ui;

import static com.google.common.collect.ImmutableList.copyOf;
import static com.google.common.collect.Iterables.transform;
import static java.util.Arrays.asList;

import java.util.List;

import org.geotools.data.FeatureSource;
import org.geotools.feature.NameImpl;
import org.opengis.feature.Feature;
import org.opengis.feature.type.FeatureType;
import org.opengis.feature.type.PropertyDescriptor;
import org.opengis.filter.Filter;

import org.apache.commons.lang.time.FastDateFormat;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.google.common.base.Function;
import com.vividsolutions.jts.geom.Geometry;

import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;

import org.polymap.core.data.ui.featuretable.DefaultFeatureTableColumn;
import org.polymap.core.data.ui.featuretable.FeatureTableViewer;
import org.polymap.core.data.ui.featuretable.IFeatureTableElement;
import org.polymap.core.data.ui.featuretable.SimpleFeatureTableElement;

import org.polymap.wbv.model.WaldBesitzer;
import org.polymap.wbv.model.WbvRepository;

/**
 * 
 *
 * @author <a href="http://www.polymap.de">Falko Bräutigam</a>
 */
public class WaldbesitzerTableViewer
        extends FeatureTableViewer {

    private static Log log = LogFactory.getLog( WaldbesitzerTableViewer.class );

    private static final FastDateFormat df = FastDateFormat.getInstance( "dd.MM.yyyy" );

    private Filter                  baseFilter;

    private FeatureSource           fs;

    private FeatureType             schema;
    
    private WbvRepository           repo = WbvRepository.instance();
    
    
    public WaldbesitzerTableViewer( Composite parent, Filter baseFilter, int style ) {
        super( parent, /*SWT.VIRTUAL | SWT.V_SCROLL | SWT.FULL_SELECTION |*/ SWT.NONE );
        try {
            NameImpl typeName = new NameImpl( repo.infoOf( WaldBesitzer.class ).getNameInStore() );
            fs = repo.ds().getFeatureSource( typeName );
            schema = fs.getSchema();

            //        addColumn( new NatureColumn() );
            //        addColumn( new NameColumn() );
            //        addColumn( new DateColumn() );

            for (PropertyDescriptor prop : schema.getDescriptors()) {
                if (Geometry.class.isAssignableFrom( prop.getType().getBinding() )) {
                    // skip Geometry
                }
                else {
                    addColumn( new DefaultFeatureTableColumn( prop ) );
                }
            }

            // suppress deferred loading to fix "empty table" issue
            //setContent( fs.getFeatures( this.baseFilter ) );
            setContent( fs, baseFilter );
        }
        catch (Exception e) {
            throw new RuntimeException( e );
        }
    }

    
    public List<WaldBesitzer> getSelected() {
        return copyOf( transform( asList( getSelectedElements() ), new Function<IFeatureTableElement,WaldBesitzer>() {
            public WaldBesitzer apply( IFeatureTableElement input ) {
                Feature feature = ((SimpleFeatureTableElement)input).feature();
                return repo.entityForState( WaldBesitzer.class, feature );
            }
        }));
    }

    
//    public IMosaicCase entity( String fid ) {
//        return repo.entity( MosaicCase2.class, fid );    
//    }
//    
//    
//    public IMosaicCase entity( Feature entityState ) {
//        return repo.entityForState( MosaicCase2.class, entityState );    
//    }
    
    
//    /**
//     * 
//     */
//    class StatusColumn
//            extends DefaultFeatureTableColumn {
//
//        public StatusColumn() {
//            super( schema.getDescriptor( new NameImpl( "name" ) ) );
//            setWeight( 1, 90 );
//            setHeader( "" );
//            setAlign( SWT.CENTER );
//            setSortable( false );
//            
//            setLabelProvider( new ColumnLabelProvider() {
//                @Override
//                public String getText( Object elm ) {
//                    String fid = ((IFeatureTableElement)elm).fid();
//                    MosaicCase2 mcase = repo.entity( MosaicCase2.class, fid );
//                    String status = mcase.getStatus();  //MosaicCaseEvents.caseStatus( mcase );
//                    if (IMosaicCaseEvent.TYPE_NEW.equals( status )) {
//                        return "NEU";
//                    }
//                    else if (IMosaicCaseEvent.TYPE_CLOSED.equals( status )) {
//                        return "ERLEDIGT";
//                    }
//                    else {
//                        return "OFFEN";
//                    }
//                }
//                @Override
//                public Color getBackground( Object elm ) {
//                    String fid = ((IFeatureTableElement)elm).fid();
//                    MosaicCase2 mcase = repo.entity( MosaicCase2.class, fid );
//                    String status = mcase.getStatus();  //MosaicCaseEvents.caseStatus( mcase );
//                    if (IMosaicCaseEvent.TYPE_NEW.equals( status )) {
//                        return MosaicUiPlugin.COLOR_NEW.get();
//                    }
//                    else if (IMosaicCaseEvent.TYPE_CLOSED.equals( status )) {
//                        return MosaicUiPlugin.COLOR_CLOSED.get();
//                    }
//                    else {
//                        return MosaicUiPlugin.COLOR_OPEN.get();
//                    }
//                }
//                @Override
//                public Color getForeground( Object elm ) {
//                    return MosaicUiPlugin.COLOR_STATUS_FOREGROUND.get();                
//                }
////                @Override
////                public Font getFont( Object element ) {
////                    FontData[] defaultFont = getTable().getFont().getFontData();
////                    FontData bold = new FontData(defaultFont[0].getName(), defaultFont[0].getHeight(), SWT.BOLD);
////                    return Graphics.getFont( bold );
////                }
//            });
//        }
//    }
//
//    
//    /**
//     * 
//     */
//    class NameColumn
//            extends DefaultFeatureTableColumn {
//
//        public NameColumn() {
//            super( createDescriptor( "name", String.class ) );
//            setWeight( 2, 120 );
//            setHeader( "Bezeichnung" );
//            setAlign( SWT.LEFT );
//            setLabelProvider( new ColumnLabelProvider() {
//                @Override
//                public String getText( Object elm ) {
//                    IMosaicCase mcase = new CaseFinder().apply( (IFeatureTableElement)elm );
//                    return mcase.getName();
//                }
//                @Override
//                public String getToolTipText( Object elm ) {
//                    String fid = ((IFeatureTableElement)elm).fid();
//                    MosaicCase2 mcase = repo.entity( MosaicCase2.class, fid );
//                    String username = mcase.get( "user" );
//                    return username;
//                }
//            });
//        }
//    }
//
//    
//    /**
//     * 
//     */
//    class DateColumn
//            extends DefaultFeatureTableColumn {
//
//        public DateColumn() {
//            super( createDescriptor( "created", Date.class ) );
//            setWeight( 1, 90 );
//            setHeader( "Angelegt" );
//            setAlign( SWT.RIGHT );
//            setLabelProvider( new ColumnLabelProvider() {
//                @Override
//                public String getText( Object elm ) {
//                    IMosaicCase mcase = new CaseFinder().apply( (IFeatureTableElement)elm );
//                    //IMosaicCaseEvent event = Iterables.getFirst( mc.getEvents(), null );
//                    return df.format( mcase.getCreated() );
//                }
//            });
//        }
//    }
//
//    
//    /**
//     * 
//     */
//    class NatureColumn
//            extends DefaultFeatureTableColumn {
//
//        public NatureColumn() {
//            super( createDescriptor( "natures", String.class ) );
//            setWeight( 1, 120 );
//            setHeader( "Art" );
//            setLabelProvider( new ColumnLabelProvider() {
//                @Override
//                public String getText( Object elm ) {
//                    IMosaicCase mc = new CaseFinder().apply( (IFeatureTableElement)elm );
//                    return Joiner.on( ", " ).join( mc.getNatures() );
//                }
//            });
//        }
//    }
//    
//    public static PropertyDescriptor createDescriptor( String _name, Class binding ) {
//        NameImpl name = new NameImpl( _name );
//        AttributeType type = new AttributeTypeImpl( name,binding, true, false, null, null, null );
//        return new AttributeDescriptorImpl( type, name, 1, 1, false, null );
//    }
    
}
