/* 
 * polymap.org
 * Copyright (C) 2020, the @authors. All rights reserved.
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
package org.polymap.wbv.ui.reports;

import java.util.ArrayDeque;
import java.util.Arrays;
import java.util.Deque;
import java.util.List;

import java.io.IOException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.polymap.wbv.model.Kontakt;
import org.polymap.wbv.model.Waldbesitzer;

import net.sf.dynamicreports.report.exception.DRException;
import net.sf.jasperreports.engine.JRException;

/**
 * 
 * @author falko
 */
public class AddressExport
        extends WbvReport<Deque<List>> {

    private static final Log log = LogFactory.getLog( AddressExport.class );

    @Override
    public String getName() {
        return "CSV Adressen-Export";
    }


    @Override
    public Deque<List> build() throws DRException, JRException, IOException {
        Deque<List> result = new ArrayDeque<>( 1024 );
        
        for (Waldbesitzer wb : gesuchteWaldbesitzer()) {
            Kontakt besitzer = wb.besitzer();
            result.push( Arrays.asList( 
                    besitzer.anrede.get(), 
                    besitzer.name.get(),
                    besitzer.vorname.get(), 
                    besitzer.strasse.get(),
                    besitzer.plz.get(),
                    besitzer.ort.get(),
                    besitzer.ortsteil.get(),
                    besitzer.land.get() ) );
        }
        return result;
    }
}
