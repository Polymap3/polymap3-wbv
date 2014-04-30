/* 
 * polymap.org
 * Copyright (C) 2014, Falko Bräutigam. All rights reserved.
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
import org.polymap.rhei.batik.IPanel;
import org.polymap.rhei.batik.PanelIdentifier;
import org.polymap.rhei.batik.toolkit.IPanelSection;
import org.polymap.rhei.batik.toolkit.IPanelToolkit;
import org.polymap.rhei.batik.toolkit.PriorityConstraint;

/**
 * 
 *
 * @author <a href="http://www.polymap.de">Falko Bräutigam</a>
 */
public class StartPanel
        extends DefaultPanel
        implements IPanel {

    private static Log log = LogFactory.getLog( StartPanel.class );

    public static final PanelIdentifier ID = new PanelIdentifier( "start" );


    @Override
    public void createContents( Composite parent ) {
        getSite().setTitle( "Start" );
        IPanelToolkit tk = getSite().toolkit();        
    
        IPanelSection welcome = tk.createPanelSection( parent, "Willkommen" );
        welcome.addConstraint( new PriorityConstraint( 10 ) );
        tk.createFlowText( welcome.getBody(), "**Waldbesitzen** ist eine wunderbare Sache. Viele Leute wollen das - nur wenige tun es. Um hier den Überblick nicht zu verlieren, ist ein Verzeichnis unabdingbar." );
    
        IPanelSection image = tk.createPanelSection( parent, null );
        image.addConstraint( new PriorityConstraint( 5 ) );
        tk.createFlowText( image.getBody(), "**[Karte]**" );
    }


    @Override
    public PanelIdentifier id() {
        return ID;
    }
    
}
