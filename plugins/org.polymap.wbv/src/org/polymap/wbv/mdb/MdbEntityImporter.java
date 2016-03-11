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
package org.polymap.wbv.mdb;

import java.util.HashMap;
import java.util.Map;

import java.io.IOException;
import java.lang.reflect.Field;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.healthmarketscience.jackcess.Database;
import com.healthmarketscience.jackcess.Row;
import com.healthmarketscience.jackcess.Table;

import org.eclipse.core.runtime.IProgressMonitor;

import org.polymap.model2.Composite;
import org.polymap.model2.Entity;
import org.polymap.model2.Property;
import org.polymap.model2.runtime.UnitOfWork;
import org.polymap.model2.runtime.ValueInitializer;

/**
 * Importiert Spalten aus einer Tabelle in ein {@link Composite}.
 * 
 * @see ImportTabel 
 * @see ImportColumn
 * @author <a href="http://www.polymap.de">Falko Bräutigam</a>
 */
public class MdbEntityImporter<T /*extends Composite*/> {

    private static Log log = LogFactory.getLog( MdbEntityImporter.class );
    
    private UnitOfWork              uow;
    
    private Class<T>                entityClass;

    private String                  tableName;
    
    private Map<String,Field>       fieldMap = new HashMap();

    
    public MdbEntityImporter( UnitOfWork uow, Class<T> entityClass ) {
        this.uow = uow;
        this.entityClass = entityClass;
        this.tableName = entityClass.getAnnotation( ImportTable.class ).value();
        
        for (Field f : entityClass.getFields()) {
            ImportColumn a = f.getAnnotation( ImportColumn.class );
            if (a != null) {
                fieldMap.put( a.value(), f );
            }
        }
    }
    
    
    public MdbEntityImporter( String tableName, Class entityClass ) {
        this.entityClass = entityClass;
        this.tableName = tableName;
    }
    
    
    public String getTableName() {
        return tableName;
    }


    public int importTable( Database db, IProgressMonitor submon ) throws IOException {
        Table table = db.getTable( getTableName() );
        submon.beginTask( entityClass != null ? entityClass.getSimpleName() : tableName, table.getRowCount() );
        int count = 0;
        for (Row row=table.getNextRow(); row != null; row = table.getNextRow()) {
            createEntity( row, buildId( row ) );
            count ++;
            submon.worked( 1 );
        }
        submon.done();
        return count;
    }
    
    
    public String buildId( Row row ) {
        return null;
    }


    public T createEntity( final Map<String,Object> row, String id ) {
        return (T)uow.createEntity( (Class<Entity>)entityClass, id, new ValueInitializer<Entity>() {
            @Override
            public Entity initialize( Entity proto ) throws Exception {
                return (Entity)fill( (T)proto, row );
            }
        });
    }
    
    
    public T fill( T composite, final Map<String,Object> row ) {
        for (Map.Entry<String,Object> entry : row.entrySet()) {
            Field f = fieldMap.get( entry.getKey() );
            if (f != null) {
                try {
                    //log.info( "    " +  );
                    Property prop = (Property)f.get( composite );
                    if (entry.getValue() != null) {
                        prop.set( entry.getValue() );
                    }
                }
                catch (IllegalArgumentException | IllegalAccessException e) {
                    throw new RuntimeException( e );
                }
            }
        }
        return composite;
    }
    
}
