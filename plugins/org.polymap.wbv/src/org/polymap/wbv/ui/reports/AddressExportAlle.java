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

import java.util.Iterator;

import com.google.common.collect.FluentIterable;

import org.polymap.wbv.model.Flurstueck;
import org.polymap.wbv.model.Waldbesitzer;
import org.polymap.wbv.model.WbvRepository;

/**
 * 
 * @author falko
 */
public class AddressExportAlle
        extends AddressExport {

    @Override
    public String getName() {
        return "CSV Adressen-Export (alle)";
    }

    
    @Override
    protected Iterator<Flurstueck> flurstuecke() {
        return FluentIterable.from( WbvRepository.unitOfWork().query( Waldbesitzer.class ).execute() )
                .transformAndConcat( wb -> wb.flurstuecke( revier.get() ) )
                .iterator();
    }

}
