/* 
 * polymap.org
 * Copyright (C) 2014, Polymap GmbH. All rights reserved.
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

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.eclipse.swt.widgets.Composite;

import org.polymap.rhei.batik.DefaultPanel;
import org.polymap.rhei.batik.IAppContext;
import org.polymap.rhei.batik.IPanel;
import org.polymap.rhei.batik.IPanelSite;
import org.polymap.rhei.batik.PanelIdentifier;
import org.polymap.rhei.batik.toolkit.IPanelSection;

/**
 * 
 *
 * @author <a href="http://www.polymap.de">Falko Bräutigam</a>
 */
public class WaldbesitzerPanel
        extends DefaultPanel
        implements IPanel {

    private static Log log = LogFactory.getLog( WaldbesitzerPanel.class );

    public static final PanelIdentifier ID = new PanelIdentifier( "wbv", "waldbesitzer" );

    
    @Override
    public boolean init( IPanelSite site, IAppContext context ) {
        super.init( site, context );
        // nur Anzeigen wenn direkt aufgerufen
        return false;
    }


    @Override
    public void createContents( Composite parent ) {
        getSite().setTitle( "Waldbesitzer" );
        IPanelSection section = getSite().toolkit().createPanelSection( parent, "Basisdaten" );
    }


    @Override
    public PanelIdentifier id() {
        return ID;
    }
    
}
