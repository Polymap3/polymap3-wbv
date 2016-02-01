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
import static org.polymap.model2.query.Expressions.and;
import static org.polymap.model2.query.Expressions.anyOf;
import static org.polymap.model2.query.Expressions.eq;
import static org.polymap.model2.query.Expressions.id;
import static org.polymap.model2.query.Expressions.the;
import static org.polymap.wbv.ui.PropertyAdapter.descriptorFor;

import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import java.beans.PropertyChangeEvent;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.time.FastDateFormat;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.google.common.base.Function;

import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Composite;

import org.eclipse.jface.viewers.ColumnLabelProvider;

import org.polymap.core.runtime.event.EventHandler;
import org.polymap.core.runtime.event.EventManager;

import org.polymap.rhei.batik.BatikApplication;
import org.polymap.rhei.batik.Context;
import org.polymap.rhei.batik.IAppContext;
import org.polymap.rhei.batik.PanelChangeEvent;
import org.polymap.rhei.batik.PanelChangeEvent.EventType;
import org.polymap.rhei.batik.PanelSite;
import org.polymap.rhei.batik.Scope;
import org.polymap.rhei.batik.app.SvgImageRegistryHelper;
import org.polymap.rhei.batik.toolkit.Snackbar.Appearance;
import org.polymap.rhei.field.IFormFieldValidator;
import org.polymap.rhei.field.NotEmptyValidator;
import org.polymap.rhei.field.NullValidator;
import org.polymap.rhei.field.NumberValidator;
import org.polymap.rhei.field.PicklistFormField;
import org.polymap.rhei.field.StringFormField;
import org.polymap.rhei.table.ActionCellEditor;
import org.polymap.rhei.table.FeatureTableViewer;
import org.polymap.rhei.table.FormFeatureTableColumn;
import org.polymap.rhei.table.IFeatureTableColumn;
import org.polymap.rhei.table.IFeatureTableElement;
import org.polymap.rhei.table.ITableFieldValidator;

import org.polymap.model2.Entity;
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

    private static Log log  = LogFactory.getLog( FlurstueckTableViewer.class );

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
            log.info( "ev:  "  + ev );
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
        for (IFeatureTableColumn col : displayed.values()) {
            if (!((FormFeatureTableColumn)col).dirtyFids().isEmpty()) {
                return true;
            }
        }
        return flurstueckDeleted;
    }
    

    public boolean isValid() {
        for (IFeatureTableColumn col : displayed.values()) {
            if (!((FormFeatureTableColumn)col).invalidFids().isEmpty()) {
                return false;
            }
        }
        return true;
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
            // Action: delete
            addColumn( new FormFeatureTableColumn( descriptorFor( "", String.class ) )
                .setWeight( 1, 25 )
                .setLabelProvider( new ColumnLabelProvider() {
                    @Override
                    public Image getImage( Object element ) {
                        return WbvPlugin.images().svgImage( "delete.svg", SvgImageRegistryHelper.NORMAL12 );
                    }
                    @Override
                    public String getText( Object element ) {
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
                    public Image getImage( Object element ) {
                        return WbvPlugin.images().svgImage( "transfer.svg", SvgImageRegistryHelper.NORMAL12 );
                    }
                    @Override
                    public String getText( Object element ) {
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
            final ColumnLabelProvider lp[] = new ColumnLabelProvider[1];
            addColumn( new FormFeatureTableColumn( descriptorFor( propName, String.class ) )
                .setWeight( 6, 80 )
                .setLabelProvider( lp[0] = new ColumnLabelProvider() {
                    @Override
                    public String getText( Object elm ) {
                        return StringUtils.abbreviate( getToolTipText( elm ), 30 );
                    }
                    @Override
                    public String getToolTipText( Object elm ) {
                        Flurstueck fst = FeatureTableElement.entity( elm );
                        Gemarkung gmk = fst.gemarkung.get();
                        return gmk != null ? gmk.label() : "(kein Gemarkung)";
                    }
                })
                .setEditing( new PicklistFormField( Gemarkung.all.get() ), new AenderungValidator( new AdoptEntityValidator() ))
                .setSortable( new Comparator<IFeatureTableElement>() {
                    public int compare( IFeatureTableElement e1, IFeatureTableElement e2 ) {
                        String l1 = lp[0].getText( e1 );
                        String l2 = lp[0].getText( e2 );
                        return l1.compareTo( l2 );
                    }
                }))
                .sort( SWT.DOWN );
            
            // Flurstücksnummer
            addColumn( new FormFeatureTableColumn( descriptorFor( Flurstueck.TYPE.zaehlerNenner ) )
                .setWeight( 3, 50 )
                .setHeader( "Nr." )
                .setLabelProvider( new NotEmptyValidator() {
                    public Object transform2Field( Object modelValue ) throws Exception {
                        log.info( "Nummer: " + modelValue );
                        return super.transform2Field( modelValue );
                    }
                })
                .setEditing( new StringFormField(), new AenderungValidator( new FlurstueckExistsValidator() ) ) );
            
            // Fläche
            NumberValidator flaecheValidator = new NumberValidator( Double.class, Locale.GERMANY, 10, 4, 1, 4 );
            addColumn( new FormFeatureTableColumn( descriptorFor( Flurstueck.TYPE.flaeche ) )
                .setWeight( 3, 50 )
                .setHeader( "Fläche\n(in ha)" )
                .setLabelProvider( flaecheValidator )
                .setEditing( new StringFormField(), new AenderungValidator( flaecheValidator ) )
                .setSortable( false ) );  // standard comparator: ClassCastException wenn null
            
            // davon Wald
            addColumn( new FormFeatureTableColumn( descriptorFor( Flurstueck.TYPE.flaecheWald ) )
                .setWeight( 3, 50 )
                .setHeader( "Wald\n(in ha)" )
                .setLabelProvider( flaecheValidator )
                .setEditing( new StringFormField(), new AenderungValidator( new WaldflaecheValidator() ) )
                .setSortable( false ) );  // standard comparator: ClassCastException wenn null
            
            // Änderungsdatum
            addColumn( new FormFeatureTableColumn( descriptorFor( Flurstueck.TYPE.aenderung ) )
                .setWeight( 4, 80 )
                .setHeader( "Änderung" )
                .setLabelProvider( new ColumnLabelProvider() {
                    @Override
                    public String getText( Object elm ) {
                        Flurstueck fst = FeatureTableElement.entity( elm );
                        return fst.aenderung.get() != null ? WbvPlugin.df.format( fst.aenderung.get() ) : "--";
                    }
                }));

            // Bemerkung
            addColumn( new FormFeatureTableColumn( descriptorFor( Flurstueck.TYPE.bemerkung ) )
                .setWeight( 11, 120 )
                .setEditing( new StringFormField(), new AenderungValidator() ) );

            // suppress deferred loading to fix "empty table" issue
            // setContent( fs.getFeatures( this.baseFilter ) );
            setContent( new CompositesFeatureContentProvider() );
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
     * 
     */
    protected class AenderungValidator
            extends NullValidator
            implements ITableFieldValidator {
        
        private Flurstueck              flurstueck;
        
        private IFormFieldValidator     next;

        public AenderungValidator() {
        }

        public AenderungValidator( IFormFieldValidator next ) {
            this.next = next;
        }

        @Override
        public void init( IFeatureTableElement elm ) {
            if (next instanceof ITableFieldValidator) {
                ((ITableFieldValidator)next).init( elm );
            }
            flurstueck = FeatureTableElement.entity( elm );
        }

        @Override
        public Object transform2Model( Object fieldValue ) throws Exception {
            flurstueck.aenderung.set( new Date() );
            return next != null ? next.transform2Model( fieldValue ) : fieldValue;
        }

        @Override
        public String validate( Object value ) {
            return next != null ? next.validate( value ) : null;
        }

        @Override
        public Object transform2Field( Object modelValue ) throws Exception {
            return next != null ? next.transform2Field( modelValue ) : modelValue;
        }
    }
        

    /**
     * 
     */
    protected class AdoptEntityValidator
            extends NullValidator {
    
        @Override
        public Object transform2Model( Object fieldValue ) throws Exception {
            return fieldValue != null ? uow.get().entity( (Entity)fieldValue ) : null; // adopt entity to local uow
        }
    }


    /**
     * 
     */
    protected class FlurstueckExistsValidator
            extends NotEmptyValidator
            implements ITableFieldValidator {
        
        Flurstueck      flurstueck;

        @Override
        public void init( IFeatureTableElement elm ) {
            flurstueck = FeatureTableElement.entity( elm );
        }
        
        @Override
        public String validate( Object fieldValue ) {
            String notEmpty = super.validate( fieldValue );
            if (notEmpty != null) {
                return notEmpty;
            }
            else {
                if (fieldValue.equals( flurstueck.zaehlerNenner.get() )) {
                    return null;
                }
                else {
                    Gemarkung gmk = flurstueck.gemarkung.get();
                    if (gmk == null) {
                        return "Noch keine Gemarkung";
                    }
                    else {
                        ResultSet<Waldbesitzer> rs = uow.get().query( Waldbesitzer.class )
                                // FIXME geloescht beachten!
                                .where( anyOf( Waldbesitzer.TYPE.flurstuecke, 
                                        and(
                                                the( Flurstueck.TYPE.gemarkung, id( gmk.id() ) ),
                                                eq( Flurstueck.TYPE.zaehlerNenner, (String)fieldValue ) ) ) )
                                .execute();
                        return rs.size() == 0 ? null : "Dieser Zähler/Nenner existiert bereits";
                    }
                }
            }
        }
    }
    

    /**
     * 
     */
    protected class WaldflaecheValidator
            extends NumberValidator
            implements ITableFieldValidator {
        
        Flurstueck      flurstueck;

        public WaldflaecheValidator() {
            super( Double.class, Locale.GERMANY, 10, 4, 1, 4 );
        }

        @Override
        public void init( IFeatureTableElement elm ) {
            flurstueck = FeatureTableElement.entity( elm );
        }

        @Override
        public String validate( Object fieldValue ) {
            String isNumber = super.validate( fieldValue );
            if (isNumber != null) {
                return isNumber;
            }
            else {
                try {
                    Double flaeche = flurstueck.flaeche.get();
                    Double neueWaldflaeche = (Double)super.transform2Model( fieldValue );
                    return flaeche == null || neueWaldflaeche > flaeche ? "Dieser Zähler/Nenner existiert bereits" : null;
                }
                catch (Exception e) {
                    log.warn( "", e );
                    return "Fehler beim validieren der Eingabe: " + e.getLocalizedMessage(); 
                }
            }
        }
    }
    
}
