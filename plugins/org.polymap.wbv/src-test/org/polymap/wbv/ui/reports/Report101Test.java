/*
 * polymap.org 
 * Copyright (C) 2015 individual contributors as indicated by the @authors tag. 
 * All rights reserved.
 * 
 * This is free software; you can redistribute it and/or modify it under the terms of
 * the GNU Lesser General Public License as published by the Free Software
 * Foundation; either version 3 of the License, or (at your option) any later
 * version.
 * 
 * This software is distributed in the hope that it will be useful, but WITHOUT ANY
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A
 * PARTICULAR PURPOSE. See the GNU Lesser General Public License for more details.
 */
package org.polymap.wbv.ui.reports;

import java.io.File;
import java.io.FileOutputStream;
import java.util.ArrayList;
import java.util.List;

import net.sf.dynamicreports.jasper.builder.JasperReportBuilder;

import org.junit.Test;
import org.polymap.model2.runtime.EntityRepository;
import org.polymap.model2.runtime.UnitOfWork;
import org.polymap.model2.store.recordstore.RecordStoreAdapter;
import org.polymap.recordstore.lucene.LuceneRecordStore;
import org.polymap.wbv.model.Flurstueck;
import org.polymap.wbv.model.Gemarkung;
import org.polymap.wbv.model.Kontakt;
import org.polymap.wbv.model.Waldbesitzer;


/**
 * @author Joerg Reichert <joerg@mapzone.io>
 *
 */
public class Report101Test {

    @Test
    public void test() throws Exception {
        LuceneRecordStore store = new LuceneRecordStore();
        @SuppressWarnings("unchecked")
        EntityRepository entityRepository = EntityRepository.newConfiguration().store.set( new RecordStoreAdapter(
                store ) ).entities.set( new Class[] { Waldbesitzer.class, Gemarkung.class } ).create();
        UnitOfWork unitOfWork = entityRepository.newUnitOfWork();
        Waldbesitzer wb = unitOfWork.createEntity( Waldbesitzer.class, null );
        Kontakt kontakt = wb.kontakte.createElement( null );
        kontakt.vorname.set( "Hans" );
        kontakt.name.set( "Mueller" );
        wb.besitzerIndex.set( 0 );
        Flurstueck fs1 = wb.flurstuecke.createElement( null );
        fs1.zaehlerNenner.set( "1" );
        fs1.flaeche.set( 0.3 );
        fs1.flaecheWald.set( 0.2 );
        Gemarkung gem11 = unitOfWork.createEntity( Gemarkung.class, null );
        gem11.gemeinde.set( "Gemeinde 11" );
        gem11.gemarkung.set( "Gemarkung 11" );
        fs1.gemarkung.set( gem11 );
        Flurstueck fs2 = wb.flurstuecke.createElement( null );
        fs2.zaehlerNenner.set( "2" );
        fs2.flaeche.set( 0.4 );
        fs2.flaecheWald.set( 0.3 );
        Gemarkung gem21 = unitOfWork.createEntity( Gemarkung.class, null );
        gem21.gemeinde.set( "Gemeinde 21" );
        gem21.gemarkung.set( "Gemarkung 21" );
        fs2.gemarkung.set( gem21 );
        Flurstueck fs3 = wb.flurstuecke.createElement( null );
        fs3.zaehlerNenner.set( "3" );
        fs3.flaeche.set( 0.5 );
        fs3.flaecheWald.set( 0.4 );
        Gemarkung gem22 = unitOfWork.createEntity( Gemarkung.class, null );
        gem22.gemeinde.set( "Gemeinde 21" );
        gem22.gemarkung.set( "Gemarkung 22" );
        fs3.gemarkung.set( gem22 );
        List<Waldbesitzer> wbs = new ArrayList<Waldbesitzer>();
        wbs.add( wb );
        Report101 report = new Report101() {
          
            @Override
            protected EntityRepository getRepository() {
                return entityRepository;
            }
        };
        report.setEntities( wbs );
        JasperReportBuilder builder = report.build();
        File file = new File("report.pdf");
        try (FileOutputStream fos = new FileOutputStream( file )) {
            builder.toPdf( fos  );
        }
    }
}
