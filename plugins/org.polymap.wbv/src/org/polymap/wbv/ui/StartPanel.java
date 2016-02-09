/*
 * Copyright (C) 2014-2015, Falko Bräutigam. All rights reserved.
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

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import java.io.IOException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Menu;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.ActionContributionItem;
import org.eclipse.jface.action.ContributionItem;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.SelectionChangedEvent;

import org.eclipse.rap.rwt.RWT;
import org.eclipse.rap.rwt.client.service.JavaScriptExecutor;
import org.eclipse.rap.rwt.client.service.UrlLauncher;
import org.eclipse.rap.rwt.service.SettingStore;

import org.polymap.core.mapeditor.ContextMenuSite;
import org.polymap.core.mapeditor.IContextMenuContribution;
import org.polymap.core.mapeditor.IContextMenuProvider;
import org.polymap.core.runtime.i18n.IMessages;
import org.polymap.core.ui.FormDataFactory;
import org.polymap.core.ui.FormLayoutFactory;
import org.polymap.core.ui.UIUtils;

import org.polymap.rhei.batik.BatikPlugin;
import org.polymap.rhei.batik.Context;
import org.polymap.rhei.batik.PanelIdentifier;
import org.polymap.rhei.batik.app.SvgImageRegistryHelper;
import org.polymap.rhei.batik.toolkit.IPanelSection;
import org.polymap.rhei.batik.toolkit.PriorityConstraint;
import org.polymap.rhei.batik.toolkit.Snackbar.Appearance;
import org.polymap.rhei.field.PicklistFormField;
import org.polymap.rhei.field.PlainValuePropertyAdapter;
import org.polymap.rhei.form.IFormPageSite;
import org.polymap.rhei.form.batik.BatikFormContainer;
import org.polymap.rhei.fulltext.FulltextIndex;
import org.polymap.rhei.fulltext.ui.EntitySearchField;
import org.polymap.rhei.fulltext.ui.FulltextProposal;
import org.polymap.rhei.um.ui.LoginPanel;
import org.polymap.rhei.um.ui.LoginPanel.LoginForm;

import org.polymap.model2.query.Expressions;
import org.polymap.model2.query.ResultSet;
import org.polymap.model2.runtime.UnitOfWork;
import org.polymap.rap.updownload.download.DownloadService;
import org.polymap.wbv.Messages;
import org.polymap.wbv.WbvPlugin;
import org.polymap.wbv.model.Revier;
import org.polymap.wbv.model.Waldbesitzer;
import org.polymap.wbv.model.WbvRepository;
import org.polymap.wbv.ui.reports.DownloadableReport;
import org.polymap.wbv.ui.reports.DownloadableReport.OutputType;
import org.polymap.wbv.ui.reports.Report101;
import org.polymap.wbv.ui.reports.Report102;
import org.polymap.wbv.ui.reports.Report103;
import org.polymap.wbv.ui.reports.Report105;
import org.polymap.wbv.ui.reports.Report106;
import org.polymap.wbv.ui.reports.Report106b;
import org.polymap.wbv.ui.reports.Report106c;
import org.polymap.wbv.ui.reports.WbvReport;

/**
 * 
 * 
 * @author <a href="http://www.polymap.de">Falko Bräutigam</a>
 */
public class StartPanel
        extends WbvPanel {

    private static Log log = LogFactory.getLog( StartPanel.class );

    public static final PanelIdentifier     ID  = new PanelIdentifier( "start" );

    private static final IMessages          i18n = Messages.forPrefix( "StartPanel" );
    
    private UnitOfWork                      uow = WbvRepository.unitOfWork();

    /** Der selektierte {@link Waldbesitzer}. */
    private Context<Waldbesitzer>           selected;

    private WbvMapViewer                    map;


    @Override
    public boolean wantsToBeShown() {
        return getSite().getPath().size() == 1;
    }

    
    @Override
    public void createContents( Composite parent ) {
        getSite().setTitle( "Login" );
        getSite().setPreferredWidth( 500 ); // table viewer
        createLoginContents( parent );
    }
    
    
    protected void createLoginContents( final Composite parent ) {
        // welcome
        site().title.set( i18n.get( "loginTitle" ) );
        
        IPanelSection welcome = tk().createPanelSection( parent, "Hinweise" /*i18n.get( "loginTitle" )*/ );
        welcome.addConstraint( new PriorityConstraint( 10 ), WbvPlugin.MIN_COLUMN_WIDTH );
        String t = i18n.get( "welcomeText" );
        tk().createFlowText( welcome.getBody(), t );

//        tk().createSnackbar( Appearance.FadeIn, "Snackbar!", 
//                new ActionItem( null ).text.put( "Apply" ) );

        // login
        IPanelSection section = tk().createPanelSection( parent, "Anmelden", SWT.BORDER );
        section.addConstraint( new PriorityConstraint( 0 ), WbvPlugin.MIN_COLUMN_WIDTH );

        LoginForm loginForm = new LoginPanel.LoginForm( getContext(), getSite(), user ) {
            
            SettingStore        settings = RWT.getSettingStore();
            
            @Override
            public void createFormContents( IFormPageSite site ) {
                Map<String,Revier> reviere = new TreeMap( Revier.all.get() );
                reviere.put( Revier.UNKNOWN.name, Revier.UNKNOWN );

                String cookieRevier = settings.getAttribute( WbvPlugin.ID + ".revier" );
                Revier preSelected = cookieRevier != null ? Revier.all.get().get( cookieRevier ) : null;
                
                site.newFormField( new PlainValuePropertyAdapter( "revier", preSelected ) )
                        .field.put( new PicklistFormField( reviere ) )
                        .label.put( i18n.get( "revier" ) )
                        .tooltip.put( i18n.get( "revierTip" ) )
                        .create();
                super.createFormContents( site );
            }
            
            @Override
            protected boolean login( String name, String passwd ) {
                if (super.login( name, passwd )) {
                    getContext().setUserName( username );
                    
                    // Revier
//                    Revier r = null; //formSite.getFieldValue( "revier" );
//                    revier.set( r );
                    Revier r = formSite.getFieldValue( "revier" );
                    if (r != Revier.UNKNOWN) {
                        revier.set( r );
                    }
                    try {
                        if (r != null) {
                            settings.setAttribute( WbvPlugin.ID + ".revier", r.name );
                        }
                    }
                    catch (IOException e) {
                        log.warn( "", e );
                    }

                    for (Control child : parent.getChildren()) {
                        child.dispose();
                    }
                    createMainContents( parent );
                    parent.layout( true );
                    return true;
                }
                else {
                    site().toolkit().createSnackbar( Appearance.FadeIn, "Nutzername oder Passwort ist nicht korrekt." );
                    return false;
                }
            }
        };
        loginForm.setShowStoreCheck( true );
        //loginForm.setShowRegisterLink( false );
        //loginForm.setShowLostLink( true );
        new BatikFormContainer( loginForm ).createContents( section );
    }
    
    
    protected void createMainContents( Composite parent ) {
        site().title.set( i18n.get( "title", "--" ) );
        
        Composite body = parent;
        body.setLayout( FormLayoutFactory.defaults().spacing( 5 ).margins( 0, 10 ).create() );
        
        final WaldbesitzerTableViewer viewer = new WaldbesitzerTableViewer( uow, body, Collections.EMPTY_LIST, SWT.BORDER );
        getContext().propagate( viewer );
        // waldbesitzer öffnen
        viewer.addSelectionChangedListener( new ISelectionChangedListener() {
            @Override
            public void selectionChanged( SelectionChangedEvent ev ) {
                if (!viewer.getSelected().isEmpty()) {
                    selected.set( viewer.getSelected().get( 0 ) );
                    getContext().openPanel( getSite().getPath(), WaldbesitzerPanel.ID );
                }
            }
        });

        // waldbesitzer anlegen
        Button createBtn = tk().createButton( body, "Neu", SWT.PUSH );
        createBtn.setToolTipText( "Einen neuen Waldbesitzer anlegen" );
        createBtn.addSelectionListener( new SelectionAdapter() {
            @Override
            public void widgetSelected( SelectionEvent ev ) {
                selected.set( null );
                getContext().openPanel( getSite().getPath(), WaldbesitzerPanel.ID );
            }
        });

        // exit app
        Button exitBtn = tk().createButton( body, null, SWT.PUSH );
        exitBtn.setImage( BatikPlugin.images().svgImage( "close.svg", SvgImageRegistryHelper.WHITE24 ) );
        exitBtn.setToolTipText( "Anwendung verlassen und neu anmelden" );
        exitBtn.addSelectionListener( new SelectionAdapter() {
            @Override
            public void widgetSelected( SelectionEvent ev ) {
                RWT.getClient().getService( JavaScriptExecutor.class ).execute( "window.location.reload(true);" );
            }
        });

        // reports
        final List<WbvReport> reportsMap = new ArrayList();
        reportsMap.add( getContext().propagate( new Report101() ) );
        reportsMap.add( getContext().propagate( new Report102() ) );
        reportsMap.add( getContext().propagate( new Report103() ) );
        reportsMap.add( getContext().propagate( new Report105() ) );
        reportsMap.add( getContext().propagate( new Report106() ) );
        reportsMap.add( getContext().propagate( new Report106b() ) );
        reportsMap.add( getContext().propagate( new Report106c() ) );
        
        final Combo reports = new Combo( body, SWT.BORDER | SWT.READ_ONLY );
        reports.add( "Auswertung wählen..." );
        for (DownloadableReport report : reportsMap) {
            reports.add( report.getName() );
        }
        reports.setVisibleItemCount( 8 );
        reports.select( 0 );
        reports.addSelectionListener( new SelectionAdapter() {
            @Override
            public void widgetSelected( SelectionEvent ev ) {
                try {
                    if (reports.getSelectionIndex() > 0) {
                        WbvReport report = reportsMap.get( reports.getSelectionIndex()-1 );
                        report.setOutputType( OutputType.PDF );
                        if (report instanceof Report103) {
                            report.setEntities( viewer.getInput() );
                        }
                        else {
                            ResultSet<Waldbesitzer> all = uow.query( Waldbesitzer.class )
                                    .where( revier.isPresent() ? revier.get().waldbesitzerFilter.get() : Expressions.TRUE )
                                    .execute();
                            report.setEntities( all );
                        }
                        String url = DownloadService.registerContent( report );

                        UrlLauncher launcher = RWT.getClient().getService( UrlLauncher.class );
                        launcher.openURL( url );
                        //ExternalBrowser.open( "download_window", url, ExternalBrowser.NAVIGATION_BAR | ExternalBrowser.STATUS );
                        reports.select( 0 );
                    }
                }
                catch (Exception e) {
                    throw new RuntimeException( e );
                }
            }
        });
            
        // searchField
        FulltextIndex fulltext = WbvRepository.fulltextIndex();
        EntitySearchField search = new EntitySearchField<Waldbesitzer>( body, fulltext, uow, Waldbesitzer.class ) {
            @Override
            protected void doSearch( String _queryString ) throws Exception {
                super.doSearch( _queryString );
                queryString.set( _queryString );
            }
            @Override
            protected void doRefresh() {
                // nach revier filtern
                revier.ifPresent( r-> query.andWhere( r.waldbesitzerFilter.get() ) );

                // SelectionEvent nach refresh() verhindern
                viewer.clearSelection();
                
                ResultSet<Waldbesitzer> results = query.execute();
                viewer.setInput( results );
                
                site().title.set( i18n.get( "title", results.size() ) );
            }
        };

        if ("falko".equals( System.getProperty( "user.name" ) )) {
            search.searchOnEnter.set( false );
            search.getText().setText( "Stadtverwaltung" );
        }

        search.searchOnEnter.set( true );
        search.getText().setFocus();
        search.getControl().moveAbove( null );
        new FulltextProposal( fulltext, search.getText() ).activationDelayMillis.put( 250 );
        
        // layout
        int displayHeight = UIUtils.sessionDisplay().getBounds().height;
        int tableHeight = (displayHeight - (2*50) - 75 - 70);  // margins, searchbar, toolbar+banner 
        exitBtn.setLayoutData( FormDataFactory.filled().clearRight().clearBottom().height( 28 ).width( 30 ).create() );
        createBtn.setLayoutData( FormDataFactory.defaults().left( exitBtn ).create() );
        reports.setLayoutData( FormDataFactory.filled().left( createBtn ).noBottom().height( 26 ).right( 50 ).create() );
        search.getControl().setLayoutData( FormDataFactory.filled().height( 27 ).bottom( viewer.getTable() ).left( reports ).create() );
        viewer.getTable().setLayoutData( FormDataFactory.filled()
                .top( createBtn ).height( tableHeight ).width( 300 ).create() );
        
//        // map
//        IPanelSection karte = tk.createPanelSection( parent, null );
//        karte.addConstraint( new PriorityConstraint( 5 ) );
//        karte.getBody().setLayout( ColumnLayoutFactory.defaults().columns( 1, 1 ).create() );
//
//        try {
//            map = new WbvMapViewer( getSite() );
//            map.createContents( karte.getBody() )
//                    .setLayoutData( new ColumnLayoutData( SWT.DEFAULT, tableHeight + 35 ) );
//                    //.setLayoutData( FormDataFactory.filled().height( tableHeight + 35 ).create() );
//        }
//        catch (Exception e) {
//            throw new RuntimeException( e );
//        }
//    
//        // context menu
//        map.getContextMenu().addProvider( new WaldflaechenMenu() );
//        map.getContextMenu().addProvider( new IContextMenuProvider() {
//            @Override
//            public IContextMenuContribution createContribution() {
//                return new FindFeaturesMenuContribution() {
//                    @Override
//                    protected void onMenuOpen( FeatureStore fs, Feature feature, ILayer layer ) {
//                        log.info( "Feature: " + feature );
//                    }
//                };            
//            }
//        });
    }


    class WaldflaechenMenu
            extends ContributionItem
            implements IContextMenuContribution, IContextMenuProvider {

        @Override
        public boolean init( ContextMenuSite site ) {
            return true;
        }

        @Override
        public void fill( Menu menu, int index ) {
            Action action = new Action( "Test", Action.AS_PUSH_BUTTON ) {
                public void run() {
                }            
            };
            new ActionContributionItem( action ).fill( menu, index );
        }

        @Override
        public String getMenuGroup() {
            return GROUP_HIGH;
        }

        public IContextMenuContribution createContribution() {
            return this;
        }

    }
    
}
