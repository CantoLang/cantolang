/* Canto Compiler and Runtime Engine
 * 
 * BinaryOperator.java
 *
 * Copyright (c) 2024 by cantolang.org
 * All rights reserved.
 */

package canto.lang;

/**
 * 
 */
abstract public class BinaryOperator extends Operator {

    protected BinaryOperator() {}
    
    public abstract Value operate(Value val1, Value val2);
    
    public Value operate(Value[] vals) {
        return operate(vals[0], vals[1]);
    }
    
}
