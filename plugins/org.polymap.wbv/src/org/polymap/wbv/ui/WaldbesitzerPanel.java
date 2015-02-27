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

import static org.eclipse.ui.forms.widgets.ExpandableComposite.TREE_NODE;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import org.geotools.data.FeatureStore;
import org.opengis.feature.Feature;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.google.common.collect.Iterables;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.SelectionChangedEvent;

import org.eclipse.ui.forms.widgets.ColumnLayoutData;
import org.eclipse.ui.forms.widgets.Section;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;

import org.polymap.core.model2.runtime.ValueInitializer;
import org.polymap.core.project.ILayer;
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
import org.polymap.rhei.batik.app.Enableable;
import org.polymap.rhei.batik.app.FormContainer;
import org.polymap.rhei.batik.app.SubmitStatusManager;
import org.polymap.rhei.batik.map.FindFeaturesMenuContribution;
import org.polymap.rhei.batik.map.IContextMenuContribution;
import org.polymap.rhei.batik.map.IContextMenuProvider;
import org.polymap.rhei.batik.toolkit.IPanelSection;
import org.polymap.rhei.batik.toolkit.IPanelToolkit;
import org.polymap.rhei.batik.toolkit.PriorityConstraint;
import org.polymap.rhei.field.FormFieldEvent;
import org.polymap.rhei.field.IFormFieldListener;
import org.polymap.rhei.field.PicklistFormField;
import org.polymap.rhei.field.TextFormField;
import org.polymap.rhei.form.IFormEditorPageSite;

import org.polymap.wbv.WbvPlugin;
import org.polymap.wbv.model.Flurstueck;
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

    private ContextProperty<Waldbesitzer> wbParam;
    
    private Waldbesitzer                  wb;

    private IPanelToolkit                 tk;

    private WaldbesitzerForm              wbForm;

    private IFormFieldListener            wbFormListener;

    private Map<KontaktForm,IFormFieldListener> kForms = new HashMap();

    private WbvMapViewer                  map;

    private Action                        submitAction;
    
    private SubmitStatusManager           statusAdapter;


    @Override
    public boolean init( IPanelSite site, IAppContext context ) {
        super.init( site, context );

        // submit tool
        submitAction = new Action( "Übernehmen" ) {
            public void run() {
                try {
                    wbForm.submit();
                    for (KontaktForm form : kForms.keySet()) {
                        form.submit();
                    }
                    closeUnitOfWork( Completion.STORE );
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

        //
        getContext().addListener( this, new EventFilter<PanelChangeEvent>() {
            public boolean apply( PanelChangeEvent input ) {
                return input.getPanel() == WaldbesitzerPanel.this && input.getType() == TYPE.ACTIVATING;
            }
        });
        
        statusAdapter = new SubmitStatusManager( this ).setSubmit( Enableable.of( submitAction ) );
        
        // nur Anzeigen wenn direkt aufgerufen
        return false;
    }


    @Override
    public void dispose() {
        // wenn vorher commit, dann schadet das nicht; ansonsten neue Entity verwerfen
        closeUnitOfWork( Completion.CANCEL );
        wb = null;
        
        if (wbFormListener != null) {
            wbForm.removeFieldListener( wbFormListener );
        }
        for (Map.Entry<KontaktForm,IFormFieldListener> entry : kForms.entrySet()) {
            entry.getKey().removeFieldListener( entry.getValue() );
        }
        super.dispose();
    }

    
    @EventHandler
    public void activating( PanelChangeEvent ev ) {
        log.info( "activating()..." );
        newUnitOfWork();
        
        // create new
        if (wbParam.get() == null) {
            wb = uow().createEntity( Waldbesitzer.class, null, new ValueInitializer<Waldbesitzer>() {
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
            });
        }
        // re-fetch
        else {
            wb = uow().entity( Waldbesitzer.class, wbParam.get().id() );
        }
    }

    
    protected void enableSubmit( boolean enable, String msg ) {
        submitAction.setEnabled( submitAction.isEnabled() || enable );
    }
    
    
    @Override
    public void createContents( Composite parent ) {
        log.info( "createContents()..." );
        while (wb == null) {
            try { Thread.sleep( 100 ); } catch (InterruptedException e) {}
        }
        
        String title = StringUtils.abbreviate( wb.besitzer().anzeigename(), 20 );
        getSite().setTitle( title );
        tk = getSite().toolkit();

        // Basisdaten
        IPanelSection basis = tk.createPanelSection( parent, "Basisdaten", IPanelSection.EXPANDABLE );
        basis.addConstraint( WbvPlugin.MIN_COLUMN_WIDTH, new PriorityConstraint( 100 ) );
        
        (wbForm = new WaldbesitzerForm()).createContents( basis );
        wbForm.addFieldListener( wbFormListener = new EnableSubmitFormFieldListener( wbForm ) );

        // Kontakte
        final IPanelSection besitzer = tk.createPanelSection( parent, "Besitzer/Kontakte" );
        besitzer.addConstraint( 
                WbvPlugin.MIN_COLUMN_WIDTH, 
                new PriorityConstraint( 1 ) );
                //new NeighborhoodConstraint( basis.getControl(), Neighborhood.BOTTOM, 1 ) );
        besitzer.getBody().setLayout( ColumnLayoutFactory.defaults().spacing( 10 ).columns( 1, 1 ).create() );
        
        for (final Kontakt kontakt : wb.kontakte) {
            createKontaktSection( besitzer.getBody(), kontakt );
        }

        // Flurstücke
        final IPanelSection flurstuecke = tk.createPanelSection( parent, "Flurstücke" );
        flurstuecke.addConstraint( 
                WbvPlugin.MIN_COLUMN_WIDTH, 
                new PriorityConstraint( 0 ) );
        flurstuecke.getBody().setLayout( FormLayoutFactory.defaults().create() );
        createFlurstueckSection( flurstuecke.getBody() );

        // map
        IPanelSection karte = tk.createPanelSection( parent, null );
        karte.addConstraint( WbvPlugin.MIN_COLUMN_WIDTH, new PriorityConstraint( 10 ) );
        karte.getBody().setLayout( ColumnLayoutFactory.defaults().columns( 1, 1 ).create() );

        try {
            map = new WbvMapViewer( getSite() );
            map.createContents( karte.getBody() )
                    .setLayoutData( new ColumnLayoutData( SWT.DEFAULT, 500 ) );
        }
        catch (Exception e) {
            throw new RuntimeException( e );
        }
    
        // context menu
        //map.getContextMenu().addProvider( new WaldflaechenMenu() );
        map.getContextMenu().addProvider( new IContextMenuProvider() {
            @Override
            public IContextMenuContribution createContribution() {
                return new FindFeaturesMenuContribution() {
                    @Override
                    protected void onMenuOpen( FeatureStore fs, Feature feature, ILayer layer ) {
                        log.info( "Feature: " + feature );
                    }
                };            
            }
        });
    }

    
    protected void createFlurstueckSection( Composite parent ) {
        parent.setLayout( FormLayoutFactory.defaults().spacing( 3 ).create() );

        final FlurstueckTableViewer viewer = new FlurstueckTableViewer( uow(), parent, wb.flurstuecke );
        getContext().propagate( viewer );
        viewer.getTable().setLayoutData( FormDataFactory.filled().right( 100, -33 ).height( 200 ).create() );
        
        // addBtn
        final Button addBtn = tk.createButton( parent, "+", SWT.PUSH );
        addBtn.setToolTipText( "Ein neues Flurstück anlegen" );
        addBtn.setLayoutData( FormDataFactory.defaults().left( 100, -30 ).right( 100 ).top( 0 ).create() );
        addBtn.addSelectionListener( new SelectionAdapter() {
            @Override
            public void widgetSelected( SelectionEvent ev ) {
                wb.flurstuecke.createElement( new ValueInitializer<Flurstueck>() {
                    @Override
                    public Flurstueck initialize( Flurstueck proto ) throws Exception {
                        proto.landkreis.set( "Mittelsachsen" );
                        proto.eingabe.set( new Date() );
                        return proto;
                    }
                });
                log.info( wb.toString() );
                viewer.refresh( true );
                statusAdapter.updateStatusOf( this, new Status( IStatus.OK, WbvPlugin.ID, "Alle Eingaben sind korrekt." ) );
            }
        });

        // removeBtn
        final Button removeBtn = tk.createButton( parent, "-", SWT.PUSH );
        removeBtn.setToolTipText( "Das markierte Flurstück löschen" );
        removeBtn.setLayoutData( FormDataFactory.defaults().left( 100, -30 ).right( 100 ).top( addBtn ).create() );
        removeBtn.setEnabled( false );
        viewer.addSelectionChangedListener( new ISelectionChangedListener() {
            @Override
            public void selectionChanged( SelectionChangedEvent ev ) {
                removeBtn.setEnabled( !viewer.getSelected().isEmpty() );
            }
        });
        removeBtn.addSelectionListener( new SelectionAdapter() {
            @Override
            public void widgetSelected( SelectionEvent ev ) {
                Flurstueck selected = Iterables.getOnlyElement( viewer.getSelected(), null );
                boolean success = wb.flurstuecke.remove( selected );
//                log.info( wb.toString() );
//                log.info( "Flurstücke: " + Iterables.toString( wb.flurstuecke ) );
                if (!success) {
                    BatikApplication.handleError( "Eintrag konnte nicht gelöscht werden.", null );
                }
                //viewer.setInput( wb.flurstuecke );
                viewer.refresh( true );
                statusAdapter.updateStatusOf( this, new Status( IStatus.OK, WbvPlugin.ID, "Alle Eingaben sind korrekt." ) );
            }
        });
    }
    
    
    protected void createKontaktSection( final Composite parent, final Kontakt kontakt ) {
        final Section section = tk.createSection( parent, kontakt.anzeigename(), TREE_NODE | Section.SHORT_TITLE_BAR | Section.FOCUS_TITLE );
        //section.setFont( JFaceResources.getFontRegistry().getBold( JFaceResources.DEFAULT_FONT ) );
        ((Composite)section.getClient()).setLayout( FormLayoutFactory.defaults().spacing( 3 ).create() );

        // KontaktForm
        final KontaktForm form = new KontaktForm( kontakt, getSite() );
        form.createContents( tk.createComposite( (Composite)section.getClient() ) )
                .setLayoutData( FormDataFactory.filled().right( 100, -33 ).create() );

        EnableSubmitFormFieldListener listener = new EnableSubmitFormFieldListener( form );
        form.addFieldListener( listener );
        kForms.put( form, listener );
        
        // removeBtn
        Button removeBtn = tk.createButton( (Composite)section.getClient(), "-", SWT.PUSH );
        removeBtn.setToolTipText( "Diesen Kontakt löschen" );
        removeBtn.setLayoutData( FormDataFactory.defaults().left( 100, -30 ).right( 100 ).top( 0 ).create() );
        removeBtn.addSelectionListener( new SelectionAdapter() {
            @Override
            public void widgetSelected( SelectionEvent ev ) {
                wb.kontakte.remove( kontakt );
                section.dispose();
                kForms.remove( form );
                getSite().layout( true );
            }
        });

        // addBtn
        Button addBtn = tk.createButton( (Composite)section.getClient(), "+", SWT.PUSH );
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
                createKontaktSection( parent, neu );
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
    class WaldbesitzerForm
            extends FormContainer {

        @Override
        public void createFormContent( IFormEditorPageSite site ) {
            Composite body = site.getPageBody();
            body.setLayout( ColumnLayoutFactory.defaults().spacing( 3 ).margins( 10, 10 ).columns( 1, 1 ).create() );

            assert wb != null;

            createField( body, new PropertyAdapter( wb.eigentumsArt ) )
                    .setLabel( "Eigentumsart" )
                    .setField( new PicklistFormField( Waldeigentumsart.map() ) )
                    .create();

            createField( body, new PropertyAdapter( wb.pächter ) ).create();

            createField( body, new PropertyAdapter( wb.bemerkung ) )
                    .setField( new TextFormField() )
                    .create().setLayoutData( new ColumnLayoutData( SWT.DEFAULT, 80 ) );

            // // name
            // createField( feature.getProperty( entity.name.getInfo().getName() ) )
            // .setLabel( "Nachname" ).setField( new StringFormField() )
            // .setValidator( new NotEmptyValidator() ).create();
        }
    }


    /**
     * 
     */
    class EnableSubmitFormFieldListener
            implements IFormFieldListener {

        private FormContainer       form;

        public EnableSubmitFormFieldListener( FormContainer form ) {
            this.form = form;
        }

        @Override
        public void fieldChange( FormFieldEvent ev ) {
            if (ev.getEventCode() == IFormFieldListener.VALUE_CHANGE && wb != null) {
                //submitAction.setEnabled( form.isDirty() && form.isValid() );

                if (!form.isDirty()) {
                    statusAdapter.updateStatusOf( this, Status.OK_STATUS );                        
                }
                else if (!form.isValid()) {
                    statusAdapter.updateStatusOf( this, new Status( IStatus.ERROR, WbvPlugin.ID, "Eine oder mehrere Eingaben sind fehlerhaft." ) );
                    //getSite().setStatus( Status.OK_STATUS );
                }
                else {
                    statusAdapter.updateStatusOf( this, new Status( IStatus.OK, WbvPlugin.ID, "Alle Eingaben sind korrekt." ) );
                }
            }
        }
    }
}
