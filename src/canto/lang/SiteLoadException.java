/* Canto Compiler and Runtime Engine
 * 
 * SiteLoadException.java
 *
 * Copyright (c) 2026 by cantolang.org
 * All rights reserved.
 */

package canto.lang;

/**
 *  Exception thrown when a site cannot be loaded.
 */
public class SiteLoadException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    public SiteLoadException() {
        super();
    }
    public SiteLoadException(String str) {
        super(str);
    }
}
