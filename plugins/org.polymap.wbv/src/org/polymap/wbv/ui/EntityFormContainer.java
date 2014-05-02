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

import java.io.IOException;

import org.geotools.data.FeatureStore;
import org.opengis.feature.Feature;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.polymap.core.data.util.NameImpl;
import org.polymap.core.model2.Entity;
import org.polymap.core.ui.ColumnLayoutFactory;

import org.polymap.rhei.batik.app.FormContainer;
import org.polymap.rhei.form.IFormEditorPageSite;

import org.polymap.wbv.model.WbvRepository;

/**
 * 
 *
 * @author <a href="http://www.polymap.de">Falko Bräutigam</a>
 */
public abstract class EntityFormContainer
        extends FormContainer {

    private static Log log = LogFactory.getLog( EntityFormContainer.class );

    protected Entity                entity;
    
    protected WbvRepository         repo = WbvRepository.instance();

    private FeatureStore            fs;

    private Feature                 feature;
    

    @SuppressWarnings("hiding")
    public abstract void createFormContent( Feature feature, FeatureStore fs );
    
    
    public final void createFormContent( IFormEditorPageSite site ) {
        try {
            site.getPageBody().setLayout( ColumnLayoutFactory.defaults().spacing( 5 ).margins( 10, 10 ).columns( 1, 1 ).create() );

            NameImpl typeName = new NameImpl( repo.infoOf( entity.getClass() ).getNameInStore() );
            fs = (FeatureStore)repo.ds().getFeatureSource( typeName );
            feature = (Feature)entity.state();
            
            createFormContent( feature, fs );            
        }
        catch (IOException e) {
            throw new RuntimeException( e );
        }
    }
    
}
