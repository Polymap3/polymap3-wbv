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

import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.function.Supplier;

import java.io.IOException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.google.common.collect.FluentIterable;

import org.polymap.wbv.model.Flurstueck;
import org.polymap.wbv.model.Gemarkung;
import org.polymap.wbv.model.Kontakt;
import org.polymap.wbv.model.Waldbesitzer;

import net.sf.dynamicreports.report.exception.DRException;
import net.sf.jasperreports.engine.JRException;

/**
 * 
 * @author falko
 */
public class AddressExport
        extends WbvReport<Supplier<List<?>>> {

    private static final Log log = LogFactory.getLog( AddressExport.class );

    @Override
    public String getName() {
        return "CSV Adressen-Export";
    }


    protected Iterator<Flurstueck> flurstuecke() {
        return FluentIterable.from( gesuchteWaldbesitzer() )
                .transformAndConcat( wb -> wb.flurstuecke( revier.get() ) )
                .iterator();
    }


    @Override
    public Supplier<List<?>> build() throws DRException, JRException, IOException {
        Iterator<Flurstueck> it = flurstuecke();

        return () -> {
            if (it.hasNext()) {
                Flurstueck flurstueck = it.next();
                Waldbesitzer wb = flurstueck.waldbesitzer();
                Kontakt besitzer = wb.besitzer();
                Gemarkung gemarkung = flurstueck.gemarkung.get();
                
                return Arrays.asList(
                        gemarkung != null ? gemarkung.revier.get() : "",
                        gemarkung != null ? gemarkung.gemeinde.get() : "",
                        gemarkung != null ? gemarkung.gemarkung.get() : "",
                        flurstueck.flaeche.get(),
                        flurstueck.flaecheWald.get(),
                        
                        besitzer.anrede.get(), 
                        besitzer.name.get(),
                        besitzer.vorname.get(), 
                        besitzer.strasse.get(),
                        besitzer.plz.get(),
                        besitzer.ort.get(),
                        besitzer.ortsteil.get(),
                        besitzer.land.get(), 
                        
                        besitzer.email.get(),
                        besitzer.telefon1.get(),
                        besitzer.telefon2.get(),
                        besitzer.fax.get() );
            }
            else {
                return null;
            }
        };
    }
}
