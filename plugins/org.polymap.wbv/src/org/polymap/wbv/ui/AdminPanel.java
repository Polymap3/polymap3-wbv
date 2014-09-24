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

import java.util.List;

import java.io.InputStream;
import java.io.InputStreamReader;

import org.supercsv.io.CsvListReader;
import org.supercsv.io.ICsvListReader;
import org.supercsv.prefs.CsvPreference;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;

import org.eclipse.rwt.lifecycle.WidgetUtil;

import org.polymap.core.model2.runtime.ValueInitializer;
import org.polymap.core.runtime.event.EventFilter;
import org.polymap.core.runtime.event.EventHandler;
import org.polymap.core.security.SecurityUtils;
import org.polymap.core.ui.upload.IUploadHandler;
import org.polymap.core.ui.upload.Upload;

import org.polymap.rhei.batik.IAppContext;
import org.polymap.rhei.batik.IPanelSite;
import org.polymap.rhei.batik.PanelIdentifier;
import org.polymap.rhei.batik.PropertyAccessEvent;
import org.polymap.rhei.batik.app.BatikApplication;
import org.polymap.rhei.batik.layout.desktop.DesktopToolkit;
import org.polymap.rhei.batik.toolkit.ConstraintData;
import org.polymap.rhei.batik.toolkit.IPanelSection;
import org.polymap.rhei.batik.toolkit.IPanelToolkit;
import org.polymap.rhei.batik.toolkit.PriorityConstraint;

import org.polymap.wbv.WbvPlugin;
import org.polymap.wbv.model.Baumart;

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
    public PanelIdentifier id() {
        return ID;
    }


    @Override
    public boolean init( IPanelSite site, IAppContext context ) {
        super.init( site, context );

        if (site.getPath().size() == 1) {
            // warten auf login
            site.setTitle( null );
            user.addListener( this, new EventFilter<PropertyAccessEvent>() {
                public boolean apply( PropertyAccessEvent input ) {
                    return input.getType() == PropertyAccessEvent.TYPE.SET;
                }
            });
            return true;
        }
        return false;
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
        createBaumartenSection( parent );
    }
    
    
    protected void createBaumartenSection( Composite parent ) {
        IPanelToolkit tk = getSite().toolkit();
        IPanelSection section = tk.createPanelSection( parent, "Baumarten: Import" );
        section.addConstraint( new PriorityConstraint( 10 ) );
        section.getBody().setData( WidgetUtil.CUSTOM_VARIANT, DesktopToolkit.CSS_FORM  );

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
        Upload upload = new Upload( formSection.getBody(), SWT.NONE );
        upload.setHandler( new IUploadHandler() {
            @Override
            public void uploadStarted( String name, String contentType, int contentLength, InputStream in ) throws Exception {
                newUnitOfWork();
                
                // quoteChar, delimiterChar, endOfLineSymbols
                CsvPreference prefs = new CsvPreference( '"', ',', "\r\n" );
                ICsvListReader csv = new CsvListReader( new InputStreamReader( in, "UTF-8" ), prefs );
                try {
                    for (List<String> l = csv.read(); l != null; l = csv.read()) {
                        final String[] line = l.toArray( new String[l.size()] );
                        repo.get().createEntity( Baumart.class, null, new ValueInitializer<Baumart>() {
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
                    repo.get().commit();
                }
                catch (Exception e) {
                    repo.get().rollback();
                    BatikApplication.handleError( "Die Daten konnten nicht korrekt importiert werden.", e );
                }
            }
        });
    }
    
}
