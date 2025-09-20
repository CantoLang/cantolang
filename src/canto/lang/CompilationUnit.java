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
public class CompilationUnit extends CantoNode {
    private static final Log LOG = Log.getLogger(CompilationUnit.class);
    
    private Site site;
    
    public CompilationUnit(Site site) {
        super();
        this.site = site;
    }
    
    public Site getSite() {
        return site;
    }

    @Override
    public boolean isPrimitive() {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public boolean isStatic() {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public boolean isDynamic() {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public boolean isDefinition() {
        // TODO Auto-generated method stub
        return false;
    }

}
