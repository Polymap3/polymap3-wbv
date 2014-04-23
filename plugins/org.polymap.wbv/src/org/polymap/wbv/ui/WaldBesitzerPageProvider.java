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

import java.util.ArrayList;
import java.util.List;

import org.geotools.data.FeatureStore;
import org.opengis.feature.Feature;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.polymap.rhei.field.NullValidator;
import org.polymap.rhei.field.StringFormField;
import org.polymap.rhei.form.DefaultFormEditorPage;
import org.polymap.rhei.form.FormEditor;
import org.polymap.rhei.form.IFormEditorPage;
import org.polymap.rhei.form.IFormEditorPageSite;
import org.polymap.rhei.form.IFormEditorToolkit;
import org.polymap.rhei.form.IFormPageProvider;

import org.polymap.wbv.model.WaldBesitzer;
import org.polymap.wbv.model.WbvRepository;

/**
 * 
 *
 * @author <a href="http://www.polymap.de">Falko Bräutigam</a>
 */
public class WaldBesitzerPageProvider
        implements IFormPageProvider {

    private static Log log = LogFactory.getLog( WaldBesitzerPageProvider.class );


    public List<IFormEditorPage> addPages( FormEditor formEditor, Feature feature ) {
        log.debug( "addPages(): feature= " + feature );
        List<IFormEditorPage> result = new ArrayList();
        if (true /*feature.getType().getName().getLocalPart().equalsIgnoreCase( "waldbesitzer" )*/) {
            result.add( new BaseFormEditorPage( feature, formEditor.getFeatureStore() ) );
        }
        return result;
    }

    
    /**
     * The standard page.
     */
    public static class BaseFormEditorPage
            extends DefaultFormEditorPage
            implements IFormEditorPage {

        private WaldBesitzer            entity;

        private IFormEditorToolkit      tk;

        private WbvRepository           repo;
        

        protected BaseFormEditorPage( Feature feature, FeatureStore fs ) {
            super( "", "Waldbesitzer", feature, fs );
//            this.repo = WbvRepository.instance();
//            this.entity = repo.entityForState( WaldBesitzer.class, feature );
        }


        @Override
        public void createFormContent( IFormEditorPageSite _site ) {
            log.debug( "createFormContent(): feature= " + feature );
            super.createFormContent( _site );
            tk = pageSite.getToolkit();
            
            // ein Attributfeld im Formular erzeugen
            newFormField( "vorname" ).setLabel( "Besitzer" )
                    .setField( new StringFormField() )
                    .setValidator( new NullValidator() )
                    .create();
            // ein Attributfeld im Formular erzeugen
            newFormField( "nummer" ).setLabel( "Nummer" )
                    .create();
        }
        
    }
    
}
