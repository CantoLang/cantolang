/* Canto Compiler and Runtime Engine
 * 
 * DuplicateDefinitionException.java
 *
 * Copyright (c) 2025 by cantolang.org
 * All rights reserved.
 */

package canto.lang;

/**
 *  Exception thrown when an attempt is made to add a definition with the same
 *  full name and signature as a definition previously added.
 */
public class NameMismatchException extends IllegalArgumentException {

    private static final long serialVersionUID = 1L;

    public NameMismatchException() {
        super();
    }
    public NameMismatchException(String str) {
        super(str);
    }
}
