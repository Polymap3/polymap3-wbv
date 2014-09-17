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

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.polymap.core.model2.runtime.ValueInitializer;
import org.polymap.core.ui.FormDataFactory;
import org.polymap.core.ui.FormLayoutFactory;
import org.polymap.openlayers.rap.widget.OpenLayersWidget;
import org.polymap.rhei.batik.ContextProperty;
import org.polymap.rhei.batik.IAppContext;
import org.polymap.rhei.batik.IPanel;
import org.polymap.rhei.batik.IPanelSite;
import org.polymap.rhei.batik.PanelIdentifier;
import org.polymap.rhei.batik.toolkit.ConstraintData;
import org.polymap.rhei.batik.toolkit.IPanelSection;
import org.polymap.rhei.batik.toolkit.IPanelToolkit;
import org.polymap.rhei.batik.toolkit.MinHeightConstraint;
import org.polymap.rhei.batik.toolkit.MinWidthConstraint;
import org.polymap.rhei.batik.toolkit.PriorityConstraint;
import org.polymap.wbv.model.Kontakt;
import org.polymap.wbv.model.Waldbesitzer;
import org.polymap.wbv.model.Waldbesitzer.Waldeigentumsart;
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

    private ContextProperty<Waldbesitzer> selected;

    private WbvMapViewer                  map;


    @Override
    public boolean init( IPanelSite site, IAppContext context ) {
        super.init( site, context );

        // nur als start panel darstellen
        if (site.getPath().size() == 1) {
            // init root UnitOfWork
            repo.set( new WbvRepository() );
            return true;
        }
        return false;
    }


    @Override
    public void createContents( Composite parent ) {
        getSite().setTitle( "Start" );
        IPanelToolkit tk = getSite().toolkit();

        IPanelSection welcome = tk.createPanelSection( parent, "Suche" );
        welcome.addConstraint( new PriorityConstraint( 10 ) );
        tk.createText( welcome.getBody(), "Volltext...", SWT.BORDER );

        // FormContainer searchForm = new FormContainer() {
        // public void createFormContent( IFormEditorPageSite site ) {
        // BaseFormEditorPage delegate = new
        // WaldbesitzerPageProvider.BaseFormEditorPage( feature, fs );
        // }
        // };

        // results table
        IPanelSection tableSection = tk.createPanelSection( parent, null );
        tableSection.addConstraint( new PriorityConstraint( 0 ), new MinWidthConstraint( 500, 0 ) );
        tableSection.getBody().setLayout( FormLayoutFactory.defaults().create() );

        final WaldbesitzerTableViewer viewer = new WaldbesitzerTableViewer( repo.get(),
                tableSection.getBody(), repo.get().query( Waldbesitzer.class ), SWT.NONE );
        getContext().propagate( viewer );
        FormDataFactory.filled().height( 300 ).width( 420 ).applyTo( viewer.getTable() );
        viewer.addSelectionChangedListener( new ISelectionChangedListener() {

            @Override
            public void selectionChanged( SelectionChangedEvent ev ) {
                selected.set( viewer.getSelected().get( 0 ) );
                getContext().openPanel( WaldbesitzerPanel.ID );
            }
        } );

        Button createBtn = tk.createButton( parent, "Waldbesitzer anlegen...", SWT.PUSH );
        createBtn.addSelectionListener( new SelectionAdapter() {

            @Override
            public void widgetSelected( SelectionEvent e ) {
                Waldbesitzer entity = repo.get().createEntity( Waldbesitzer.class, null,
                        new ValueInitializer<Waldbesitzer>() {

                            @Override
                            public Waldbesitzer initialize( Waldbesitzer prototype )
                                    throws Exception {
                                prototype.eigentumsArt.set( Waldeigentumsart.Privat );
                                prototype.ansprechpartner
                                        .createElement( new ValueInitializer<Kontakt>() {

                                            @Override
                                            public Kontakt initialize( Kontakt kontakt )
                                                    throws Exception {
                                                kontakt.name.set( "Beispiel" );
                                                return kontakt;
                                            }
                                        } );
                                return prototype;
                            }
                        } );
                selected.set( entity );
                getContext().openPanel( WaldbesitzerPanel.ID );
            }
        } );

        // map
        IPanelSection karte = tk.createPanelSection( parent, null );
        karte.addConstraint( new PriorityConstraint( 5 ) );
        map = new WbvMapViewer();
        OpenLayersWidget widget = map.createContents( karte.getBody() );
        widget.setLayoutData( new ConstraintData( new MinWidthConstraint( 400, 1 ),
                new MinHeightConstraint( 400, 1 ) ) );

    }


    @Override
    public PanelIdentifier id() {
        return ID;
    }

}
