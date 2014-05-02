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

import java.io.IOException;

import org.geotools.data.FeatureStore;
import org.geotools.feature.NameImpl;
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
import org.polymap.rhei.batik.app.FormContainer;
import org.polymap.rhei.batik.toolkit.IPanelSection;
import org.polymap.rhei.field.NullValidator;
import org.polymap.rhei.field.StringFormField;
import org.polymap.rhei.form.IFormEditorPageSite;

import org.polymap.wbv.model.WaldBesitzer;
import org.polymap.wbv.model.WbvRepository;
import org.polymap.wbv.ui.WaldBesitzerPageProvider.BaseFormEditorPage;

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
    }


    @Override
    public void createContents( Composite parent ) {
        getSite().setTitle( "Waldbesitzer" );
        IPanelSection section = getSite().toolkit().createPanelSection( parent, "Basisdaten" );
        
        FormContainer searchForm = new FormContainer() {
            public void createFormContent( IFormEditorPageSite site ) {
                try {
                    //
                    NameImpl typeName = new NameImpl( repo.get().infoOf( WaldBesitzer.class ).getNameInStore() );
                    FeatureStore fs = (FeatureStore)repo.get().ds().getFeatureSource( typeName );
                    Feature feature = (Feature)entity.get().state();
                    
                    //
                    BaseFormEditorPage delegate = new WaldBesitzerPageProvider.BaseFormEditorPage( feature, fs );
                    delegate.createFormContent( site );
                    
                    //
                    Button okBtn = site.getToolkit().createButton( site.getPageBody(), "Anlegen", SWT.PUSH );
                    okBtn.addSelectionListener( new SelectionAdapter() {
                        @Override
                        public void widgetSelected( SelectionEvent ev ) {
                            try {
                                submitEditor();
                                repo.get().commit();
                            }
                            catch (Exception e) {
                                throw new RuntimeException( e );
                            }
                        }
                    });
                }
                catch (IOException e) {
                    throw new RuntimeException( e );
                }
            }
        };
        searchForm.createContents( section );
        
        //
        new WaldbesitzerForm( entity.get() ).createContents( section );
    }


    @Override
    public PanelIdentifier id() {
        return ID;
    }
    
    
    /**
     * 
     */
    public static class WaldbesitzerForm
            extends FormContainer {

        private WaldBesitzer            entity;
        
        
        public WaldbesitzerForm( WaldBesitzer entity ) {
            this.entity = entity;
        }


        @Override
        public void createFormContent( IFormEditorPageSite site ) {
            site.getPageBody().setLayout( ColumnLayoutFactory.defaults().spacing( 5 ).margins( 10, 10 ).columns( 1, 1 ).create() );
            Feature feature = (Feature)entity.state();
            
            // name
            createField( feature.getProperty( entity.name.getInfo().getName() ) )
                    .setLabel( "Name2" )
                    .setField( new StringFormField() )
                    .setValidator( new NullValidator() )
                    .create();
            
            // einfach, mit defaults
            createField( feature.getProperty( entity.vorname.getInfo().getName() ) ).create();
        }
        
    }
    
}
