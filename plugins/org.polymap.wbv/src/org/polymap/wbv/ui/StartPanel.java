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

import org.opengis.filter.Filter;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;

import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.SelectionChangedEvent;

import org.polymap.core.model2.runtime.ValueInitializer;

import org.polymap.rhei.batik.ContextProperty;
import org.polymap.rhei.batik.DefaultPanel;
import org.polymap.rhei.batik.IAppContext;
import org.polymap.rhei.batik.IPanel;
import org.polymap.rhei.batik.IPanelSite;
import org.polymap.rhei.batik.PanelIdentifier;
import org.polymap.rhei.batik.toolkit.IPanelSection;
import org.polymap.rhei.batik.toolkit.IPanelToolkit;
import org.polymap.rhei.batik.toolkit.PriorityConstraint;

import org.polymap.wbv.model.WaldBesitzer;
import org.polymap.wbv.model.WbvRepository;

/**
 * 
 *
 * @author <a href="http://www.polymap.de">Falko Bräutigam</a>
 */
public class StartPanel
        extends DefaultPanel
        implements IPanel {

    private static Log log = LogFactory.getLog( StartPanel.class );

    public static final PanelIdentifier ID = new PanelIdentifier( "start" );

    private ContextProperty<WaldBesitzer>   selected;
    
    private ContextProperty<WbvRepository>  repo;
    
    
    @Override
    public boolean init( IPanelSite site, IAppContext context ) {
        super.init( site, context );
        // nur als start panel darstellen (ohne kinder)
        if (site.getPath().size() == 1) {
            // init ...
            repo.set( WbvRepository.instance() );
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

//        FormContainer searchForm = new FormContainer() {
//            public void createFormContent( IFormEditorPageSite site ) {
//                BaseFormEditorPage delegate = new WaldBesitzerPageProvider.BaseFormEditorPage( feature, fs );
//            }
//        };
        
        // results table
        IPanelSection tableSection = tk.createPanelSection( parent, null );
        tableSection.addConstraint( new PriorityConstraint( 0 ) );
        final WaldbesitzerTableViewer viewer = new WaldbesitzerTableViewer( tableSection.getBody(), Filter.INCLUDE, SWT.NONE );
        viewer.addSelectionChangedListener( new ISelectionChangedListener() {
            @Override
            public void selectionChanged( SelectionChangedEvent ev ) {
                selected.set( viewer.getSelected().get( 0 ) );
                getContext().openPanel( WaldbesitzerPanel.ID );
            }
        });
        
        Button createBtn = tk.createButton( parent, "Waldbesitzer anlegen...", SWT.PUSH );
        createBtn.addSelectionListener( new SelectionAdapter() {
            @Override
            public void widgetSelected( SelectionEvent e ) {
                WbvRepository repo = WbvRepository.instance();
                WaldBesitzer entity = repo.createEntity( WaldBesitzer.class, null, new ValueInitializer<WaldBesitzer>() {
                    @Override
                    public WaldBesitzer initialize( WaldBesitzer value ) throws Exception {
                        return value;
                    }
                });
                selected.set( entity );
                getContext().openPanel( WaldbesitzerPanel.ID );
            }
        });

        // map
        IPanelSection image = tk.createPanelSection( parent, null );
        image.addConstraint( new PriorityConstraint( 5 ) );
        tk.createFlowText( image.getBody(), "**[Karte]**" );
    }


    @Override
    public PanelIdentifier id() {
        return ID;
    }
    
}
