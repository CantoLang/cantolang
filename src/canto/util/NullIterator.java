/* Canto Compiler and Runtime Engine
 * 
 * NullIterator.java
 *
 * Copyright (c) 2018-2024 by cantolang.org
 * All rights reserved.
 */

package canto.util;

import java.util.*;

/**
 * 
 */
public class NullIterator<E> implements Iterator<E> {

    public boolean hasNext() { return false; }
    
    public E next() { throw new NoSuchElementException("this is a NullIterator"); }
    
    public void remove() { throw new UnsupportedOperationException("NullIterator does not support remove()"); }
}

