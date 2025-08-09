/* Canto Compiler and Runtime Engine
 * 
 * ExternalTableBuilder.java
 *
 * Copyright (c) 2018-2025 by cantolang.org
 * All rights reserved.
 */

package canto.lang;


/**
 * Facade class to make a Java object available as a Canto definition.
 */
public class ExternalTableBuilder extends TableBuilder {

    private ExternalDefinition externalDef = null;

    public ExternalTableBuilder(ExternalCollectionDefinition collectionDef, ExternalDefinition externalDef) {
        super(collectionDef);
        this.externalDef = externalDef; 
    }

}


