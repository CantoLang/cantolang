/* Canto Compiler and Runtime Engine
 * 
 * Operator.java
 *
 * Copyright (c) 2024 by cantolang.org
 * All rights reserved.
 */

package canto.lang;

public abstract class Operator {

    protected Operator() {}
    
    public abstract Value operate(Value[] vals);
    

}
