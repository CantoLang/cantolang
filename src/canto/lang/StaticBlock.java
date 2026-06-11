/* Canto Compiler and Runtime Engine
 * 
 * StaticBlock.java
 *
 * Copyright (c) 2018-2025 by cantolang.org
 * All rights reserved.
 */

package canto.lang;

import java.util.List;

/**
 * A StaticBlock is a container whose children are static by default.
 */
public class StaticBlock extends Block {

    public StaticBlock() {
        super();
    }

    public StaticBlock(List<CantoNode> children) {
        super(children);
    }

    // intentionally not overriding isStatic/isDynamic from Block. Block iterates
    // children to determine status; the previous override returned true/false
    // unconditionally which made Construction.getData cache the first generateData
    // result on Construction.staticData and return it for every subsequent call,
    // breaking loop bodies that contained any substitution.

    public boolean isAbstract(Context context) {
        return false;
    }

    public String toString(String prefix) {
        String str = "[|\n" + super.toString(prefix) + prefix + "|]\n";
        return str;
    }

    public String toString(String firstPrefix, String prefix) {
        return firstPrefix + toString(prefix);
    }
}
