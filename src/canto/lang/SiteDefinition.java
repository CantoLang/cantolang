/* Canto Compiler and Runtime Engine
 * 
 * SiteDefinition.java
 *
 * Copyright (c) 2024 by cantolang.org
 * All rights reserved.
 */

package canto.lang;

/**
 * 
 */
public class SiteDefinition extends BlockDefinition {

    protected SiteDefinition(Name name, Block block) {
        super(name, block);
    }

    @Override
    public boolean isDynamic() {
        return true;
    }

}
