/* Canto Compiler and Runtime Engine
 * 
 * NameWithDims.java
 *
 * Copyright (c) 2018-2026 by cantolang.org
 * All rights reserved.
 */

package canto.lang;

import java.util.List;

/**
 * A TypedName is a name with an associated type.
 */
public class TypedName extends NameWithDims {

    private Type type;

    public TypedName() {
        super();
    }

    public TypedName(Type type, String name, List<ParameterList> paramLists, List<Dim> dims) {
        super(name, paramLists, dims);
        this.type = type;
    }

    public Type getType() {
        return type;
    }

    protected void setType(Type type) {
        this.type = type;
    }
}
