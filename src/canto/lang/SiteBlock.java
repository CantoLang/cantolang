/* Canto Compiler and Runtime Engine
 * 
 * Block.java
 *
 * Copyright (c) 2025 by cantolang.org
 * All rights reserved.
 */

package canto.lang;

import java.util.List;
import java.util.stream.Collectors;

import canto.util.EmptyList;

/**
 * 
 */
public class SiteBlock extends CodeBlock {

    private List<CantoNode> directives;
    
    protected SiteBlock() {
        super();
        this.directives = new EmptyList<CantoNode>();
    }
    
    public SiteBlock(List<CantoNode> directives, List<CantoNode> defs) {
        super(defs);
        this.directives = directives;
    }

}
