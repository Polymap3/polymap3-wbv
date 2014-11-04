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

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.widgets.Composite;

import org.polymap.core.ui.ColumnLayoutFactory;

import org.polymap.rhei.batik.IPanelSite;
import org.polymap.rhei.batik.app.FormContainer;
import org.polymap.rhei.field.EMailAddressValidator;
import org.polymap.rhei.field.IFormFieldLabel;
import org.polymap.rhei.field.IFormFieldValidator;
import org.polymap.rhei.field.NotEmptyValidator;
import org.polymap.rhei.field.PicklistFormField;
import org.polymap.rhei.form.IFormEditorPageSite;
import org.polymap.wbv.model.Kontakt;

/**
 * 
 *
 * @author <a href="http://www.polymap.de">Falko Bräutigam</a>
 */
public class KontaktForm
        extends FormContainer {

    private static Log log = LogFactory.getLog( KontaktForm.class );
    
    private Kontakt                 kontakt;
    
    private IPanelSite              panelSite;
    
    private Composite               body;

    
    public KontaktForm( Kontakt kontakt, IPanelSite panelSite ) {
        this.kontakt = kontakt;
        this.panelSite = panelSite;
    }


    @Override
    public void createFormContent( final IFormEditorPageSite formSite ) {
        body = formSite.getPageBody();
        if (body.getLayout() == null) {
            body.setLayout( ColumnLayoutFactory.defaults().spacing( 3 ).margins( 10, 10 ).create() );
        }

        // fields
        Composite salu = panelSite.toolkit().createComposite( body );
        salu.setLayout( new FillLayout( SWT.HORIZONTAL) );

        new FormFieldBuilder( salu, new PropertyAdapter( kontakt.anrede ) )
                .setLabel( "Anrede" )
                .setField( new PicklistFormField( new String[] {"Herr", "Frau", "Firma", "Amt"} )
                            .setTextEditable( true )
                            .setForceTextMatch( false ) )
                //.setValidator( new NotEmptyValidator() )
                .create().setFocus();

        new FormFieldBuilder( salu, new PropertyAdapter( kontakt.vorname ) ).setLabel( IFormFieldLabel.NO_LABEL ).create();
        
        new FormFieldBuilder( body, new PropertyAdapter( kontakt.name ) )
                .setValidator( new NotEmptyValidator() ).create().setFocus();

//        if (kontakt instanceof User) {
//            prop = ((User)kontakt).company();
//            new FormFieldBuilder( body, new PropertyAdapter( prop ) ).setLabel( i18n.get( prop.name() ) ).create();            
//        }
        
        new FormFieldBuilder( body, new PropertyAdapter( kontakt.email ) ).setLabel( "E-Mail" )
                .setToolTipText( "Die E-Mail-Adresse des kontaktes. Beispiel: info@example.com" )
                .setValidator( new EMailAddressValidator() )
                .create();

        new FormFieldBuilder( body, new PropertyAdapter( kontakt.telefon1 ) ).setLabel( "Telefon1" ).create();
        
        new FormFieldBuilder( body, new PropertyAdapter( kontakt.telefon2 ) ).setLabel( "Telefon2" ).create();
        
        new FormFieldBuilder( body, new PropertyAdapter( kontakt.fax ) ).setLabel( "Fax" ).create();
        
        // Adresse
        new AdresseForm( kontakt, panelSite ).createContents( this );
    }

    
    protected IFormFieldValidator validator( String propName ) {
        return new NotEmptyValidator();
    }

}
