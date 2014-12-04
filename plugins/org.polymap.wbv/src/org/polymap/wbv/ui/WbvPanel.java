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

import static com.google.common.collect.Iterables.getOnlyElement;
import static org.polymap.rhei.batik.Panels.is;
import static org.polymap.wbv.ui.WbvPanel.Completion.COMMIT;
import static org.polymap.wbv.ui.WbvPanel.Completion.STORE;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.polymap.core.model2.runtime.UnitOfWork;
import org.polymap.core.security.UserPrincipal;

import org.polymap.rhei.batik.ContextProperty;
import org.polymap.rhei.batik.DefaultPanel;
import org.polymap.rhei.batik.IAppContext;
import org.polymap.rhei.batik.IPanel;
import org.polymap.rhei.batik.IPanelSite;
import org.polymap.rhei.batik.PanelPath;

import org.polymap.wbv.model.WbvRepository;

/**
 * Basisklasse für andere Panels. Es kann eine "nested" {@link UnitOfWork} pro Panel
 * initialisiert werden. Über {@link #uow()} ist die {@link UnitOfWork} für das Panel
 * erreichbar. Panels können und sollten dann ihre Änderungen mit einem Commit
 * abschliessen.
 * 
 * @author <a href="http://www.polymap.de">Falko Bräutigam</a>
 */
public abstract class WbvPanel
        extends DefaultPanel
        implements IPanel {

    private static Log log = LogFactory.getLog( WbvPanel.class );

    public static enum Completion {
        /** Commit the locally started UnitOfWork -> modifications are not send to store. */
        COMMIT,
        /** Commit all UnitOfWorks down to the root -> modificatiosn are persistently stored. */
        STORE,
        /** Revert my UnitOfWork. */
        CANCEL
    }
    
    private ContextProperty<UnitOfWork>         rootUow;
    
    private UnitOfWork                          uow;

    private UnitOfWork                          parentUow;

    protected ContextProperty<UserPrincipal>    user;
    
    
    
    @Override
    public boolean init( IPanelSite site, IAppContext context ) {
        boolean result = super.init( site, context );

        // rootUow
        if (rootUow.get() == null) {
            rootUow.set( WbvRepository.instance.get().newUnitOfWork() );
        }
        // parentUow
        PanelPath myPath = getSite().getPath();
        if (myPath.size() == 1) {
            parentUow = rootUow.get();
        }
        else {
            WbvPanel parentPanel = (WbvPanel)getOnlyElement( getContext().findPanels( is( myPath.removeLast( 1 ) ) ) ); 
            parentUow = parentPanel.uow();
        }
        // uow
        uow = parentUow;
        return result;
    }


    /**
     * The UnitOfWork for this panel. Call {@link #newUnitOfWork()} before first
     * access to {@link #uow} to create a separate, nested UnitOfWork for this panel.
     */
    protected UnitOfWork uow() {
        return uow;
    }


    /**
     * Creates a new, nested {@link UnitOfWork} for this panel.
     * <p/>
     * Care must be taken when working with {@link ContextProperty} variables.
     * Entities have to be re-fetched from the local/nested UnitOfWork to make sure
     * that their state is properly handled by the local UnitOfWork.
     */
    protected void newUnitOfWork() {
        assert uow == parentUow : "newUnitOfWork() must be called only once per page.";
        uow = parentUow.newUnitOfWork();
        log.debug( getClass().getSimpleName() + ": new UOW: " + uow.getClass().getSimpleName() );
    }

    
    protected void closeUnitOfWork( Completion completion ) {
        if (completion == COMMIT) {
            assert uow != parentUow : "No UnitOfWork started locally for this panel.";
            uow.commit();
            uow.close();
            uow = parentUow;
        }
        else if (completion == STORE) {
            assert parentUow == rootUow.get();
            if (uow != parentUow) {
                closeUnitOfWork( COMMIT );
            }
            parentUow.commit();
        }
        else if (completion == Completion.CANCEL) {
            if (uow != parentUow) {
                uow.close();
                uow = parentUow;
            }
            else {
                log.warn( "No UnitOfWork this panel currently." );
            }
        }
    }
    
}
