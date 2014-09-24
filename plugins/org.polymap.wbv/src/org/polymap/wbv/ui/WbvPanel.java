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

import org.polymap.core.security.UserPrincipal;

import org.polymap.rhei.batik.ContextProperty;
import org.polymap.rhei.batik.DefaultPanel;
import org.polymap.rhei.batik.IPanel;
import org.polymap.wbv.model.WbvRepository;

/**
 * Basisklasse für andere Panels. Es kann ein "nested" Repository pro Panel
 * initialisiert werden. Über {@link #repo} ist dieses Repository erreichbar. Panels
 * können und sollten dann ihre Änderungen mit einem Commit abschliessen.
 * 
 * @author <a href="http://www.polymap.de">Falko Bräutigam</a>
 */
public abstract class WbvPanel
        extends DefaultPanel
        implements IPanel {

    protected ContextProperty<WbvRepository>    repo;
    
    protected ContextProperty<UserPrincipal>    user;
    
    protected WbvRepository                     parentRepo;

    
//    @Override
//    public boolean init( IPanelSite site, IAppContext context ) {
//        return super.init( site, context );
//    }
    

    protected void newUnitOfWork() {
        assert parentRepo == null : "newUnitOfWork() can by called only once per Panel!";
        parentRepo = repo.get();
        repo.set( parentRepo != null
             ? parentRepo.newNested()
             : new WbvRepository() );
    }

    
    protected void closeUnitOfWork() {
        assert parentRepo != null : "Call newUnitOfWork() before closeUnitOfWork()!";
        repo.get().close();
        
        parentRepo = repo.get();
        repo.set( parentRepo != null
             ? parentRepo.newNested()
             : new WbvRepository() );
    }

    
    @Override
    public void dispose() {
        if (parentRepo != null) {
            repo.set( parentRepo );
        }
    }
    
}
