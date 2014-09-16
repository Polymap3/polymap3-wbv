/*
 * Copyright (C) 2014 Polymap GmbH. All rights reserved.
 * 
 * This is free software; you can redistribute it and/or modify it under the terms of
 * the GNU Lesser General Public License as published by the Free Software
 * Foundation; either version 3.0 of the License, or (at your option) any later
 * version.
 * 
 * This software is distributed in the hope that it will be useful, but WITHOUT ANY
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A
 * PARTICULAR PURPOSE. See the GNU Lesser General Public License for more details.
 */
package org.polymap.wbv.model;

import static org.polymap.core.model2.query.Expressions.is;
import static org.polymap.core.model2.query.Expressions.template;

import org.polymap.core.model2.CollectionProperty;
import org.polymap.core.model2.Entity;
import org.polymap.core.model2.MinOccurs;
import org.polymap.core.model2.Property;
import org.polymap.core.model2.query.Query;
import org.polymap.core.model2.store.feature.SRS;

/**
 * 
 * 
 * @author <a href="http://www.polymap.de">Falko Bräutigam</a>
 */
@SRS("EPSG:4326")
public class Waldbesitzer
        extends Entity {
    
    public enum Klasse {
        /** Kommunen, Kirchen, Vereine */
        Körperschaft,
        /** Privates Eigentum (Einzelpersonen, Unternehmen) */
        Privat,
        /** Staatswald (Land Sachsen, Bund) */
        Staatswald
    }
    
    public Property<Klasse>             klasse;
    
    /**
     * Alle Ansprechpartner, inklusive des {@link #besitzer()}s auf Index 0.
     */
    @MinOccurs(1)
    public CollectionProperty<Kontakt>  ansprechpartner;
    
    
    public Kontakt besitzer() {
        return ansprechpartner.iterator().next();
    }
    
    
    /**
     * Andere Seite der {@link Waldstueck#waldbesitzer} Assoziation.  
     */
    public Query<Waldstueck> waldstuecke() {
        Waldstueck wanted = template( Waldstueck.class, context.getRepository() );
        return context.getUnitOfWork()
                .query( Waldstueck.class )
                .where( is( wanted.waldbesitzer, this ) );    
    }
    
}
