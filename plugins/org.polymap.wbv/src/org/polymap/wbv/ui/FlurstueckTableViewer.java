/*
 * Copyright (C) 2014-2016, Falko Bräutigam. All rights reserved.
 * 
 * This is free software; you can redistribute it and/or modify it under the terms of
 * the GNU Lesser General Public License as published by the Free Software
 * Foundation; either version 3.0 of the License, or (at your option) any later
 * version.
 * 
 * This software is distributed in the hope that it will be useful, but WITHOUT ANY
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A
 * PARTICULAR PURPOSE. See the GNU Lesser General Public License for more details.
 */
package org.polymap.wbv.ui;

import static com.google.common.collect.ImmutableList.copyOf;
import static com.google.common.collect.Iterables.transform;
import static java.util.Arrays.asList;
import static org.polymap.core.ui.FormDataFactory.on;
import static org.polymap.model2.query.Expressions.anyOf;
import static org.polymap.model2.query.Expressions.eq;
import static org.polymap.model2.query.Expressions.is;
import static org.polymap.model2.query.Expressions.notEq;
import static org.polymap.rhei.field.Validators.AND;
import static org.polymap.wbv.ui.PropertyAdapter.descriptorFor;

import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import java.beans.PropertyChangeEvent;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.time.FastDateFormat;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.google.common.base.Function;

import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;

import org.eclipse.jface.viewers.CellEditor;
import org.eclipse.jface.viewers.ColumnLabelProvider;

import org.eclipse.core.runtime.IProgressMonitor;

import org.polymap.core.runtime.event.EventHandler;
import org.polymap.core.runtime.event.EventManager;
import org.polymap.core.ui.FormLayoutFactory;

import org.polymap.rhei.batik.BatikApplication;
import org.polymap.rhei.batik.Context;
import org.polymap.rhei.batik.IAppContext;
import org.polymap.rhei.batik.PanelChangeEvent;
import org.polymap.rhei.batik.PanelChangeEvent.EventType;
import org.polymap.rhei.batik.PanelSite;
import org.polymap.rhei.batik.Scope;
import org.polymap.rhei.batik.app.SvgImageRegistryHelper;
import org.polymap.rhei.batik.toolkit.SimpleDialog;
import org.polymap.rhei.batik.toolkit.Snackbar.Appearance;
import org.polymap.rhei.field.IFormFieldValidator;
import org.polymap.rhei.field.NotEmptyValidator;
import org.polymap.rhei.field.NumberValidator;
import org.polymap.rhei.field.PicklistFormField;
import org.polymap.rhei.field.StringFormField;
import org.polymap.rhei.table.ActionCellEditor;
import org.polymap.rhei.table.DefaultTableValidator;
import org.polymap.rhei.table.DelegatingValidator;
import org.polymap.rhei.table.FeatureTableViewer;
import org.polymap.rhei.table.FormFeatureTableColumn;
import org.polymap.rhei.table.IFeatureTableColumn;
import org.polymap.rhei.table.IFeatureTableElement;
import org.polymap.model2.query.Expressions;
import org.polymap.model2.query.ResultSet;
import org.polymap.model2.runtime.UnitOfWork;
import org.polymap.wbv.WbvPlugin;
import org.polymap.wbv.model.Flurstueck;
import org.polymap.wbv.model.Gemarkung;
import org.polymap.wbv.model.Revier;
import org.polymap.wbv.model.Waldbesitzer;
import org.polymap.wbv.ui.CompositesFeatureContentProvider.FeatureTableElement;

/**
 * 
 * 
 * @author <a href="http://www.polymap.de">Falko Bräutigam</a>
 */
public class FlurstueckTableViewer
        extends FeatureTableViewer {

    private static final Log log  = LogFactory.getLog( FlurstueckTableViewer.class );

    private static final FastDateFormat df   = FastDateFormat.getInstance( "dd.MM.yyyy" );

    private Context<UnitOfWork>         uow;
    
    private Context<Waldbesitzer>       wb;
    
    private Context<Flurstueck>         selected;
    
    /** */
    @Scope( "org.polymap.wbv.ui" )
    protected Context<Revier>           revier;
    
    private PanelSite                   panelSite;
    
    private boolean                     flurstueckDeleted;
    
    private Object                      panelChangeListener = new Object() {
        @EventHandler( display=true )
        protected void fieldChange( PanelChangeEvent ev ) {
            if (!getTable().isDisposed()) {
                refresh();
            }
            else {
                IAppContext context = BatikApplication.instance().getContext();
                context.removeListener( panelChangeListener );                
            }
        }        
    };
    
    
    public boolean isDirty() {
        boolean anyColumnDirty = displayed.values().stream().filter( col -> ((FormFeatureTableColumn)col).isDirty() ).findAny().isPresent();
        return anyColumnDirty || flurstueckDeleted;
    }
    

    public boolean isValid() {
        return !displayed.values().stream().filter( col -> !((FormFeatureTableColumn)col).isValid() ).findAny().isPresent();
    }

    
    public void submit( IProgressMonitor monitor ) throws Exception {
        for (IFeatureTableColumn col : displayed.values()) {
            FormFeatureTableColumn fcol = (FormFeatureTableColumn)col;
            // aenderung datum
            for (IFeatureTableElement modified : fcol.modified().keySet()) {
                Flurstueck fst = FeatureTableElement.entity( modified );
                fst.aenderung.set( new Date() );
            }
            // submit
            fcol.submit( monitor );
        }
    }
    
    
    @EventHandler( display=true )
    protected void fieldChange( PropertyChangeEvent ev ) {
        log.info( "isDirty: " + isDirty() + ", isValid: " + isValid() );
    }

    
    public FlurstueckTableViewer( PanelSite panelSite, Composite parent ) {
        super( parent, /* SWT.VIRTUAL | SWT.V_SCROLL | SWT.FULL_SELECTION |*/ SWT.BORDER );
        this.panelSite = panelSite;
        IAppContext context = BatikApplication.instance().getContext();
        context.propagate( this );

        suppressSelection();

        context.addListener( panelChangeListener, ev -> ev.getType().isOnOf( EventType.LIFECYCLE ) );
        
        // listen to column/field changes
        EventManager.instance().subscribe( this, ev -> 
                ev instanceof PropertyChangeEvent && displayed.values().contains( ev.getSource() ) );
        
        try {
            // suppress deferred loading to fix "empty table" issue
            // setContent( fs.getFeatures( this.baseFilter ) );
            setContentProvider( new CompositesFeatureContentProvider() );

            // Action: delete
            addColumn( new FormFeatureTableColumn( descriptorFor( "", String.class ) )
                .setWeight( 1, 25 )
                .setLabelProvider( new ColumnLabelProvider() {
                    @Override
                    public Image getImage( Object elm ) {
                        return WbvPlugin.images().svgImage( "delete.svg", SvgImageRegistryHelper.NORMAL12 );
                    }
                    @Override
                    public String getText( Object elm ) {
                        return null;
                    }
                    @Override
                    public String getToolTipText( Object elm ) {
                        return "Flurstück löschen";
                    }
                })
                .setEditing( new ActionCellEditor( elm -> {
                    assert wb.isPresent();
                    Flurstueck fst = FeatureTableElement.entity( elm );
                    fst.geloescht.set( true );
                    refresh();
                    
                    flurstueckDeleted = true;
                    fieldChange( null );
                    
                    // waldbesitzer löschen, wenn (wirklich) kein flurstück mehr da
                    if (wb.get().flurstuecke( null ).isEmpty()) {
                        uow.get().removeEntity( wb.get() );
                        panelSite.toolkit().createSnackbar( Appearance.FadeIn, "Waldbesitzer wird beim Speichern <b>gelöscht</b>!" );
                    }
                })));

            // Action: transfer
            addColumn( new FormFeatureTableColumn( descriptorFor( "", String.class ) )
                .setWeight( 1, 30 )
                .setLabelProvider( new ColumnLabelProvider() {
                    @Override
                    public Image getImage( Object elm ) {
                        return WbvPlugin.images().svgImage( "transfer.svg", SvgImageRegistryHelper.NORMAL12 );
                    }
                    @Override
                    public String getText( Object elm ) {
                        return null;
                    }
                    @Override
                    public String getToolTipText( Object elm ) {
                        return "Eigentumsübergang: an einem anderen Waldbesitzer übertragen";
                    }
                })
                .setEditing( new ActionCellEditor( elm -> {
                    Flurstueck fst = FeatureTableElement.entity( elm );
                    selected.set( fst );
                    
                    BatikApplication.instance().getContext().openPanel( panelSite.path(), EigentumsuebergangPanel.ID );
                })));

            // Gemarkung
            String propName = Flurstueck.TYPE.gemarkung.info().getName();
            addColumn( new FormFeatureTableColumn( descriptorFor( propName, Gemarkung.class ) )
                .setWeight( 6, 80 )
                .setLabelsAndValidation( new DefaultTableValidator<Object,Gemarkung>() {
                    @Override
                    public Object transform2Field( Gemarkung gmk, ValidatorSite site ) throws Exception {
                        if (site.isEditing()) {
                            return gmk;
                        }
                        else {
                            //Flurstueck flst = FeatureTableElement.entity( site.element() );
                            String result = gmk != null ? gmk.label() : "-keine Gemarkung-"; //"WVK: " + flst.wvkGemarkung.get() + "/" + flst.wvkGemeinde.get();
                            return StringUtils.abbreviate( result, 30 );
                        }
                    }
                    @Override
                    public Gemarkung transform2Model( Object fieldValue, ValidatorSite site ) throws Exception {
                        return fieldValue != null ? uow.get().entity( (Gemarkung)fieldValue ) : null;
                    }
                })
                .setEditing( new PicklistFormField( Gemarkung.all.get() ) )
                .setSortable( new Comparator<IFeatureTableElement>() {
                    public int compare( IFeatureTableElement e1, IFeatureTableElement e2 ) {
                        return label( e1 ).compareTo( label( e2 ) );
                    }
                    protected String label( IFeatureTableElement elm ) {
                        Gemarkung gmk = (Gemarkung)elm.getValue( propName );
                        return gmk != null ? gmk.label() : "";
                    }
                }))
                .sort( SWT.DOWN );
            
            // Flurstücksnummer
            addColumn( new FormFeatureTableColumn( descriptorFor( Flurstueck.TYPE.zaehlerNenner ) )
                .setWeight( 3, 50 )
                .setHeader( "Nr." )
                .setLabelsAndValidation( AND( 
                        new NotEmptyValidator().forTable(), 
                        new AenderungValidator(), 
                        new NummerValidator(),
                        new FlurstueckExistsValidator() ) )
                .setEditing( new StringFormField() ) );
            
            // Fläche
            NumberValidator flaecheValidator = new NumberValidator( Double.class, Locale.GERMANY, 10, 4, 1, 4 );
            addColumn( new FormFeatureTableColumn( descriptorFor( Flurstueck.TYPE.flaeche ) )
                .setWeight( 3, 50 )
                .setHeader( "Fläche\n(in ha)" )
                .setLabelsAndValidation( AND( 
                        flaecheValidator.forTable(), 
                        new AenderungValidator() ) )
                .setEditing( new StringFormField() )
                .setSortable( true ) );
            
            // davon Wald
            addColumn( new FormFeatureTableColumn( descriptorFor( Flurstueck.TYPE.flaecheWald ) )
                .setWeight( 3, 50 )
                .setHeader( "Wald\n(in ha)" )
                .setLabelsAndValidation( AND( 
                        new WaldflaecheValidator( flaecheValidator ), 
                        new AenderungValidator() ) )
                .setEditing( new StringFormField() )
                .setSortable( true ) );
            
            // Änderungsdatum
            addColumn( new FormFeatureTableColumn( descriptorFor( Flurstueck.TYPE.aenderung ) )
                .setWeight( 4, 80 )
                .setHeader( "Änderung" )
                .setLabelsAndValidation( new DateValidator().forTable() ) );

            // Bemerkung
            FormFeatureTableColumn bcolumn = new FormFeatureTableColumn( descriptorFor( Flurstueck.TYPE.bemerkung ) );
            bcolumn.setWeight( 11, 120 );
            bcolumn.setLabelsAndValidation( new AenderungValidator() );
            bcolumn.setEditing( new CellEditor() {
                private String  value;
                private Text    txt;
                @Override
                protected Control createControl( Composite _parent ) {
                    return null;
                }
                @Override
                protected void doSetFocus() {
                }
                @Override
                protected Object doGetValue() {
                    return value;
                }
                @Override
                protected void doSetValue( Object newValue ) {
                    this.value = (String)newValue;
                }
                @Override
                public void activate() {
                    Shell appShell = BatikApplication.shellToParentOn();
                    new SimpleDialog().centerOn.put( appShell ).title.put( "Bemerkung" )
                            .setContents( dialogParent -> {
                                dialogParent.setLayout( FormLayoutFactory.defaults().create() );
                                txt = on( new Text( dialogParent, SWT.MULTI | SWT.WRAP | SWT.BORDER ) )
                                        .fill().width( 350 ).height( 150 ).control();
                                txt.setText( value != null ? value : "" );
                                txt.setFocus();
                            })
                            .addOkAction( "ÜBERNEHMEN", () -> {
                                value = txt.getText();
                                fireApplyEditorValue();
                                EventManager.instance().publish( new PropertyChangeEvent( bcolumn, "bemerkung", value, null ) );
                                return null;
                            })
                            .addCancelAction( "ABBRECHEN" )
                            .open();
                }
            });
            addColumn( bcolumn );

            setInput( wb.get().flurstuecke( revier.get() ) );

//            /* Register for property change events */
//            EventManager.instance().subscribe( this, new EventFilter<PropertyChangeEvent>() {
//                public boolean apply( PropertyChangeEvent input ) {
//                    return input.getSource() instanceof Flurstueck;
//                }
//            });
        }
        catch (Exception e) {
            throw new RuntimeException( e );
        }
    }


//    @EventHandler(display=true, delay=1000, scope=Event.Scope.JVM)
//    protected void entityChanged( List<PropertyChangeEvent> ev ) {
//        if (!getControl().isDisposed()) {
//            refresh( true );
//        }
//    }


    public List<Flurstueck> getSelected() {
        return copyOf( transform( asList( getSelectedElements() ), new Function<IFeatureTableElement,Flurstueck>() {
            public Flurstueck apply( IFeatureTableElement input ) {
                return (Flurstueck)((FeatureTableElement)input).getComposite();
            }
        }));
    }


    /**
     * Deprecated: done by {@link FlurstueckTableViewer#submit(IProgressMonitor)}:
     */
    protected class AenderungValidator
            extends DefaultTableValidator {
        
        @Override
        public Object transform2Model( Object fieldValue, ValidatorSite site ) throws Exception {
//            site.setColumnValue( Flurstueck.TYPE.aenderung.info().getName(), new Date() );
            return super.transform2Model( fieldValue, site );
        }
    }
        

    /**
     * Im Freistaat Sachsen gibt folgende Flurstückstypen:
     * <pre>
     * •  nur Zähler (Bsp.: 111)
     * •  Zähler/[Nenner] ohne Leerzeichen (Bsp.: 111/2)
     * •  ZählerKleinbuchstabe ohne Leerzeichen (Bsp.: 111a)
     * </pre>
     */
    protected static class NummerValidator
            extends DefaultTableValidator {
    
        public static final Pattern     pattern = Pattern.compile( "[0-9]+(/[0-9]+|[a-z])?" );
        
        @Override
        public String validate( Object value, ValidatorSite site ) {
            Matcher matcher = pattern.matcher( (String)value );
            return matcher.matches() ? null : "Flurstücksnummern: 111, 111a, 111/2";
        }
    }


    /**
     * 
     */
    protected class FlurstueckExistsValidator
            extends DefaultTableValidator {
        
        @Override
        public String validate( Object fieldValue, ValidatorSite site ) {
            Optional<Gemarkung> gmk = site.columnValue( Flurstueck.TYPE.gemarkung.info().getName() );
            if (!gmk.isPresent()) {
                return "Noch keine Gemarkung";
            }
            else {
                ResultSet<Waldbesitzer> rs = uow.get().query( Waldbesitzer.class )
                        .where( anyOf( Waldbesitzer.TYPE.flurstuecke, 
                                Expressions.and(
                                        notEq( Flurstueck.TYPE.geloescht, true ),
                                        is( Flurstueck.TYPE.gemarkung, gmk.get() ),
                                        eq( Flurstueck.TYPE.zaehlerNenner, (String)fieldValue ) ) ) )
                        .maxResults( 1 )
                        .execute();
                return rs.size() == 0 ? null : "Zähler/Nenner existiert bereits in " + gmk.get().label();
            }
        }
    }
    

    /**
     * 
     */
    protected class WaldflaecheValidator
            extends DelegatingValidator<String,Double> {
        
        public WaldflaecheValidator( IFormFieldValidator<String,Double> delegate ) {
            super( delegate );
        }

        @Override
        public String validate( String fieldValue, ValidatorSite site ) {
            try {
                Optional<Double> flaeche = site.columnValue( Flurstueck.TYPE.flaeche.info().getName() );
                Double neueWaldflaeche = (Double)super.transform2Model( fieldValue, site );
                return flaeche.isPresent() && neueWaldflaeche > flaeche.get()
                        ? "Die Waldfläche ist größer als die Gesamtfläche" : null;
            }
            catch (Exception e) {
                log.warn( "", e );
                return "Fehler beim validieren der Eingabe: " + e.getLocalizedMessage();
            }
        }
    }
    
}
