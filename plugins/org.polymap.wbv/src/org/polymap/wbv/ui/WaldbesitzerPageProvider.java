/* 
 * polymap.org
 * Copyright (C) 2014, Polymap GmbH. All rights reserved.
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

/**
 * 
 *
 * @author <a href="http://www.polymap.de">Falko Bräutigam</a>
 */
public class WaldbesitzerPageProvider {
//        implements IFormPageProvider {
//
//    private static Log log = LogFactory.getLog( WaldbesitzerPageProvider.class );
//
//
//    public List<IFormPage> addPages( FormEditor formEditor, Feature feature ) {
//        log.debug( "addPages(): feature= " + feature );
//        List<IFormPage> result = new ArrayList();
//        if (feature.getType().getName().getLocalPart().equalsIgnoreCase( "waldbesitzer" )) {
//            result.add( new BaseFormEditorPage( feature, formEditor.getFeatureStore() ) );
//        }
//        return result;
//    }
//
//    
//    /**
//     * The standard page.
//     */
//    public static class BaseFormEditorPage
//            extends DefaultFormPage
//            implements IFormPage {
//
//        private Waldbesitzer            entity;
//
////        private WbvRepository           repo = WbvRepository.instance();
//        
//
//        protected BaseFormEditorPage( Feature feature, FeatureStore fs ) {
//            super( "Basisdaten", "Basisdaten", feature, fs );
////            this.entity = repo.entityForState( Waldbesitzer.class, feature );
//        }
//
//
//        @Override
//        public void createFormContent( IFormPageSite _site ) {
////            log.debug( "createFormContent(): feature= " + feature );
////
////            super.createFormContent( _site );
////            //_site.setEditorTitle( "Waldbesitzer: " + entity.name.get() );
////            
////            //Waldbesitzer template = repo.infoOf( Waldbesitzer.class ).getTemplate();
////
////            //IFormToolkit tk = _site.getToolkit();
////            //Section section = newSection( "Basisdaten", false, null );
////
////            // name
////            newFormField( entity.name.getInfo().getName() )
////                    .setLabel( "Name" )
////                    .setParent( _site.getPageBody() )
////                    .setField( new StringFormField() )
////                    .setValidator( new NullValidator() )
////                    .create();
////            //
////            newFormField( entity.vorname.getInfo().getName() )
////                    .setParent( _site.getPageBody() )
////                    .create();
//        }
//        
//    }
//    
}
