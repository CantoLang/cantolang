/* Canto Compiler and Runtime Engine
 * 
 * DefinitionInstance.java
 *
 * Copyright (c) 2018-2026 by cantolang.org
 * All rights reserved.
 */

package canto.lang;


/**
 * DefinitionInstance is a wrapper for a definition with arguments and indexes.
 */

public class DefinitionInstance {
    public Definition def;
    public ConstructionList args;
    public IndexList indexes;
        
    public DefinitionInstance(Definition def, ConstructionList args, IndexList indexes) {
        this.def = def;
        this.args = args;
        this.indexes = indexes;
    }
}	    
