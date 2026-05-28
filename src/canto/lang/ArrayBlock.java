/* Canto Compiler and Runtime Engine
 *
 * ArrayBlock.java
 *
 * Copyright (c) 2024-2026 by cantolang.org
 * All rights reserved.
 */

package canto.lang;

import java.util.List;

/**
 * An ArrayBlock is a block containing array elements, used as the body of an
 * array conditional or array loop.  It implements ConstructionGenerator so that
 * ConditionalStatement and ForStatement can retrieve the elements directly via
 * generateConstructions rather than falling back to the empty getConstructions
 * default.
 */
public class ArrayBlock extends Block implements ConstructionGenerator {

    public ArrayBlock(ConstructionList elements) {
        super(elements.getList());
    }

    @Override
    public List<Construction> generateConstructions(Context context) {
        return getConstructions(context);
    }

    /** Returns true if the block is non-empty or contains a dynamic element. */
    @Override
    public boolean isDynamic() {
        if (children.length == 0) {
            return false;
        }
        for (Construction c : getConstructions()) {
            if (c.isDynamic()) return true;
        }
        return true;  // non-empty static arrays are still "present" as a body
    }
}
