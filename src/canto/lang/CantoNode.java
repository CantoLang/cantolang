/* Canto Compiler and Runtime Engine
 * 
 * CantoNode.java
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

    /** The source code for this node. */
    private String source;

    /** The documenting comment for this node. */
    private String docComment;

    /** Constructs a node. */
    protected CantoNode(CantoNode parent) {
        this.parent = parent;
    }

    public void setSource(String source) {
        this.source = source;
    }
    
    public String getSource() {
        return source;
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

    /** If true, this node cannot have children. */
    abstract public boolean isPrimitive();

    /** If true, this chunk represents static information. */
    abstract public boolean isStatic();

    /** If true, this chunk is dynamically generated at runtime. */
    abstract public boolean isDynamic();

    /** If true, this chunk is a definition. */
    abstract public boolean isDefinition();
    

    public CantoNode getChild(int n) {
        throw new IndexOutOfBoundsException("This node has no children.");
    }

    public Iterator<CantoNode> getChildren() {
        return new NullIterator<CantoNode>();
    }

    public int getNumChildren() {
        return 0;
    }

    public CantoNode getParent() {
        return parent;
    }

    public String toString() {
        return "[CantoNode of type " + getClass().getName() + "]\n";
    }

    public String toString(String prefix) {
        return prefix + toString();
    }
}
