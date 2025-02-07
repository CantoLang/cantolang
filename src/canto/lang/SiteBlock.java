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

    private List<Directive> directives;
    
    protected SiteBlock() {
        super();
        this.directives = new EmptyList<Directive>();
    }
    
    public SiteBlock(List<CantoNode> children) {
        super(children);
        this.directives = ExtractDirectives(children);
    }

    private static List<Directive> ExtractDirectives(List<CantoNode> children) {
        List<Directive> directives = children.stream().filter(c -> c instanceof Directive).map(c -> (Directive) c).collect(Collectors.toList());        
        return directives;
    }
}
