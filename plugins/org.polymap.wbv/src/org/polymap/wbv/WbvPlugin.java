/*
 * polymap.org 
 * Copyright (C) 2014 Polymap GmbH. All rights reserved.
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
package org.polymap.wbv;

import org.osgi.framework.BundleContext;

import org.eclipse.ui.plugin.AbstractUIPlugin;

import org.polymap.core.runtime.DefaultSessionContextProvider;
import org.polymap.core.runtime.Polymap;
import org.polymap.core.runtime.SessionContext;
import org.polymap.core.security.SecurityUtils;
import org.polymap.core.security.UserPrincipal;

import org.polymap.wbv.model.WbvRepository;

/**
 * 
 *
 * @author <a href="http://www.polymap.de">Falko Bräutigam</a>
 * @author <a href="http://www.polymap.de">Bertram Kirsch</a>
 */
public class WbvPlugin extends AbstractUIPlugin {

	public static final String PLUGIN_ID = "org.polymap.wbv"; //$NON-NLS-1$

	private static WbvPlugin       instance;
	
    private static DefaultSessionContextProvider contextProvider;

	/**
     * Returns the shared instance
     */
    public static WbvPlugin getDefault() {
    	return instance;
    }

    
    // instance *******************************************
    
    public WbvPlugin() {
        contextProvider = new DefaultSessionContextProvider();
        SessionContext.addProvider( contextProvider );
	}

    
	public void start( BundleContext context ) throws Exception {
		super.start( context );
		instance = this;

		try {
		    contextProvider.mapContext( "wbv_init", true );
            Polymap.instance().addPrincipal( new AdminPrincipal() );

            WbvRepository.init();
	    }
		finally {
            contextProvider.unmapContext();
		}
	}

	
	public void stop( BundleContext context ) throws Exception {
		instance = null;
		super.stop( context );
	}

	
    /**
     * 
     */
    static class AdminPrincipal
            extends UserPrincipal {

        public AdminPrincipal() {
            super( SecurityUtils.ADMIN_USER );
        }

        public String getPassword() {
            throw new RuntimeException( "not yet implemented." );
        }
        
    }

}
