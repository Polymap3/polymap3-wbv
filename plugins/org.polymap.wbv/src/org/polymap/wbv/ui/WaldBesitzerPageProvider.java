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
        if (feature.getType().getName().getLocalPart().equalsIgnoreCase( "waldbesitzer" )) {
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

        private WbvRepository           repo = WbvRepository.instance();
        

        protected BaseFormEditorPage( Feature feature, FeatureStore fs ) {
            super( "Basisdaten", "Basisdaten", feature, fs );
            this.entity = repo.entityForState( WaldBesitzer.class, feature );
        }


        @Override
        public void createFormContent( IFormEditorPageSite _site ) {
            log.debug( "createFormContent(): feature= " + feature );

            super.createFormContent( _site );
            //_site.setEditorTitle( "Waldbesitzer: " + entity.name.get() );
            
            //WaldBesitzer template = repo.infoOf( WaldBesitzer.class ).getTemplate();

            //IFormEditorToolkit tk = _site.getToolkit();
            //Section section = newSection( "Basisdaten", false, null );

            // name
            newFormField( entity.name.getInfo().getName() ).setLabel( "Name" )
                    .setParent( _site.getPageBody() )
                    .setField( new StringFormField() )
                    .setValidator( new NullValidator() )
                    .create();
            //
            newFormField( entity.vorname.getInfo().getName() )
                    .setParent( _site.getPageBody() )
                    .create();
        }
        
    }
    
}
