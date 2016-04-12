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
package org.polymap.wbv.mdb;

import static org.polymap.wbv.model.fulltext.WaldbesitzerFulltextTransformer.whitespace;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

import org.apache.commons.io.FilenameUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.healthmarketscience.jackcess.Database;
import com.healthmarketscience.jackcess.DatabaseBuilder;
import com.healthmarketscience.jackcess.Row;
import com.healthmarketscience.jackcess.Table;
import com.healthmarketscience.jackcess.util.LinkResolver;

import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.commands.operations.AbstractOperation;
import org.eclipse.core.commands.operations.IUndoableOperation;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.rap.rwt.client.ClientFile;

import org.polymap.core.runtime.SubMonitor;

import org.polymap.model2.runtime.UnitOfWork;
import org.polymap.wbv.model.Flurstueck;
import org.polymap.wbv.model.Gemarkung;
import org.polymap.wbv.model.Kontakt;
import org.polymap.wbv.model.Revier;
import org.polymap.wbv.model.Waldbesitzer;
import org.polymap.wbv.model.Waldbesitzer.Waldeigentumsart;

/**
 * Importiert Daten aus WVK_dat_<Reviername>.mdb 
 *
 * @author <a href="http://www.polymap.de">Falko Bräutigam</a>
 */
public class WvkImporter
        extends AbstractOperation
        implements IUndoableOperation {

    private static Log log = LogFactory.getLog( WvkImporter.class );
    
    public static final File    BASEDIR = new File( "." );  // lokales Dir beim Start (workspace)

    private UnitOfWork          uow;

    private InputStream         in;

    private ClientFile          clientFile;

    private String              revier;

    private Map<String,String>  gmks = new HashMap();

    private Map<String,String>  gmds = new HashMap();
    
    private int                 wbImportCount = 0;

    private int                 flstImportCount = 0;
    
    private int                 adrImportCount = 0;

    private Map<String,Row>     wbRows = new HashMap( 5000 );
    
    private Set<String>         wbIds = new HashSet( 5000 );
    

    public WvkImporter( UnitOfWork uow, ClientFile clientFile, InputStream in ) {
        super( "WVK-Daten importieren" );
        this.uow = uow;
        this.in = in;
        this.clientFile = clientFile;
        
        this.revier = StringUtils.substringAfterLast( FilenameUtils.getBaseName( clientFile.getName() ), "_" );
        if (!Revier.all.get().containsKey( revier )) {
            throw new IllegalStateException( "Das Revier gibt es nicht: " + revier );
        }
    }
    
    
    @Override
    public IStatus execute( IProgressMonitor monitor, IAdaptable info ) throws ExecutionException {
        try {
            importData( monitor );
            return monitor.isCanceled() ? Status.CANCEL_STATUS : Status.OK_STATUS;
        }
        catch (Exception e) {
            throw new ExecutionException( e.getMessage(), e );
        }
    }


    public void importData( IProgressMonitor monitor ) throws Exception {
        // upload -> temp file
        monitor.beginTask( getLabel(), 12 );
        monitor.subTask( "Upload" );
        File f = File.createTempFile( "WVK_dat_", ".mdb" );
        try (FileOutputStream out = new FileOutputStream( f )) {
            IOUtils.copy( in, out );
        }
        monitor.worked( 1 );
        
        //
        monitor.subTask( "Datenbank öffnen" );
        try (
            Database db = DatabaseBuilder.open( f )
        ){
            db.setLinkResolver( new LinkResolver() {
                @Override
                public Database resolveLinkedDatabase( Database linkerDb, String linkeeFileName ) throws IOException {
                    return DatabaseBuilder.open( new File( FilenameUtils.getName( linkeeFileName ) ) );
                }
            });
            monitor.worked( 1 );

            // Gemarkung: id -> name
            monitor.subTask( "Gemarkungsnamen lesen" );
            Table table = db.getTable( "ESt_Gemarkung" );
            for (Row row=table.getNextRow(); row != null && !monitor.isCanceled(); row=table.getNextRow()) {
                String id = String.valueOf( row.get( "ID_Gemarkung" ) );
                gmks.put( id, (String)row.get( "Gemarkung_Name" ) );
            }
            monitor.worked( 1 );
            log.info( revier + ": " + gmks.size() + " Gemarkungsschlüssel gelesen." );

            // Gemeinden: id -> name
            monitor.subTask( "Gemeindenamen lesen" );
            table = db.getTable( "ESt_Gemeinde" );
            for (Row row=table.getNextRow(); row != null && !monitor.isCanceled(); row=table.getNextRow()) {
                String id = String.valueOf( row.get( "ID_Gemeinde" ) );
                gmds.put( id, (String)row.get( "Gemeinde_Name" ) );
            }
            monitor.worked( 1 );
            log.info( revier + ": " + gmds.size() + " Gemeindeschlüssel gelesen." );
            
            // wbRows
            monitor.subTask( "Waldbesitzer lesen" );
            table = db.getTable( new MdbEntityImporter( uow, Waldbesitzer.class ).getTableName() );
            for (Row row=table.getNextRow(); row != null && !monitor.isCanceled(); row=table.getNextRow()) {
                wbRows.put( wbId( row, true ), row );
            }
            monitor.worked( 1 );
            log.info( revier + ": " + wbRows.size() + " Waldbesitzer-Rows gelesen." );

            // Flurstuecke -> Waldbesitzer
            SubMonitor submon = new SubMonitor( monitor, 4 );
            importFlurstuecke( db, submon );

            // Adressen für importierte Waldbesitzer
            new MdbEntityImporter<Kontakt>( uow, Kontakt.class ) {
                @Override
                public Kontakt createEntity( final Map row, String id ) {
                    Waldbesitzer wb = uow.entity( Waldbesitzer.class, wbId( row, true ) );
                    if (wb != null && wbIds.contains( wb.id() )/*wb.status().equals( EntityStatus.CREATED )*/) {
                        return wb.kontakte.createElement( (Kontakt proto) -> {
                            adrImportCount++; 
                            return fill( proto, row );
                        });
                    }
                    return null;
                }
            }.importTable( db, submon );
            log.info( revier + ": Kontakte: " + adrImportCount );

            monitor.done();
        }
    }

    
    protected String wbId( Map<String,Object> row, boolean excOnFail ) {
        Object id = row.get( "ID_WBS" );
        if (id == null && excOnFail) {
            throw new IllegalStateException( "Row has no ID_WBS: " + row );
        }
        else if (id == null) {
            log.info( "Row has no ID_WBS: " + row );
            return null;
        }
        else {
            return "Waldbesitzer." + id.toString();
        }
    }
    
    
    protected void importFlurstuecke( Database db, IProgressMonitor monitor ) throws IOException {
        GmkImporter gmkHelper = new GmkImporter();
        new MdbEntityImporter<Flurstueck>( uow, Flurstueck.class ) {
            @Override
            public Flurstueck createEntity( final Map row, String id ) {
                // Gemarkung finden
                String gemeindeId = String.valueOf( row.get( "FL_Gemeinde" ) );
                String gemarkungId = String.valueOf( row.get( "FL_Gemarkung" ) );

                String gmkschl = gmkHelper.gmkschl( gemeindeId, gemarkungId );
                Gemarkung gmk = gmkschl != null ? uow.entity( Gemarkung.class, gmkschl ) : null;
                
                // Revier prüfen
                if (gmk != null && !gmk.revier.get().equals( revier )) {
                    return null;
                }

                // Flurstück trotzdem anlegen
                if (gmk == null) {
                    log.warn( "Keine Gemarkung für: " + gemeindeId + " / " + gemarkungId );
                }

                // Waldbesitzer
                Row wbRow = wbRows.get( wbId( row, false ) );
                if (wbRow == null) {
                    log.warn( "Flurstück ohne ID_WBS: " + row );
                    return null;
                }
                Waldbesitzer wb = importWaldbesitzer( wbId( row, true ), wbRow );

                // ohne Gmk gibt es keine Revierzuordnung und wir könnten ein Flst
                // mehrfach importieren
                Integer wkvId = (Integer)row.get( "ID_FL" );
                if (wb.flurstuecke.stream().filter( fst -> Objects.equals( fst.wvkId.get(), wkvId ) ).findAny().isPresent()) {
                    log.warn( "Flurstück ohne Gmk bereits importiert: " + row );                    
                    return null;
                }
                
                return wb.flurstuecke.createElement( (Flurstueck proto) -> {
                    fill( proto, row );
                    
                    proto.gemarkung.set( gmk );
                    proto.zaehlerNenner.set( whitespace.matcher( proto.zaehlerNenner.get() ).replaceAll( "" ) );                            
                    flstImportCount++;
                    return proto;
                });
            }
        }.importTable( db, monitor );
        
        log.info( revier + ": Waldbesitzer: " + wbImportCount + ", Flurstücke: " + flstImportCount );
    }

    
    protected Waldbesitzer importWaldbesitzer( String id, Row row ) {
        Waldbesitzer wb = uow.entity( Waldbesitzer.class, id );
        if (wb == null) {
            // create new
            wb = uow.createEntity( Waldbesitzer.class, id, (Waldbesitzer proto) -> {
                MdbEntityImporter<Waldbesitzer> wbImporter = new MdbEntityImporter( uow, Waldbesitzer.class );
                wbImporter.fill( proto, row );

                String ea = (String)row.get( "WBS_EA" );
                switch (ea != null ? ea : "null") {
                    case "P": proto.eigentumsArt.set( Waldeigentumsart.Privat ); break;
                    case "K42": proto.eigentumsArt.set( Waldeigentumsart.Kirche42 ); break;
                    case "K43": proto.eigentumsArt.set( Waldeigentumsart.Kirche43 ); break;
                    case "C": proto.eigentumsArt.set( Waldeigentumsart.Körperschaft_Kommune ); break;
                    case "T": proto.eigentumsArt.set( Waldeigentumsart.BVVG ); break;
                    case "B": proto.eigentumsArt.set( Waldeigentumsart.Staat_Bund ); break;
                    case "A": proto.eigentumsArt.set( Waldeigentumsart.Körperschaft_ZVB ); break;
                    case "L": proto.eigentumsArt.set( Waldeigentumsart.Staat_Sachsen ); break;
                    default : {
                        log.warn( "Unbekannte Eigentumsart: " + ea );
                        proto.eigentumsArt.set( Waldeigentumsart.Unbekannt );
                        break;
                    }
                }
                wbImportCount++;
                return proto;
            });
            wbIds.add( (String)wb.id() );
        }
        else {
            // XXX check existing
        }
        return wb;
    }

    
    @Override
    public IStatus redo( IProgressMonitor monitor, IAdaptable info ) throws ExecutionException {
        throw new RuntimeException( "not yet implemented." );
    }


    @Override
    public IStatus undo( IProgressMonitor monitor, IAdaptable info ) throws ExecutionException {
        throw new RuntimeException( "not yet implemented." );
    }
    
}
