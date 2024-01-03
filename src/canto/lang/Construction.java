/* Canto Compiler and Runtime Engine
 * 
 * Construction.java
 *
 * Copyright (c) 2018-2024 by cantolang.org
 * All rights reserved.
 */

package canto.lang;

import canto.runtime.Context;

/**
 * A construction is a Canto statement which generates data.
 *
 * @author Michael St. Hippolyte
 */

public abstract class Construction extends Node {

    protected Construction(String source) {
        super(source);
    }

    protected Construction(String source, String docComment) {
        super(source);
    }

    public abstract Object construct(Context context);
    
    @Override
    public boolean isDefinition() {
        return false;
    }

}
