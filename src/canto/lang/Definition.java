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
import java.util.LinkedList;
import java.util.List;

import canto.runtime.Log;

/**
 * 
 */
abstract public class Definition extends CantoNode implements Name {
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

    /** The parameters for this definition, if any */
    private List<ParameterList> paramLists = null;

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

    public NameNode getNameNode() {
        return null;
    }

    public List<ParameterList> getParamLists() {
        return paramLists;
    }

    protected void setParamLists(List<ParameterList> paramLists) {
        this.paramLists = paramLists;
    }

    /** The default Type is DefaultType.TYPE.
     */
    public Type getType() {
        return DefaultType.TYPE;
    }

    /** Anonymous definitions have no supertype; returns null. */
    /** A <code>sub</code> statement in an anonymous definition would
     *  be meaningless.
     */
    public boolean hasSub(Context context) {
        return false;
    }
    
    /** A <code>next</code> statement in an anonymous definition would
     *  be meaningless.
     */
    public boolean hasNext(Context context) {
        return false;
    }

    /** Anonymous definitions have no superdefinitions with <code>next</code>
     *  statements; returns null.
     */
    public LinkedList<Definition> getNextList(Context context) {
        return null;
    }
    
    public Type getSuper() {
        return null;
    }

    public Type getSuper(Context context) {
        return getSuper();
    }

    /** Anonymous definitions have no supertype; returns null. */
    public Type getSuperForChild(Context context, Definition childDef) {
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

    /** If this definition is a reference to another definition, returns the definition
     *  ultimately referenced after the entire chain of references has been resolved.
     * 
     */
    public Definition getUltimateDefinition(Context context) {
        return this;
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

    /** Returns true if this definition can have child definitions.  The base class
     *  returns true unless the definition is an alias, identity or primitive type.
     */
    public boolean canHaveChildDefinitions() {
        return !isAlias() && !isParamAlias() && !isIdentity() && !isPrimitive();
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

    public Definition getChildDefinition(NameNode name, ArgumentList args, List<Index> indexes, ArgumentList parentArgs, Context argContext, Definition resolver) {
        try {
            Object obj = getChild(name, args, indexes, parentArgs, argContext, false, true, null, resolver);
            if (obj instanceof Definition) {
                return (Definition) obj;
            } else if (obj instanceof DefinitionInstance) {
                return ((DefinitionInstance) obj).def;
            // presume obj is UNDEFINED
            } else {
                return null;
            }
        } catch (Throwable t) {
            LOG.error("Unable to find definition for " + name.getName() + " in " + getFullName());
            t.printStackTrace();
            return null;
        }
    }

    /** Unnamed definitions are opaque, and the definitions they contain
     *  cannot be retrieved, so the base class returns null.  Definitions which
     *  support the retrieval of contained definitions (ComplexDefinition, for
     *  example) must override this to return the specified definition, if it
     *  exists, else null.
     */
    public Definition getChildDefinition(NameNode name, Context context) {
        return null;
    }

    /** Find the child definition, if any, by the specified name; if <code>generate</code> is
     *  false, return the definition, else instantiate it and return the result.  If <code>generate</code>
     *  is true and a definition is not found, return UNDEFINED.
     */
    public Object getChild(NameNode name, ArgumentList args, List<Index> indexes, ArgumentList parentArgs, Context argContext, boolean generate, boolean trySuper, Object parentObj, Definition resolver) {
        if (generate) {
            return UNDEFINED;
        } else {
            return null;
        }
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

    public ParameterList getParamsForArgs(ArgumentList args, Context argContext) {
        return getMatch(args, argContext);
    }

    public Definition getDefinitionForArgs(ArgumentList args, Context argContext) {
        ParameterList params = getParamsForArgs(args, argContext);
        // if args is nonnull and params is null or smaller than args, there is no match.
        if (args != null && args.size() > 0 && (params == null || params.size() < args.size())) {
            return null;
        } else {
            return getDefinitionFlavor(argContext, params);
        }
    }

    protected Definition getDefinitionFlavor(Context context, ParameterList params) {
        List<ParameterList> paramLists = getParamLists();
        if (paramLists != null && paramLists.size() > 1) {
            return new DefinitionFlavor(this, context, params);
        } else {
            return this;
        }
    }

    /** Go through all the parameter lists belonging to this definition and
     *  select the best match (if any) for the specified argument list and context.
     *
     *  Returns the most closely matching parameter list, or null if none matches.
     */
    protected ParameterList getMatch(ArgumentList args, Context argContext) {
        ParameterList params = null;
        List<ParameterList> paramLists = getParamLists(); 
        if (paramLists != null) {
            int score = NO_MATCH;
            Iterator<ParameterList> it = paramLists.iterator();
            while (it.hasNext()) {
                ParameterList p = it.next();
                int s = p.getScore(args, argContext, this);
                if (s < score) {
                    params = p;
                    score = s;
                }
            }
        }
        return params;
    }

    /** Go through all the parameter lists belonging to this definition and
     *  all the passed argument lists and select the best match (if any).
     *
     *  Returns an array of ListNodes with two elements, a ParameterList
     *  and an ArgumentList, or null if none matches.
     */
    public ListNode<?>[] getMatch(ArgumentList[] argLists, Context argContext) {
        ListNode<?>[] paramsAndArgs = null; 
        ListNode<?> args = null;
        ListNode<?> params = null;
        List<ParameterList> paramLists = getParamLists(); 
        if (paramLists != null) {
            int score = NO_MATCH;
            Iterator<ParameterList> itParams = paramLists.iterator();
            while (itParams.hasNext()) {
                ParameterList p = itParams.next();
                for (int i = 0; i < argLists.length; i++) {
                    ArgumentList a = argLists[i];
                    int s = p.getScore(a, argContext, this);
                    if (s < score) {
                        params = p;
                        args = a;
                        score = s;
                    }
                }
                
            }
        }
        if (params != null || args != null) {
            paramsAndArgs = new ListNode<?>[2];
            paramsAndArgs[0] = params;
            paramsAndArgs[1] = args;
        }
        return paramsAndArgs;
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

    /** Returns true if this definition is abstract, i.e., it has an abstract
     *  block as its contents.
     */
    public boolean isAbstract(Context context) {
        CantoNode contents = getContents();
        if (contents instanceof Block && ((Block) contents).isAbstract(context)) {
            return true;
        } else {
            return false;
        }
    }

    public boolean isIdentity() {
        return false;
    }

    public boolean isFormalParam() {
        return false;
    }
    
    public boolean isAlias() {
        return false;
    }

    public NameNode getAlias(){
        return null;
    }

    public boolean isParamAlias() {
        return false;
    }

    public NameNode getParamAlias(){
        return null;
    }

    public Instantiation getAliasInstance() {
        return null;
    }

    public boolean isAliasInContext(Context context) {
        return false;
    }

    public NameNode getAliasInContext(Context context) {
        return null;
    }
    
    public Instantiation getAliasInstanceInContext(Context context) {
        return null;
    }
    
    public boolean isReference() {
        return false;
    }

    public NameNode getReference() {
        return null;
    }

    public boolean isExternal() {
        return false;
    }

    /** Returns true if this is a collection; the base class returns false by default. */
    public boolean isCollection() {
        return false;
    }

    /** Returns true if this is an array; the base class returns false by default. */
    public boolean isArray() {
        return false;
    }

    /** Returns true if this is a table; the base class returns false by default. */
    public boolean isTable() {
        return false;
    }

    public CollectionDefinition getCollectionDefinition(Context context, ArgumentList args) {
        return null;
    }

    public List<Dim> getDims() {
        return null;
    }
    
    public abstract Value instantiate(Context context, ArgumentList args, List<Index> indexes);

    public String getFullName() {
        if (owner == null) {
            return name.getName();
        } else {
            return owner.getFullName() + "." + name.getName();
        }
    }

    /** Returns the full name, with the ownership chain adjusted to reflect the
     *  actual subclasses, in dot notation.
     */
    public String getFullNameInContext(Context context) {
        String name = getName();
        if (name != null && name != Name.ANONYMOUS) {
            Definition owner = getOwner();
            if (owner == null || owner instanceof Site) {
                return name;
            }
            Definition contextDef = owner.getSubdefInContext(context);
            if (this.equals(contextDef)) {
                return getFullName();
            }
            String ownerName = contextDef.getFullNameInContext(context);
            if (ownerName != null && ownerName.length() > 0) {
                name = ownerName + '.' + name;
            }
        }
        return name;
    }

    public int numParts() {
        return 1;
    }

    public Definition getExplicitChildDefinition(NameNode node) {
        for (int i = 0; i < childDefs.length; i++) {
            if (childDefs[i].getName().equals(node.getName())) {
                return childDefs[i];
            }
        }
        return null;
    }

    
    /** Returns the first scope in the context stack whose definition equals or extends 
     *  this definition.  
     */
    public Scope getScopeInContext(Context context) {
        Scope scope = context.peek();
        if (scope.def.equalsOrExtends(this)) {
            return scope;
        } else {
            while (scope != null) {
                if (scope.def.equalsOrExtends(this)) {
                    return scope;
                }
                scope = scope.getPrevious();
            }
        }
        return null;
    }

    
    public Definition getSubdefInContext(Context context) {
        Scope scope = getScopeInContext(context);
        if (scope != null) {
            return scope.def;
        } else {
            return this;
        }
    }

    public Definition getOwnerInContext(Context context) {
        Definition owner = getOwner();
        if (owner == null) {
            return null;
        }
        return owner.getSubdefInContext(context);
    }
        
    /** Returns the keep statement in this definition for the specified key.
     */
    public KeepStatement getKeep(String key) {
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

    public static Access minAccess(Access access2, Access access3) {
        if (access2 == Access.LOCAL || access3 == Access.LOCAL) {
            return Access.LOCAL;
        } else if (access2 == Access.SITE || access3 == Access.SITE) {
            return Access.SITE;
        } else {
            return Access.PUBLIC;
        }
    }

    public static Durability minDurability(Durability dur2, Durability durability) {
        if (dur2 == Durability.DYNAMIC || durability == Durability.DYNAMIC) {
            return Durability.DYNAMIC;
        } else if (dur2 == Durability.IN_CONTEXT || durability == Durability.IN_CONTEXT) {
            return Durability.IN_CONTEXT;
        } else if (dur2 == Durability.GLOBAL || durability == Durability.GLOBAL) {
            return Durability.GLOBAL;
        } else if (dur2 == Durability.COSMIC || durability == Durability.COSMIC) {
            return Durability.COSMIC;
        } else {
            return Durability.STATIC;
        }
    }

    
}

