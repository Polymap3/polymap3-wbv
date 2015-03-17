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

import org.geotools.geometry.jts.ReferencedEnvelope;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.eclipse.swt.widgets.Composite;

import org.polymap.core.data.util.Geometries;
import org.polymap.core.mapeditor.ContextMenuControl;
import org.polymap.core.mapeditor.ContextMenuExtension;
import org.polymap.core.mapeditor.HomeMapAction;
import org.polymap.core.mapeditor.MapViewer;
import org.polymap.core.mapeditor.ScaleMapAction;

import org.polymap.rhei.batik.IPanelSite;

import org.polymap.rap.openlayers.layers.WMSLayer;

/**
 * 
 *
 * @author <a href="http://www.polymap.de">Falko Bräutigam</a>
 */
public class WbvMapViewer
        extends MapViewer {

    private static Log log = LogFactory.getLog( WbvMapViewer.class );
    
    private ContextMenuControl      contextMenu;

    
    public WbvMapViewer( IPanelSite site ) throws Exception {
        super( site, new ReferencedEnvelope( 4500000, 4700000, 5550000, 5700000, Geometries.crs( "EPSG:31468" ) ) );
    }

    
    public ContextMenuControl getContextMenu() {
        return contextMenu;
    }


    @Override
    public Composite createContents( Composite parent ) {
        Composite result = super.createContents( parent );
        
        WMSLayer osm = new WMSLayer( "OSM", "services/WBV/", "OSM" );
        addLayer( osm, true, false );
        
        WMSLayer waldflaechen = new WMSLayer( "Waldflächen", "services/WBV/", "Waldflaechen" );
        addLayer( waldflaechen, false, false );
        setLayerVisible( waldflaechen, true );

        addToolbarItem( new HomeMapAction( this ) );
        addToolbarItem( new ScaleMapAction( this, 1000 ) );
        addToolbarItem( new ScaleMapAction( this, 5000 ) );
        getMap().zoomTo( 12 );
        
        // context menu
        contextMenu = new ContextMenuControl( this );
        contextMenu.addProvider( ContextMenuExtension.all() );
//        contextMenu.addProvider( new IContextMenuProvider() {
//            @Override
//            public IContextMenuContribution createContribution() {
//                return new FindFeaturesMenuContribution() {
//                    @Override
//                    protected void onMenuOpen( FeatureStore fs, Feature feature, ILayer layer ) {
//                        log.info( "Feature: " + feature );
//                    }
//                };            
//            }
//        });
//        //map.addMapControl( contextMenu );
        return result;
    }

}
