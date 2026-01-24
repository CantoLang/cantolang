/* Canto Compiler and Runtime Engine
 * 
 * Identifier.java
 *
 * Copyright (c) 2024 by cantolang.org
 * All rights reserved.
 */

package canto.lang;

/**
 * 
 */
public final class Identifier implements Name {
    private String name;

    public Identifier(String name) {
        this.name = name;
    }
    
    @Override
    public String getName() {
        return name;
    }

    @Override
    public int numParts() {
        return 1;
    }
    
}
