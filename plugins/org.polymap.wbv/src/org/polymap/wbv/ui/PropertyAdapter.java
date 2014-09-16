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

import java.util.Map;

import org.geotools.feature.NameImpl;
import org.geotools.feature.type.AttributeTypeImpl;
import org.opengis.feature.Property;
import org.opengis.feature.type.Name;
import org.opengis.feature.type.PropertyDescriptor;
import org.opengis.feature.type.PropertyType;

/**
 * 
 *
 * @author <a href="http://www.polymap.de">Falko Bräutigam</a>
 */
class PropertyAdapter
        implements Property {

    private org.polymap.core.model2.Property    delegate;
    

    public PropertyAdapter( org.polymap.core.model2.Property delegate ) {
        assert delegate != null;
        this.delegate = delegate;
    }

    @Override
    public Object getValue() {
        return delegate.get();
    }

    @Override
    public void setValue( Object newValue ) {
        if (newValue != null 
                && !delegate.getInfo().getType().isAssignableFrom( newValue.getClass() )) {
            throw new ClassCastException( "Wrong value for Property of type '" + delegate.getInfo().getType() + "': " + newValue.getClass() );
        }
        delegate.set( newValue );
    }

    @Override
    public PropertyType getType() {
        return new AttributeTypeImpl( getName(), delegate.getInfo().getType(), false, false, null, null, null );
    }

    @Override
    public PropertyDescriptor getDescriptor() {
        // XXX Auto-generated method stub
        throw new RuntimeException( "not yet implemented." );
    }

    @Override
    public Name getName() {
        return new NameImpl( delegate.getInfo().getName() );
    }

    @Override
    public boolean isNillable() {
        return delegate.getInfo().isNullable();
    }

    @Override
    public Map<Object, Object> getUserData() {
        throw new RuntimeException( "not yet implemented." );
    }
    
}
