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

    private ConstructionList elements;

    public ArrayBlock(ConstructionList elements) {
        super();
        this.elements = (elements != null ? elements : new ConstructionList());
    }

    @Override
    public List<Construction> generateConstructions(Context context) {
        return elements;
    }

    /** Returns true if the block is non-empty or contains a dynamic element. */
    @Override
    public boolean isDynamic() {
        if (elements.isEmpty()) return false;
        for (Construction c : elements) {
            if (c.isDynamic()) return true;
        }
        return true;  // non-empty static arrays are still "present" as a body
    }
}
