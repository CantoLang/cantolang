/* Canto Compiler and Runtime Engine
 * 
 * ConstructionContainer.java
 *
 * Copyright (c) 2018-2025 by cantolang.org
 * All rights reserved.
 */

package canto.lang;

import java.util.List;


/**
 * ConstructionContainer is the interface for objects which own dynamic and/or
 * static constructions.
 */

public interface ConstructionContainer {

    /** Returns the list of constructions owned by this container. */
    public List<Construction> getConstructions(Context context);
}
