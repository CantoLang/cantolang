/* Canto Compiler and Runtime Engine
 * 
 * Initializable.java
 *
 * Copyright (c) 2018-2025 by cantolang.org
 * All rights reserved.
 */

package canto.lang;


/**
 * Interface for nodes which need to be initialized once the site is loaded.
 */
public interface Initializable {
    public void init();
}
