/* Canto Compiler and Runtime Engine
 * 
 * SingleItemIterator.java
 *
 * Copyright (c) 2018-2025 by cantolang.org
 * All rights reserved.
 */

package canto.util;

import java.util.*;


/**
 * A SingleItemIterator is an iterator which only returns one item.
 */

public class SingleItemIterator<E> implements Iterator<E> {
    boolean done = false;
    E item;
        
    public SingleItemIterator(E item) {
        this.item = item;
    }
   
    @Override
    public boolean hasNext() {
        return !done;
    }
    
    @Override
    public E next() {
        done = true;
        return item;
    }
   
    @Override
    public void remove() {
        throw new UnsupportedOperationException("SingleItemIterator does not support the remove method.");
    }
}
