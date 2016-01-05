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
package org.polymap.wbv.model;

import static com.google.common.collect.Lists.newArrayList;

import java.io.File;
import java.io.IOException;

import org.geotools.factory.CommonFactoryFinder;
import org.opengis.filter.FilterFactory;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.lucene.search.BooleanQuery;

import org.polymap.core.runtime.Polymap;
import org.polymap.core.runtime.session.SessionContext;
import org.polymap.core.runtime.session.SessionSingleton;

import org.polymap.rhei.fulltext.FullQueryProposalDecorator;
import org.polymap.rhei.fulltext.FulltextIndex;
import org.polymap.rhei.fulltext.indexing.LowerCaseTokenFilter;
import org.polymap.rhei.fulltext.model2.FulltextIndexer;
import org.polymap.rhei.fulltext.model2.FulltextIndexer.TypeFilter;
import org.polymap.rhei.fulltext.store.lucene.LuceneFulltextIndex;

import org.polymap.model2.Composite;
import org.polymap.model2.Entity;
import org.polymap.model2.query.Query;
import org.polymap.model2.runtime.CompositeInfo;
import org.polymap.model2.runtime.ConcurrentEntityModificationException;
import org.polymap.model2.runtime.EntityRepository;
import org.polymap.model2.runtime.ModelRuntimeException;
import org.polymap.model2.runtime.UnitOfWork;
import org.polymap.model2.runtime.ValueInitializer;
import org.polymap.model2.store.OptimisticLocking;
import org.polymap.model2.store.recordstore.RecordStoreAdapter;
import org.polymap.recordstore.IRecordStore;
import org.polymap.recordstore.lucene.LuceneRecordStore;
import org.polymap.wbv.WbvPlugin;
import org.polymap.wbv.model.fulltext.WaldbesitzerFulltextTransformer;
import org.polymap.wbv.model.fulltext.WbvTokenizer;

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

    private static LuceneFulltextIndex      fulltextIndex;
    
    
    /**
     * Configure and initializing the repository.
     */
    public static void init() {
        try {
            log.info( "Assembling repository..." );
            
//            // find service for SERVICE_ID
//            IService service = null;
//            URL url = RServiceExtension.toURL( DB_NAME );
//            ICatalog catalog = CatalogPlugin.getDefault().getLocalCatalog();
//            List<IResolve> canditates = catalog.find( url, new NullProgressMonitor() );
//            for (IResolve resolve : canditates) {
//                if (resolve instanceof IService) {
//                    service = (IService)resolve;
//                }
//            }
//            if (service == null) {
//                throw new RuntimeException( "Kein Service im Katalog für URL: " + url );
//            }

            //
            BooleanQuery.setMaxClauseCount( 8 * 1024 );
            log.info( "Maximale Anzahl Lucene-Klauseln erhöht auf: " + BooleanQuery.getMaxClauseCount() );
            
            // init fulltext
            File wbvDir = new File( Polymap.getDataDir(), WbvPlugin.ID );
            fulltextIndex = new LuceneFulltextIndex( new File( wbvDir, "fulltext" ) );
            fulltextIndex.setTokenizer( new WbvTokenizer() );
            fulltextIndex.addTokenFilter( new LowerCaseTokenFilter() );
            
            WaldbesitzerFulltextTransformer wbTransformer = new WaldbesitzerFulltextTransformer();
            wbTransformer.setHonorQueryableAnnotation( true );

//            // find DataStore from service
//            DataAccess ds = service.resolve( DataAccess.class, new NullProgressMonitor() );
//            if (ds == null) {
//                throw new RuntimeException( "Kein DataStore für Service: " + service );
//            }
            // create repo
            IRecordStore store = LuceneRecordStore.newConfiguration()
                    .indexDir.put( new File( Polymap.getDataDir(), "recordstore/WBV" ) )
                    .create();
            repo = EntityRepository.newConfiguration()
                    .entities.set( new Class[] {
                            Revier.class, 
                            Waldstueck.class, 
                            Waldbesitzer.class, 
                            Kontakt.class,
                            Flurstueck.class,
                            Gemarkung.class} )
                    .store.set( 
                            new OptimisticLocking(
                            new FulltextIndexer( fulltextIndex, new TypeFilter( Waldbesitzer.class ), newArrayList( wbTransformer ),
                            new RecordStoreAdapter( store ) ) ) )
                    .create();
        }
        catch (RuntimeException e) {
            throw e;
        }
        catch (Exception e) {
            throw new RuntimeException( e );
        }
    }
    
    
    public static EntityRepository repo() {
        return repo;
    }

    
    public static FulltextIndex fulltextIndex() {
        return new FullQueryProposalDecorator(
               new LowerCaseTokenFilter( fulltextIndex ) );
    }


    public static <T extends Composite> CompositeInfo<T> infoOf( Class<T> compositeClass ) {
        return repo.infoOf( compositeClass );
    }


    /**
     * The instance of the current {@link SessionContext}. This is the <b>read</b>
     * cache for all entities used by the UI.
     * <p/>
     * Do <b>not</b> use this for <b>modifications</b> that might be canceled or
     * otherwise may left pending changes! Create a
     * {@link UnitOfWorkWrapper#newUnitOfWork()} nested instance for that. This
     * prevents your modifications from being committed by another party are leaving
     * half-done, uncommitted changes. Commiting a nested instance commits also the
     * parent, hence making changes persistent, in one atomic action. If that fails
     * the <b>parent</b> is rolled back.
     */
    public static UnitOfWork unitOfWork() {
        return UnitOfWorkWrapper.instance( UnitOfWorkWrapper.class );
    }

    
    /**
     * This is for import. See {@link #unitOfWork()}.
     */
    public static UnitOfWork newUnitOfWork() {
        return repo.newUnitOfWork();
    }
    
    
    /**
     * 
     */
    static class UnitOfWorkWrapper
            extends SessionSingleton
            implements UnitOfWork {
        
        private UnitOfWork          delegate;
        
        private UnitOfWork          parent;

        /** This is the {@link SessionSingleton} ctor. */
        public UnitOfWorkWrapper() {
            this.delegate = repo.newUnitOfWork();    
        }
        
        /** This is the ctor fpr nested instances. */
        public UnitOfWorkWrapper( UnitOfWork parent ) {
            this.delegate = parent.newUnitOfWork();
            this.parent = parent;
        }
        
        public <T extends Entity> T entityForState( Class<T> entityClass, Object state ) {
            return delegate.entityForState( entityClass, state );
        }

        public <T extends Entity> T entity( Class<T> entityClass, Object id ) {
            return delegate.entity( entityClass, id );
        }

        public <T extends Entity> T entity( T entity ) {
            return delegate.entity( entity );
        }

        public <T extends Entity> T createEntity( Class<T> entityClass, Object id, ValueInitializer<T>... initializers ) {
            return delegate.createEntity( entityClass, id, initializers );
        }

        public void removeEntity( Entity entity ) {
            delegate.removeEntity( entity );
        }

        public void prepare() throws IOException, ConcurrentEntityModificationException {
            throw new RuntimeException( "the nested UoW thing does not (yet) support prepare()." );
            //delegate.prepare();
        }

        public void commit() throws ModelRuntimeException {
            synchronized( parent) {
                try {
                    delegate.commit();
                    parent.commit();
                }
                catch (Exception e) {
                    log.info( "Commit nested ProjectRepository failed.", e );
                    parent.rollback();
                }
            }
        }

        public void rollback() throws ModelRuntimeException {
            delegate.rollback();
        }

        public void close() {
            delegate.close();
        }

        public boolean isOpen() {
            return delegate.isOpen();
        }

        public <T extends Entity> Query<T> query( Class<T> entityClass ) {
            return delegate.query( entityClass );
        }

        public UnitOfWork newUnitOfWork() {
            return new UnitOfWorkWrapper( delegate );
        }
    }

}
