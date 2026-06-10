/* Canto Compiler and Runtime Engine
 * 
 * NameWithArgs.java
 *
 * Copyright (c) 2018-2026 by cantolang.org
 * All rights reserved.
 */

package canto.lang;

import java.util.*;


/**
 * A NameWithArgs is an identifier, an associated list of arguments, and
 * an optionl list of indexes.
 */
public class NameWithArgs extends NameNode {
    
    public NameWithArgs() {
        super();
        init(null, null);
    }

    public NameWithArgs(String name, ConstructionList args) {
        super(name);
        init(args, null);
    }

    public NameWithArgs(String name, IndexList indexes) {
        super(name);
        init(null, indexes);
    }

    public NameWithArgs(String name, ConstructionList args, IndexList indexes) {
        super(name);
        init(args, indexes);
    }
    
    private void init(ConstructionList args, IndexList indexes) {
        if (args != null) {
            children = new CantoNode[indexes == null ? 1 : 2];
            setChild(0, args);
        }
        if (indexes != null) {
            if (args == null) {
                children = new CantoNode[1];
                setChild(0, indexes);
            } else {
                setChild(1, indexes);
            }
        } else if (args == null) {
            children = new CantoNode[0];
        }
    }

    /** Returns true if there are arguments. */
    public boolean hasArguments() {
        return (getArguments() != null);
    }

    /** Returns the list of arguments associated with this name. */
    public ConstructionList getArguments() {
        if (children.length > 0) {
            CantoNode node = getChild(0);
            if (node instanceof ConstructionList) {
                return (ConstructionList) node;
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
        ConstructionList args = getArguments();
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
