/* Canto Compiler and Runtime Engine
 * 
 * CantoNode.java
 *
 * Copyright (c) 2018-2024 by cantolang.org
 * All rights reserved.
 */

package canto.lang;

import java.util.*;

/**
 * The CantoNode interface represents an element of a canto program.
 *
 * @author Michael St. Hippolyte
 */
public interface CantoNode {

    /** If true, this node cannot have children. */
    public boolean isPrimitive();

    /** If true, this node represents static information. */
    public boolean isStatic();

    /** If true, this node represents data generated at runtime. */
    public boolean isDynamic();

    /** If true, this node is a definition. */
    public boolean isDefinition();

    /** Returns the nth child node of this node */
    public CantoNode getChild(int n);

    /** Returns an iterator over the child nodes of this node. */
    public Iterator<CantoNode> getChildren();

    /** Returns the number of child nodes belonging to this node. */
    public int getNumChildren();

    /** Returns this node's parent, or null if this is the root node. */
    public CantoNode getParent();
    
    /** Returns the next child after this node in the parent's child nodes. */
    public CantoNode getNextSibling();
    
    /** Returns the owner, which is the Definition in whose namespace this
     *  node is in.
     */
    public Definition getOwner();

    /** Return the string representation of this node, suitable for display. */
    public String toString(String indent);
}
