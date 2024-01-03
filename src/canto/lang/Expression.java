/* Canto Compiler and Runtime Engine
 * 
 * Expression.java
 *
 * Copyright (c) 2024 by cantolang.org
 * All rights reserved.
 */

package canto.lang;

import canto.runtime.Context;

/**
 * 
 */
public abstract class Expression extends Construction {

    /**
     * @param source
     */
    public Expression(String source) {
        super(source);
    }

    /**
     * @param source
     * @param docComment
     */
    public Expression(String source, String docComment) {
        super(source, docComment);
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

class BinaryExpression extends Expression {

    private BinaryOperator operator;
    private Expression left;
    private Expression right;
    
    public BinaryExpression(String source) {
        super(source);
        // TODO Auto-generated constructor stub
    }

    @Override
    public Object construct(Context context) {
        return null;
    }

    
}


