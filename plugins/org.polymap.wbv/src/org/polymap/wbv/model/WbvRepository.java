/* 
 * polymap.org
 * Copyright (C) 2014 Polymap GmbH. All rights reserved.
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
package org.polymap.wbv.model;

import java.util.List;

import java.io.IOException;
import java.net.URL;

import net.refractions.udig.catalog.CatalogPlugin;
import net.refractions.udig.catalog.ICatalog;
import net.refractions.udig.catalog.IResolve;
import net.refractions.udig.catalog.IService;

import org.geotools.data.DataAccess;
import org.geotools.data.DataStore;
import org.geotools.factory.CommonFactoryFinder;
import org.opengis.filter.FilterFactory;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.eclipse.core.runtime.NullProgressMonitor;

import org.polymap.core.data.feature.recordstore.catalog.RServiceExtension;
import org.polymap.core.model2.engine.EntityRepositoryImpl;
import org.polymap.core.model2.runtime.EntityRepository;
import org.polymap.core.model2.runtime.EntityRepositoryConfiguration;
import org.polymap.core.model2.runtime.UnitOfWork;
import org.polymap.core.model2.store.feature.FeatureStoreAdapter;
import org.polymap.core.runtime.SessionSingleton;

/**
 * 
 *
 * @author <a href="http://www.polymap.de">Falko Bräutigam</a>
 */
public class WbvRepository
        extends SessionSingleton {

    private static Log log = LogFactory.getLog( WbvRepository.class );
    
    public static final String              DB_NAME = "WBV";
    
    public static final FilterFactory       ff = CommonFactoryFinder.getFilterFactory( null );
    
    private static EntityRepository         repo;
    
    private static DataAccess               ds;
    
    private static FeatureStoreAdapter      store;
    
    
    /**
     * Configure and initializing the global #repo.
     */
    static {
        try {
            // find service for SERVICE_ID
            IService service = null;
            URL url = RServiceExtension.toURL( DB_NAME );
            ICatalog catalog = CatalogPlugin.getDefault().getLocalCatalog();
            List<IResolve> canditates = catalog.find( url, new NullProgressMonitor() );
            for (IResolve resolve : canditates) {
                if (resolve.getStatus() == IResolve.Status.BROKEN) {
                    continue;
                }
                if (resolve instanceof IService) {
                    service = (IService)resolve;
                }
            }
            if (service == null) {
                throw new RuntimeException( "Kein Service im Katalog für URL: " + url );
            }

            // find DataStore from service
            ds = service.resolve( DataStore.class, new NullProgressMonitor() );
            if (ds == null) {
                throw new RuntimeException( "Kein DataStore für Service: " + service );
            }
            // create repo
            EntityRepositoryConfiguration repoConfig = EntityRepository.newConfiguration()
                    .setEntities( new Class[] {
                            WaldBesitzer.class} )
                    .setStore( new FeatureStoreAdapter( ds ) );
            repo = new EntityRepositoryImpl( repoConfig );
        }
        catch (RuntimeException e) {
            throw e;
        }
        catch (Exception e) {
            throw new RuntimeException( e );
        }
    }
    
    
    /**
     * The {@link WbvRepository} instance for the session of the calling thread. 
     */
    public static WbvRepository instance() {
        return instance( WbvRepository.class );
    };

        
    // instance *******************************************
        
    private UnitOfWork                  uow;
    

    public WbvRepository( EntityRepositoryConfiguration config ) throws IOException {
        store = (FeatureStoreAdapter)config.getStore();
        ds = store.getStore();
        uow = repo.newUnitOfWork();
    }

}
