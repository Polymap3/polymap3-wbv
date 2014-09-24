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
import org.geotools.factory.CommonFactoryFinder;
import org.opengis.filter.FilterFactory;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.eclipse.core.runtime.NullProgressMonitor;

import org.polymap.core.data.feature.recordstore.RDataStore;
import org.polymap.core.data.feature.recordstore.catalog.RServiceExtension;
import org.polymap.core.model2.Composite;
import org.polymap.core.model2.Entity;
import org.polymap.core.model2.query.Query;
import org.polymap.core.model2.runtime.CompositeInfo;
import org.polymap.core.model2.runtime.ConcurrentEntityModificationException;
import org.polymap.core.model2.runtime.EntityRepository;
import org.polymap.core.model2.runtime.ModelRuntimeException;
import org.polymap.core.model2.runtime.UnitOfWork;
import org.polymap.core.model2.runtime.ValueInitializer;
import org.polymap.core.model2.store.OptimisticLocking;
import org.polymap.core.model2.store.recordstore.RecordStoreAdapter;
import org.polymap.core.runtime.recordstore.IRecordStore;

/**
 * 
 *
 * @author <a href="http://www.polymap.de">Falko Bräutigam</a>
 */
public class WbvRepository {

    private static Log log = LogFactory.getLog( WbvRepository.class );
    
    public static final String              DB_NAME = "WBV";
    
    public static final FilterFactory       ff = CommonFactoryFinder.getFilterFactory( null );
    
    private static EntityRepository         repo;
    
    
    /**
     * Configure and initializing the global {@link #repo}.
     */
    public static void init() {
        try {
            log.info( "Assembling repository..." );
            
            // find service for SERVICE_ID
            IService service = null;
            URL url = RServiceExtension.toURL( DB_NAME );
            ICatalog catalog = CatalogPlugin.getDefault().getLocalCatalog();
            List<IResolve> canditates = catalog.find( url, new NullProgressMonitor() );
            for (IResolve resolve : canditates) {
                if (resolve instanceof IService) {
                    service = (IService)resolve;
                }
            }
            if (service == null) {
                throw new RuntimeException( "Kein Service im Katalog für URL: " + url );
            }

            // find DataStore from service
            DataAccess ds = service.resolve( DataAccess.class, new NullProgressMonitor() );
            if (ds == null) {
                throw new RuntimeException( "Kein DataStore für Service: " + service );
            }
            // create repo
            IRecordStore store = ((RDataStore)ds).getStore();
            repo = EntityRepository.newConfiguration()
                    .setEntities( new Class[] {Revier.class, Waldstueck.class, Waldbesitzer.class, Kontakt.class} )
                    .setStore( new OptimisticLocking( new RecordStoreAdapter( store ) ) )
                    .create();
        }
        catch (RuntimeException e) {
            throw e;
        }
        catch (Exception e) {
            throw new RuntimeException( e );
        }
    }
    
    
//    /**
//     * The {@link WbvRepository} instance for the session of the calling thread. 
//     */
//    public static WbvRepository instance() {
//        if (repo == null) {
//            init();
//        }
//        return instance( WbvRepository.class );
//    };

        
    public static EntityRepository repo() {
        return repo;
    }


    // instance *******************************************
        
    private UnitOfWork                  uow;
    

    public WbvRepository() {
        this.uow = repo.newUnitOfWork();
    }

    
    public WbvRepository( UnitOfWork uow ) {
        this.uow = uow;
    }

    
    public WbvRepository newNested() {
        return new WbvRepository( uow.newUnitOfWork() );
    }

    
    public <T extends Entity> T entityForState( Class<T> entityClass, Object state ) {
        return uow.entityForState( entityClass, state );
    }


    public <T extends Entity> T entity( Class<T> entityClass, Object id ) {
        return uow.entity( entityClass, id );
    }


    public <T extends Entity> Query<T> query( Class<T> entityClass ) {
        return uow.query( entityClass );
    }


    public <T extends Composite> CompositeInfo<T> infoOf( Class<T> compositeClass ) {
        return repo.infoOf( compositeClass );
    }


    public <T extends Entity> T createEntity( Class<T> entityClass, Object id,
            ValueInitializer<T> initializer ) {
        return uow.createEntity( entityClass, id, initializer );
    }


    public void removeEntity( Entity entity ) {
        uow.removeEntity( entity );
    }


    public void prepare() throws IOException, ConcurrentEntityModificationException {
        uow.prepare();
    }


    public void commit() throws ModelRuntimeException {
        uow.commit();
    }


    public void rollback() throws ModelRuntimeException {
        uow.rollback();
    }


    public void close() {
        uow.close();
    }


    public UnitOfWork uow() {
        return uow;
    }


//    public <T extends Entity> Collection<T> find( Class<T> entityClass ) {
//        return uow.find( entityClass );
//    }
    
}
