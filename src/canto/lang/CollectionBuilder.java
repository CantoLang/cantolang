/* Canto Compiler and Runtime Engine
 * 
 * CollectionBuilder.java
 *
 * Copyright (c) 2018-2025 by cantolang.org
 * All rights reserved.
 */

package canto.lang;

import java.util.*;

/**
 * CollectionBuilder is the common base for classes that construct arrays and tables
 */

abstract public class CollectionBuilder {

    public CollectionBuilder() {
        super();
    }

    
    public static CollectionInstance createCollectionInstanceForDef(Definition def, Context context, ArgumentList args, IndexList indexes, Object collectionData) {
        if (def.isArray()) {
            return new ResolvedArray(def, context, args, indexes, collectionData);
        } else if (def.isTable()) {
            return new ResolvedTable(def, context, args, indexes, collectionData);
        } else {
            return null;
        }
    }

    
    /** Creates a resolved instance of this collection in the specified context with the specified
     *  arguments.
     */
    abstract public CollectionInstance createCollectionInstance(Context context, ArgumentList args, IndexList indexes) throws Redirection;

    /** Wraps the passed data in a collection instance in the specified context with the specified
     *  arguments.
     */
    abstract public CollectionInstance createCollectionInstance(Context context, ArgumentList args, IndexList indexes, Object collectionData) throws Redirection;

}

