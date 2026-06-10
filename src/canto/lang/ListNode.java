/* Canto Compiler and Runtime Engine
 * 
 * ListNode.java
 *
 * Copyright (c) 2018-2026 by cantolang.org
 * All rights reserved.
 */

package canto.lang;

import java.util.*;


/**
 * An ListNode is a node which contains a list of nodes.
 */
public class ListNode<E extends CantoNode> extends CantoNode implements List<E> {

    private List<CantoNode> list;
    private int numInserted = 0;

    public ListNode() {
        super();
        list = new ArrayList<CantoNode>();
        setChildren(list);   
    }

    public ListNode(int capacity) {
        super();
        list = new ArrayList<CantoNode>(capacity);
        setChildren(list);   
    }

    @SuppressWarnings("unchecked")
    public ListNode(List<E> list) {
        super();
        this.list = (List<CantoNode>) list;
        setChildren(this.list);
    }

    protected List<CantoNode> getList() {
        return list;
    }
    
    @SuppressWarnings("unchecked")
    protected void setList(List<E> list) {
        this.list = (List<CantoNode>) list;
        setChildren((List<CantoNode>) list);   
    }

    /** Returns <code>false</code> */
    public boolean isPrimitive() {
        return false;
    }

    /** Returns <code>false</code> */
    public boolean isStatic() {
        return false;
    }

    /** Returns <code>false</code> */
    public boolean isDynamic() {
        return false;
    }

    /** Returns <code>false</code> */
    public boolean isDefinition() {
        return false;
    }

    /** Clone this node, including the list it contains.  The cloned node's list will
     *  be an ArrayList containing the same elements as the list in this node, regardless
     *  of the actual type of the list in this node (which need not be an ArrayList, or
     *  cloneable).
     */
    @SuppressWarnings("unchecked")
    public Object clone() {
        Object copy = super.clone();
        if (list != null) {
            ((ListNode<E>) copy).list = Context.newArrayList(list);
        }
        return copy;
    }

    /** Insert items into this list.  The wrapped list is replaced with a new
     *  ArrayList combining the contents of the passed list and the current list.
     */
    @SuppressWarnings("unchecked")
    public void insert(List<E> newItems) {
        if (list == null || list.size() == 0) {
            setList((List<E>) Context.newArrayList(newItems));
        } else {
            List<CantoNode> newList = (List<CantoNode>) Context.newArrayList(newItems);
            newList.addAll((Collection<? extends E>) list);
            setList((List<E>) newList);
        }
        numInserted += newItems.size();
    }

    /** Undo the combined effect of all insert calls to this list.
     */
    public synchronized void uninsert() {
        if (numInserted > 0) {
            removeFirst(numInserted);
            numInserted = 0;
        }
    }

    /** Remove the specified number of items from the beginning of this list.
     */
    private void removeFirst(int n) {
        if (list == null || list.size() == 0) {
            throw new IndexOutOfBoundsException("Cannot remove elements from list; list is empty.");
        }
        int len = list.size();
        if (n > len) {
            throw new IndexOutOfBoundsException("Cannot remove " + n + "elements from list; list size is only " + len);
        }
        List<CantoNode> newList = (List<CantoNode>) Context.newArrayList(len - n, list);
        for (int i = n; i < len; i++) {
            newList.add(list.get(i));
        }
        list = newList;
    }

    public String toString() {
        return toString("(", ")");
    }
    
    
    /** Convert this list into a string with the specified delimeters at each
     *  end.  This is used to handle use of ConstructionList in colleciont
     *  definitions.
     */
    public String toString(String leftDelim, String rightDelim) {
        StringBuffer sb = new StringBuffer();
        sb.append(leftDelim);
        Iterator<E> it = iterator();
        while (it.hasNext()) {
            Object obj = it.next();
            sb.append(obj.toString());
            if (it.hasNext()) {
                sb.append(',');
            }
        }
        sb.append(rightDelim);
        return sb.toString();
    }
    
    
    // List implementation

    @Override
    public boolean add(E o) {
        addChild(o);
        return list.add(o);
    }
    
    @SuppressWarnings("unchecked")
    @Override
    public E set(int index, E element) {
        setChild(index, element);
        return (E) list.set(index, element);
    }
    
    @Override
    public void add(int index, E element) {
        addChild(element);
        list.add(index, element);
    }
    
    @Override
    public boolean addAll(Collection<? extends E> c) {
        boolean change = list.addAll(c);
        setChildren(list);
        return change;
    }
    
    @Override
    public boolean addAll(int index, Collection<? extends E> c) {
        boolean change = list.addAll(index, c);
        setChildren(list);
        return change;
    }
    
    
    
    @Override
    public int size() { return list.size(); }
    
    @Override
    public boolean isEmpty() { return list.isEmpty(); }
    
    @Override
    public boolean contains(Object o) { return list.contains(o); }
    
    @SuppressWarnings("unchecked")
    public Iterator<E> iterator() {
        return (Iterator<E>) list.iterator();
    }
    
    @Override
    public Object[] toArray() { return list.toArray(); }
    
    @Override
    public <T> T[] toArray(T a[]) { return list.toArray(a); }
    
    @Override
    public boolean remove(Object o) { return list.remove(o); }
    
    @Override
    public boolean containsAll(Collection<?> c) { return list.containsAll(c); }
    
    @Override
    public boolean removeAll(Collection<?> c) { return list.removeAll(c); }
    
    @Override
    public boolean retainAll(Collection<?> c) { return list.retainAll(c); }
    
    @Override
    public void clear() { list.clear(); }
    
    @Override
    public boolean equals(Object o) { return list.equals(o); }
    
    @Override
    public int hashCode() { return list.hashCode(); }

    @SuppressWarnings("unchecked")
    @Override
    public E get(int index) { return (E) list.get(index); }
    
    @SuppressWarnings("unchecked")
    @Override
    public E remove(int index) { return (E) list.remove(index); }
    
    @Override
    public int indexOf(Object o) { return list.indexOf(o); }
    
    @Override
    public int lastIndexOf(Object o) { return list.lastIndexOf(o); }
    
    @SuppressWarnings("unchecked")
    @Override
    public ListIterator<E> listIterator() { return (ListIterator<E>) list.listIterator(); }
    
    @SuppressWarnings("unchecked")
    @Override
    public ListIterator<E> listIterator(int index) { return (ListIterator<E>) list.listIterator(index); }
    
    @SuppressWarnings("unchecked")
    @Override
    public List<E> subList(int fromIndex, int toIndex) { return (List<E>) list.subList(fromIndex, toIndex); }
}
