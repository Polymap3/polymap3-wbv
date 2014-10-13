/*
 * Copyright (C) 2014, Polymap GmbH. All rights reserved.
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

import java.util.concurrent.TimeUnit;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;

import org.polymap.core.model2.Entity;
import org.polymap.core.model2.runtime.EntityRuntimeContext.EntityStatus;
import org.polymap.core.model2.runtime.ValueInitializer;
import org.polymap.core.runtime.event.EventFilter;
import org.polymap.core.runtime.event.EventHandler;
import org.polymap.core.ui.ColumnLayoutFactory;

import org.polymap.rhei.batik.ContextProperty;
import org.polymap.rhei.batik.IAppContext;
import org.polymap.rhei.batik.IPanel;
import org.polymap.rhei.batik.IPanelSite;
import org.polymap.rhei.batik.PanelChangeEvent;
import org.polymap.rhei.batik.PanelChangeEvent.TYPE;
import org.polymap.rhei.batik.PanelIdentifier;
import org.polymap.rhei.batik.app.BatikApplication;
import org.polymap.rhei.batik.app.FormContainer;
import org.polymap.rhei.batik.toolkit.ConstraintData;
import org.polymap.rhei.batik.toolkit.IPanelSection;
import org.polymap.rhei.batik.toolkit.IPanelToolkit;
import org.polymap.rhei.batik.toolkit.MinHeightConstraint;
import org.polymap.rhei.batik.toolkit.MinWidthConstraint;
import org.polymap.rhei.batik.toolkit.PriorityConstraint;
import org.polymap.rhei.field.FormFieldEvent;
import org.polymap.rhei.field.IFormFieldListener;
import org.polymap.rhei.form.IFormEditorPageSite;

import org.polymap.openlayers.rap.widget.OpenLayersWidget;
import org.polymap.wbv.model.Kontakt;
import org.polymap.wbv.model.Waldbesitzer;
import org.polymap.wbv.model.Waldbesitzer.Waldeigentumsart;

/**
 * Dieses Panel zeigt einen {@link Waldbesitzer} und erlaubt es dessen Properties zu
 * verändern. Die Entity wird als {@link ContextProperty} übergeben. Ist dieses bei
 * {@link TYPE#ACTIVATED Aktivierung} null, dann wird eine neue Entity erzeugt.
 * 
 * @author <a href="http://www.polymap.de">Falko Bräutigam</a>
 */
public class WaldbesitzerPanel
        extends WbvPanel
        implements IPanel {

    private static Log                    log = LogFactory.getLog( WaldbesitzerPanel.class );

    public static final PanelIdentifier   ID  = new PanelIdentifier( "wbv", "waldbesitzer" );

    private ContextProperty<Waldbesitzer> waldbesitzer;

    private IPanelToolkit                 toolKit;

    private WaldbesitzerForm              wbForm;

    private IFormFieldListener            wbFormListener;

    private WbvMapViewer                  map;


    @Override
    public boolean init( IPanelSite site, IAppContext context ) {
        super.init( site, context );

        getContext().addListener( this, new EventFilter<PanelChangeEvent>() {
            public boolean apply( PanelChangeEvent input ) {
                return input.getPanel() == WaldbesitzerPanel.this && input.getType() == TYPE.ACTIVATING;
            }
        });
        
        // nur Anzeigen wenn direkt aufgerufen
        return false;
    }


    @Override
    public void dispose() {
        // wenn vorher commit, dann schadet das nicht; ansonsten neue Entity verwerfen
        closeUnitOfWork( false );
        wbForm.removeFieldListener( wbFormListener );
        super.dispose();
    }

    
    @EventHandler
    public void activating( PanelChangeEvent ev ) {
        newUnitOfWork();
        
        if (waldbesitzer.get() == null) {
            waldbesitzer.set( uow().createEntity( Waldbesitzer.class, null, new ValueInitializer<Waldbesitzer>() {
                @Override
                public Waldbesitzer initialize( Waldbesitzer prototype ) throws Exception {
                    prototype.eigentumsArt.set( Waldeigentumsart.Privat );
                    prototype.kontakte.createElement( new ValueInitializer<Kontakt>() {
                        @Override
                        public Kontakt initialize( Kontakt kontakt ) throws Exception {
                            kontakt.name.set( "Beispiel" );
                            return kontakt;
                        }
                    });
                    return prototype;
                }
            }));
        }
    }

    
    @Override
    public void createContents( Composite parent ) {
        getSite().setTitle( "Waldbesitzer" );
        toolKit = getSite().toolkit();

        IPanelSection basis = toolKit.createPanelSection( parent, "Basisdaten" );
        basis.addConstraint( new PriorityConstraint( 8 ) );
        (wbForm = new WaldbesitzerForm()).createContents( basis );

        IPanelSection besitzer = toolKit.createPanelSection( parent, "Besitzer" );
        besitzer.addConstraint( new PriorityConstraint( 7 ) );

        IPanelSection karte = toolKit.createPanelSection( parent, null );
        karte.addConstraint( new PriorityConstraint( 9 ) );

        map = new WbvMapViewer();
        OpenLayersWidget widget = map.createContents( karte.getBody() );
        widget.setLayoutData( new ConstraintData( new MinWidthConstraint( 400, 1 ),
                new MinHeightConstraint( 400, 1 ) ) );

        IPanelSection action = toolKit.createPanelSection( parent, null );
        action.addConstraint( new PriorityConstraint( 10 ) );
        createActions( action );
        // addDeleteAction( action );
    }


    protected void createActions( IPanelSection section ) {
        // submitBtn
        final Button submitBtn = toolKit.createButton( section.getBody(), "Fertig", SWT.PUSH );
        submitBtn.setEnabled( false );
        submitBtn.addSelectionListener( new SelectionAdapter() {
            @Override
            public void widgetSelected( SelectionEvent ev ) {
                try {
                    wbForm.submit();
                    uow().commit();
                    getContext().closePanel( getSite().getPath() );
                }
                catch (Exception e) {
                    BatikApplication.handleError( "Änderungen konnten nicht korrekt gespeichert werden.", e );
                }
            }
        } );
        wbForm.addFieldListener( wbFormListener = new IFormFieldListener() {
            @Override
            public void fieldChange( FormFieldEvent ev ) {
                if (ev.getEventCode() == IFormFieldListener.VALUE_CHANGE && !submitBtn.isDisposed()) {
                    submitBtn.setEnabled( wbForm.isDirty() && wbForm.isValid() );
                }
            }
        } );
    }


    private boolean entityMayBeDeleted( Entity entity ) {
        return entity.status() == EntityStatus.LOADED || entity.status() == EntityStatus.MODIFIED;
    }


    // protected void addDeleteAction( IPanelSection section ) {
    // Waldbesitzer entity = waldbesitzer.get();
    // if (entityMayBeDeleted( entity )) {
    // final String formatString = (entity.vorname.get() != null && entity.name.get()
    // != null)
    // ? "'%s %s' Löschen"
    // : "Löschen";
    // final String btnText = String.format( formatString, entity.vorname.get(),
    // entity.name.get() );
    // final Button submitBtn = toolKit.createButton( section.getBody(), btnText,
    // SWT.PUSH );
    //
    // submitBtn.addSelectionListener( new SelectionAdapter() {
    //
    // @Override
    // public void widgetSelected( SelectionEvent eb ) {
    // try {
    // repo.removeEntity( entity );
    // repo.commit();
    // getContext().closePanel( getSite().getPath() );
    // }
    // catch (Exception e) {
    // BatikApplication.handleError(
    // "Das Löschen des Waldbesitzers ist fehlgeschlagen.", e );
    // }
    // };
    // } );
    // }
    // }

    @Override
    public PanelIdentifier id() {
        return ID;
    }


    /**
     * 
     */
    public class WaldbesitzerForm
            extends FormContainer {

        @Override
        public void createFormContent( IFormEditorPageSite site ) {
            site.getPageBody().setLayout(
                    ColumnLayoutFactory.defaults().spacing( 5 ).margins( 10, 10 ).columns( 1, 1 )
                            .create() );

            Waldbesitzer entity = waldbesitzer.getOrWait( 10, TimeUnit.SECONDS );
            assert entity != null;

            // einfach, mit defaults
            createField( new PropertyAdapter( entity.eigentumsArt ) ).create();

            // // name
            // createField( feature.getProperty( entity.name.getInfo().getName() ) )
            // .setLabel( "Nachname" ).setField( new StringFormField() )
            // .setValidator( new NotEmptyValidator() ).create();
        }

    }

}
