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
public class CodeBlock extends Block {

    private List<Definition> defs;
    
    protected CodeBlock() {
        super();
        this.defs = new EmptyList<Definition>();
    }
    
    protected CodeBlock(List<CantoNode> children) {
        super(children);
        this.defs = ExtractDefinitions(children);
    }

    private static List<Definition> ExtractDefinitions(List<CantoNode> children) {
        List<Definition> defs = children.stream().filter(c -> c instanceof Definition).map(c -> (Definition) c).collect(Collectors.toList());        
        return defs;
    }
}
