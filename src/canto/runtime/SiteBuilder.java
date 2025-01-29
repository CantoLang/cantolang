/* Canto Compiler and Runtime Engine
 * 
 * SiteBuilder.java
 *
 * Copyright (c) 2024-2025 by cantolang.org
 * All rights reserved.
 */

package canto.runtime;

import canto.lang.CantoNode;
import canto.lang.CompilationUnit;
import canto.lang.Core;
import canto.parser.CantoParser;
import canto.parser.CantoParserBaseVisitor;

/**
 * 
 */
public class SiteBuilder extends CantoParserBaseVisitor<CantoNode> {

    private static final Log LOG = Log.getLogger(SiteBuilder.class);
    
    private Exception exception = null;
    protected Core core;

    public SiteBuilder(Core core) {
        this.core = core;
    }

    public Exception getException() {
        return exception;
    }

    public CompilationUnit build(CantoParser parser) {

        try {
            return (CompilationUnit) parser.compilationUnit().accept(this);
        } catch (Exception e) {
            exception = e;
            LOG.error("Error building site", e);
        }
        
        return null;
    }

    
    
    

}
