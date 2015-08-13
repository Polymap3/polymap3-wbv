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
package org.polymap.wbv.model.fulltext;

/**
 * 
 * @deprecated
 * @author <a href="http://www.polymap.de">Falko Bräutigam</a>
 */
public class ZaehlerNennerQueryDecorator {
//        extends QueryDecorator {
//
//    private static Log log = LogFactory.getLog( ZaehlerNennerQueryDecorator.class );
//    
//    public static final Pattern         zaehlerNenner = Pattern.compile( "([0-9]*)/([a-zA-Z0-9]*)" );
//
//    public ZaehlerNennerQueryDecorator( FullTextIndex next ) {
//        super( next );
//    }
//
//
//    @Override
//    public Iterable<JSONObject> search( String query, int maxResults ) throws Exception {
//        Matcher matcher = zaehlerNenner.matcher( query );
//        if (matcher.find()) {
//            // zaehler/nenner Suche (complex query -> without analyser)
//            String zaehler = matcher.group( 1 );
//            String nenner = matcher.group( 2 );
//            String znQuery = FlurstueckTransformer.ZAEHLER + ":" + zaehler;
//            if (nenner.length() > 0) {
//                    znQuery += " AND " + FlurstueckTransformer.NENNER + ":" + nenner;
//            }
//            Iterable<JSONObject> znResults = next.search( znQuery, maxResults*2 );
//            
//            // andere Suche
//            String other = matcher.replaceFirst( "" );
//            if (other.length() > 1) {
//                Iterable<JSONObject> otherResults = next.search( other, maxResults );
//                Set<String> otherIds = StreamIterable.of( otherResults )
//                        .stream()
//                        .map( json -> json.getString( FlurstueckTransformer.ID ) )
//                        .collect( Collectors.toSet() );
//                
//                // zusammenbauen
//                return StreamIterable.of( znResults )
//                        .stream()
//                        .filter( json -> otherIds.contains( json.getString( FlurstueckTransformer.ID ) ) )
//                        .collect( Collectors.toList() );
//            }
//            else {
//                return znResults;
//            }
//        }
//        else {
//            return next.search( query, maxResults );
//        }
//    }
    
}
