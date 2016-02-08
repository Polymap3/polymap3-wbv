/*
 * polymap.org 
 * Copyright (C) 2014-2016 Polymap GmbH. All rights reserved.
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
package org.polymap.wbv;

import java.text.DateFormat;
import java.text.SimpleDateFormat;

import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.osgi.service.http.HttpService;
import org.osgi.service.http.NamespaceException;
import org.osgi.util.tracker.ServiceTracker;

import org.eclipse.ui.plugin.AbstractUIPlugin;

import org.polymap.core.runtime.Closer;
import org.polymap.core.runtime.session.DefaultSessionContextProvider;
import org.polymap.core.runtime.session.SessionContext;
import org.polymap.core.security.SecurityContext;
import org.polymap.core.security.SecurityUtils;
import org.polymap.core.security.StandardConfiguration;
import org.polymap.core.security.UserPrincipal;
import org.polymap.core.ui.StatusDispatcher;

import org.polymap.rhei.batik.app.SvgImageRegistryHelper;
import org.polymap.rhei.batik.toolkit.BatikDialogStatusAdapter;
import org.polymap.rhei.batik.toolkit.MinWidthConstraint;
import org.polymap.rhei.fulltext.FulltextPlugin;
import org.polymap.rhei.fulltext.FulltextPlugin.ErrorHandler;

import org.polymap.wbv.model.WbvRepository;

/**
 * 
 *
 * @author <a href="http://www.polymap.de">Falko Bräutigam</a>
 * @author <a href="http://www.polymap.de">Bertram Kirsch</a>
 */
public class WbvPlugin 
        extends AbstractUIPlugin {

	public static final String             ID = "org.polymap.wbv"; //$NON-NLS-1$

	public static final MinWidthConstraint MIN_COLUMN_WIDTH = new MinWidthConstraint( 450, 1 );
	
	/** "dd.MM.yyyy" - one gloabel instance, not thread safe! */
	public static final DateFormat         df = new SimpleDateFormat( "dd.MM.yyyy" );

	private static WbvPlugin               instance;
	
    private static DefaultSessionContextProvider contextProvider;
    
    static {
        System.setProperty( "org.apache.commons.logging.simplelog.log.org.polymap.core.data.feature.recordstore", "debug" );
        System.setProperty( "org.apache.commons.logging.simplelog.log.org.polymap.core.runtime.recordstore.lucene", "debug" );
        System.setProperty( "org.apache.commons.logging.simplelog.log.org.polymap.core.model2.store.recordstore", "debug" );
        System.setProperty( "org.apache.commons.logging.simplelog.log.org.polymap.wbv", "debug" );
    }
    
	/**
     * Returns the shared instance
     */
    public static WbvPlugin instance() {
    	return instance;
    }

    
    public static SvgImageRegistryHelper images() {
        return instance().images;
    }

    
    // instance *******************************************
    

    private ServiceTracker          httpServiceTracker;

    private SvgImageRegistryHelper  images;               

    
    public WbvPlugin() {
        contextProvider = new DefaultSessionContextProvider();
        SessionContext.addProvider( contextProvider );
	}

    
	public void start( BundleContext context ) throws Exception {
		super.start( context );
		instance = this;

		// Handling errors in the UI
        StatusDispatcher.registerAdapter( new StatusDispatcher.LogAdapter() );
        StatusDispatcher.registerAdapter( new BatikDialogStatusAdapter() );

		images = new SvgImageRegistryHelper( this );
		
        // register HTTP resource
		httpServiceTracker = new ServiceTracker( context, HttpService.class.getName(), null ) {
            public Object addingService( ServiceReference reference ) {
                HttpService httpService = (HttpService)super.addingService( reference );                
                if (httpService != null) {
                    try {
                        httpService.registerResources( "/wbvres", "/resources", null );
                    }
                    catch (NamespaceException e) {
                        throw new RuntimeException( e );
                    }
                }
                return httpService;
            }
        };
        httpServiceTracker.open();

		// init FullText error handler
		FulltextPlugin.instance().setErrorHandler( new ErrorHandler() {
            @Override
            public void handleError( String msg, Throwable e ) {
                StatusDispatcher.handleError( msg, e );
            }
		});

        // JAAS config: no dialog; let LoginPanel create UI
        SecurityContext.registerConfiguration( () -> new StandardConfiguration() {
            @Override
            public String getConfigName() {
                return SecurityContext.SERVICES_CONFIG_NAME;
            }
        });
        
        // init the global instance
        WbvRepository.init();
	}

	
	public void stop( BundleContext context ) throws Exception {
	    httpServiceTracker = Closer.create().runAndClose( () -> httpServiceTracker.close() ).setNull();
	    images = null;
		super.stop( context );
        instance = null;
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
