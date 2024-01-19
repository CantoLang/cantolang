/* Canto Compiler and Runtime Engine
 * 
 * MultiBlockDefinition.java
 *
 * Copyright (c) 2024 by cantolang.org
 * All rights reserved.
 */

package canto.lang;

import java.util.List;

/**
 * 
 */
public class MultiBlockDefinition extends Definition {

    protected MultiBlockDefinition(Name name, List<Block> constructions) {
        super(name, constructions);
    }

    @Override
    public Value instantiate(Context context) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public boolean isPrimitive() {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public boolean isStatic() {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public boolean isDynamic() {
        // TODO Auto-generated method stub
        return false;
    }

}
