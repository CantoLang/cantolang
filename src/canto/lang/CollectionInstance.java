/* Canto Compiler and Runtime Engine
 * 
 * CollectionInstance.java
 *
 * Copyright (c) 2018-2025 by cantolang.org
 * All rights reserved.
 */

package canto.lang;

import java.util.Iterator;

/**
 * Interface for resolved collection instances.
 */

public interface CollectionInstance {

    /** Returns the object containing the collection.  The collection is shallowly
     *  instantiated; i.e., the container (table or array) is instantiated but the
     *  individual items in the container are not necessarily instantiated.
     * @throws Redirection 
     */
    public Object getCollectionObject();

    public int getSize();
    
    public String getName();

    public Iterator<Definition> iterator();
    
    public Iterator<Construction> constructionIterator();

    public Iterator<Index> indexIterator();

    public Definition getDefinition();

    public Definition getElement(Index index, Context context);

    /** If the collection has been resolved, return a ResolvedInstance representing the element. 
     */
    public ResolvedInstance getResolvedElement(Index index, Context context);
    
    /** For a growable collection, adds an element to the collection.  Throws
     *  an UnsupportedOperationException on a fixed collection.
     */
    //public void add(Object element);

}
