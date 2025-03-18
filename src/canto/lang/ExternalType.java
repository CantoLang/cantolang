/* Canto Compiler and Runtime Engine
 * 
 * ExternalType.java
 *
 * Copyright (c) 2018-2025 by cantolang.org
 * All rights reserved.
 */

package canto.lang;


/**
 * A Type corresponding to an externally defined class.
 */

public class ExternalType extends ComplexType {

    public ExternalType(ExternalDefinition def) {
        super(def, def.getExternalTypeName(), def.getDims(), def.getArguments());
    }

    public Class<?> getTypeClass(Context context) {
        return ((ExternalDefinition) getDefinition()).getExternalClass(context);
    }
}


