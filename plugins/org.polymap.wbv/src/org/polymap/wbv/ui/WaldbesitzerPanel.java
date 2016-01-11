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
import static org.polymap.rhei.batik.app.SvgImageRegistryHelper.WHITE24;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import java.beans.PropertyChangeEvent;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.google.common.collect.Iterables;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;

import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.SelectionChangedEvent;

import org.eclipse.ui.forms.widgets.ColumnLayoutData;
import org.eclipse.ui.forms.widgets.Section;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;

import org.polymap.model2.runtime.UnitOfWork;
import org.polymap.model2.runtime.ValueInitializer;

import org.polymap.core.runtime.event.EventHandler;
import org.polymap.core.security.SecurityContext;
import org.polymap.core.ui.ColumnLayoutFactory;
import org.polymap.core.ui.FormDataFactory;
import org.polymap.core.ui.FormLayoutFactory;
import org.polymap.core.ui.StatusDispatcher;

import org.polymap.rhei.batik.BatikPlugin;
import org.polymap.rhei.batik.Context;
import org.polymap.rhei.batik.IPanel;
import org.polymap.rhei.batik.IPanelSite.PanelStatus;
import org.polymap.rhei.batik.PanelChangeEvent;
import org.polymap.rhei.batik.PanelChangeEvent.EventType;
import org.polymap.rhei.batik.PanelIdentifier;
import org.polymap.rhei.batik.app.MultiStatusManager;
import org.polymap.rhei.batik.toolkit.IPanelSection;
import org.polymap.rhei.batik.toolkit.PriorityConstraint;
import org.polymap.rhei.batik.toolkit.Snackbar.Appearance;
import org.polymap.rhei.field.FormFieldEvent;
import org.polymap.rhei.field.IFormFieldListener;
import org.polymap.rhei.field.PicklistFormField;
import org.polymap.rhei.field.TextFormField;
import org.polymap.rhei.form.DefaultFormPage;
import org.polymap.rhei.form.IFormPageSite;
import org.polymap.rhei.form.batik.BatikFormContainer;

import org.polymap.wbv.WbvPlugin;
import org.polymap.wbv.model.Ereignis;
import org.polymap.wbv.model.Flurstueck;
import org.polymap.wbv.model.Kontakt;
import org.polymap.wbv.model.Waldbesitzer;
import org.polymap.wbv.model.Waldbesitzer.Waldeigentumsart;
import org.polymap.wbv.model.WbvRepository;

/**
 * Dieses Panel zeigt einen {@link Waldbesitzer} und erlaubt es dessen Properties zu
 * verändern. Die Entity wird als {@link Context} übergeben. Ist dieses bei
 * {@link EventType#ACTIVATED Aktivierung} null, dann wird eine neue Entity erzeugt.
 * 
 * @author <a href="http://www.polymap.de">Falko Bräutigam</a>
 */
public class WaldbesitzerPanel
        extends WbvPanel
        implements IPanel {

    private static Log log = LogFactory.getLog( WaldbesitzerPanel.class );

    public static final PanelIdentifier ID  = new PanelIdentifier( "wbv", "waldbesitzer" );

    /** Der aktuell selektierte {@link Waldbesitzer}. */
    private Context<Waldbesitzer>       selected;

    private UnitOfWork                  uow;
    
    private Waldbesitzer                wb;

    private BatikFormContainer          wbFormContainer;

    private IFormFieldListener          wbFormListener;

    private Map<BatikFormContainer,IFormFieldListener> kForms = new HashMap();

    private Map<BatikFormContainer,IFormFieldListener> eForms = new HashMap();
    
    private List<IFormFieldListener>    eFormListeners = new ArrayList();

    private WbvMapViewer                map;

    private Button                      submitBtn;

    private MultiStatusManager          statusAdapter;


    @Override
    public void init() {
        super.init();
        
        uow = WbvRepository.unitOfWork().newUnitOfWork();
                
        getContext().addListener( this, ev -> 
                ev.getPanel() == WaldbesitzerPanel.this && 
                ev.getType() == EventType.LIFECYCLE /*&&
                ev.getPanel().site().panelStatus() == PanelStatus.FOCUSED*/ );
    }


    @Override
    public void dispose() {
        wb = null;
        
        if (wbFormListener != null) {
            wbFormContainer.removeFieldListener( wbFormListener );
        }
        for (Map.Entry<BatikFormContainer,IFormFieldListener> entry : kForms.entrySet()) {
            entry.getKey().removeFieldListener( entry.getValue() );
        }
        for (Map.Entry<BatikFormContainer,IFormFieldListener> entry : eForms.entrySet()) {
            entry.getKey().removeFieldListener( entry.getValue() );
        }
        super.dispose();
    }

    
    @EventHandler( display=true )
    public void activating( PanelChangeEvent ev ) {
        log.debug( "panelStatus: " + ev.getPanel().site().panelStatus() );
        if (ev.getPanel().site().panelStatus() == PanelStatus.FOCUSED) {
            log.debug( "activating()..." );

            // create new
            if (selected.get() == null) {
                wb = uow.createEntity( Waldbesitzer.class, null, new ValueInitializer<Waldbesitzer>() {
                    @Override
                    public Waldbesitzer initialize( Waldbesitzer prototype ) throws Exception {
                        prototype.eigentumsArt.set( Waldeigentumsart.Privat );
                        prototype.kontakte.createElement( new ValueInitializer<Kontakt>() {
                            @Override
                            public Kontakt initialize( Kontakt kontakt ) throws Exception {
                                //kontakt.name.set( "Beispiel" );
                                return kontakt;
                            }
                        });
                        // damit die sch** tabelle den ersten Eintrag zeigt
                        prototype.flurstuecke.createElement( new ValueInitializer<Flurstueck>() {
                            @Override
                            public Flurstueck initialize( Flurstueck proto ) throws Exception {
                                proto.landkreis.set( "Mittelsachsen" );
                                proto.eingabe.set( new Date() );
                                return proto;
                            }
                        });
                        return prototype;
                    }
                });
            }
            // re-fetch
            else {
                wb = uow.entity( selected.get() );
            }
        }
    }

    
    protected void submit() {
        try {
            wbFormContainer.submit( null );
            for (BatikFormContainer formContainer : kForms.keySet()) {
                formContainer.submit( null );
            }
            for (BatikFormContainer formContainer : eForms.keySet()) {
                formContainer.submit( null );
            }
            uow.commit();
            uow.close();
            getContext().closePanel( getSite().getPath() );
        }
        catch (Exception e) {
            StatusDispatcher.handleError( "Änderungen konnten nicht korrekt gespeichert werden.", e );
        }
    }
    
    
    @Override
    public void createContents( Composite parent ) {
        while (wb == null) {
            try { Thread.sleep( 100 ); } catch (InterruptedException e) {}
        }
        
        String title = StringUtils.abbreviate( wb.besitzer().anzeigename(), 20 );
        getSite().setTitle( title.length() > 1 ? title : "Neu" );

        // submit FAB
        submitBtn = tk().createFab( BatikPlugin.images().svgImage( "check.svg", WHITE24 ), SWT.TOP|SWT.RIGHT );
        submitBtn.setToolTipText( "Änderungen in die Datenbank übernehmen" );
        submitBtn.setVisible( false );
        submitBtn.addSelectionListener( new SelectionAdapter() {
            @Override
            public void widgetSelected( SelectionEvent ev ) {
                submit();
            }
        });
        statusAdapter = new MultiStatusManager() {
            private IStatus previousStatus;
            @Override
            protected void doUpdateUI( IStatus highestSeverity ) {
                if (!highestSeverity.isOK() && (previousStatus == null || previousStatus.isOK())) {
                    log.info( "severity: " + highestSeverity );
                    tk().createSnackbar( Appearance.FadeIn, highestSeverity.getMessage() );
                }
                submitBtn.setVisible( true );
                submitBtn.setEnabled( highestSeverity.isOK() && highestSeverity != Status.OK_STATUS );
                previousStatus = highestSeverity;
            }
        };

        // Basisdaten
        IPanelSection basis = tk().createPanelSection( parent, "Basisdaten", IPanelSection.EXPANDABLE, SWT.BORDER );
        basis.addConstraint( WbvPlugin.MIN_COLUMN_WIDTH, new PriorityConstraint( 100 ) );
        
        wbFormContainer = new BatikFormContainer( new WaldbesitzerForm() );
        wbFormContainer.createContents( basis );
        wbFormContainer.addFieldListener( wbFormListener = new EnableSubmitFormFieldListener( wbFormContainer ) );

        // Kontakte
        final IPanelSection besitzer = tk().createPanelSection( parent, "Besitzer/Kontakte", SWT.NONE );
        besitzer.addConstraint( 
                WbvPlugin.MIN_COLUMN_WIDTH, 
                new PriorityConstraint( 1 ) );
                //new NeighborhoodConstraint( basis.getControl(), Neighborhood.BOTTOM, 1 ) );
        besitzer.getBody().setLayout( ColumnLayoutFactory.defaults().spacing( 10 ).columns( 1, 1 ).create() );
        
        for (final Kontakt kontakt : wb.kontakte) {
            createKontaktSection( besitzer.getBody(), kontakt );
        }

        // Ereignisse
        final IPanelSection ereignisse = tk().createPanelSection( parent, "Ereignisse", SWT.NONE );
        ereignisse.addConstraint( 
                WbvPlugin.MIN_COLUMN_WIDTH, 
                new PriorityConstraint( -10 ) );
        ereignisse.getBody().setLayout( FormLayoutFactory.defaults().spacing( 3 ).create() );

        final Composite liste = tk().createComposite( ereignisse.getBody() );
        liste.setLayoutData( FormDataFactory.filled().right( 100, -30 ).clearBottom().create() );
        liste.setLayout( ColumnLayoutFactory.defaults().spacing( 0 ).columns( 1, 1 ).create() );
        
        // addBtn
        Button addBtn = tk().createButton( ereignisse.getBody(), "+", SWT.PUSH );
        addBtn.setToolTipText( "Ein Ereignis hinzufügen" );
        addBtn.setLayoutData( FormDataFactory.defaults().left( 100, -30 ).right( 100 ).top( 0 ).create() );
        addBtn.addSelectionListener( new SelectionAdapter() {
            @Override
            public void widgetSelected( SelectionEvent ev ) {
                Ereignis neu = wb.ereignisse.createElement( new ValueInitializer<Ereignis>() {
                    @Override
                    public Ereignis initialize( Ereignis proto ) throws Exception {
                        //proto.titel.set( "Neu" );
                        proto.angelegt.set( new Date() );
                        proto.angelegtVon.set( SecurityContext.instance().getUser().getName() );
                        proto.geaendert.set( new Date() );
                        proto.geaendertVon.set( SecurityContext.instance().getUser().getName() );
                        return proto;
                    }
                });
                Control child = liste.getChildren()[0];
                if (child instanceof Label) {
                    child.dispose();
                }
                Section newSection = createEreignisSection( liste, neu );
                newSection.setExpanded( true );
                getSite().layout( true );
                liste.layout( new Control[] {newSection}, SWT.ALL|SWT.CHANGED );

                statusAdapter.updateStatusOf( liste, new Status( IStatus.OK, WbvPlugin.ID, "Ein Ereignis hinzugefügt" ) );
            }
        });
        //
        if (wb.ereignisse.isEmpty()) {
            tk().createLabel( liste, "Noch keine Ereignisse." );
        }
        else {
            List<Ereignis> reversed = new ArrayList( wb.ereignisse );
            Collections.reverse( reversed );
            for (final Ereignis ereignis : reversed ) {
                createEreignisSection( liste, ereignis ).setExpanded( false );
            }
        }

        // Flurstücke
        final IPanelSection flurstuecke = tk().createPanelSection( parent, "Flurstücke" );
        flurstuecke.addConstraint( 
                WbvPlugin.MIN_COLUMN_WIDTH, 
                new PriorityConstraint( 0 ) );
        flurstuecke.getBody().setLayout( FormLayoutFactory.defaults().create() );
        createFlurstueckSection( flurstuecke.getBody() );

//        // map
//        IPanelSection karte = tk.createPanelSection( parent, null );
//        karte.addConstraint( WbvPlugin.MIN_COLUMN_WIDTH, new PriorityConstraint( 10 ) );
//        karte.getBody().setLayout( ColumnLayoutFactory.defaults().columns( 1, 1 ).create() );
//
//        try {
//            map = new WbvMapViewer( getSite() );
//            map.createContents( karte.getBody() )
//                    .setLayoutData( new ColumnLayoutData( SWT.DEFAULT, 500 ) );
//        }
//        catch (Exception e) {
//            throw new RuntimeException( e );
//        }
    
        // context menu
        //map.getContextMenu().addProvider( new WaldflaechenMenu() );
//        map.getContextMenu().addProvider( new IContextMenuProvider() {
//            @Override
//            public IContextMenuContribution createContribution() {
//                return new FindFeaturesMenuContribution() {
//                    @Override
//                    protected void onMenuOpen( FeatureStore fs, Feature feature, ILayer layer ) {
//                        log.info( "Feature: " + feature );
//                    }
//                };            
//            }
//        });
    }

    
    protected void createFlurstueckSection( Composite parent ) {
        parent.setLayout( FormLayoutFactory.defaults().spacing( 3 ).create() );

        final FlurstueckTableViewer viewer = new FlurstueckTableViewer( uow, parent, wb.flurstuecke ) {
            @Override
            protected void fieldChange( PropertyChangeEvent ev ) {
                IStatus status = null;
                if (!isDirty()) {
                    status = Status.OK_STATUS;
                }
                else if (!isValid()) {
                    status = new Status( IStatus.ERROR, WbvPlugin.ID, "Eine Eingabe ist nicht korrekt." );
                }
                else {
                    status = new Status( IStatus.OK, WbvPlugin.ID, "Alle Eingaben sind in Ordnung" );
                }
                statusAdapter.updateStatusOf( this, status );
            }
        };
        getContext().propagate( viewer );
        viewer.getTable().setLayoutData( FormDataFactory.filled().right( 100, -33 ).height( 250 ).width( 300 ).create() );
        
        // addBtn
        final Button addBtn = tk().createButton( parent, "+", SWT.PUSH );
        addBtn.setToolTipText( "Ein neues Flurstück anlegen" );
        addBtn.setLayoutData( FormDataFactory.defaults().left( 100, -30 ).right( 100 ).top( 0 ).create() );
        addBtn.addSelectionListener( new SelectionAdapter() {
            @Override
            public void widgetSelected( SelectionEvent ev ) {
                Flurstueck newElm = wb.flurstuecke.createElement( new ValueInitializer<Flurstueck>() {
                    @Override
                    public Flurstueck initialize( Flurstueck proto ) throws Exception {
                        proto.landkreis.set( "Mittelsachsen" );
                        proto.eingabe.set( new Date() );
                        return proto;
                    }
                });
                log.info( wb.toString() );
                viewer.setInput( wb.flurstuecke );
                //viewer.reveal( new CompositesFeatureContentProvider.FeatureTableElement( newElm ) );
                viewer.selectElement( String.valueOf( newElm.hashCode() ), true, true );
                //statusAdapter.updateStatusOf( this, new Status( IStatus.OK, WbvPlugin.ID, "Alle Eingaben sind korrekt." ) );
            }
        });

        // removeBtn
        final Button removeBtn = tk().createButton( parent, "-", SWT.PUSH );
        removeBtn.setToolTipText( "Das markierte Flurstück löschen" );
        removeBtn.setLayoutData( FormDataFactory.defaults().left( 100, -30 ).right( 100 ).top( addBtn ).create() );
        removeBtn.setEnabled( false );
        viewer.addSelectionChangedListener( new ISelectionChangedListener() {
            @Override
            public void selectionChanged( SelectionChangedEvent ev ) {
                // FIXME selection is disabled for viewer 
               // removeBtn.setEnabled( !viewer.getSelected().isEmpty() );
            }
        });
        removeBtn.addSelectionListener( new SelectionAdapter() {
            @Override
            public void widgetSelected( SelectionEvent ev ) {
                Flurstueck selectedFs = Iterables.getOnlyElement( viewer.getSelected(), null );
                boolean success = wb.flurstuecke.remove( selectedFs );
                if (!success) {
                    StatusDispatcher.handleError( "Eintrag konnte nicht gelöscht werden.", null );
                }
                //viewer.setInput( wb.flurstuecke );
                viewer.refresh( true );
                statusAdapter.updateStatusOf( this, new Status( IStatus.OK, WbvPlugin.ID, "Alle Eingaben sind korrekt." ) );
            }
        });
    }
    
    
    protected Section createKontaktSection( final Composite parent, final Kontakt kontakt ) {
        final Section section = tk().createSection( parent, kontakt.anzeigename(), TREE_NODE | Section.SHORT_TITLE_BAR | Section.FOCUS_TITLE );
        //section.setFont( JFaceResources.getFontRegistry().getBold( JFaceResources.DEFAULT_FONT ) );
        ((Composite)section.getClient()).setLayout( FormLayoutFactory.defaults().spacing( 3 ).create() );
        section.setExpanded( false );

        // KontaktForm
        KontaktForm form = new KontaktForm( kontakt, getSite() );
        BatikFormContainer formContainer = new BatikFormContainer( form );
        formContainer.createContents( (Composite)section.getClient() );
        formContainer.getContents().setLayoutData( FormDataFactory.filled().right( 100, -33 ).create() );

        
        // FIXME this listener gets soon GCed
        // after porting KontaktForm to page the hackish hard reference is no longer there
        formContainer.addFieldListener( new IFormFieldListener() {
            public void fieldChange( FormFieldEvent ev ) {
                if (ev.getEventCode() == VALUE_CHANGE 
                        && ev.getFieldName().equals( kontakt.name.info().getName() )
                        && !section.isDisposed()) {
                    section.setText( (String)ev.getNewFieldValue() );
                    section.layout();
                }
            }
        });
        
        EnableSubmitFormFieldListener listener = new EnableSubmitFormFieldListener( formContainer );
        formContainer.addFieldListener( listener );
        kForms.put( formContainer, listener );
        
        // addBtn
        Button addBtn = tk().createButton( (Composite)section.getClient(), "+", SWT.PUSH );
        addBtn.setToolTipText( "Einen neuen Kontakt hinzufügen" );
        addBtn.setLayoutData( FormDataFactory.defaults().left( 100, -30 ).right( 100 ).top( 0 ).create() );
        addBtn.addSelectionListener( new SelectionAdapter() {
            @Override
            public void widgetSelected( SelectionEvent ev ) {
                Kontakt neu = wb.kontakte.createElement( new ValueInitializer<Kontakt>() {
                    @Override
                    public Kontakt initialize( Kontakt proto ) throws Exception {
                        proto.name.set( "Neu" );
                        return proto;
                    }
                });
                Section newSection = createKontaktSection( parent, neu );
                getSite().layout( true );
                parent.layout( new Control[] {newSection}, SWT.ALL|SWT.CHANGED );

                statusAdapter.updateStatusOf( parent, new Status( IStatus.OK, WbvPlugin.ID, "Ein Kontakt hinzugefügt" ) );
            }
        });

        // removeBtn
        Button removeBtn = null;
        if (kontakt != wb.besitzer()) {
            removeBtn = tk().createButton( (Composite)section.getClient(), "-", SWT.PUSH );
            removeBtn.setToolTipText( "Diesen Kontakt löschen" );
            removeBtn.setLayoutData( FormDataFactory.defaults().left( 100, -30 ).right( 100 ).top( addBtn ).create() );
            removeBtn.addSelectionListener( new SelectionAdapter() {
                @Override
                public void widgetSelected( SelectionEvent ev ) {
                    wb.kontakte.remove( kontakt );
                    section.dispose();
                    kForms.remove( form );
                    getSite().layout( true );

                    statusAdapter.updateStatusOf( parent, new Status( IStatus.OK, WbvPlugin.ID, "Ein Kontakt gelöscht" ) );
                }
            });
        }
        return section;
    }


    protected Section createEreignisSection( final Composite parent, final Ereignis ereignis ) {
        String titel = WbvPlugin.df.format( ereignis.geaendert.get() ) +  " -- " + ereignis.titel.get();
        final Section section = tk().createSection( parent, titel, TREE_NODE | Section.SHORT_TITLE_BAR | Section.FOCUS_TITLE );
        section.setToolTipText( ereignis.geaendertVon.get() );
        //((Composite)section.getClient()).setLayout( new FillLayout() );

        // form
        final EreignisForm form = new EreignisForm( ereignis, getSite() );
        BatikFormContainer formContainer = new BatikFormContainer( form );
        formContainer.createContents( (Composite)section.getClient() );
//        form.createContents( tk.createComposite( (Composite)section.getClient() ) );

        IFormFieldListener l = new IFormFieldListener() {
            public void fieldChange( FormFieldEvent ev ) {
                if (ev.getEventCode() == VALUE_CHANGE) {
                    if (ev.getFieldName().equals( ereignis.titel.info().getName() )
                            && !section.isDisposed()) {
                        section.setText( (String)ev.getNewFieldValue() );
                        section.layout();
                    }
                    
                    ereignis.geaendert.set( new Date() );
                    ereignis.geaendertVon.set( SecurityContext.instance().getUser().getName() );
                }
            }
        };
        formContainer.addFieldListener( l );
        eFormListeners.add( l );
        
        EnableSubmitFormFieldListener listener = new EnableSubmitFormFieldListener( formContainer );
        formContainer.addFieldListener( listener );
        eForms.put( formContainer, listener );

        return section;
    }

    
    /**
     * 
     */
    class WaldbesitzerForm
            extends DefaultFormPage {

        @Override
        public void createFormContents( IFormPageSite site ) {
            Composite body = site.getPageBody();
            body.setLayout( ColumnLayoutFactory.defaults().spacing( 3 ).margins( 0, 0 ).columns( 1, 1 ).create() );

            assert wb != null;

            site.newFormField( new PropertyAdapter( wb.eigentumsArt ) )
                    .label.put( "Eigentumsart" )
                    .field.put( new PicklistFormField( Waldeigentumsart.map() ) )
                    .create();

            site.newFormField( new PropertyAdapter( wb.pächter ) ).create();

            site.newFormField( new PropertyAdapter( wb.bemerkung ) )
                    .field.put( new TextFormField() )
                    .create().setLayoutData( new ColumnLayoutData( SWT.DEFAULT, 80 ) );
        }
    }


    /**
     * 
     */
    class EnableSubmitFormFieldListener
            implements IFormFieldListener {

        private BatikFormContainer       form;

        public EnableSubmitFormFieldListener( BatikFormContainer form ) {
            this.form = form;
        }

        @Override
        public void fieldChange( FormFieldEvent ev ) {
            if (ev.getEventCode() == IFormFieldListener.VALUE_CHANGE && wb != null) {
                //submitAction.setEnabled( form.isDirty() && form.isValid() );

                IStatus status = null;
                if (!form.isDirty()) {
                    status = Status.OK_STATUS;
                }
                else if (!form.isValid()) {
                    status = new Status( IStatus.ERROR, WbvPlugin.ID, "Eingaben sind nicht vollständig/korrekt." );
                }
                else {
                    status = new Status( IStatus.OK, WbvPlugin.ID, "Alle Eingaben sind in Ordnung" );
                }
                statusAdapter.updateStatusOf( EnableSubmitFormFieldListener.this, status );
                //getSite().setStatus( status );
            }
        }
    }
}
