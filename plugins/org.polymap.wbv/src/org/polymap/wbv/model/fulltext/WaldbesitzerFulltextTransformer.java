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
package org.polymap.wbv.model.fulltext;

import java.util.Locale;
import java.util.regex.Pattern;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.polymap.model2.Association;
import org.polymap.model2.CollectionProperty;
import org.polymap.model2.Composite;
import org.polymap.model2.Property;
import org.polymap.rhei.fulltext.model2.EntityFeatureTransformer;

import org.polymap.wbv.model.Flurstueck;
import org.polymap.wbv.model.Gemarkung;
import org.polymap.wbv.model.Kontakt;
import org.polymap.wbv.model.Waldbesitzer.Waldeigentumsart;

/**
 * 
 *
 * @author <a href="http://www.polymap.de">Falko Bräutigam</a>
 */
public class WaldbesitzerFulltextTransformer
        extends EntityFeatureTransformer {

    private static Log log = LogFactory.getLog( WaldbesitzerFulltextTransformer.class );

    public static final Pattern         whitespace = Pattern.compile( "\\s" );
    
    
    public WaldbesitzerFulltextTransformer() {
        super();
        locale.set( Locale.GERMAN );
        honorQueryableAnnotation.set( true );
    }


    @Override
    protected void visitProperty( Property prop ) {        
        Object value = prop.get();

        // Waldeigentumsart
        if (value instanceof Waldeigentumsart) {
            putValue( prop, ((Waldeigentumsart)value).label() );
        }
        else {
            super.visitProperty( prop );
        }
    }
    
    
    @Override
    protected void visitAssociation( Association prop ) {
        Object value = prop.get();
        if (value instanceof Gemarkung) {
            processComposite( (Composite)value );
        }
    }


    @Override
    protected boolean visitCompositeCollectionProperty( CollectionProperty prop ) {
        // Flurstueck
        if (Flurstueck.class.isAssignableFrom( prop.info().getType() )) {
            for (Object fst : prop) {
                visitFlurstueck( (Flurstueck)fst );
            }
            return false;
        }
        // Kontakt
        else if (Kontakt.class.isAssignableFrom( prop.info().getType() )) {
            for (Object kontakt : prop) {
                if (!((Kontakt)kontakt).geloescht.get()) {
                    processComposite( (Kontakt)kontakt );
                }
            }
            return false;
        }
        // default process
        return true;
    }
    
    
    protected void visitFlurstueck( Flurstueck fst ) {
        if (!fst.geloescht.get()) {
            String zn = fst.zaehlerNenner.get();
            if (zn != null && zn.length() > 0) {
                String normalized = whitespace.matcher( zn ).replaceAll( "" );
                if (!normalized.contains( "/" )) {
                    normalized = normalized + "/";
                }
                //log.debug( "zaehlerNenner: " + zn + " -> " + normalized );
                putValue( "zaehlerNenner", normalized );
            }
            processComposite( fst );
        }
    }
    
}
