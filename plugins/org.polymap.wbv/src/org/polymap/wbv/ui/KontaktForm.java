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

import java.util.HashSet;
import java.util.Set;

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
import org.polymap.rhei.field.IFormFieldListener;
import org.polymap.rhei.field.NotEmptyValidator;
import org.polymap.rhei.field.PicklistFormField;
import org.polymap.rhei.field.TextFormField;
import org.polymap.rhei.form.DefaultFormPage;
import org.polymap.rhei.form.IFormPageSite;
import org.polymap.rhei.form.batik.BatikFormContainer;

import org.polymap.wbv.model.Kontakt;

/**
 * 
 *
 * @author <a href="http://www.polymap.de">Falko Bräutigam</a>
 */
public class KontaktForm
        extends DefaultFormPage {

    private static Log log = LogFactory.getLog( KontaktForm.class );
    
    private Kontakt                 kontakt;
    
    private IPanelSite              panelSite;
    
    private Composite               body;

    private Set<IFormFieldListener> listeners = new HashSet();

    
    public KontaktForm( Kontakt kontakt, IPanelSite panelSite ) {
        this.kontakt = kontakt;
        this.panelSite = panelSite;
    }


//    @Override
//    public void addFieldListener( IFormFieldListener l ) {
//        super.addFieldListener( l );
//        listeners.add( l );
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
                .label.put( "Anrede / Name*" )
                .field.put( new PicklistFormField( new String[] {"Herr", "Frau", "Firma", "Amt"} )
                            .setTextEditable( true )
                            .setForceTextMatch( false ) )
                //.setValidator( new NotEmptyValidator() )
                .create();

        formSite.newFormField( new PropertyAdapter( kontakt.name ) )
                .parent.put( salu )
                .label.put( IFormFieldLabel.NO_LABEL )
                .tooltip.put( "Name des Waldbesitzers, inklusive Titel" )
                .create();
        
        formSite.newFormField( new PropertyAdapter( kontakt.vorname ) )
                .validator.put( new NotEmptyValidator() )
                .create()
                .setFocus();

//        if (kontakt instanceof User) {
//            prop = ((User)kontakt).company();
//            new FormFieldBuilder( body, new PropertyAdapter( prop ) ).setLabel( i18n.get( prop.name() ) ).create();            
//        }
        
        formSite.newFormField( new PropertyAdapter( kontakt.email ) )
                .label.put( "E-Mail" )
                .tooltip.put( "Die E-Mail-Adresse des kontaktes. Beispiel: info@example.com" )
                .validator.put( new EMailAddressValidator().msg.put( "Das ist keine gültige E-Mail-Adresse" ) )
                .create();

        formSite.newFormField( new PropertyAdapter( kontakt.telefon1 ) ).label.put( "Telefon1" ).create();
        
        formSite.newFormField( new PropertyAdapter( kontakt.telefon2 ) ).label.put( "Telefon2" ).create();
        
        formSite.newFormField( new PropertyAdapter( kontakt.fax ) ).label.put( "Fax" ).create();
        
        // Adresse
        new BatikFormContainer( new AdresseForm( kontakt, panelSite ) ).createContents( body );
        
        // Bemerkung 
        formSite.newFormField( new PropertyAdapter( kontakt.bemerkung ) )
                .field.put( new TextFormField() )
                .tooltip.put( "Zusätzliche Informationen zu diesem Kontakt.\nZum Besipiel: 'Besitzer', 'Ansprechpartner', 'Verwalter', 'Erbengemeinschaft'" )
                .create().setLayoutData( new ColumnLayoutData( SWT.DEFAULT, 80 ) );
    }
    
}
