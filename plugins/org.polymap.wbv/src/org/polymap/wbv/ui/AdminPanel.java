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

import static org.polymap.wbv.model.Waldbesitzer.HERRENLOSE_FLURSTÜCKE;

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
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;

import org.eclipse.core.runtime.jobs.IJobChangeEvent;
import org.eclipse.core.runtime.jobs.JobChangeAdapter;
import org.eclipse.rap.rwt.client.ClientFile;

import org.polymap.core.operation.OperationSupport;
import org.polymap.core.runtime.Timer;
import org.polymap.core.security.SecurityContext;
import org.polymap.core.security.SecurityUtils;
import org.polymap.core.ui.StatusDispatcher;

import org.polymap.rhei.batik.BatikPlugin;
import org.polymap.rhei.batik.IPanel;
import org.polymap.rhei.batik.PanelIdentifier;
import org.polymap.rhei.batik.app.SvgImageRegistryHelper;
import org.polymap.rhei.batik.toolkit.ConstraintData;
import org.polymap.rhei.batik.toolkit.IPanelSection;
import org.polymap.rhei.batik.toolkit.IPanelToolkit;
import org.polymap.rhei.batik.toolkit.MinWidthConstraint;
import org.polymap.rhei.batik.toolkit.PriorityConstraint;
import org.polymap.rhei.batik.toolkit.Snackbar.Appearance;

import org.polymap.model2.query.ResultSet;
import org.polymap.model2.runtime.CopyCompositeState;
import org.polymap.model2.runtime.UnitOfWork;
import org.polymap.model2.runtime.ValueInitializer;
import org.polymap.rap.updownload.upload.IUploadHandler;
import org.polymap.rap.updownload.upload.Upload;
import org.polymap.wbv.WbvPlugin;
import org.polymap.wbv.mdb.WvkImporter;
import org.polymap.wbv.model.Baumart;
import org.polymap.wbv.model.Flurstueck;
import org.polymap.wbv.model.Gemarkung;
import org.polymap.wbv.model.Kontakt;
import org.polymap.wbv.model.Revier;
import org.polymap.wbv.model.Waldbesitzer;
import org.polymap.wbv.model.Waldbesitzer.Waldeigentumsart;
import org.polymap.wbv.model.WbvRepository;

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
        IPanel parent = getContext().getPanel( getSite().getPath().removeLast( 1 ) );
        if (parent instanceof StartPanel) {
            site().title.set( "" );
            site().tooltip.set( "Administration und Import" );
            site().icon.set( BatikPlugin.images().svgImage( "settings.svg", SvgImageRegistryHelper.WHITE24 ) );
            return true;
        }
        return false;
    }


    @Override
    public void createContents( Composite parent ) {
        site().preferredWidth.set( 600 );
        if ((!SecurityContext.instance().isLoggedIn() || !SecurityUtils.isAdmin())
                && !"falko".equals( System.getProperty( "user.name" ))) {
            site().toolkit().createFlowText( parent, "Dieser Bereich ist nur für **Administratoren** zugänglich." );
        }
        else {
            createDeleteWkvSection( parent );
            createWkvSection( parent );
            createBaumartenSection( parent );
            createGemarkungSection( parent );
            createHerrenlosSection( parent );
        }
    }
    

    protected void createHerrenlosSection( Composite parent ) {
        IPanelSection section = tk().createPanelSection( parent, "Flurstücke ohne WB" );
        section.addConstraint( new PriorityConstraint( 0 ), new MinWidthConstraint( 400, 1 ) );

        tk().createFlowText( section.getBody(), "Herrenlose Flurstücke zuordnen.")
                .setLayoutData( new ConstraintData( new PriorityConstraint( 1 ) ) );
        
        Button btn = tk().createButton( section.getBody(), "Suche starten...", SWT.PUSH );
        btn.addSelectionListener( new SelectionAdapter() {
            @Override
            public void widgetSelected( SelectionEvent ev ) {
                Timer timer = new Timer().start();
                try (
                    UnitOfWork uow = WbvRepository.newUnitOfWork();
                ){
                    // check target
                    Waldbesitzer target = uow.entity( Waldbesitzer.class, HERRENLOSE_FLURSTÜCKE ); 
                    if (target == null) {
                        target = uow.createEntity( Waldbesitzer.class, HERRENLOSE_FLURSTÜCKE, (Waldbesitzer proto) -> {
                            proto.bemerkung.set( "Pseudo-Waldbesitzer, der alle Flurstücke enthält, die nach dem Import nicht zugeordnet werden konnten." );
                            proto.eigentumsArt.set( Waldeigentumsart.Privat );
                            proto.kontakte.createElement( (Kontakt kontakt) -> {
                                Kontakt.defaults.initialize( kontakt );
                                kontakt.name.set( "Herrenlose Flurstücke" );
                                kontakt.bemerkung.set( "Pseudo-Waldbesitzer, der alle Flurstücke enthält, die nach dem Import nicht zugeordnet werden konnten." );
                                return kontakt;
                            });
                            return proto;
                        });
                    }
                    
                    // scan all
                    try (
                        ResultSet<Waldbesitzer> rs = uow.query( Waldbesitzer.class ).execute();
                    ){
                        int count = 0, found = 0;
                        for (Waldbesitzer wb : rs) {
                            if (wb.besitzer() == null || StringUtils.isBlank( wb.besitzer().name.get() )) {
                                log.info( "    Waldbesitzer: " + wb );
                                found++;
                                
                                for (Flurstueck fst : wb.flurstuecke) {
                                    target.flurstuecke.createElement( (Flurstueck proto) -> {
                                        CopyCompositeState.from( fst ).to( proto );
                                        return proto;
                                    });
                                }
                                uow.removeEntity( wb );
                            }
                            count++;
                        }
                        log.info( "Scanned: " + count + ", found: " + found );
                        site().toolkit().createSnackbar( Appearance.FadeIn, count + " scanned, " + found + " moved, " + timer.elapsedTime() + "ms" );
                    }
                    
                    uow.commit();
                }
            }
        });
    }
    
    
    
    protected void createDeleteWkvSection( Composite parent ) {
        IPanelSection section = tk().createPanelSection( parent, "WKV-Daten löschen" );
        section.addConstraint( new PriorityConstraint( 10 ), new MinWidthConstraint( 400, 1 ) );
//        section.getBody().setData( WidgetUtil.CUSTOM_VARIANT, DesktopToolkit.CSS_FORM  );

        tk().createFlowText( section.getBody(), "Alle WKV-Daten vor neuem Import löschen.")
                .setLayoutData( new ConstraintData( new PriorityConstraint( 1 ) ) );
        
        Button btn = tk().createButton( section.getBody(), "Löschen", SWT.PUSH );
        btn.addSelectionListener( new SelectionAdapter() {
            @Override
            public void widgetSelected( SelectionEvent e ) {
                UnitOfWork uow = WbvRepository.newUnitOfWork();
                ResultSet<Waldbesitzer> rs = uow.query( Waldbesitzer.class ).execute();
                for (Waldbesitzer wb : rs) {
                    uow.removeEntity( wb );
                }
                uow.commit();
                site().toolkit().createSnackbar( Appearance.FlyIn, "Löschen war erfolgreich." );
            }
        });
    }
    
    
    protected void createWkvSection( Composite parent ) {
        IPanelSection section = tk().createPanelSection( parent, "WKV-Daten: Import (WKV_dat_XXX.mdb)" );
        section.addConstraint( new PriorityConstraint( 5 ), new MinWidthConstraint( 400, 1 ) );
//        section.getBody().setData( WidgetUtil.CUSTOM_VARIANT, DesktopToolkit.CSS_FORM  );

        tk().createFlowText( section.getBody(), "Import von WKV-Daten aus einer MS-Access-Datei (WKV_dat_[Reviername].mdb).")
                .setLayoutData( new ConstraintData( new PriorityConstraint( 1 ) ) );

        IPanelSection formSection = tk().createPanelSection( section, null );
        formSection.addConstraint( new PriorityConstraint( 0 ) );

        Upload upload = new Upload( formSection.getBody(), SWT.NONE, Upload.SHOW_PROGRESS );
        upload.setHandler( new IUploadHandler() {
            @Override
            public void uploadStarted( ClientFile clientFile, InputStream in ) throws Exception {
                try {
                    UnitOfWork uow = WbvRepository.newUnitOfWork();
                    site().toolkit().createSnackbar( Appearance.FlyIn, "Import läuft..." );

                    WvkImporter op = new WvkImporter( uow, clientFile, in );
                    OperationSupport.instance().execute( op, false, false, new JobChangeAdapter() {
                        @Override
                        public void done( IJobChangeEvent ev ) {
                            if (ev.getResult().isOK()) {
                                uow.commit();
                                site().toolkit().createSnackbar( Appearance.FlyIn, "Import war erfolgreich." );
                            }
                            else {
                                site().toolkit().createSnackbar( Appearance.FlyIn, ev.getResult().getMessage() );
                            }
                            uow.close();
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
                try (
                    UnitOfWork uow = WbvRepository.newUnitOfWork();
                ){
                    for (List<String> l = csv.read(); l != null; l = csv.read()) {
                        final String[] line = l.toArray( new String[l.size()] );
                        uow.createEntity( Baumart.class, null, new ValueInitializer<Baumart>() {
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
                    uow.commit();
                }
                catch (Exception e) {
                    StatusDispatcher.handleError( "Die Daten konnten nicht korrekt importiert werden.", e );
                }
            }
        });
    }

    
    protected void createGemarkungSection( Composite parent ) {
        IPanelToolkit tk = getSite().toolkit();
        IPanelSection section = tk.createPanelSection( parent, "Gemarkungen/Forstreviere: Import CSV-Daten" );
        section.addConstraint( new PriorityConstraint( 100 ), WbvPlugin.MIN_COLUMN_WIDTH );
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
                try (
                    UnitOfWork uow = WbvRepository.newUnitOfWork();
                ){
                    // aktuelle Gemarkung Entities löschen
                    for (Gemarkung gmk : uow.query( Gemarkung.class ).execute()) {
                        uow.removeEntity( gmk );
                    }
                    uow.commit();

                    // neue importieren
                    int count = 0;
                    for (List<String> l = csv.read(); l != null; l = csv.read(), count++) {
                        final String[] line = l.toArray( new String[l.size()] );
                        String id = line[0];
                        if (!StringUtils.isNumeric( id )) {
                            log.warn( "Skipping header line: " + Arrays.toString( line ) );
                            continue;
                        }
                        uow.createEntity( Gemarkung.class, id, new ValueInitializer<Gemarkung>() {
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
                    log.info( "IMPORT: CSV lines: " + count + ", now in store: " + uow.query( Gemarkung.class ).execute().size() );
                    uow.commit();
                    log.info( "IMPORT: now in store: " + uow.query( Gemarkung.class ).execute().size() );
                }
                catch (Exception e) {
                    StatusDispatcher.handleError( "Die Daten konnten nicht korrekt importiert werden.", e );
                }
            }
        });
    }
    
}
