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

import org.polymap.core.runtime.event.EventFilter;
import org.polymap.core.runtime.event.EventHandler;
import org.polymap.core.security.SecurityUtils;

import org.polymap.rhei.batik.IAppContext;
import org.polymap.rhei.batik.IPanelSite;
import org.polymap.rhei.batik.PanelIdentifier;
import org.polymap.rhei.batik.PropertyAccessEvent;
import org.polymap.rhei.batik.toolkit.IPanelSection;
import org.polymap.rhei.batik.toolkit.IPanelToolkit;
import org.polymap.rhei.batik.toolkit.PriorityConstraint;

import org.polymap.wbv.WbvPlugin;

/**
 * 
 *
 * @author <a href="http://www.polymap.de">Falko Bräutigam</a>
 */
public class AdminPanel
        extends WbvPanel {

    private static Log log = LogFactory.getLog( AdminPanel.class );

    public static final PanelIdentifier   ID  = new PanelIdentifier( "wbv", "admin" );


    @Override
    public PanelIdentifier id() {
        return ID;
    }


    @Override
    public boolean init( IPanelSite site, IAppContext context ) {
        super.init( site, context );

        if (site.getPath().size() == 1) {
            // warten auf login
            site.setTitle( null );
            user.addListener( this, new EventFilter<PropertyAccessEvent>() {
                public boolean apply( PropertyAccessEvent input ) {
                    return input.getType() == PropertyAccessEvent.TYPE.SET;
                }
            });
            return true;
        }
        return false;
    }

    
    @EventHandler( display=true )
    protected void userLoggedIn( PropertyAccessEvent ev ) {
        if (SecurityUtils.isAdmin()) {
            getSite().setTitle( "Administration" );
            getSite().setIcon( WbvPlugin.instance().imageForName( "icons/cog.png" ) ); //$NON-NLS-1$
        }
    }

    
    @Override
    public void createContents( Composite parent ) {
        createBaumartenSection( parent );
    }
    
    
    protected void createBaumartenSection( Composite parent ) {
        IPanelToolkit tk = getSite().toolkit();
        IPanelSection section = tk.createPanelSection( parent, "Baumarten: Import" );
        section.addConstraint( new PriorityConstraint( 10 ) );
        //tk.createFlowText( welcome.getBody(), "Willkommen ..." );
    }
    
}
