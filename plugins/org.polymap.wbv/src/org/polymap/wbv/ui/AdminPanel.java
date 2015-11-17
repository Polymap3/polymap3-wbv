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

import java.util.Arrays;
import java.util.List;

import java.io.InputStream;
import java.io.InputStreamReader;

import org.supercsv.io.CsvListReader;
import org.supercsv.io.ICsvListReader;
import org.supercsv.prefs.CsvPreference;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.widgets.Composite;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.IJobChangeEvent;
import org.eclipse.core.runtime.jobs.JobChangeAdapter;
import org.eclipse.rap.rwt.client.ClientFile;

import org.polymap.core.operation.OperationSupport;
import org.polymap.core.runtime.event.EventHandler;
import org.polymap.core.security.SecurityUtils;
import org.polymap.core.ui.StatusDispatcher;

import org.polymap.rhei.batik.PanelIdentifier;
import org.polymap.rhei.batik.PropertyAccessEvent;
import org.polymap.rhei.batik.toolkit.ConstraintData;
import org.polymap.rhei.batik.toolkit.IPanelSection;
import org.polymap.rhei.batik.toolkit.IPanelToolkit;
import org.polymap.rhei.batik.toolkit.MinWidthConstraint;
import org.polymap.rhei.batik.toolkit.PriorityConstraint;

import org.polymap.model2.runtime.ValueInitializer;
import org.polymap.rap.updownload.upload.IUploadHandler;
import org.polymap.rap.updownload.upload.Upload;
import org.polymap.wbv.WbvPlugin;
import org.polymap.wbv.mdb.WvkImporter;
import org.polymap.wbv.model.Baumart;
import org.polymap.wbv.model.Gemarkung;
import org.polymap.wbv.model.Revier;

/**
 * 
 *
 * @author <a href="http://www.polymap.de">Falko Bräutigam</a>
 */
public class AdminPanel
        extends WbvPanel {

    private static Log log = LogFactory.getLog( AdminPanel.class );

    public static final PanelIdentifier   ID  = new PanelIdentifier( "wbv", "admin" );


    @Override
    public boolean wantsToBeShown() {
        return getSite().getPath().size() == 1;
    }


    @Override
    public void init() {
        super.init();

        // warten auf login
        getSite().setTitle( null );
        user.addListener( this, ev -> ev.getType() == PropertyAccessEvent.TYPE.SET );
    }

    
    @EventHandler( display=true )
    protected void userLoggedIn( PropertyAccessEvent ev ) {
        if (SecurityUtils.isAdmin()) {
            getSite().setTitle( "Administration" );
            getSite().setIcon( WbvPlugin.instance().imageForName( "icons/cog.png" ) ); //$NON-NLS-1$
        }
    }

    
    @Override
    public void createContents( Composite parent ) {
        createWkvSection( parent );
        createBaumartenSection( parent );
        createGemarkungSection( parent );
    }
    
    
    protected void createWkvSection( Composite parent ) {
        IPanelToolkit tk = getSite().toolkit();
        IPanelSection section = tk.createPanelSection( parent, "WKV-Daten: Import (WKV_dat.mdb)" );
        section.addConstraint( new PriorityConstraint( 5 ), new MinWidthConstraint( 400, 1 ) );
//        section.getBody().setData( WidgetUtil.CUSTOM_VARIANT, DesktopToolkit.CSS_FORM  );

        tk.createFlowText( section.getBody(), "Import von WKV-Daten aus einer MS-Access-Datei (WKV_dat.mdb).")
                .setLayoutData( new ConstraintData( new PriorityConstraint( 1 ) ) );

        IPanelSection formSection = tk.createPanelSection( section, null );
        formSection.addConstraint( new PriorityConstraint( 0 ) );
        tk.createButton( formSection.getBody(), "Import starten..." ).addSelectionListener( new SelectionAdapter() {
            @Override
            public void widgetSelected( SelectionEvent ev ) {
                try {
                    newUnitOfWork();
                    getSite().setStatus( new Status( IStatus.ERROR, WbvPlugin.ID, "Import läuft...") );

                    WvkImporter op = new WvkImporter( uow() );
                    OperationSupport.instance().execute( op, true, false, new JobChangeAdapter() {
                        @Override
                        public void done( IJobChangeEvent jev ) {
                            if (jev.getResult().isOK()) {
                                closeUnitOfWork( Completion.STORE );
                                getSite().setStatus( new Status( IStatus.OK, WbvPlugin.ID, "Import war erfolgreich." ) );
                            }
                            else {
                                closeUnitOfWork( Completion.CANCEL );                                
                                getSite().setStatus( new Status( IStatus.ERROR, WbvPlugin.ID, jev.getResult().getMessage() ) );
                            }
                        }
                    });
                }
                catch (Exception e) {
                    StatusDispatcher.handleError( "Der Import konnte nicht erfolgreich durchgeführt werden.", e );
                }
            }
        });
    }
    
    
    protected void createBaumartenSection( Composite parent ) {
        IPanelToolkit tk = getSite().toolkit();
        IPanelSection section = tk.createPanelSection( parent, "Baumarten: Import CSV-Daten" );
        section.addConstraint( new PriorityConstraint( 0 ), WbvPlugin.MIN_COLUMN_WIDTH );
//        section.getBody().setData( WidgetUtil.CUSTOM_VARIANT, DesktopToolkit.CSS_FORM  );

        tk.createFlowText( section.getBody(), "Import einer **CSV-Datei** mit Stammdaten für Baumarten." + 
                " Der Import startet sofort nach der Auswahl der Datei. Die bisherigen Einträge werden dabei **gelöscht**!" +
                "\n\nDie CSV-Datei muss im **Zeichensatz UTF-8** kodiert sein und folgende **Spalten** in der folgenden Reihenfolge enthalten:" +
                "\n\n* BA-Kategorie" +
                "\n* BA-Gruppe" + 
                "\n* BA-Gruppe_lt. Waldfeststellung" + 
                "\n* NR" + 
                "\n* BA" + 
                "\n* NAME_dt." + 
                "\n* NAME_lat."
                ).setLayoutData( new ConstraintData( new PriorityConstraint( 1 ) ) );

        IPanelSection formSection = tk.createPanelSection( section, null );
        formSection.addConstraint( new PriorityConstraint( 0 ) );
        Upload upload = new Upload( formSection.getBody(), SWT.NONE, Upload.SHOW_PROGRESS );
        upload.setHandler( new IUploadHandler() {
            @Override
            public void uploadStarted( ClientFile clientFile, InputStream in ) throws Exception {
                // quoteChar, delimiterChar, endOfLineSymbols
                CsvPreference prefs = new CsvPreference( '"', ',', "\r\n" );
                ICsvListReader csv = new CsvListReader( new InputStreamReader( in, "UTF-8" ), prefs );
                try {
                    newUnitOfWork();

                    for (List<String> l = csv.read(); l != null; l = csv.read()) {
                        final String[] line = l.toArray( new String[l.size()] );
                        uow().createEntity( Baumart.class, null, new ValueInitializer<Baumart>() {
                            @Override
                            public Baumart initialize( Baumart proto ) throws Exception {
                                proto.kategorie.set( line[0] );
                                proto.gruppe.set( line[1] );
                                proto.gruppeWaldfeststellung.set( line[2] );
                                proto.nr.set( line[3] );
                                proto.kennung.set( line[4] );
                                proto.name.set( line[5] );
                                proto.nameLateinisch.set( line[6] );
                                log.info( proto );
                                return proto;
                            }
                        });
                    }
                    closeUnitOfWork( Completion.STORE );
                }
                catch (Exception e) {
                    StatusDispatcher.handleError( "Die Daten konnten nicht korrekt importiert werden.", e );
                }
                finally {
                    closeUnitOfWork( Completion.CANCEL );
                }
            }
        });
    }

    
    protected void createGemarkungSection( Composite parent ) {
        IPanelToolkit tk = getSite().toolkit();
        IPanelSection section = tk.createPanelSection( parent, "Gemarkungen/Forstreviere: Import CSV-Daten" );
        section.addConstraint( new PriorityConstraint( 10 ), WbvPlugin.MIN_COLUMN_WIDTH );
//        section.getBody().setData( WidgetUtil.CUSTOM_VARIANT, DesktopToolkit.CSS_FORM  );

        tk.createFlowText( section.getBody(), "Import einer **CSV-Datei** mit Stammdaten der Gemarkungen/Gemeinden." + 
                " Der Import startet sofort nach der Auswahl der Datei. Die bisherigen Einträge werden dabei **gelöscht**!" +
                "\n\nDie CSV-Datei muss im **Zeichensatz UTF-8** kodiert sein und folgende **Spalten** in der folgenden Reihenfolge enthalten:" +
                "\n\n* Gemarkungsschlüssel" +
                "\n* Gemarkung" + 
                "\n* Gemeinde" + 
                "\n* Forstrevier" 
                ).setLayoutData( new ConstraintData( new PriorityConstraint( 1 ) ) );

        IPanelSection formSection = tk.createPanelSection( section, null );
        formSection.addConstraint( new PriorityConstraint( 0 ) );
        Upload upload = new Upload( formSection.getBody(), SWT.NONE, Upload.SHOW_PROGRESS );
        upload.setHandler( new IUploadHandler() {
            @Override
            public void uploadStarted( ClientFile clientFile, InputStream in ) throws Exception {
                // quoteChar, delimiterChar, endOfLineSymbols
                CsvPreference prefs = new CsvPreference( '"', ',', "\r\n" );
                ICsvListReader csv = new CsvListReader( new InputStreamReader( in, "UTF-8" ), prefs );
                try {
                    newUnitOfWork();
                    
                    // aktuelle Gemarkung Entities löschen
                    for (Gemarkung gmk : uow().query( Gemarkung.class ).execute()) {
                        uow().removeEntity( gmk );
                    }

                    // neue importieren
                    int count = 0;
                    for (List<String> l = csv.read(); l != null; l = csv.read(), count++) {
                        final String[] line = l.toArray( new String[l.size()] );
                        String id = line[0];
                        if (!StringUtils.isNumeric( id )) {
                            log.warn( "Skipping header line: " + Arrays.toString( line ) );
                            continue;
                        }
                        uow().createEntity( Gemarkung.class, id, new ValueInitializer<Gemarkung>() {
                            @Override
                            public Gemarkung initialize( Gemarkung proto ) throws Exception {
                                proto.gemarkung.set( line[1] );
                                proto.gemeinde.set( line[2] );
                                proto.revier.set( line[3] );
                                log.debug( proto );
                                return proto;
                            }
                        });
                        Revier.all.clear();
                    }
                    log.info( "IMPORT: CSV lines: " + count + ", now in store: " + uow().query( Gemarkung.class ).execute().size() );
                    closeUnitOfWork( Completion.STORE );
                    log.info( "IMPORT: now in store: " + uow().query( Gemarkung.class ).execute().size() );
                }
                catch (Exception e) {
                    StatusDispatcher.handleError( "Die Daten konnten nicht korrekt importiert werden.", e );
                }
                finally {
                    closeUnitOfWork( Completion.CANCEL );
                }
            }
        });
    }
    
}
