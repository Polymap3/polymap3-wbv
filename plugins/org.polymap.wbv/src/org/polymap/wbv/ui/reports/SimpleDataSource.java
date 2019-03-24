/* 
 * polymap.org
 * Copyright (C) 2017-2018, the @authors. All rights reserved.
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
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.tuple.Pair;

import net.sf.jasperreports.engine.JRDataSource;
import net.sf.jasperreports.engine.JRException;
import net.sf.jasperreports.engine.JRField;

/**
 * In-memory data source used by the reports.
 *
 * @author Falko Bräutigam
 */
public class SimpleDataSource
        implements JRDataSource {

    private Map<Pair<String,Integer>,Object> values = new HashMap();
    
    private List<String>        columnNames;
    
    private int                 iteratorRowIndex = -1;
    
    private int                 maxRowIndex;
    
    
    public SimpleDataSource( String... columnNames ) {
        this.columnNames = Arrays.asList( columnNames );
//        datasource.setNumberFormat( NumberFormat.getInstance( Locale.US ) );
    }

    
    public SimpleDataSource put( Enum columnName, int rowIndex, Object value ) {
        return put( columnName.name(), rowIndex, value );
    }

    
    public SimpleDataSource put( String columnName, int rowIndex, Object value ) {
        assert columnNames.contains( columnName );
        Pair<String,Integer> key = Pair.of( columnName, rowIndex );
        Object previous = values.put( key, value );
        assert previous == null;
        maxRowIndex = Math.max( maxRowIndex, rowIndex );
        return this;
    }
    
    
    @Override
    public boolean next() throws JRException {
        iteratorRowIndex ++;
        return iteratorRowIndex <= maxRowIndex;
    }

    
    @Override
    public Object getFieldValue( JRField field ) throws JRException {
        assert columnNames.contains( field.getName() );
        assert iteratorRowIndex <= maxRowIndex;
        
        Pair<String,Integer> key = Pair.of( field.getName(), iteratorRowIndex );
        return values.get( key );
    }

}
