/* Canto Compiler and Runtime Engine
 * 
 * Expression.java
 *
 * Copyright (c) 2024-2025 by cantolang.org
 * All rights reserved.
 */

package canto.lang;


/**
 * 
 */
public abstract class Expression extends Construction {

    /**
     * @param source
     */
    public Expression() {
        super();
    }

    @Override
    public int getNumChildren() {
        return 0;
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
        return true;
    }

    @Override
    public boolean isDefinition() {
        return false;
    }

}



