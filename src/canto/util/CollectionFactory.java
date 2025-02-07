/* Canto Compiler and Runtime Engine
 * 
 * CollectionFactory.java
 *
 * Copyright (c) 2025 by cantolang.org
 * All rights reserved.
 */

package canto.util;

import java.util.*;

/**
 * EmptyList is an implementation of List with no elements.
 *
 * @author Michael St. Hippolyte
 */

public class CollectionFactory {
    /** The default size for ArrayLists */
    public final static int TYPICAL_LIST_SIZE = 4;

    private static int hashMapsCreated = 0;
    public static int getNumHashMapsCreated() {
        return hashMapsCreated;
    }

    public static <E> HashMap<String, E> newHashMap(Class<E> c) {
        hashMapsCreated++;
        return new HashMap<String, E>();
    }

    public static <E> HashMap<String, E> newHashMap(Map<String, E> map) {
        hashMapsCreated++;
        return new HashMap<String, E>(map);
    }

    public static <E> HashMap<String, Map<String,E>> newHashMapOfMaps(Class<E> c) {
        hashMapsCreated++;
        return new HashMap<String, Map<String, E>>();
    }

    private static int arrayListsCreated = 0;
    private static long totalListSize = 0L;
    public static int getNumArrayListsCreated() {
        return arrayListsCreated;
    }
    public static long getTotalListSize() {
        return totalListSize;
    }

    public static <E> ArrayList<E> newArrayList(int size, Class<E> c) {
        arrayListsCreated++;
        totalListSize += size;
        return new ArrayList<E>(size);
    }

    public static <E> ArrayList<E> newArrayList(int size, List<E> list) {
        arrayListsCreated++;
        totalListSize += size;
        return new ArrayList<E>(size);
    }

    public static <E> ArrayList<E> newArrayList(List<E> list) {
        arrayListsCreated++;
        totalListSize += list.size();
        return new ArrayList<E>(list);
    }

    public static <E> ArrayList<E> newArrayList(Class<E> c) {
        arrayListsCreated++;
        totalListSize += TYPICAL_LIST_SIZE;
        return new ArrayList<E>(TYPICAL_LIST_SIZE);
    }
}
