/* Canto Compiler and Runtime Engine
 * 
 * Construction.java
 *
 * Copyright (c) 2018-2025 by cantolang.org
 * All rights reserved.
 */

package canto.lang;

import java.util.List;

/**
 * A construction is a Canto statement which generates data.
 *
 * @author Michael St. Hippolyte
 */

public abstract class Construction extends CantoNode {

    protected Construction() {
        super();
    }

    protected Construction(CantoNode child) {
        super(child);
    }

    protected Construction(List<CantoNode> children) {
        super(children);
    }

    public abstract Value construct(Context context);
    
    @Override
    public boolean isDefinition() {
        return false;
    }

}
