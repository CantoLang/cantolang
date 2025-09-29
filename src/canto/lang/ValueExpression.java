/* Canto Compiler and Runtime Engine
 * 
 * ValueExpression.java
 *
 * Copyright (c) 2018-2025 by cantolang.org
 * All rights reserved.
 */

package canto.lang;

import canto.util.SingleItemList;

/**
 * A ValueExpression is a generic value container.
 *
 * @author Michael St. Hippolyte
 * @version $Revision: 1.8 $
 */
public class ValueExpression extends Expression {

    public ValueExpression() {
        super();
    }
    
    public ValueExpression(CantoNode node) {
        super();
        ListNode<CantoNode> list = new ListNode<CantoNode>(new SingleItemList<CantoNode>(node));
        addChildren(list);
    }
    
    private ValueExpression(ValueExpression expression) {
        super(expression);
    }

    public Object generateData(Context context, Definition def) {
        // not the right logic, just to get things rolling
        int n = getNumChildren();
        for (int i = 0; i < n; i++) {
            CantoNode node = getChild(i);
            if (node instanceof Value) {
                return node;
            } else if (node instanceof ValueGenerator) {
                Value value = ((ValueGenerator) node).getValue(context);
                if (value != null) {
                    return value;
                }
            }
        }
        return NullValue.NULL_VALUE;
    }

    public Type getType(Context context, boolean generate) {
        int n = getNumChildren();
        if (n > 0) {
            return getChildType(context, generate, 0);
        } else {
            return DefaultType.TYPE;
        }
    }

    public Expression resolveExpression(Context context) {
        ValueExpression resolvedExpression = new ValueExpression(this);
        resolvedExpression.resolveChildrenInPlace(context);
        return resolvedExpression;
    }
}
