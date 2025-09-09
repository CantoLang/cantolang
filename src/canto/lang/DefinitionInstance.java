/* Canto Compiler and Runtime Engine
 * 
 * DefinitionInstance.java
 *
 * Copyright (c) 2018-2025 by cantolang.org
 * All rights reserved.
 */

package canto.lang;

import java.util.List;

/**
 * DefinitionInstance is a wrapper for a definition with arguments and indexes.
 */

public class DefinitionInstance {
    public Definition def;
    public ArgumentList args;
    public IndexList indexes;
        
    public DefinitionInstance(Definition def, ArgumentList args, IndexList indexes) {
        this.def = def;
        this.args = args;
        this.indexes = indexes;
    }
}	    
