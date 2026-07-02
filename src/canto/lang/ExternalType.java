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
        Definition def = getDefinition();
        if (def instanceof AliasedDefinition) {
            def = ((AliasedDefinition) def).getAliasedDefinition(context);
        }
        return ((ExternalDefinition) def).getExternalClass(context);
    }
}


