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

    public Expression(Expression expression) {
        super();
        copyChildren(expression);
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

    
    abstract public Type getType(Context context, boolean generate);

    abstract public Expression resolveExpression(Context context);
    
    protected void resolveChildrenInPlace(Context context) {
        int numChildren = getNumChildren();
        for (int i = 0; i < numChildren; i++) {
            CantoNode node = getChild(i);
            if (node instanceof Instantiation && !(node instanceof ResolvedInstance)) {
                setChild(i, new ResolvedInstance((Instantiation) node, context, true));
            } else if (node instanceof Expression) {
                setChild(i, ((Expression) node).resolveExpression(context));
            }
        }
    }

    protected Value getChildValue(Context context, int n) throws Redirection {
        Object child = getChild(n);
        if (child instanceof Value) {
            return (Value) child;
        } else {
            return ((ValueGenerator) child).getValue(context);
        }
    }

    protected Type getChildType(Context context, boolean generate, int n) {
        Object child = getChild(n);
        if (child instanceof Construction) {
            return ((Construction) child).getType(context, generate);
        } else {
            try {
                Value value = getChildValue(context, n);
                if (value instanceof PrimitiveValue) {
                    return ((PrimitiveValue)value).getType();
                }
            } catch (Redirection r) {
                ;
            }
        }
        return DefaultType.TYPE;
    }

}



