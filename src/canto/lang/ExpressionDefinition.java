/* Canto Compiler and Runtime Engine
 * 
 * ExpressionDefinition.java
 *
 * Copyright (c) 2024 by cantolang.org
 * All rights reserved.
 */

package canto.lang;

/**
 * 
 */
public class ExpressionDefinition extends ConstructiveDefinition {

    /**
     * @param name
     * @param construction
     */
    public ExpressionDefinition(Name name, Construction construction) {
        super(name, construction);
    }

    @Override
    public boolean isElementDefinition() {
        return true;
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
        return false;
    }

}
