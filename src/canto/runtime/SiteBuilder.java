/* Canto Compiler and Runtime Engine
 * 
 * SiteBuilder.java
 *
 * Copyright (c) 2024-2025 by cantolang.org
 * All rights reserved.
 */

package canto.runtime;

import canto.lang.Core;

/**
 * 
 */
public class SiteBuilder {

    private static final Log LOG = Log.getLogger(SiteLoader.class);
    
    private Exception exception = null;
    protected Core core;


    public SiteBuilder(Core core) {
        this.core = core;
    }

    public Exception getException() {
        return exception;
    }


}
