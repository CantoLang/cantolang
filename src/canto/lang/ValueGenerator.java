/* Canto Compiler and Runtime Engine
 * 
 * ValueGenerator.java
 *
 * Copyright (c) 2018-2025 by cantolang.org
 * All rights reserved.
 */

package canto.lang;


/**
 * Interface for objects which can generate Values.
 *
 * @author Michael St. Hippolyte
 * @version $Revision: 1.8 $
 */

public interface ValueGenerator extends ValueSource {
    public Value getValue(Context context);
}
