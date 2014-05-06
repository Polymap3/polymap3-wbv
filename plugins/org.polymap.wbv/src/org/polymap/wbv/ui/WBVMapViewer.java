package org.polymap.wbv.ui;

import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;
import org.polymap.openlayers.rap.widget.OpenLayersWidget;
import org.polymap.openlayers.rap.widget.base_types.Bounds;
import org.polymap.openlayers.rap.widget.base_types.OpenLayersMap;
import org.polymap.openlayers.rap.widget.base_types.Projection;
import org.polymap.openlayers.rap.widget.controls.LayerSwitcherControl;
import org.polymap.openlayers.rap.widget.controls.MousePositionControl;
import org.polymap.openlayers.rap.widget.controls.NavigationControl;
import org.polymap.openlayers.rap.widget.controls.PanZoomBarControl;
import org.polymap.openlayers.rap.widget.controls.ScaleControl;
import org.polymap.openlayers.rap.widget.controls.ScaleLineControl;
import org.polymap.openlayers.rap.widget.layers.WMSLayer;

public class WBVMapViewer {

    OpenLayersMap map;


    public OpenLayersWidget createContents( Composite parent ) {
        OpenLayersWidget mapWidget = new OpenLayersWidget( parent, SWT.MULTI | SWT.WRAP,
                "openlayers/full/OpenLayers-2.12.1.js" );

        String srs = "EPSG:4326";
        Projection proj = new Projection( srs );
        String units = srs.equals( "EPSG:4326" ) ? "degrees" : "m";
        float maxResolution = srs.equals( "EPSG:4326" ) ? (360 / 256) : 500000;
        Bounds maxExtent = new Bounds( 12.34, 50.46, 13.4, 51.21 );
        mapWidget.createMap( proj, proj, units, maxExtent, maxResolution );

        WMSLayer osm = new WMSLayer( "Terrestris OSM", "http://ows.terrestris.de/osm/service",
                "OSM-WMS" );
        osm.setIsBaseLayer( true );
        map = mapWidget.getMap();

        map.addLayer( osm );
        addMapControls();
        map.zoomToExtent( maxExtent, true );
        map.zoomTo( 10 );

        return mapWidget;
    }


    private void addMapControls() {
        map.addControl( new NavigationControl() );
        map.addControl( new LayerSwitcherControl() );
        map.addControl( new MousePositionControl() );
        map.addControl( new ScaleLineControl() );
        map.addControl( new ScaleControl() );
        map.addControl( new PanZoomBarControl() );
    }

}
