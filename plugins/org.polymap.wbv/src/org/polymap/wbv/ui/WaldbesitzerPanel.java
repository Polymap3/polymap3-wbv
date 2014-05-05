/*
 * polymap.org Copyright (C) 2014, Polymap GmbH. All rights reserved.
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
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.opengis.feature.Feature;
import org.polymap.core.model2.Entity;
import org.polymap.core.model2.runtime.EntityRuntimeContext.EntityStatus;
import org.polymap.core.ui.ColumnLayoutFactory;
import org.polymap.rhei.batik.ContextProperty;
import org.polymap.rhei.batik.DefaultPanel;
import org.polymap.rhei.batik.IAppContext;
import org.polymap.rhei.batik.IPanel;
import org.polymap.rhei.batik.IPanelSite;
import org.polymap.rhei.batik.PanelIdentifier;
import org.polymap.rhei.batik.app.BatikApplication;
import org.polymap.rhei.batik.app.FormContainer;
import org.polymap.rhei.batik.toolkit.IPanelSection;
import org.polymap.rhei.batik.toolkit.IPanelToolkit;
import org.polymap.rhei.batik.toolkit.PriorityConstraint;
import org.polymap.rhei.field.FormFieldEvent;
import org.polymap.rhei.field.IFormFieldListener;
import org.polymap.rhei.field.NotEmptyValidator;
import org.polymap.rhei.field.StringFormField;
import org.polymap.rhei.form.IFormEditorPageSite;
import org.polymap.wbv.model.WaldBesitzer;
import org.polymap.wbv.model.WbvRepository;

/**
 * 
 * 
 * @author <a href="http://www.polymap.de">Falko Bräutigam</a>
 */
@SuppressWarnings("serial")
public class WaldbesitzerPanel
        extends DefaultPanel
        implements IPanel {

    private static Log                     log = LogFactory.getLog( WaldbesitzerPanel.class );

    public static final PanelIdentifier    ID  = new PanelIdentifier( "wbv", "waldbesitzer" );

    private ContextProperty<WaldBesitzer>  entityCtxProperty;

    private ContextProperty<WbvRepository> repoCtxProperty;

    private WbvRepository                  repo;

    private WaldBesitzer                   entity;

    private IPanelToolkit                  toolKit;

    private WaldbesitzerForm               wbForm;

    private IFormFieldListener             wbFormListener;


    @Override
    public boolean init( IPanelSite site, IAppContext context ) {
        super.init( site, context );
        if (entityCtxProperty.get() != null) {
            entity = entityCtxProperty.get();
            log.info( "Waldbesitzer: " + entityCtxProperty.get() );
        }
        repo = repoCtxProperty.get();

        // nur Anzeigen wenn direkt aufgerufen
        return false;
    }


    @Override
    public void dispose() {
        // wenn vorher commit, dann schadet das nicht; ansonsten neue Entity
        // verwerfen
        repo.rollback();
        wbForm.removeFieldListener( wbFormListener );
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
        getSite().toolkit().createLabel( karte.getBody(),
                "[Karte]\n.\n.\n.\n.\n.\n.\n.\n.\n.\n.\n.\n.\n.\n.\n.\n.\n.\n.\n.\n.\n.\n.\n.\n." );

        IPanelSection action = toolKit.createPanelSection( parent, null );
        action.addConstraint( new PriorityConstraint( 10 ) );
        createActions( action );
        addDeleteAction( action );
    }


    protected void createActions( IPanelSection section ) {
        final Button submitBtn = toolKit.createButton( section.getBody(), "Fertig", SWT.PUSH );
        submitBtn.setEnabled( false );
        submitBtn.addSelectionListener( new SelectionAdapter() {

            @Override
            public void widgetSelected( SelectionEvent ev ) {
                try {
                    wbForm.submit();
                    repo.commit();
                    getContext().closePanel( getSite().getPath() );
                }
                catch (Exception e) {
                    BatikApplication.handleError(
                            "Änderungen konnten nicht korrekt gespeichert werden.", e );
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


    protected void addDeleteAction( IPanelSection section ) {
        if (entityMayBeDeleted( entity )) {
            final String formatString = (entity.vorname.get() != null && entity.name.get() != null)
                    ? "'%s %s' Löschen"
                    : "Löschen";
            final String btnText = String.format( formatString, entity.vorname.get(),
                    entity.name.get() );
            final Button submitBtn = toolKit.createButton( section.getBody(), btnText, SWT.PUSH );

            submitBtn.addSelectionListener( new SelectionAdapter() {

                @Override
                public void widgetSelected( SelectionEvent eb ) {
                    try {
                        repo.removeEntity( entity );
                        repo.commit();
                        getContext().closePanel( getSite().getPath() );
                    }
                    catch (Exception e) {
                        BatikApplication.handleError(
                                "Das Löschen des Waldbesitzers ist fehlgeschlagen.", e );
                    }
                };
            } );
        }
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
            site.getPageBody().setLayout(
                    ColumnLayoutFactory.defaults().spacing( 5 ).margins( 10, 10 ).columns( 1, 1 )
                            .create() );
            Feature feature = (Feature)entity.state();

            // einfach, mit defaults
            createField( feature.getProperty( entity.vorname.getInfo().getName() ) ).create();

            // name
            createField( feature.getProperty( entity.name.getInfo().getName() ) )
                    .setLabel( "Nachname" ).setField( new StringFormField() )
                    .setValidator( new NotEmptyValidator() ).create();
        }

    }

}
