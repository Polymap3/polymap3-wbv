/* 
 * polymap.org
 * Copyright (C) 2015, Falko Bräutigam. All rights reserved.
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

import static net.sf.dynamicreports.report.builder.DynamicReports.*;

import java.io.IOException;
import net.sf.dynamicreports.jasper.builder.JasperReportBuilder;
import net.sf.dynamicreports.report.exception.DRException;
import net.sf.jasperreports.engine.JRException;
import net.sf.jasperreports.engine.data.JsonDataSource;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * Waldflächen aller Waldbesitzer.
 *
 * @author <a href="http://www.polymap.de">Falko Bräutigam</a>
 */
public class Report101
        extends WbvReport {

    private static Log log = LogFactory.getLog( Report101.class );
    
    
    @Override
    public String getName() {
        return "WBV 1.01";
    }


    @Override
    public JasperReportBuilder build() throws DRException, JRException, IOException {
        return report()
            .setDataSource( new JsonDataSource( new JsonBuilderJob( entities ).run() ) )
        
            .title( cmp.text( "Waldflächen aller Waldbesitzer" ) )
            .columns( col.column( "Eigentumsart", "eigentumsArt", type.stringType() ) );
    }
    
}
