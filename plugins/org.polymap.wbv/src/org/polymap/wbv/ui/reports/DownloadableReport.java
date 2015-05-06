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

import org.polymap.core.data.operation.DownloadServiceHandler.ContentProvider;
import org.polymap.core.runtime.UIJob;

/**
 * Base class for downloadable Jasper reports. The report can serve as a
 * {@link ContentProvider} for the download service. In this case the report is
 * {@link #build()} and executed asynchronously inside an {@link UIJob}.
 * 
 * @author <a href="http://www.polymap.de">Falko Bräutigam</a>
 */
public abstract class DownloadableReport
        implements ContentProvider {

    private static Log log = LogFactory.getLog( DownloadableReport.class );
    
    /**
     * 
     */
    public enum OutputType {
        PDF( "application/pdf", "PDF" ) {
            public void create( JasperReportBuilder report, OutputStream out ) throws Exception { report.toPdf( out ); }
        },
        HTML( "text/html", "HTML" ) {
            public void create( JasperReportBuilder report, OutputStream out ) throws Exception { report.toHtml( out ); }
        },
        ODT( "application/vnd.oasis.opendocument.text", "LibreOffice" ) {
            public void create( JasperReportBuilder report, OutputStream out ) throws Exception { report.toOdt( out ); }
        },
        ODS( "application/vnd.oasis.opendocument.spreadsheet", "LibreOffice" ) {
            public void create( JasperReportBuilder report, OutputStream out ) throws Exception { report.toOds( out ); }
        },
        DOCX( "application/vnd.openxmlformats-officedocument.wordprocessingml.document", "MS-Word" ) {
            public void create( JasperReportBuilder report, OutputStream out ) throws Exception { report.toDocx( out ); }
        },
        XLSX( "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", "MS-Excel" ) {
            public void create( JasperReportBuilder report, OutputStream out ) throws Exception { report.toXlsx( out ); }
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
        
        public abstract void create( JasperReportBuilder report, OutputStream out ) throws Exception;
    }

    
    // instance *******************************************
    
    protected OutputType                    outputType;
    
    
    public DownloadableReport setOutputType( OutputType outputType ) {
        this.outputType = outputType;
        return this;
    }

    public abstract String getName();
    
    public abstract JasperReportBuilder build() throws DRException, JRException, IOException;


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
                JasperReportBuilder report = build();
                report.toJrXml( System.out );

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
