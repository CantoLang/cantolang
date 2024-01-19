/* Canto Compiler and Runtime Engine
 * 
 * Definition.java
 *
 * Copyright (c) 2018-2024 by cantolang.org
 * All rights reserved.
 */

package canto.lang;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * 
 */
abstract public class Definition extends CantoNode {

    // The modifier values are such that for groups of definitions, the lowest value
    // governs.  For example, if a group of five definitions includes one dynamic one,
    // the group as a whole is considered dynamic.  Definition groups arise when a
    // definition has multiple supertypes; the superdefinition of such a definition is
    // a definition group.


    /** access modifiers */
    public enum Access {
        LOCAL, SITE, PUBLIC
    }

    /** durability modifiers */
    public enum Durability {
        DYNAMIC,       // corresponds to the dynamic keyword 
        IN_CONTEXT,    // corresponds to no durability modifier keyword
        GLOBAL,        // corresponds to the global keyword
        COSMIC,        // corresponds to the cosmic keyword
        STATIC         // corresponds to the static keyword
    }
  

    // signature-matching scores

    /** The signatures match perfectly */
    public final static int PERFECT_MATCH = 0;

    /** The signatures don't match */
    public final static int NO_MATCH = Integer.MAX_VALUE / 2;

    /** The signatures don't match */
    public final static int QUESTIONABLE_MATCH = NO_MATCH - 16384; 
            
    /** The parameter type is the default type */
    public final static int PARAM_DEFAULT = 256;

    /** An argument is a null literal, which can match anything */
    public final static int ARG_NULL = 128;

    /** An argument is missing */
    public final static int ARG_MISSING = 16384;

    protected Name name;
    
    /** The owner of this definition, or null if this is the root. */
    protected Definition owner;

    /** The children of this definition. */
    protected Definition[] childDefs;

    /** The contents of this definition. */
    protected CantoNode contents;

    protected Definition(CantoNode parent, Name name, CantoNode contents) {
        super(parent);
        this.name = name;
        this.contents = contents;
        List<Definition> defs = extractDefinitions(contents);
        this.childDefs = defs.toArray(new Definition[0]);
    }
    
    public Name getName() {
        return name;
    }

    private List<Definition> extractDefinitions(CantoNode contents) {
        List<Definition> defs = new ArrayList<Definition>();
        if (contents.getNumChildren() > 0) {
            Iterator<CantoNode> it = contents.getChildren();
            while (it.hasNext()) {
                CantoNode child = it.next();
                if (child instanceof Definition) {
                    defs.add((Definition) child);
                }
            }
        }
        return defs;
    }

    public boolean isElementDefinition() {
        return false;
    }

    public boolean isBlockDefinition() {
        return false;
    }
    
    public boolean isParameterDefinition() {
        return false;
    }
    
    @Override
    public boolean isDefinition() {
        return true;
    }

    public abstract Value instantiate(Context context);

}

