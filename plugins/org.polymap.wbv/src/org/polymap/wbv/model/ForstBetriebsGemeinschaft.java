/*
 * polymap.org Copyright (C) 2014 Polymap GmbH. All rights reserved.
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
package org.polymap.wbv.model;

import org.polymap.core.model2.Entity;
import org.polymap.core.model2.Property;
import org.polymap.core.model2.store.feature.SRS;

@SRS("EPSG:4326")
public class ForstBetriebsGemeinschaft extends Entity {

    /**
     * Der Name der Forstbetriebsgemeinschaft
     */
    public Property<String> name;

    /**
     * Rechtsform nach BGB
     */
    public Property<String> rechtsForm;
//  public Property<RechtsForm> rechtsForm;
    
    public enum RechtsForm {
    	/** Eingetragener Verein */
    	EV, 

    	/** Wirtschaftlicher Verein */
    	WV, 
    	
    	/** Eingetragene Erwerbs- und Wirtschaftsgenossenschaft */
    	EG, 
    	
    	/** Gesellschaft mit beschränkter Haftung */
    	GMBH, 
    	
    	/** Aktiengesellschaft */
    	AG, 
    	
    	/** Kommanditgesellschaft auf Aktien */
    	KG
    }
}
