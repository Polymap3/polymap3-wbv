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

import static org.eclipse.ui.forms.widgets.ExpandableComposite.TWISTIE;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.google.common.base.Predicates;
import com.google.common.collect.Iterables;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;

import org.eclipse.jface.action.Action;
import org.eclipse.ui.forms.widgets.ColumnLayoutData;
import org.eclipse.ui.forms.widgets.Section;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;

import org.polymap.core.model2.runtime.ValueInitializer;
import org.polymap.core.runtime.event.EventFilter;
import org.polymap.core.runtime.event.EventHandler;
import org.polymap.core.ui.ColumnLayoutFactory;
import org.polymap.core.ui.FormDataFactory;
import org.polymap.core.ui.FormLayoutFactory;

import org.polymap.rhei.batik.ContextProperty;
import org.polymap.rhei.batik.IAppContext;
import org.polymap.rhei.batik.IPanel;
import org.polymap.rhei.batik.IPanelSite;
import org.polymap.rhei.batik.PanelChangeEvent;
import org.polymap.rhei.batik.PanelChangeEvent.TYPE;
import org.polymap.rhei.batik.PanelIdentifier;
import org.polymap.rhei.batik.app.BatikApplication;
import org.polymap.rhei.batik.app.FormContainer;
import org.polymap.rhei.batik.toolkit.IPanelSection;
import org.polymap.rhei.batik.toolkit.IPanelToolkit;
import org.polymap.rhei.batik.toolkit.NeighborhoodConstraint;
import org.polymap.rhei.batik.toolkit.NeighborhoodConstraint.Neighborhood;
import org.polymap.rhei.batik.toolkit.PriorityConstraint;
import org.polymap.rhei.field.FormFieldEvent;
import org.polymap.rhei.field.IFormFieldListener;
import org.polymap.rhei.field.TextFormField;
import org.polymap.rhei.form.IFormEditorPageSite;

import org.polymap.wbv.WbvPlugin;
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

    private IPanelToolkit                 tk;

    private WaldbesitzerForm              wbForm;

    private IFormFieldListener            wbFormListener;

    private List<KontaktForm>             kForms = new ArrayList();

    private WbvMapViewer                  map;

    private Action                        submitAction;

    private IFormFieldListener            formFieldListener;


    @Override
    public boolean init( IPanelSite site, IAppContext context ) {
        super.init( site, context );

        // submit tool
        submitAction = new Action( "Übernehmen" ) {
            public void run() {
                try {
                    wbForm.submit();
                    //besitzerForm.submit();
                    uow().commit();
                    getContext().closePanel( getSite().getPath() );
                }
                catch (Exception e) {
                    BatikApplication.handleError( "Änderungen konnten nicht korrekt gespeichert werden.", e );
                }
            }
        };
        submitAction.setToolTipText( "Änderungen in die Datenbank übernehmen" );
        submitAction.setEnabled( false );
        submitAction.setDescription( IPanelSite.SUBMIT );
        site.addToolbarAction( submitAction );

        // test tool
        Action testTool = new Action( "Test" ) {
            public void run() {
            }
        };
        testTool.setEnabled( false );
        site.addToolbarAction( testTool );

        //
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
        if (wbFormListener != null) {
            wbForm.removeFieldListener( wbFormListener );
        }
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
        tk = getSite().toolkit();

        formFieldListener = new IFormFieldListener() {
            public void fieldChange( FormFieldEvent ev ) {
                if (ev.getEventCode() == IFormFieldListener.VALUE_CHANGE) {
                    submitAction.setEnabled( 
                            wbForm.isDirty() && wbForm.isValid() 
                            /*&& besitzerForm.isDirty() && besitzerForm.isValid()*/ );
                    
                    if (!wbForm.isDirty() /*&& !besitzerForm.isDirty()*/ ) {
                        getSite().setStatus( Status.OK_STATUS );                        
                    }
                    else if (!wbForm.isValid() /*|| !besitzerForm.isValid()*/ ) {
                        getSite().setStatus( new Status( IStatus.ERROR, WbvPlugin.ID, "Nicht alle Eingaben sind korrekt." ) );
                        //getSite().setStatus( Status.OK_STATUS );                        
                    }
                    else {
                        getSite().setStatus( new Status( IStatus.OK, WbvPlugin.ID, "Alle Eingaben sind korrekt." ) );
                    }
                }
            }
        };
        
        // Basisdaten
        IPanelSection basis = tk.createPanelSection( parent, "Basisdaten" );
        basis.addConstraint( WbvPlugin.MIN_COLUMN_WIDTH, new PriorityConstraint( 10 ) );
        
        (wbForm = new WaldbesitzerForm()).createContents( basis );
        wbForm.addFieldListener( formFieldListener );

        // Kontakte
        final IPanelSection besitzer = tk.createPanelSection( parent, "Kontakt" );
        besitzer.addConstraint( 
                WbvPlugin.MIN_COLUMN_WIDTH, 
                new PriorityConstraint( 10 ), 
                new NeighborhoodConstraint( basis.getControl(), Neighborhood.BOTTOM, 100 ) );
        besitzer.getBody().setLayout( ColumnLayoutFactory.defaults().spacing( 10 ).columns( 1, 1 ).create() );

        final Waldbesitzer wb = waldbesitzer.getOrWait( 10, TimeUnit.SECONDS );
        
        for (final Kontakt kontakt : wb.kontakte) {
            createKontaktSection( besitzer.getBody(), kontakt, wb );
        }

        // Flurstücke
        final IPanelSection flurstuecke = tk.createPanelSection( parent, "Flurstücke" );
        flurstuecke.addConstraint( 
                WbvPlugin.MIN_COLUMN_WIDTH, 
                new PriorityConstraint( 5 ) );
        flurstuecke.getBody().setLayout( FormLayoutFactory.defaults().create() );
        final FlurstueckTableViewer flViewer = new FlurstueckTableViewer( uow(), flurstuecke.getBody(), wb.flurstuecke );
        getContext().propagate( flViewer );
        flViewer.getTable().setLayoutData( FormDataFactory.filled().height( 200 ).create() );

        // map
        IPanelSection karte = tk.createPanelSection( parent, null );
        karte.addConstraint( WbvPlugin.MIN_COLUMN_WIDTH, new PriorityConstraint( 0 ) );
        karte.getBody().setLayout( FormLayoutFactory.defaults().create() );

        map = new WbvMapViewer();
        map.createContents( karte.getBody() )
                .setLayoutData( FormDataFactory.filled().height( 500 ).create() );

//        IPanelSection action = tk.createPanelSection( parent, null );
//        action.addConstraint( new PriorityConstraint( 10 ) );
//        createActions( action );
    }

    
    protected void createKontaktSection( final Composite parent, final Kontakt kontakt, final Waldbesitzer wb ) {
        final Section section = tk.createSection( parent, kontakt.anzeigename(), TWISTIE | Section.SHORT_TITLE_BAR | Section.FOCUS_TITLE );
        //section.setFont( JFaceResources.getFontRegistry().getBold( JFaceResources.DEFAULT_FONT ) );
        ((Composite)section.getClient()).setLayout( FormLayoutFactory.defaults().spacing( 3 ).create() );

        // KontaktForm
        final KontaktForm form = new KontaktForm( kontakt, getSite() );
        form.createContents( tk.createComposite( (Composite)section.getClient() ) )
                .setLayoutData( FormDataFactory.filled().right( 100, -33 ).create() );
        form.addFieldListener( formFieldListener );
        
        kForms.add( form );
        
        // removeBtn
        Button removeBtn = tk.createButton( (Composite)section.getClient(), "X", SWT.PUSH );
        removeBtn.setToolTipText( "Diesen Kontakt löschen" );
        removeBtn.setLayoutData( FormDataFactory.defaults().left( 100, -30 ).right( 100 ).top( 0 ).create() );
        removeBtn.addSelectionListener( new SelectionAdapter() {
            @Override
            public void widgetSelected( SelectionEvent ev ) {
                Iterables.removeIf( wb.kontakte, Predicates.equalTo( kontakt ) );
                section.dispose();
                kForms.remove( form );
                getSite().layout( true );
            }
        });

        // addBtn
        Button addBtn = tk.createButton( (Composite)section.getClient(), "A", SWT.PUSH );
        addBtn.setToolTipText( "Einen neuen Kontakt hinzufügen" );
        addBtn.setLayoutData( FormDataFactory.defaults().left( 100, -30 ).right( 100 ).top( removeBtn ).create() );
        addBtn.addSelectionListener( new SelectionAdapter() {
            @Override
            public void widgetSelected( SelectionEvent ev ) {
                Kontakt neu = wb.kontakte.createElement( new ValueInitializer<Kontakt>() {
                    @Override
                    public Kontakt initialize( Kontakt proto ) throws Exception {
                        return proto;
                    }
                });
                createKontaktSection( parent, neu, wb );
                getSite().layout( true );
            }
        });
    }

    
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
            Composite body = site.getPageBody();
            body.setLayout( ColumnLayoutFactory.defaults().spacing( 3 ).margins( 10, 10 ).columns( 1, 1 ).create() );

            Waldbesitzer entity = waldbesitzer.getOrWait( 10, TimeUnit.SECONDS );
            assert entity != null;

            createField( body, new PropertyAdapter( entity.eigentumsArt ) )
                    .setLabel( "Eigentumsart" ).create();

            createField( body, new PropertyAdapter( entity.pächter ) ).create();

            createField( body, new PropertyAdapter( entity.bemerkung ) )
                    .setField( new TextFormField() )
                    .create().setLayoutData( new ColumnLayoutData( SWT.DEFAULT, 120 ) );

            // // name
            // createField( feature.getProperty( entity.name.getInfo().getName() ) )
            // .setLabel( "Nachname" ).setField( new StringFormField() )
            // .setValidator( new NotEmptyValidator() ).create();
        }

    }

}
