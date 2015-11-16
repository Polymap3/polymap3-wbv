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
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import net.sf.dynamicreports.jasper.builder.JasperReportBuilder;
import net.sf.dynamicreports.report.exception.DRException;
import net.sf.jasperreports.engine.JRException;

import org.junit.Before;
import org.junit.Test;
import org.polymap.model2.runtime.EntityRepository;
import org.polymap.model2.runtime.UnitOfWork;
import org.polymap.model2.store.recordstore.RecordStoreAdapter;
import org.polymap.recordstore.lucene.LuceneRecordStore;
import org.polymap.wbv.model.Flurstueck;
import org.polymap.wbv.model.Gemarkung;
import org.polymap.wbv.model.Kontakt;
import org.polymap.wbv.model.Waldbesitzer;
import org.polymap.wbv.model.Waldbesitzer.Waldeigentumsart;

/**
 * @author Joerg Reichert <joerg@mapzone.io>
 *
 */
public class ReportTest {

    private EntityRepository entityRepository;

    private UnitOfWork       unitOfWork;


    @Before
    public void setUp() throws IOException {
        LuceneRecordStore store = new LuceneRecordStore();
        entityRepository = EntityRepository.newConfiguration().store.set( new RecordStoreAdapter( store ) ).entities
                .set( new Class[] { Waldbesitzer.class, Gemarkung.class } ).create();
        unitOfWork = entityRepository.newUnitOfWork();
    }


    @Test
    public void test101_1() throws Exception {
        Waldbesitzer wb = createWaldbesitzer1();
        List<Waldbesitzer> wbs = new ArrayList<Waldbesitzer>();
        wbs.add( wb );
        executeTest101( entityRepository, wbs, 1 );
    }


    private Waldbesitzer createWaldbesitzer1() {
        Waldbesitzer wb = createWaldbesitzer( "Bumbaitschi", "Heidi", "Feldweg 1", "0815", "Dorf" );
        Flurstueck fs1 = createFlurstueck( wb, "1", 0.3, 0.2 );
        Gemarkung gem11 = createGemarkung( "Gemeinde 11", "Gemarkung 11" );
        fs1.gemarkung.set( gem11 );
        Flurstueck fs2 = createFlurstueck( wb, "2", 0.4, 0.3 );
        Gemarkung gem21 = createGemarkung( "Gemeinde 21", "Gemarkung 21" );
        fs2.gemarkung.set( gem21 );
        Flurstueck fs3 = createFlurstueck( wb, "3", 0.5, 0.4 );
        Gemarkung gem22 = createGemarkung( "Gemeinde 21", "Gemarkung 22" );
        fs3.gemarkung.set( gem22 );
        return wb;
    }


    private Waldbesitzer createWaldbesitzer2() {
        Waldbesitzer wb2 = createWaldbesitzer( "Müller", "Hans", "Feldweg 1", "0815", "Dorf" );
        Flurstueck fs4 = createFlurstueck( wb2, "3", 0.5, 0.4 );
        Gemarkung gem22 = createGemarkung( "Gemeinde 21", "Gemarkung 22" );
        fs4.gemarkung.set( gem22 );
        return wb2;
    }


    @Test
    public void test102() throws Exception {
        Waldbesitzer wb1 = createWaldbesitzer1();
        Waldbesitzer wb2 = createWaldbesitzer2();
        List<Waldbesitzer> wbs = new ArrayList<Waldbesitzer>();
        wbs.add( wb1 );
        wbs.add( wb2 );
        executeTest102( entityRepository, wbs, 2 );
    }


    @Test
    public void test103() throws Exception {
        Waldbesitzer wb1 = createWaldbesitzer1();
        Waldbesitzer wb2 = createWaldbesitzer2();
        List<Waldbesitzer> wbs = new ArrayList<Waldbesitzer>();
        wbs.add( wb1 );
        wbs.add( wb2 );
        executeTest103( entityRepository, wbs, 2 );
    }


    @Test
    public void test105() throws Exception {
        Waldbesitzer wb1 = createWaldbesitzer1();
        Waldbesitzer wb2 = createWaldbesitzer2();
        List<Waldbesitzer> wbs = new ArrayList<Waldbesitzer>();
        wbs.add( wb1 );
        wbs.add( wb2 );
        executeTest105( entityRepository, wbs, 2 );
    }


    @Test
    public void test106() throws Exception {
        Waldbesitzer wb1 = createWaldbesitzer1();
        Waldbesitzer wb2 = createWaldbesitzer2();
        List<Waldbesitzer> wbs = new ArrayList<Waldbesitzer>();
        wbs.add( wb1 );
        wbs.add( wb2 );
        executeTest106( entityRepository, wbs, 2 );
    }


    @Test
    public void test106b() throws Exception {
        Waldbesitzer wb1 = createWaldbesitzer1();
        Waldbesitzer wb2 = createWaldbesitzer2();
        List<Waldbesitzer> wbs = new ArrayList<Waldbesitzer>();
        wbs.add( wb1 );
        wbs.add( wb2 );
        executeTest106b( entityRepository, wbs, 2 );
    }


    @Test
    public void test106c() throws Exception {
        Waldbesitzer wb1 = createWaldbesitzer1();
        Waldbesitzer wb2 = createWaldbesitzer2();
        List<Waldbesitzer> wbs = new ArrayList<Waldbesitzer>();
        wbs.add( wb1 );
        wbs.add( wb2 );
        executeTest106c( entityRepository, wbs, 2 );
    }


    private Waldbesitzer createWaldbesitzer( String name, String vorname, String strasse, String plz, String ort ) {
        Waldbesitzer wb = unitOfWork.createEntity( Waldbesitzer.class, null );
        Kontakt kontakt = wb.kontakte.createElement( null );
        kontakt.vorname.set( vorname );
        kontakt.name.set( name );
        kontakt.strasse.set( strasse );
        kontakt.plz.set( plz );
        kontakt.ort.set( ort );
        wb.besitzerIndex.set( 0 );
        wb.eigentumsArt.set( Waldeigentumsart.Privat );
        return wb;
    }


    private Flurstueck createFlurstueck( Waldbesitzer wb, String nr, double flaeche, double waldFlaeche ) {
        Flurstueck fs = wb.flurstuecke.createElement( null );
        fs.zaehlerNenner.set( nr );
        fs.flaeche.set( flaeche );
        fs.flaecheWald.set( waldFlaeche );
        return fs;
    }


    private Gemarkung createGemarkung( String gemeinde, String gemarkung ) {
        Gemarkung gem = unitOfWork.createEntity( Gemarkung.class, null );
        gem.gemeinde.set( gemeinde );
        gem.gemarkung.set( gemarkung );
        gem.revier.set( "Geringswalde" );
        return gem;
    }


    private void executeTest101( EntityRepository entityRepository, List<Waldbesitzer> wbs, int counter )
            throws DRException, JRException, IOException, FileNotFoundException {
        Report101 report = new Report101() {

            @Override
            protected EntityRepository getRepository() {
                return entityRepository;
            }
        };
        executeTest( entityRepository, wbs, report, "report101_", counter );
    }


    private void executeTest102( EntityRepository entityRepository, List<Waldbesitzer> wbs, int counter )
            throws DRException, JRException, IOException, FileNotFoundException {
        Report102 report = new Report102() {

            @Override
            protected EntityRepository getRepository() {
                return entityRepository;
            }
        };
        executeTest( entityRepository, wbs, report, "report102_", counter );
    }


    private void executeTest103( EntityRepository entityRepository, List<Waldbesitzer> wbs, int counter )
            throws DRException, JRException, IOException, FileNotFoundException {
        Report103 report = new Report103() {

            @Override
            protected EntityRepository getRepository() {
                return entityRepository;
            }


            protected String getQuery() {
                return "Hedwig";
            }


            protected String getRevier() {
                return "Geringswalde";
            }
        };
        executeTest( entityRepository, wbs, report, "report103_", counter );
    }


    private void executeTest105( EntityRepository entityRepository, List<Waldbesitzer> wbs, int counter )
            throws DRException, JRException, IOException, FileNotFoundException {
        Report105 report = new Report105() {

            @Override
            protected EntityRepository getRepository() {
                return entityRepository;
            }
        };
        executeTest( entityRepository, wbs, report, "report105_", counter );
    }


    private void executeTest106( EntityRepository entityRepository, List<Waldbesitzer> wbs, int counter )
            throws DRException, JRException, IOException, FileNotFoundException {
        Report106 report = new Report106() {

            @Override
            protected EntityRepository getRepository() {
                return entityRepository;
            }

            @Override
            protected String getQuery() {
                return "Hedwig";
            }

            @Override
            protected String getRevier() {
                return "Geringswalde";
            }
        };
        executeTest( entityRepository, wbs, report, "report106_", counter );
    }


    private void executeTest106b( EntityRepository entityRepository, List<Waldbesitzer> wbs, int counter )
            throws DRException, JRException, IOException, FileNotFoundException {
        Report106b report = new Report106b() {

            @Override
            protected EntityRepository getRepository() {
                return entityRepository;
            }
            
            @Override
            protected String getQuery() {
                return "Hedwig";
            }

            @Override
            protected String getRevier() {
                return "Geringswalde";
            }         
        };
        executeTest( entityRepository, wbs, report, "report106b_", counter );
    }


    private void executeTest106c( EntityRepository entityRepository, List<Waldbesitzer> wbs, int counter )
            throws DRException, JRException, IOException, FileNotFoundException {
        Report106c report = new Report106c() {

            @Override
            protected EntityRepository getRepository() {
                return entityRepository;
            }
            
            @Override
            protected String getQuery() {
                return "Hedwig";
            }

            @Override
            protected String getRevier() {
                return "Geringswalde";
            }            
        };
        executeTest( entityRepository, wbs, report, "report106c_", counter );
    }


    private void executeTest( EntityRepository entityRepository, List<Waldbesitzer> wbs, WbvReport report, String name,
            int counter ) throws DRException, JRException, IOException, FileNotFoundException {
        report.setEntities( wbs );
        JasperReportBuilder builder = report.build();
        File file = new File( name + counter + ".pdf" );
        try (FileOutputStream fos = new FileOutputStream( file )) {
            builder.toPdf( fos );
        }
    }
}
