/* Canto Compiler and Runtime Engine
 * 
 * ConstructionGenerator.java
 *
 * Copyright (c) 2018-2025 by cantolang.org
 * All rights reserved.
 */

package canto.lang;

import java.util.List;


/**
 * A ConstructionGenerator is an object which generates a list of constructions
 * for a given context.  It is used in dynamic arrays; if an element of such an
 * array is a ConstructionGenerator, the elements retrieved from the array are
 * the generated constructions.
 */

public interface ConstructionGenerator {

    /** Generates a list of constructions for a context. */
    public List<Construction> generateConstructions(Context context);
}
