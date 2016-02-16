/* 
 * polymap.org
 * Copyright (C) 2013-2015, Falko Bräutigam. All rights reserved.
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

import java.util.Date;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.polymap.rhei.field.IFormFieldValidator;

import org.polymap.wbv.WbvPlugin;

/**
 * 
 *
 * @author <a href="http://www.polymap.de">Falko Bräutigam</a>
 */
public class DateValidator
        implements IFormFieldValidator<String,Date> {

    private static Log log = LogFactory.getLog( DateValidator.class );


    @Override
    public String validate( String fieldValue ) {
        return null;
    }


    @Override
    public Date transform2Model( String fieldValue ) throws Exception {
        return WbvPlugin.df.parse( fieldValue );
    }


    @Override
    public String transform2Field( Date modelValue ) throws Exception {
        return modelValue != null ? WbvPlugin.df.format( modelValue ) : "-";
    }
    
}
