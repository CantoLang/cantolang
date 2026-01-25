/* Canto Compiler and Runtime Engine
 * 
 * ChoiceExpression.java
 *
 * Copyright (c) 2018-2025 by cantolang.org
 * All rights reserved.
 */

package canto.lang;

import canto.runtime.Log;

/**
 * A ChoiceExpression is a construction based on the question mark operator.
 */
public class ChoiceExpression extends Expression {
    private static final Log LOG = Log.getLogger(ChoiceExpression.class);

    public ChoiceExpression() {
        super();
    }

    public ChoiceExpression(CantoNode cond, CantoNode ifTrue, CantoNode ifFalse) {
        super();
        setChild(0, cond);
        setChild(1, ifTrue);
        setChild(2, ifFalse);
    }

    private ChoiceExpression(ChoiceExpression expression) {
        super(expression);
    }

    public Object generateData(Context context, Definition def) throws Redirection {
        Value test = getChildValue(context, 0);
        if (test.getBoolean()) {
            return getChildValue(context, 1);
        } else {
            return getChildValue(context, 2);
        }
    }

    /** Return the construction that this choice resolves to.
     */
    public Construction getUltimateConstruction(Context context) {
        Value test = getChildValue(context, 0);
        Object resolvedObj;
        if (test.getBoolean()) {
            resolvedObj = getChild(1);
        } else {
            resolvedObj = getChild(2);
        }
        if (resolvedObj instanceof Construction) {
            return (Construction) resolvedObj;
        }
        return this;
    }

    public Type getType(Context context, boolean generate) {
        try {
            Value test = getChildValue(context, 0);
            if (test.getBoolean()) {
                return getChildType(context, generate, 1);
            } else {
                return getChildType(context, generate, 2);
            }
        } catch (Redirection r) {
            return DefaultType.TYPE;
        }
    }

    public String toString(String prefix) {
        StringBuffer sb = new StringBuffer(prefix);
        sb.append('(');
        sb.append(getChild(0).toString());
        sb.append(" ? ");
        sb.append(getChild(1).toString());
        sb.append(" : ");
        sb.append(getChild(2).toString());
        sb.append(')');
        return sb.toString();
    }

    public Expression resolveExpression(Context context) {
        ChoiceExpression resolvedExpression = new ChoiceExpression(this);
        resolvedExpression.resolveChildrenInPlace(context);
        return resolvedExpression;
    }
}
