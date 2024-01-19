/* Canto Compiler and Runtime Engine
 * 
 * ConstructiveDefinition.java
 *
 * Copyright (c) 2018-2024 by cantolang.org
 * All rights reserved.
 */

package canto.lang;

/**
 * 
 */
abstract public class ConstructiveDefinition extends Definition {

    /** The contents of this definition. */
    protected Construction construction;

    protected ConstructiveDefinition(Name name, Construction construction) {
        super(name, construction);
        this.construction = construction;
    }

    public Value instantiate(Context context) {
        Value val = construction.construct(context);
        
        return val;
    }

}

