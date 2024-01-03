/* Canto Compiler and Runtime Engine
 * 
 * EmptyList.java
 *
 * Copyright (c) 2018-2024 by cantolang.org
 * All rights reserved.
 */

package canto.util;

import java.util.*;

/**
 * EmptyList is an implementation of List with no elements.
 *
 * @author Michael St. Hippolyte
 */

public class EmptyList<E> extends AbstractList<E> {

    public EmptyList() {
    }

    public int size() {
        return 0;
    }

    public E get(int index) {
        throw new IndexOutOfBoundsException("this list is empty");
    }
}
