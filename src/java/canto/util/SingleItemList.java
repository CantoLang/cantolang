/* Canto Compiler and Runtime Engine
 * 
 * SingleItemList.java
 *
 * Copyright (c) 2018-2025 by cantolang.org
 * All rights reserved.
 */

package canto.util;

import java.util.*;


/**
 * A SingleItemList is a List which only contains one entry.
 */

public class SingleItemList<E> extends AbstractList<E> {

    private E item;

    public SingleItemList(E item) {
        this.item = item;
    }

    public int size() {
        return 1;
    }

    public E get(int index) {
        if (index != 0) {
            throw new IndexOutOfBoundsException("only valid index for this list is zero");
        }
        return item;
    }

    public E set(int index, E element) {
        if (index != 0) {
            throw new IndexOutOfBoundsException("only valid index for this list is zero");
        }
        E oldElement = item;
        item = element;
        return oldElement;
    }
}
