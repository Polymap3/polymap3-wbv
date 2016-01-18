/* 
 * polymap.org
 * Copyright (C) 2016, Falko Bräutigam. All rights reserved.
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
package org.polymap.wbv.ui;

import static org.apache.commons.lang3.StringUtils.repeat;
import static org.polymap.core.ui.FormDataFactory.on;

import java.util.Collections;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;

import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.SelectionChangedEvent;

import org.polymap.core.ui.FormLayoutFactory;
import org.polymap.core.ui.UIUtils;

import org.polymap.rhei.batik.Context;
import org.polymap.rhei.batik.PanelIdentifier;
import org.polymap.rhei.batik.toolkit.IPanelSection;
import org.polymap.rhei.batik.toolkit.Snackbar.Appearance;
import org.polymap.rhei.fulltext.FulltextIndex;
import org.polymap.rhei.fulltext.ui.EntitySearchField;
import org.polymap.rhei.fulltext.ui.FulltextProposal;

import org.polymap.model2.query.ResultSet;
import org.polymap.model2.runtime.CopyCompositeState;
import org.polymap.model2.runtime.UnitOfWork;
import org.polymap.wbv.model.Flurstueck;
import org.polymap.wbv.model.Waldbesitzer;
import org.polymap.wbv.model.WbvRepository;

/**
 * 
 *
 * @author <a href="http://www.polymap.de">Falko Bräutigam</a>
 */
public class EigentumsuebergangPanel
        extends WbvPanel {

    private static Log log = LogFactory.getLog( EigentumsuebergangPanel.class );

    public static final PanelIdentifier ID  = new PanelIdentifier( "wbv", "waldbesitzerWaehlen" );

    private Context<UnitOfWork>         uow;
    
    private Context<Waldbesitzer>       origin;
    
    private Context<Flurstueck>         fst;
    
    private Waldbesitzer                selected;

    private Button                      fab;


    @Override
    public void init() {
        super.init();
        site().preferredWidth.set( 500 );
        site().title.set( "Waldbesitzer wählen..." );
    }


    protected void perform() {
        assert selected != null;
        assert origin.isPresent();
        assert fst.isPresent();
        
        selected.flurstuecke.createElement( (Flurstueck proto) -> {
            CopyCompositeState.from( fst.get() ).to( proto );
            return proto;
        });
        
        fst.get().geloescht.set( true );
        uow.get().commit();
    }
    
    
    @Override
    public void createContents( Composite parent ) {
        Composite body = parent;
        body.setLayout( FormLayoutFactory.defaults().spacing( 5 ).margins( 0, 10 ).create() );

        fab = tk().createFab();
        fab.setToolTipText( "Flurstück an den gewählten Waldbesitzer übertragen" );
        fab.setVisible( false );
        fab.addSelectionListener( new SelectionAdapter() {
            @Override
            public void widgetSelected( SelectionEvent ev ) {
                perform();
                ((WbvPanel)getContext().getPanel( site().path().removeLast( 1 ) )).tk().createSnackbar( Appearance.FadeIn, "Flurstück wurde übertragen." );
                getContext().closePanel( site().path() );
            }
        });
        
        // info section
        IPanelSection infoSection = tk().createPanelSection( body, "Eigentumsübergang" );
        tk().createFlowText( infoSection.getBody(), 
                "des Flurstücks:<br/>" + 
                repeat( "&nbsp;", 10 ) + " *" + fst.get().gemarkung.get().label() + " " + fst.get().zaehlerNenner.get() + "*<br/>" +
                repeat( "&nbsp;", 10 ) + " *" + origin.get().besitzer().anzeigename() + "*\n\n" +
                "an: ");
        
        // table viewer
        final WaldbesitzerTableViewer viewer = new WaldbesitzerTableViewer( uow.get(), body, Collections.EMPTY_LIST, SWT.BORDER );
        getContext().propagate( viewer );
        
        // Waldbesitzer selektiert
        viewer.addSelectionChangedListener( new ISelectionChangedListener() {
            @Override
            public void selectionChanged( SelectionChangedEvent ev ) {
                if (!viewer.getSelected().isEmpty()) {
                    selected = viewer.getSelected().get( 0 );
                    fab.setVisible( selected != null );
                }
            }
        });
        
        // searchField
        FulltextIndex fulltext = WbvRepository.fulltextIndex();
        EntitySearchField search = new EntitySearchField<Waldbesitzer>( body, fulltext, uow.get(), Waldbesitzer.class ) {
            @Override
            protected void doRefresh() {
//                if (revier.get() != null) {
//                    Waldbesitzer wb = Expressions.template( Waldbesitzer.class, WbvRepository.repo() );
//                    Flurstueck fl = Expressions.template( Flurstueck.class, WbvRepository.repo() );
//                    
//                    List<Gemarkung> gemarkungen = revier.get().gemarkungen;
//                    Gemarkung[] revierGemarkungen = gemarkungen.toArray( new Gemarkung[gemarkungen.size()] );
//                    query.andWhere( Expressions.anyOf( wb.flurstuecke, 
//                                    Expressions.isAnyOf( fl.gemarkung, revierGemarkungen ) ) );
//                }
                // SelectionEvent nach refresh() verhindern
                viewer.clearSelection();
                
                ResultSet<Waldbesitzer> results = query.execute();
                viewer.setInput( results );
            }
        };
        search.searchOnEnter.set( false );
        search.getText().setText( "Hedwig" );
        
        search.searchOnEnter.set( true );
        search.getText().setFocus();
        new FulltextProposal( fulltext, search.getText() );
        
        // layout
        int displayHeight = UIUtils.sessionDisplay().getBounds().height;
        int tableHeight = (displayHeight - (2*50) - 75 - 70 - 140);  // margins, searchbar, toolbar+banner
        on( infoSection.getControl() ).fill().height( 140 ).noBottom();
        on( search.getControl() ).fill().height( 27 ).top( infoSection.getControl() ).noBottom();
        on( viewer.getTable() ).fill().top( search.getControl() ).height( tableHeight ).width( 300 );
    }
    
}
