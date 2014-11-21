/*
 * Copyright (C) 2014, Falko Bräutigam. All rights reserved.
 * 
 * This is free software; you can redistribute it and/or modify it under the terms of
 * the GNU Lesser General Public License as published by the Free Software
 * Foundation; either version 3.0 of the License, or (at your option) any later
 * version.
 * 
 * This software is distributed in the hope that it will be useful, but WITHOUT ANY
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A
 * PARTICULAR PURPOSE. See the GNU Lesser General Public License for more details.
 */
package org.polymap.wbv.ui;

import org.geotools.geometry.jts.ReferencedEnvelope;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;

import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.SelectionChangedEvent;

import org.eclipse.core.runtime.Status;

import org.polymap.core.data.ui.featuretable.FeatureTableFilterBar;
import org.polymap.core.data.util.Geometries;
import org.polymap.core.model2.query.ResultSet;
import org.polymap.core.runtime.IMessages;
import org.polymap.core.ui.FormDataFactory;
import org.polymap.core.ui.FormLayoutFactory;

import org.polymap.rhei.batik.ContextProperty;
import org.polymap.rhei.batik.IAppContext;
import org.polymap.rhei.batik.IPanel;
import org.polymap.rhei.batik.IPanelSite;
import org.polymap.rhei.batik.PanelIdentifier;
import org.polymap.rhei.batik.app.BatikApplication;
import org.polymap.rhei.batik.map.HomeMapAction;
import org.polymap.rhei.batik.map.MapViewer;
import org.polymap.rhei.batik.map.ScaleMapAction;
import org.polymap.rhei.batik.toolkit.IPanelSection;
import org.polymap.rhei.batik.toolkit.IPanelToolkit;
import org.polymap.rhei.batik.toolkit.PriorityConstraint;
import org.polymap.rhei.fulltext.FullTextIndex;
import org.polymap.rhei.fulltext.ui.EntitySearchField;
import org.polymap.rhei.fulltext.ui.FulltextProposal;
import org.polymap.rhei.um.ui.LoginPanel;
import org.polymap.rhei.um.ui.LoginPanel.LoginForm;

import org.polymap.openlayers.rap.widget.layers.WMSLayer;
import org.polymap.wbv.Messages;
import org.polymap.wbv.WbvPlugin;
import org.polymap.wbv.model.Waldbesitzer;
import org.polymap.wbv.model.WbvRepository;

/**
 * 
 * 
 * @author <a href="http://www.polymap.de">Falko Bräutigam</a>
 */
public class StartPanel
        extends WbvPanel
        implements IPanel {

    private static Log                    log = LogFactory.getLog( StartPanel.class );

    public static final PanelIdentifier   ID  = new PanelIdentifier( "start" );

    private static final IMessages        i18n = Messages.forPrefix( "StartPanel" );
    
    private ContextProperty<Waldbesitzer> selected;

    private MapViewer                     map;


    @Override
    public boolean init( IPanelSite site, IAppContext context ) {
        super.init( site, context );

        // nur als start panel darstellen
        if (site.getPath().size() == 1) {
            
//            // test tool
//            site.addToolbarAction( new Action( "Test" ) {
//                public void run() {
//                }
//            });

            return true;
        }
        return false;
    }

    
    @Override
    public void createContents( Composite parent ) {
        getSite().setTitle( "Login" );
        createLoginContents( parent );
    }
    
    
    protected void createLoginContents( final Composite parent ) {
        // welcome
        IPanelToolkit tk = getSite().toolkit();
        IPanelSection welcome = tk.createPanelSection( parent, "Anmeldung" );
        welcome.addConstraint( new PriorityConstraint( 10 ) );
        tk.createFlowText( welcome.getBody(), i18n.get( "welcomeText" ) );

        // login
        IPanelSection section = tk.createPanelSection( parent, null );
        section.addConstraint( new PriorityConstraint( 0 ), WbvPlugin.MIN_COLUMN_WIDTH );

        LoginForm loginForm = new LoginPanel.LoginForm( getContext(), getSite(), user ) {
            @Override
            protected boolean login( String name, String passwd ) {
                if (super.login( name, passwd )) {
                    getSite().setTitle( "Start" );
                    getSite().setIcon( WbvPlugin.instance().imageForName( "icons/house.png" ) ); //$NON-NLS-1$
                    getSite().setStatus( new Status( Status.OK, WbvPlugin.ID, "Erfolgreich angemeldet als: <b>" + name + "</b>" ) );
                    
                    getContext().setUserName( username );

                    for (Control child : parent.getChildren()) {
                        child.dispose();
                    }
                    createMainContents( parent );
                    parent.layout( true );
                    return true;
                }
                else {
                    getSite().setStatus( new Status( Status.ERROR, WbvPlugin.ID, "Nutzername oder Passwort nicht korrekt." ) );
                    return false;
                }
            }
        };
        loginForm.setShowRegisterLink( false );
        loginForm.setShowStoreCheck( true );
        loginForm.setShowLostLink( true );
        loginForm.createContents( section );
    }
    
    
    protected void createMainContents( Composite parent ) {
        IPanelToolkit tk = getSite().toolkit();

        // results table
        IPanelSection tableSection = tk.createPanelSection( parent, "Waldbesitzer" );
        tableSection.addConstraint( new PriorityConstraint( 10 ), WbvPlugin.MIN_COLUMN_WIDTH );
        tableSection.getBody().setLayout( FormLayoutFactory.defaults().spacing( 5 ).create() );

        final WaldbesitzerTableViewer viewer = new WaldbesitzerTableViewer( uow(), 
                tableSection.getBody(), ResultSet.EMPTY, SWT.NONE );
        getContext().propagate( viewer );
        // waldbesitzer öffnen
        viewer.addSelectionChangedListener( new ISelectionChangedListener() {
            @Override
            public void selectionChanged( SelectionChangedEvent ev ) {
                if (!viewer.getSelected().isEmpty()) {
                    selected.set( viewer.getSelected().get( 0 ) );
                    getContext().openPanel( WaldbesitzerPanel.ID );
                }
            }
        });

        // waldbesitzer anlegen
        Button createBtn = tk.createButton( tableSection.getBody(), "Neu", SWT.PUSH );
        createBtn.setToolTipText( "Einen neuen Waldbesitzer anlegen" );
        createBtn.addSelectionListener( new SelectionAdapter() {
            @Override
            public void widgetSelected( SelectionEvent e ) {
                selected.set( null );
                getContext().openPanel( WaldbesitzerPanel.ID );
            }
        });

        // filterBar
        FeatureTableFilterBar filterBar = new FeatureTableFilterBar( viewer, tableSection.getBody() );

        // searchField
        FullTextIndex fulltext = WbvRepository.instance.get().fulltextIndex();
        EntitySearchField search = new EntitySearchField<Waldbesitzer>( tableSection.getBody(), fulltext, uow(), Waldbesitzer.class ) {
            @Override
            protected void doRefresh() {
                log.info( "Results: " + results.size() );
                viewer.setInput( results );
            }
        };
        search.setSearchOnEnter( false );
        search.getText().setText( "Im" );
        search.setSearchOnEnter( true );
        new FulltextProposal( fulltext, search.getText() );
        
        // layout
        int displayHeight = BatikApplication.sessionDisplay().getBounds().height;
        int tableHeight = (displayHeight - (2*65) - (2*75));  // margins, titles+icons
        createBtn.setLayoutData( FormDataFactory.filled().clearRight().clearBottom().create() );
        filterBar.getControl().setLayoutData( FormDataFactory.filled().bottom( viewer.getTable() ).left( createBtn ).right( 50 ).create() );
        search.getControl().setLayoutData( FormDataFactory.filled().height( 27 ).bottom( viewer.getTable() ).left( filterBar.getControl() ).create() );
        viewer.getTable().setLayoutData( FormDataFactory.filled().top( createBtn ).height( tableHeight ).create() );
        
        // map
        IPanelSection karte = tk.createPanelSection( parent, null );
        karte.addConstraint( new PriorityConstraint( 5 ) );
        karte.getBody().setLayout( FormLayoutFactory.defaults().create() );

        try {  //4492491.287605779 : 4717565.782433721, 5559024.99376705 : 5730490.820256532
            map = new MapViewer( getSite(), new ReferencedEnvelope( 4500000, 4700000, 5550000, 5700000, Geometries.crs( "EPSG:31468" ) ) );
            map.createContents( karte.getBody() )
                    .setLayoutData( FormDataFactory.filled().height( 500 ).create() );
        }
        catch (Exception e) {
            throw new RuntimeException( e );
        }
    
        WMSLayer osm = new WMSLayer( "OSM", "../services/WBV/", "OSM" );
        map.addLayer( osm, true, false );
        WMSLayer waldflaechen = new WMSLayer( "Waldflächen", "../services/WBV/", "Waldflaechen" );
        map.addLayer( waldflaechen, false, false );
        map.setLayerVisible( waldflaechen, true );

        map.addToolbarItem( new HomeMapAction( map ) );
        map.addToolbarItem( new ScaleMapAction( map, 1000 ) );
        map.addToolbarItem( new ScaleMapAction( map, 5000 ) );
        map.getMap().zoomTo( 12 );
    }


    @Override
    public PanelIdentifier id() {
        return ID;
    }

}
