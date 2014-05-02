/* 
 * polymap.org
 * Copyright (C) 2014, Polymap GmbH. All rights reserved.
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

import org.opengis.feature.Feature;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;

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
public class WaldbesitzerPanel
        extends DefaultPanel
        implements IPanel {

    private static Log log = LogFactory.getLog( WaldbesitzerPanel.class );

    public static final PanelIdentifier ID = new PanelIdentifier( "wbv", "waldbesitzer" );

    private ContextProperty<WaldBesitzer>   entity;
    
    private ContextProperty<WbvRepository>  repo;

    private WaldbesitzerForm                wbForm;

    private IFormFieldListener              wbFormListener;
    

    @Override
    public boolean init( IPanelSite site, IAppContext context ) {
        super.init( site, context );
        if (entity.get() != null) {
            log.info( "Waldbesitzer: " + entity.get() );
        }
        // nur Anzeigen wenn direkt aufgerufen
        return false;
    }


    @Override
    public void dispose() {
        // wenn vorher commit, dann schadet das nicht; ansonsten neue Entity verwerfen
        repo.get().rollback();
        wbForm.removeFieldListener( wbFormListener );
    }


    @Override
    public void createContents( Composite parent ) {
        getSite().setTitle( "Waldbesitzer" );

        IPanelSection basis = getSite().toolkit().createPanelSection( parent, "Basisdaten" );
        basis.addConstraint( new PriorityConstraint( 8 ) );
        (wbForm = new WaldbesitzerForm()).createContents( basis );

        IPanelSection besitzer = getSite().toolkit().createPanelSection( parent, "Besitzer" );
        besitzer.addConstraint( new PriorityConstraint( 7 ) );

        IPanelSection karte = getSite().toolkit().createPanelSection( parent, null );
        karte.addConstraint( new PriorityConstraint( 9 ) );
        getSite().toolkit().createLabel( karte.getBody(), "[Karte]\n.\n.\n.\n.\n.\n.\n.\n.\n.\n.\n.\n.\n.\n.\n.\n.\n.\n.\n.\n.\n.\n.\n.\n." );

        IPanelSection action = getSite().toolkit().createPanelSection( parent, null );
        action.addConstraint( new PriorityConstraint( 10 ) );
        createActions( action );
    }


    protected void createActions( IPanelSection section ) {
        final Button submitBtn = getSite().toolkit().createButton( section.getBody(), "Fertig", SWT.PUSH );
        submitBtn.setEnabled( false );
        submitBtn.addSelectionListener( new SelectionAdapter() {
            public void widgetSelected( SelectionEvent ev ) {
                try {
                    wbForm.submit();
                    repo.get().commit();
                    getContext().closePanel( getSite().getPath() );
                }
                catch (Exception e) {
                    BatikApplication.handleError( "Änderungen konnten nicht korrekt gespeichert werden.", e );
                }
            }
        });
        wbForm.addFieldListener( wbFormListener = new IFormFieldListener() {
            public void fieldChange( FormFieldEvent ev ) {
                if (ev.getEventCode() == IFormFieldListener.VALUE_CHANGE && !submitBtn.isDisposed()) {
                    submitBtn.setEnabled( wbForm.isDirty() && wbForm.isValid() );
                }
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
            site.getPageBody().setLayout( ColumnLayoutFactory.defaults().spacing( 5 ).margins( 10, 10 ).columns( 1, 1 ).create() );
            WaldBesitzer e = entity.get();
            Feature feature = (Feature)e.state();
            
            // name
            createField( feature.getProperty( e.name.getInfo().getName() ) )
                    .setLabel( "Name2" )
                    .setField( new StringFormField() )
                    .setValidator( new NotEmptyValidator() )
                    .create();
            
            // einfach, mit defaults
            createField( feature.getProperty( e.vorname.getInfo().getName() ) ).create();
        }
        
    }
    
}
