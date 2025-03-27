/* Canto Compiler and Runtime Engine
 * 
 * NameWithArgs.java
 *
 * Copyright (c) 2018-2025 by cantolang.org
 * All rights reserved.
 */

package canto.lang;

import java.util.*;

import canto.runtime.Log;

/**
 * A NameWithArgs is an identifier, an associated list of arguments, and
 * an optionl list of indexes.
 */
public class NameWithArgs extends NameNode {
    private static final Log LOG = Log.getLogger(NameWithArgs.class);
    
    public NameWithArgs() {
        super();
    }

    public NameWithArgs(String name, ArgumentList args) {
        super(name);
        children = new CantoNode[1];
        children[0] = args;
    }

    public NameWithArgs(String name, IndexList indexes) {
        super(name);
        children = new CantoNode[1];
        children[0] = indexes;
    }

    public NameWithArgs(String name, ArgumentList args, IndexList indexes) {
        super(name);
        children = new CantoNode[2];
        children[0] = args;
        children[1] = indexes;
    }

    /** Returns true if there are arguments. */
    public boolean hasArguments() {
        return (getArguments() != null);
    }

    /** Returns the list of arguments associated with this name. */
    public ArgumentList getArguments() {
        if (children.length > 0) {
            CantoNode node = getChild(0);
            if (node instanceof ArgumentList) {
                return (ArgumentList) node;
            }
        }
        return null;
    }
        
    /** Returns true if there are indexes. */
    public boolean hasIndexes() {
        return (getArguments() != null);
    }

    public IndexList getIndexes() {
        if (children.length > 1) {
            CantoNode node = getChild(1);
            if (node instanceof IndexList) {
                return (IndexList) node;
            }
        } else if (children.length > 0) {
            CantoNode node = getChild(0);
            if (node instanceof IndexList) {
                return (IndexList) node;
            }
        }
        return null;
    }

    public String toString(String prefix) {
        StringBuffer sb = new StringBuffer(super.getName());
        ArgumentList args = getArguments();
        if (args != null) {
            sb.append('(');
            if (args.isDynamic()) {
                sb.append(": ");
            }
            Iterator<Construction> it = args.iterator();
            while (it.hasNext()) {
                CantoNode node = (CantoNode) it.next();
                sb.append(node.toString());
                if (it.hasNext()) {
                    sb.append(',');
                }
            }
            if (args.isDynamic()) {
                sb.append(" :");
            }
            sb.append(')');
        }
        return sb.toString();
    }
}
