/* Canto Compiler and Runtime Engine
 * 
 * BlockDefinition.java
 *
 * Copyright (c) 2024 by cantolang.org
 * All rights reserved.
 */

package canto.lang;

/**
 * 
 */
public class BlockDefinition extends ConstructiveDefinition {

    private Block block;
    
    protected BlockDefinition(Name name, Block block) {
        super(name, block);
        this.block = block;
    }

    @Override
    public int getNumChildren() {
        return block.getNumChildren();
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
