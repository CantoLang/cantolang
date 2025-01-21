/* Canto Compiler and Runtime Engine
 * 
 * CantoSourceFile.java
 *
 * Copyright (c) 2025 by cantolang.org
 * All rights reserved.
 */

package canto.lang;


/**
 * 
 */
public class CantoSourceFile extends CantoNode {

    private String source;    
    
    protected CantoSourceFile(Name name) {
        super();
    }

    public void setSource(String source) {
        this.source = source;
    }
    
    public String getSource() {
        return source;
    }

    @Override
    public boolean isPrimitive() {
        return false;
    }

    @Override
    public boolean isStatic() {
        return false;
    }

    @Override
    public boolean isDynamic() {
        return false;
    }

    @Override
    public boolean isDefinition() {
        return false;
    }

}
