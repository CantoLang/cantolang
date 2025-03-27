/* Canto Compiler and Runtime Engine
 * 
 * CantoNode.java
 *
 * Copyright (c) 2018-2025 by cantolang.org
 * All rights reserved.
 */

package canto.lang;

import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;

import canto.util.*;


/**
 * Base class for canto nodes. Every statement in a canto program is a node.
 *
 */
abstract public class CantoNode {

    /** Standard indent when displaying source */
    public final static String indent = "    ";

    /** An undefined object.  This can be returned by methods which need to distinguish
     *  between an undefined object and an object which is defined but null.
     */
    public final static class Undefined extends Object {}
    public final static Undefined UNDEFINED = new Undefined();
    
    public final static class Uninstantiated extends Object {}
    public final static Uninstantiated UNINSTANTIATED = new Uninstantiated();

    /** The definition in whose namespace this node resides. */
    protected Definition owner;

    /** The parent of this node, or null if this is the root. */
    protected CantoNode parent;

    /** The children of this node. */
    protected CantoNode[] children;

    /** The documenting comment for this node. */
    private String docComment;

    /** Constructs a node. */
    protected CantoNode() {}

    /** Gets the name of this node.  The default name is just the class name, but nodes types that have
     *  meaningful names should override this to return the name.
     */
    public String getName() {
        return '[' + getClass().getName() + ']';
    }

    public void setDocComment(String docComment) {
        this.docComment = docComment;
    }
    
    public String getDocComment() {
        return docComment;
    }
    
    public void setOwner(Definition owner) {
        this.owner = owner;
    }

    public Definition getOwner() {
        return owner;
    }

    /** Returns the site containing this node. */
    public Site getSite() {
        Definition owner = getOwner();
        if (owner == null) {
            return null;
        } else if (owner == this) {
            throw new IllegalStateException("Circular definition: " + getName() + " owns itself");
        
        } else {
            return owner.getSite();
        }
    }
    
    public void setParent(CantoNode parent) {
        this.parent = parent;
    }

    public CantoNode getParent() {
        return parent;
    }
    
    /** Set this node as the owner for a generated node. */
    public void initNode(CantoNode node) {
        node.setOwner(getOwner());
        if (node instanceof Type) {
            ((Type) node).resolve();
        }
    }

    /** If true, this node cannot have children. */
    abstract public boolean isPrimitive();

    /** If true, this chunk represents static information. */
    abstract public boolean isStatic();

    /** If true, this chunk is dynamically generated at runtime. */
    abstract public boolean isDynamic();

    /** If true, this chunk is a definition. */
    abstract public boolean isDefinition();
    
    /** Clone this node.  Cloning is shallow, in that the child nodes are not
     *  cloned, and a cloned node contains the exact same child nodes as the
     *  original.  However, the array containing those nodes is cloned, not
     *  copied, so that child nodes may be added to or removed from a cloned node
     *  without affecting the original node.
     */
    public Object clone() {
        try {
            CantoNode copy = (CantoNode) super.clone();
            if (children != null) {
                copy.children = (CantoNode[]) children.clone();
            }
            return copy;
        } catch (CloneNotSupportedException e) {
            // this is purely to avoid a "throws CloneNotSupportedException" clause
            throw new InternalError("Unexpected CloneNotSupportedException; Java version may be incompatible");
        }
    }

    public boolean isEmpty() {
        return children == null || children.length == 0;
    }

    protected void setChild(int n, CantoNode child) {
        if (children == null) {
            children = new CantoNode[n + 1];
        } else if (n >= children.length) {
            CantoNode[] newChildren = new CantoNode[n + 1];
            System.arraycopy(children, 0, newChildren, 0, children.length);
            children = newChildren;
        }
        children[n] = child;
    }
    
    protected void setChildren(List<CantoNode> childList) {
        if (childList != null) {
            children = new CantoNode[childList.size()];
            childList.toArray(children);
        } else {
            children = null;
        }
    }

    void addChildren(CantoNode node) {
        int currentLen = (children == null ? 0 : children.length);
        int newLen = currentLen + (node.children == null ? 0 : node.children.length);
        if (newLen > currentLen) {
            CantoNode c[] = new CantoNode[newLen];
            if (children != null) {
                System.arraycopy(children, 0, c, 0, currentLen);
            }
            System.arraycopy(node.children, 0, c, currentLen, node.children.length);
            children = c;
        }
    }

    void copyChildren(CantoNode node) {
        copyChildren(node, 0, node.getNumChildren());
    }
    
    void copyChildren(CantoNode node, int start, int len) {
        int newLen = Math.min(node.getNumChildren(), start + len) - start;
        if (newLen < 0) newLen = 0;
        CantoNode c[] = new CantoNode[newLen];
        for (int i = start; i < start + newLen; i++) {
            c[i - start] = (CantoNode) node.getChild(i).clone();
            c[i - start].parent = this;
        }
        children = c;
    }


    public CantoNode getChild(int n) {
        return children[n];
    }

    public Iterator<CantoNode> getChildren() {
        if (children == null || children.length == 0) {
            return new NullIterator<CantoNode>();
        } else {
            return Arrays.asList((CantoNode[]) children).iterator();
        }
    }

    public List<CantoNode> getChildList() {
        if (children == null || children.length == 0) {
            return new EmptyList<CantoNode>();
        } else {
            return Arrays.asList((CantoNode[]) children);
        }
    }
    
    
    private static class NullIterator<E> implements Iterator<E> {
        public boolean hasNext() { return false; }
        public E next() { throw new NoSuchElementException("this is a NullIterator"); }
        public void remove() { throw new UnsupportedOperationException("NullIterator does not support remove()"); }
    }

    public int getNumChildren() {
        return (children == null) ? 0 : children.length;
    }

    public String toString() {
        return "[CantoNode of type " + getClass().getName() + "]\n";
    }

    public String toString(String prefix) {
        return prefix + toString();
    }

    /** Static utility method to retrieve the value of an arbitrary object. */
    public static Object getObjectValue(Context context, Object obj) {
        Object data = null;
        if (obj instanceof ValueSource) {
            Value value = ((ValueSource) obj).getValue(context);
            data = value.getData();
        } else {
            data = obj;
        }
        return data;
    }

}
