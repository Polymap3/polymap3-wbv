/* 
 * polymap.org
 * Copyright (C) 2014-2016, Falko Bräutigam. All rights reserved.
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

import org.polymap.core.security.UserPrincipal;

import org.polymap.rhei.batik.Context;
import org.polymap.rhei.batik.DefaultPanel;
import org.polymap.rhei.batik.Scope;
import org.polymap.rhei.batik.toolkit.md.MdToolkit;

import org.polymap.wbv.model.Revier;

/**
 * Basisklasse für andere Panels.
 * 
 * @author <a href="http://www.polymap.de">Falko Bräutigam</a>
 */
public abstract class WbvPanel
        extends DefaultPanel {

    private static Log log = LogFactory.getLog( WbvPanel.class );

    protected Context<UserPrincipal>    user;

    /** */
    @Scope( "org.polymap.wbv.ui" )
    protected Context<Revier>           revier;
    
    /** */
    @Scope( "org.polymap.wbv.ui" )
    protected Context<String>           queryString;
    
    
    protected MdToolkit tk() {
        return (MdToolkit)site().toolkit();    
    }
    
}
