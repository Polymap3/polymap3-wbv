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

import static com.google.common.collect.Lists.newArrayList;

import java.util.function.Supplier;

import java.io.File;
import org.geotools.factory.CommonFactoryFinder;
import org.opengis.filter.FilterFactory;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.lucene.search.BooleanQuery;

import org.polymap.core.runtime.LockedLazyInit;
import org.polymap.core.runtime.Polymap;

import org.polymap.rhei.fulltext.FullQueryProposalDecorator;
import org.polymap.rhei.fulltext.FulltextIndex;
import org.polymap.rhei.fulltext.indexing.LowerCaseTokenFilter;
import org.polymap.rhei.fulltext.model2.FulltextIndexer;
import org.polymap.rhei.fulltext.model2.FulltextIndexer.TypeFilter;
import org.polymap.rhei.fulltext.store.lucene.LuceneFulltextIndex;

import org.polymap.model2.Composite;
import org.polymap.model2.runtime.CompositeInfo;
import org.polymap.model2.runtime.EntityRepository;
import org.polymap.model2.runtime.UnitOfWork;
import org.polymap.model2.store.OptimisticLocking;
import org.polymap.model2.store.recordstore.RecordStoreAdapter;
import org.polymap.recordstore.IRecordStore;
import org.polymap.recordstore.lucene.LuceneRecordStore;
import org.polymap.wbv.WbvPlugin;
import org.polymap.wbv.model.fulltext.WbvTokenizer;
import org.polymap.wbv.model.fulltext.WaldbesitzerFulltextTransformer;

/**
 * 
 *
 * @author <a href="http://www.polymap.de">Falko Bräutigam</a>
 */
public class WbvRepository {

    private static Log log = LogFactory.getLog( WbvRepository.class );
    
    public static final String              DB_NAME = "WBV";
    
    public static final FilterFactory       ff = CommonFactoryFinder.getFilterFactory( null );
    
    /**
     * The global, static instance.
     */
    public static Supplier<WbvRepository>   instance = new LockedLazyInit( () -> new WbvRepository() );
    
    
    // instance *******************************************

    private EntityRepository                repo;

    private LuceneFulltextIndex             fulltextIndex;
    
    
    /**
     * Configure and initializing the one and only global instance.
     */
    private WbvRepository() {
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
    
    
    public EntityRepository repo() {
        return repo;
    }

    
    public FulltextIndex fulltextIndex() {
        return new FullQueryProposalDecorator(
               new LowerCaseTokenFilter( fulltextIndex ) );

    }


    public <T extends Composite> CompositeInfo<T> infoOf( Class<T> compositeClass ) {
        return repo.infoOf( compositeClass );
    }


    public UnitOfWork newUnitOfWork() {
        return repo.newUnitOfWork();
    }

}
