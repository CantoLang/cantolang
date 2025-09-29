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
    
    private Site site = null;
    
    public CompilationUnit() {
        super();
    }
    
    public void addSite(Site site) {
        if (this.site == null) {
            this.site = site;
        } else if (site.getName().equals(this.site.getName())) {
            this.site.mergeSite(site);
        } else {
            throw new NameMismatchException("Site name mismatch in compilation unit: " + this.site.getName() + " and " + this.site.getName());
        }
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
