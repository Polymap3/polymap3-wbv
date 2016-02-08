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

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.eclipse.swt.widgets.Composite;

import org.polymap.core.ui.ColumnLayoutFactory;

import org.polymap.rhei.batik.IPanelSite;
import org.polymap.rhei.field.IFormFieldLabel;
import org.polymap.rhei.field.NotEmptyValidator;
import org.polymap.rhei.form.DefaultFormPage;
import org.polymap.rhei.form.IFormPageSite;
import org.polymap.wbv.model.Adresse;

/**
 * 
 *
 * @author <a href="http://www.polymap.de">Falko Bräutigam</a>
 */
public class AdresseForm
        extends DefaultFormPage {

    private static Log log = LogFactory.getLog( AdresseForm.class );
    
    private Adresse                 adresse;
    
    private IPanelSite              panelSite;
    
    private Composite               body;

    
    public AdresseForm( Adresse adresse, IPanelSite panelSite ) {
        this.adresse = adresse;
        this.panelSite = panelSite;
    }


    @Override
    public void createFormContents( final IFormPageSite formSite ) {
        body = formSite.getPageBody();
//        if (body.getLayout() == null) {
            body.setLayout( ColumnLayoutFactory.defaults().spacing( 3 ).columns( 1, 1 ).create() );
//        }

        formSite.newFormField( new PropertyAdapter( adresse.strasse ) )
                .label.put( "Straße" ).tooltip.put( "Straße und Hausnummer" )
                .validator.put( new NotEmptyValidator() )
                .create();
                //.setLayoutData( FormDataFactory.filled().right( 75 ).create() );

        Composite city = panelSite.toolkit().createComposite( body );
        formSite.newFormField( new PropertyAdapter( adresse.plz ) )
                .parent.put( city )
                .label.put( "PLZ / Ort" )
                .validator.put( new NotEmptyValidator() {
                    @Override
                    public String validate( Object fieldValue ) {
                        String result = super.validate( fieldValue );
                        if (result == null) {
                            if (((String)fieldValue).length() != 5) {
                                result = "Postleitzahl muss 5 Stellen haben";
                            }
                            else if (!StringUtils.isNumeric( (String)fieldValue )) {
                                result = "Postleitzahl darf nur Ziffern enthalten";
                            }
                        }
                        return result;
                    }
                })
                .create();

        formSite.newFormField( new PropertyAdapter( adresse.ort ) )
                .parent.put( city )
                .label.put( IFormFieldLabel.NO_LABEL )
                .validator.put( new NotEmptyValidator() )
                .create();

        formSite.newFormField( new PropertyAdapter( adresse.ortsteil ) )
                .label.put( "Ortsteil" )
                .create();

        formSite.newFormField( new PropertyAdapter( adresse.land ) )
                .label.put( "Staat" )
                .create();
    }

}
