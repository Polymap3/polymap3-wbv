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

import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.widgets.Composite;

import org.eclipse.ui.forms.widgets.ColumnLayoutData;

import org.polymap.core.ui.ColumnLayoutFactory;

import org.polymap.rhei.batik.IPanelSite;
import org.polymap.rhei.field.EMailAddressValidator;
import org.polymap.rhei.field.IFormFieldLabel;
import org.polymap.rhei.field.NotEmptyValidator;
import org.polymap.rhei.field.PicklistFormField;
import org.polymap.rhei.field.TextFormField;
import org.polymap.rhei.form.DefaultFormPage;
import org.polymap.rhei.form.IFormPageSite;
import org.polymap.wbv.model.Kontakt;

/**
 * 
 *
 * @author <a href="http://www.polymap.de">Falko Bräutigam</a>
 */
public class KontaktForm
        extends DefaultFormPage {
        //implements IFormPage2 {

    private static Log log = LogFactory.getLog( KontaktForm.class );
    
    private Kontakt                 kontakt;
    
    private IPanelSite              panelSite;
    
    private Composite               body;

//    private BatikFormContainer      adresseForm;

    
    public KontaktForm( Kontakt kontakt, IPanelSite panelSite ) {
        this.kontakt = kontakt;
        this.panelSite = panelSite;
    }

//    public void addAdresseFieldListener( IFormFieldListener l ) {
//        adresseForm.addFieldListener( l );
//    }
//
//    @Override
//    public boolean isDirty() {
//        return adresseForm.isDirty();
//    }
//
//    @Override
//    public boolean isValid() {
//        return adresseForm.isValid();
//    }
//
//    @Override
//    public void doLoad( IProgressMonitor monitor ) throws Exception {
//        //throw new RuntimeException( "not yet implemented." );
//    }
//
//    @Override
//    public void doSubmit( IProgressMonitor monitor ) throws Exception {
//        adresseForm.submit( monitor );
//    }
//
//    @Override
//    public void dispose() {
//    }


    @Override
    public void createFormContents( final IFormPageSite formSite ) {
        super.createFormContents( formSite );
        body = formSite.getPageBody();
//        if (body.getLayout() == null) {
            body.setLayout( ColumnLayoutFactory.defaults().spacing( 3 ).margins( 0, 0 ).create() );
//        }

        // fields
        Composite salu = panelSite.toolkit().createComposite( body );
        salu.setLayout( new FillLayout( SWT.HORIZONTAL) );

        formSite.newFormField( new PropertyAdapter( kontakt.anrede ) )
                .parent.put( salu )
                .label.put( "Anrede / Name" )
                .field.put( new PicklistFormField( new String[] {"Herr", "Frau", "Firma", "Amt"} )
                            .setTextEditable( true )
                            .setForceTextMatch( false ) )
                .create();

        formSite.newFormField( new PropertyAdapter( kontakt.name ) )
                .parent.put( salu )
                .label.put( IFormFieldLabel.NO_LABEL )
                .tooltip.put( "Name des Waldbesitzers, inklusive Titel" )
                .validator.put( new NotEmptyValidator() )
                .create();
        
        formSite.newFormField( new PropertyAdapter( kontakt.vorname ) )
                .create()
                .setFocus();

        formSite.newFormField( new PropertyAdapter( kontakt.strasse ) )
                .label.put( "Straße" ).tooltip.put( "Straße und Hausnummer" )
                .validator.put( new NotEmptyValidator() )
                .create();
        //.setLayoutData( FormDataFactory.filled().right( 75 ).create() );

        Composite city = panelSite.toolkit().createComposite( body );
        formSite.newFormField( new PropertyAdapter( kontakt.plz ) )
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

        formSite.newFormField( new PropertyAdapter( kontakt.ort ) )
                .parent.put( city )
                .label.put( IFormFieldLabel.NO_LABEL )
                .validator.put( new NotEmptyValidator() )
                .create();

        formSite.newFormField( new PropertyAdapter( kontakt.ortsteil ) )
                .label.put( "Ortsteil" )
                .create();

        formSite.newFormField( new PropertyAdapter( kontakt.land ) )
                .label.put( "Staat" )
                .create();
        
        formSite.newFormField( new PropertyAdapter( kontakt.email ) )
                .label.put( "E-Mail" )
                .tooltip.put( "Die E-Mail-Adresse des Kontaktes. Beispiel: info@example.com" )
                .validator.put( new EMailAddressValidator().msg.put( "Das ist keine gültige E-Mail-Adresse" ) )
                .create();

        formSite.newFormField( new PropertyAdapter( kontakt.telefon1 ) ).label.put( "Telefon" ).create();
        
        formSite.newFormField( new PropertyAdapter( kontakt.telefon2 ) ).label.put( "Mobiltelefon" ).create();
        
        formSite.newFormField( new PropertyAdapter( kontakt.fax ) ).label.put( "Telefax" ).create();
        
//        // Adresse
//        adresseForm = new BatikFormContainer( new AdresseForm( kontakt, panelSite ) );
//        adresseForm.createContents( body );
        
        // Bemerkung 
        formSite.newFormField( new PropertyAdapter( kontakt.bemerkung ) )
                .field.put( new TextFormField() )
                .tooltip.put( "Zusätzliche Informationen zu diesem Kontakt.\nZum Beispiel: 'Besitzer', 'Ansprechpartner', 'Verwalter', 'Erbengemeinschaft'" )
                .create().setLayoutData( new ColumnLayoutData( SWT.DEFAULT, 80 ) );
    }
    
}
