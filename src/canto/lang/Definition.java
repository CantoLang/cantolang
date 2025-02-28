/* Canto Compiler and Runtime Engine
 * 
 * Definition.java
 *
 * Copyright (c) 2018-2025 by cantolang.org
 * All rights reserved.
 */

package canto.lang;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import canto.runtime.Log;

/**
 * 
 */
abstract public class Definition extends CantoNode {
    private static final Log LOG = Log.getLogger(Definition.class);

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
    
    /** Access level. */
    private Access access = Access.SITE;

    /** Durability. */
    private Durability dur = Durability.IN_CONTEXT;

    /** The children of this definition. */
    protected Definition[] childDefs = null;

    /** The contents of this definition. */
    protected CantoNode contents = null;

    protected Definition(Name name, CantoNode contents) {
        this(name);
        setContents(contents);
    }
    
    protected Definition(Name name) {
        super();
        this.name = name;
    }

    public void setContents(CantoNode contents) {
        this.contents = contents;
        List<Definition> defs = extractDefinitions(contents);
        this.childDefs = defs.toArray(new Definition[0]);
    }

    public CantoNode getContents() {
        return contents;
    }
    
    protected void setAccess(Access access) {
        this.access = access;
    }

    public Access getAccess() {
        return access;
    }

    /** Convenience method; returns true if this is a local definition
     *  (i.e., access is LOCAL).
     */
    public boolean isLocal() {
        return (access == Access.LOCAL);
    }

    protected void setDurability(Durability dur) {
        this.dur = dur;
    }

    public Durability getDurability() {
        return dur;
    }

    /** Convenience method; returns true if the definition is
     *  global or static (i.e., durability is GLOBAL, COSMIC, or STATIC).
     */
    public boolean isGlobal() {
        return (dur == Durability.GLOBAL || dur == Durability.COSMIC || dur == Durability.STATIC);
    }
    
    public String getName() {
        return name.getName();
    }

    /** The default Type is DefaultType.TYPE.
     */
    public Type getType() {
        return DefaultType.TYPE;
    }

    /** Anonymous definitions have no supertype; returns null. */
    public Type getSuper() {
        return null;
    }

    public Type getSuper(Context context) {
        return getSuper();
    }

    /** Anonymous definitions have no supertype; returns null. */
    public Type getSuperForChild(Context context, Definition childDef) throws Redirection {
        return null;
    }

    /** Anonymous definitions have no supertype; returns null. */
    public NamedDefinition getSuperDefinition() {
        return null;
    }

    /** Anonymous definitions have no supertype; returns null. */
    public NamedDefinition getSuperDefinition(Context context) {
        return null;
    }

    /** Returns true if <code>def</code> equals this definition or is a subdefinition of
     *  this definition.
     */
    public boolean equalsOrExtends(Definition def) {
        return equals(def);
    }

    /** Returns true if <code>name</code> is the name of an ancestor of this
     *  definition.
     */
    public boolean isSuperType(String name) {
        return (name.equals(""));
    }

    /** Returns true if this definition has a child definition by the specified name.
     */
    public boolean hasChildDefinition(String name, boolean localAllowed) {
        for (int i = 0; i < childDefs.length; i++) {
            if (childDefs[i].getName().equals(name)) {
                return true;
            }
        }
        return false;
    }

    private List<Definition> extractDefinitions(CantoNode contents) {
        List<Definition> defs = new ArrayList<Definition>();
        if (contents != null && contents.getNumChildren() > 0) {
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

    public abstract Value instantiate(Context context, ArgumentList args, List<Index> indexes);

    public String getFullName() {
        if (owner == null) {
            return name.getName();
        } else {
            return owner.getFullName() + "." + name.getName();
        }
    }

    public Definition getExplicitChildDefinition(NameNode node) {
        for (int i = 0; i < childDefs.length; i++) {
            if (childDefs[i].getName().equals(node.getName())) {
                return childDefs[i];
            }
        }
        return null;
    }

    DefinitionTable getDefinitionTable() {
        if (owner == null) {
            LOG.error("Definition " + name.getName() + " has no owner!");
            return null;
        }
        return owner.getDefinitionTable();
    }

    void setDefinitionTable(DefinitionTable table) {
        ;
    }

    
}

