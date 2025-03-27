/* Canto Compiler and Runtime Engine
 * 
 * ArgumentList.java
 *
 * Copyright (c) 2018-2025 by cantolang.org
 * All rights reserved.
 */

package canto.lang;

import java.util.*;

/**
 * An IndexList is a list of indexes.
 */
public class IndexList extends ListNode<Index> {

    private boolean dynamic = false;
    private boolean concurrent = false;
    private boolean array = false;
    private boolean table = false;

    public IndexList() {
        super();
    }

    public IndexList(boolean dynamic) {
        super(1);
        this.dynamic = dynamic;
    }

    public IndexList(int capacity) {
        super(capacity);
    }

    public IndexList(IndexList indexes) {
        super(Context.newArrayList(indexes));
        setDynamic(indexes.isDynamic());
        setConcurrent(indexes.isConcurrent());
    }
    
    public IndexList(ListNode<Index> list) {
        super(list);
    }
    
    public IndexList(CantoNode[] nodes) {
        super();
        init(nodes);
    }    

    protected void init(CantoNode[] nodes) {
        
        int len = (nodes == null ? 0 : nodes.length);       
        
        List<Index> list = new ArrayList<Index>(len);
        
        for (int i = 0; i < len; i++) {
            CantoNode node = nodes[i];
            
            if (node instanceof Index) {
                list.add((Index) node);
            } else {
                list.add(new Index(new PrimitiveValue(node)));
            }
        }
        
        setList(list);
    }

    public boolean equals(Object obj) {
        if (obj instanceof List<?> && ((List<?>) obj).size() == size()) {
            Iterator<?> thisIt = iterator();
            Iterator<?> otherIt = ((List<?>) obj).iterator();
            while (thisIt.hasNext()) {
                if (!thisIt.next().equals(otherIt.next())) {
                    return false;
                }
            }
            return true;
        } else {
            return false;
        }
    }

    /** Returns true if these are dynamic arguments, i.e. enclosed in (: :) rather
     *  than ( )
     **/
    public boolean isDynamic() {
        return dynamic;
    }

    public void setDynamic(boolean dynamic) {
        this.dynamic = dynamic;
    }

    /** Returns true if this a concurrent argument list, i.e. enclosed in (+ +)
     *  rather than ( )
     **/
    public boolean isConcurrent() {
        return concurrent;
    }

    public void setConcurrent(boolean concurrent) {
        this.concurrent = concurrent;
    }

    /** Returns true if this list defines the elements of an array, i.e. enclosed
     *  in [ ] rather than ( )
     **/ 
    public boolean isArray() {
        return array;
    }
    
    /** Sets the flag indicating whether or not this list defines the elements of an
     *  array.
     **/ 
    protected void setArray(boolean array) {
        this.array = array;
    }

    /** Returns true if this list defines the elements of an table, i.e. enclosed
     *  in { } rather than ( )
     **/ 
    public boolean isTable() {
        return table;
    }
    
    /** Sets the flag indicating whether or not this list defines the elements of an
     *  table.
     **/ 
    protected void setTable(boolean table) {
        this.table = table;
    }


    public Object clone() {
        return new IndexList(this);
    }

    public String toString(String prefix) {
        StringBuffer sb = new StringBuffer();
        Iterator<Index> it = iterator();
        while (it.hasNext()) {
            sb.append(it.next().toString());
        }
        return sb.toString();
    }

}
