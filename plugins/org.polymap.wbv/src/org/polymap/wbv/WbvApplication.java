/* 
 * polymap.org
 * Copyright (C) 2015, Falko Bräutigam. All rights reserved.
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

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.eclipse.equinox.app.IApplication;
import org.eclipse.equinox.app.IApplicationContext;

/**
 * Nur notwendig, um das Product exportieren zu können. Wenn aus IDE gestartet wird,
 * dann wird WbvApplication überhaupt nicht aufgerufen. Nach dem Deploy wird sie
 * ausgerufen.
 *
 * @author <a href="http://www.polymap.de">Falko Bräutigam</a>
 */
public class WbvApplication
        implements IApplication {

    private static Log log = LogFactory.getLog( WbvApplication.class );


    @Override
    public Object start( IApplicationContext context ) throws Exception {
        log.info( "start()" );
        while (true) {
            Thread.sleep( 1000 );
        }
//        int returnCode = EXIT_OK;
//        try {
//            returnCode = PlatformUI.createAndRunWorkbench( display, workbenchAdvisor );
//        }
//        catch (Throwable e) {
//            log.warn( "Error:" + e.getLocalizedMessage(), e );
//        }
//        finally {
//            context.applicationRunning();
//            display.dispose();
//        }
//        if (returnCode == PlatformUI.RETURN_RESTART) {
//            return EXIT_RESTART;
//        }
//        return EXIT_OK;
    }


    @Override
    public void stop() {
        log.info( "stop()" );
    }
    
}
