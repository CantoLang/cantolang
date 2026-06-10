/* Canto Compiler and Runtime Engine
 * 
 * CantoBlock.java
 *
 * Copyright (c) 2018-2026 by cantolang.org
 * All rights reserved.
 */

package canto.lang;

import java.util.List;
import java.util.stream.Collectors;

import canto.util.EmptyList;

/**
 * 
 */
public class CantoBlock extends Block {

    private List<Definition> defs;
    
    protected CantoBlock() {
        super();
        this.defs = new EmptyList<Definition>();
    }
    
    public CantoBlock(List<CantoNode> children) {
        super(children);
        this.defs = ExtractDefinitions(children);
    }

    private static List<Definition> ExtractDefinitions(List<CantoNode> children) {
        List<Definition> defs = children.stream().filter(c -> c instanceof Definition).map(c -> (Definition) c).collect(Collectors.toList());        
        return defs;
    }

    @Override
    public List<Definition> getDefinitions() {
        return defs;
    }

    public boolean isDynamic() {
        return true;
    }

    public boolean isStatic() {
        return false;
    }

    public boolean isAbstract(Context context) {
        return false;
    }

    public String toString(String prefix) {
        String str = "{=\n" + super.toString(prefix) + prefix + "=}";
        return str;
    }

    public String toString(String firstPrefix, String prefix) {
        return firstPrefix + toString(prefix);
    }
}
