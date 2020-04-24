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

import java.util.Deque;
import java.util.List;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;

import net.sf.dynamicreports.jasper.builder.JasperReportBuilder;
import net.sf.dynamicreports.report.exception.DRException;
import net.sf.jasperreports.engine.JRException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.eclipse.core.runtime.IProgressMonitor;

import org.polymap.core.runtime.UIJob;
import org.polymap.rap.updownload.download.DownloadService;

/**
 * Base class for downloadable Jasper reports. The report can serve as a
 * {@link ContentProvider} for the download service. In this case the report is
 * {@link #build()} and executed asynchronously inside an {@link UIJob}.
 * 
 * @param <R> The type of the report builder ({@link JasperReportBuilder} for example).
 * @author <a href="http://www.polymap.de">Falko Bräutigam</a>
 */
public abstract class DownloadableReport<R>
        implements DownloadService.ContentProvider {

    private static final Log log = LogFactory.getLog( DownloadableReport.class );
    
    /**
     * Decouples the creation of the content of an individual report from building
     * the particular report output format.
     */
    public enum OutputType {
        PDF( "application/pdf", "PDF" ) {
            public void create( Object report, OutputStream out ) throws Exception { ((JasperReportBuilder)report).toPdf( out ); }
        },
        HTML( "text/html", "HTML" ) {
            public void create( Object report, OutputStream out ) throws Exception { ((JasperReportBuilder)report).toHtml( out ); }
        },
        ODT( "application/vnd.oasis.opendocument.text", "LibreOffice" ) {
            public void create( Object report, OutputStream out ) throws Exception { ((JasperReportBuilder)report).toOdt( out ); }
        },
        ODS( "application/vnd.oasis.opendocument.spreadsheet", "LibreOffice" ) {
            public void create( Object report, OutputStream out ) throws Exception { ((JasperReportBuilder)report).toOds( out ); }
        },
        DOCX( "application/vnd.openxmlformats-officedocument.wordprocessingml.document", "MS-Word" ) {
            public void create( Object report, OutputStream out ) throws Exception { ((JasperReportBuilder)report).toDocx( out ); }
        },
        XLSX( "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", "MS-Excel" ) {
            public void create( Object report, OutputStream out ) throws Exception { ((JasperReportBuilder)report).toXlsx( out ); }
        },
        CSV( "text/csv", "CSV" ) {
            public void create( Object report, OutputStream out ) throws Exception { CsvBuilder.toExcelCsv( (Deque<List<?>>)report, out ); }
        };
        
        public String      mimeType;
        
        public String      description;
        
        private OutputType( String mime, String description ) {
            this.mimeType = mime;
            this.description = description;
        }

        public String extension() {
            return name().toLowerCase();
        }
        
        public abstract void create( Object report, OutputStream out ) throws Exception;
    }

    
    // instance *******************************************
    
    protected OutputType                    outputType;
    
    
    public DownloadableReport<R> setOutputType( OutputType outputType ) {
        this.outputType = outputType;
        return this;
    }

    public abstract String getName();
    
    public abstract R build() throws DRException, JRException, IOException;


    // ContentProvider ************************************
    
    @Override
    public InputStream getInputStream() throws Exception {
        return new ReportBuilderJob().run();
    }

    @Override
    public String getFilename() {
        return "WBV-Report." + outputType.extension();
    }

    @Override
    public String getContentType() {
        assert outputType != null;
        return outputType.mimeType;
    }

    @Override
    public boolean done( boolean success ) {
        return true;
    }

    
    /**
     * 
     */
    protected class ReportBuilderJob
            extends UIJob {

        private PipedOutputStream           out;

        
        public ReportBuilderJob() {
            super( "ReportBuilder" );
        }

        
        public InputStream run() throws IOException {
            assert out == null;
            out = new PipedOutputStream();
            PipedInputStream result = new PipedInputStream( out, 4*1024 );
            schedule();
            return result;
        }

        
        @Override
        protected void runWithException( IProgressMonitor monitor ) throws Exception {
            try {
                R report = build();
                if (log.isDebugEnabled()) {
                    if (report != null && report instanceof JasperReportBuilder) {
                        ((JasperReportBuilder)report).toJrXml( System.out );
                    }
                    else {
                        System.out.println( "report is null!" );
                    }
                }

                outputType.create( report, out );
                out.flush();
                out.close();
            }
            catch (Throwable e) {
                out.close();
                throw e;
            }
        }
    }

}
