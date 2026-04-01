/* Canto Compiler and Runtime Engine
 *
 * TableBlock.java
 *
 * Copyright (c) 2024-2026 by cantolang.org
 * All rights reserved.
 */

package canto.lang;

import java.util.ArrayList;
import java.util.List;

/**
 * A TableBlock is a block containing table elements (TableElement and
 * ConstructionGenerator instances), used as the body of a table conditional or
 * table loop.  It implements ConstructionGenerator so that ConditionalStatement
 * and ForStatement call generateConstructions on it.
 *
 * generateConstructions returns the element list with an unchecked cast to
 * List&lt;Construction&gt;.  ResolvedTable.addElements processes the list via
 * instanceof checks at runtime, so the cast is safe in practice.
 */
public class TableBlock extends Block implements ConstructionGenerator {

    private List<Object> elements;

    public TableBlock(List<Object> elements) {
        super();
        this.elements = (elements != null ? elements : new ArrayList<>());
    }

    @Override
    @SuppressWarnings("unchecked")
    public List<Construction> generateConstructions(Context context) {
        return (List<Construction>) (List<?>) elements;
    }

    /** Returns true whenever the block has any elements. */
    @Override
    public boolean isDynamic() {
        return !elements.isEmpty();
    }
}
