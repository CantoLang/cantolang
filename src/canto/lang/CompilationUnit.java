/* Canto Compiler and Runtime Engine
 * 
 * CompilationUnit.java
 *
 * Copyright (c) 2024-2025 by cantolang.org
 * All rights reserved.
 */

package canto.lang;

import canto.runtime.Log;

/**
 * 
 */
public class CompilationUnit extends BlockDefinition {
    private static final Log LOG = Log.getLogger(CompilationUnit.class);
    
    public CompilationUnit(Name name, Block block) {
        super(name, block);
    }

}
