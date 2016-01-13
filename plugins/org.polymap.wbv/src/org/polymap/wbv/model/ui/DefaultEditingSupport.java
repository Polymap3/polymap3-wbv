/* 
 * polymap.org
 * Copyright (C) 2016, Falko Bräutigam. All rights reserved.
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
package org.polymap.wbv.model.ui;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.eclipse.jface.viewers.ColumnViewer;
import org.eclipse.jface.viewers.EditingSupport;

/**
 * 
 *
 * @author <a href="http://www.polymap.de">Falko Bräutigam</a>
 */
public abstract class DefaultEditingSupport
        extends EditingSupport {

    private static Log log = LogFactory.getLog( DefaultEditingSupport.class );

    public DefaultEditingSupport( ColumnViewer viewer ) {
        super( viewer );
    }

    @Override
    protected boolean canEdit( Object element ) {
        return true;
    }

    @Override
    protected Object getValue( Object element ) {
        return null;
    }

    @Override
    protected void setValue( Object element, Object value ) {
    }
    
}
