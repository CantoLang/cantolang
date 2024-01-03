/* Canto Compiler and Runtime Engine
 * 
 * Node.java
 *
 * Copyright (c) 2018-2024 by cantolang.org
 * All rights reserved.
 */

package canto.lang;

import canto.util.*;

import java.util.Arrays;
import java.util.Iterator;
import java.util.List;


/**
 * Base class for canto nodes. Every statement in a canto program is a node.
 *
 * @author Michael St. Hippolyte
 */
abstract public class Node implements CantoNode {

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
    protected Node parent;

    /** The children of this node.  This field is protected
     *  protected rather than private to allow for efficient
     *  node initialization and tree walking.
     */
    protected Node[] children;

    /** The source code for this node. */
    private String source;

    /** The documenting comment for this node. */
    private String docComment;

    /** Constructs a node. */
    protected Node(String source) {
        this.source = source;
    }

    /** Constructs a node with a documenting comment. */
    protected Node(String source, String docComment) {
        this.source = source;
        this.docComment = docComment;
    }
    
    public String getSource() {
        return source;
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

    /** Resolves any references.  Assumes that the owner of this node has been set. **/
    public void resolve() {}
    

    /** If true, this node cannot have children. */
    abstract public boolean isPrimitive();

    /** If true, this chunk represents static information. */
    abstract public boolean isStatic();

    /** If true, this chunk is dynamically generated at runtime. */
    abstract public boolean isDynamic();

    /** If true, this chunk is a definition. */
    abstract public boolean isDefinition();
    

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
    
    public CantoNode getParent() {
        return parent;
    }

    /** Returns the next child after this node in the parent's child nodes. */
    public CantoNode getNextSibling() {
        Node[] nodes = parent.children;
        for (int i = 0; i < nodes.length - 1; i++) {
            if (nodes[i] == this) {
                return nodes[i + 1];
            }
        }
        
        return null;
    }

    public String toString() {
        return "[Node of type " + getClass().getName() + "]\n";
    }

    public String toString(String prefix) {
        return prefix + toString();
    }
}
