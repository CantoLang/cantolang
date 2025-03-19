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
import canto.util.EmptyList;
import canto.util.SingleItemList;

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

    /** The constructions comprising this definition */
    private List<Construction> constructions = null;

    /** The children of this definition. */
    protected Definition[] childDefs = null;

    /** The contents of this definition. */
    protected CantoNode contents = null;

    /** The context, if this definition is a copy initialized for a particular
     *  context, else null.
     */
    protected Context initContext = null;

    /** static data cache, unused if this definition is not declared to be static. **/
    private static class StaticData {
        public Object data = null;
    }
    private StaticData staticData;

    protected boolean hasStaticData() {
        return (staticData.data != null);
    }

    protected void setStaticData(Object data) {
        if (dur == Durability.GLOBAL || dur == Durability.STATIC) {
            LOG.debug("Setting " + (dur == Durability.GLOBAL ? "global" : "static") + " data for " + getFullName());
            staticData.data = data;
        }
    }

    protected Object getStaticData() {
        return staticData.data;
    }
    
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

    public boolean isAnonymous() {
        return true;
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

    @SuppressWarnings("unchecked")
    public List<Construction> getConstructions(Context context) {
        CantoNode contents = getContents();
        if (contents == null) {
            if (constructions == null) {
                constructions = new EmptyList<Construction>();
            }
          
        } else if (constructions == null || contents.isDynamic()) {
            if (contents instanceof List<?>) {
                constructions = (List<Construction>) contents;
            } else if (contents instanceof Block) {
                constructions = ((Block) contents).getConstructions(context);
                if (constructions == null) {
                    constructions = new EmptyList<Construction>();
                }
            } else if (contents instanceof Construction) {
                constructions = new SingleItemList<Construction>((Construction) contents);

            } else if (contents instanceof CollectionDefinition) {
                CollectionInstance collectionInstance = null;
                try {
                    collectionInstance = ((CollectionDefinition) contents).getCollectionInstance(context, null, null);
                } catch (Redirection r) {
                    LOG.error(" ******** unable to obtain collection instance for " + (contents == null ? "(anonymous)" : ((CollectionDefinition) contents).getName()) + " ******");
                }
                if (collectionInstance != null) {
                    constructions = new SingleItemList<Construction>((Construction) collectionInstance);
                } else {
                    constructions = new EmptyList<Construction>();
                }
            } else if (contents instanceof Definition) {
                constructions = ((Definition) contents).getConstructions(context);

            } else {
                if (contents != null) LOG.error(" ******** unexpected contents: " + contents.getClass().getName() + " ******");
                constructions = new EmptyList<Construction>();
            }
        }
        return constructions;
    }

    public Block getCatchBlock() {
        CantoNode contents = getContents();
        if (contents instanceof Block) {
            return ((Block) contents).getCatchBlock();
        } else {
            return null;
        }
    }

    public String getCatchIdentifier() {
        Block catchBlock = getCatchBlock();
        if (catchBlock != null) {
            return catchBlock.getCatchIdentifier();
        } else {
            return null;
        }
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

    /** Get a child of this definition as a definition. This only works for named definitions. 
     */
    public Definition getDefinitionChild(NameNode childName, Context context, ArgumentList args) {
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

    /** Instantiates a child definition in a specified context and returns the result.  The
     *  type parameter is only used if the child definition is external, in which case it
     *  is the Canto supertype of the external object.
     **/
    public Object getChildData(NameNode childName, Type type, Context context, ArgumentList args) throws Redirection {
        Object data = null;
        ArgumentList childArgs = childName.getArguments();
        List<Index> childIndexes = childName.getIndexes();

        // see if the argument definition has a child definition by that name
        Definition childDef = getChildDefinition(childName, childArgs, childIndexes, args, context, null);

        // if not, then look for alternatives 
        if (childDef == null) {

            // if not, then look for an aliased external definition
            if (isAlias()) {
                childDef = ExternalDefinition.createForName(this, new ComplexName(getAlias(), childName), type, getAccess(), getDurability(), context);
            }

            // if that didn't work, look for a special definition child
            if (childDef == null) {
                if (type != null && type.getName().equals("definition")) {
                    childDef = getDefinitionChild(childName, context, args);

                } else {
                    String cName = childName.getName();
                    if (cName.equals("defs") || cName.equals("descendants_of_type") || cName.equals("full_name")) {
                        ExternalDefinition externalDef = new ExternalDefinition("canto.lang.AnonymousDefinition", getParent(), getOwner(), type, getAccess(), Durability.DYNAMIC, this, null);
                        childDef = externalDef.getExternalChildDefinition(childName, context);
                    }
                }
            }
        }
        
        if (childDef != null) {
            if (childDef instanceof DefParameter) {
                return context.getParameter(childName, false, Object.class);
            }

            if (args == null) {
                Scope scope = context.peek();
                args = scope.args;
            }
            //context.unpush();
            //int numUnpushes = 1;
            int numPushes = 0;
            
            try {
                if (childDef instanceof ElementReference) {
                    childDef = ((ElementReference) childDef).getElementDefinition(context);
                    if (childDef == null) {
                        throw new Redirection(Redirection.STANDARD_ERROR, "No definition for element " + childName.toString());
                    }

                // if the child name has one or more indexes, and the definition is a
                // collection definition, get the appropriate element in the collection.
                } else if (childDef instanceof CollectionDefinition && childName.hasIndexes()) {
                    CollectionDefinition collectionDef = (CollectionDefinition) childDef;
                    childDef = collectionDef.getElementReference(context, childName.getArguments(), childName.getIndexes());
                }
                
                numPushes = context.pushSupersAndAliases(this, args, childDef);
                //context.repush();
                //numUnpushes = 0;

                Durability dur = childDef.getDurability();
                if (dur == Durability.STATIC || dur == Durability.GLOBAL) {
                    synchronized (childDef.staticData) {
                        if (childDef.staticData.data == null) {
                            childDef.staticData.data = context.constructDef(childDef, childArgs, childIndexes);
                        }
                    }
                    if (dur == Durability.GLOBAL && childArgs != null && childArgs.isDynamic()) {
                        data = context.constructDef(childDef, childArgs, childIndexes);
                    } else {
                        data = childDef.staticData.data;
                    }
                } else if (dur != Durability.DYNAMIC) {
                    data = context.getData(childDef, childDef.getName(), childArgs, childIndexes);
                    if (data == null) {
                        data = context.constructDef(childDef, childArgs, childIndexes);
                    }
                } else {
                    data = context.constructDef(childDef, childArgs, childIndexes);
                }

            } finally {
                if (numPushes > 0) {
                    //if (numUnpushes == 0) {
                    //    context.unpush();
                    //    numUnpushes = 1;
                    //}
                    while (numPushes > 0) {
                        context.pop();
                        numPushes--;
                    }
                }
                //if (numUnpushes > 0) {
                //    context.repush();
                //}
            }
            if (data == null) {
                data = NullValue.NULL_VALUE;
            }
        }
        return data;
    }

    public ParameterList getParamsForArgs(ArgumentList args, Context context, boolean validate) {
        ParameterList params = null;
        List<ParameterList> paramLists = getParamLists();
        int numParamLists = (paramLists == null ? 0 : paramLists.size());
        if (numParamLists > 1 || validate) {
            params = getMatch(args, context);

        } else if (numParamLists == 1) {
            params = paramLists.get(0);
        }
        return params;
    }


    public Scope getEntryForArgs(ArgumentList args, Context context) throws Redirection {
        ParameterList params = getParamsForArgs(args, context);
        // if args is nonnull and params is null or smaller than args, there is no match.
        if (args != null && args.size() > 0 && (params == null || params.size() < args.size())) {
            throw new Redirection(Redirection.STANDARD_ERROR, "Attempt to reference " + getFullName() + " with incorrect number of arguments");
        }

        return context.newScope(getDefinitionFlavor(context, params), this, params, args);
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
    
    /** Construct this definition without arguments in the specified context.
     */
    public Object instantiate(Context context) throws Redirection {
        try {
            return instantiate(null, null, context);
        } catch (NullPointerException e) {
            return null;
        }
    }

    /** Construct this definition with the specified arguments in the specified context. */
    public Object instantiate(ArgumentList args, List<Index> indexes, Context context) throws Redirection {
        Definition initializedDef = context.initDef(this, args, indexes);
        if (initializedDef == null && indexes != null) {
            initializedDef = context.initDef(this, args, null);
        } else if (initializedDef != this) {
            indexes = null;
        }
        if (initializedDef != this && initializedDef != null) {
            return initializedDef._instantiate(context, args, indexes);
        } else {
            return _instantiate(context, args, indexes);
        }
    }
        
    private Object _instantiate(Context context, ArgumentList args, List<Index> indexes) throws Redirection {        
        if ((dur == Durability.GLOBAL || dur == Durability.STATIC) && staticData.data != null && (args == null || !args.isDynamic())) {
            return staticData.data;
        }

        if (isAbstract(context)) {
            throw new Redirection(Redirection.STANDARD_ERROR, getFullName() + " is abstract; cannot instantiate");
        }

        if (dur == Durability.GLOBAL || dur == Durability.STATIC) {
            LOG.debug("Constructing " + (dur == Durability.GLOBAL ? "global" : "static") + " data for " + getFullName());
            staticData.data = construct(context, args, indexes);
            return staticData.data;
        } else {
            return construct(context, args, indexes);
        }
    }

    /* TODO: this is probably the best place to coerce the constructed data into the proper type for this definition. */
    protected Object construct(Context context, ArgumentList args, List<Index> indexes) throws Redirection {
        // dynamic objects are objects such as arrays with logic in their
        // initialization expressions
        Definition def = this;
        
        Object obj = context.construct(def, args);

        if (def.isCollection() && indexes != null) {
            obj = context.dereference(obj, indexes);
        }

        return obj;
    }

    
    /** Returns an object wrapping this definition with arguments and indexes. */ 
    public DefinitionInstance getDefInstance(ArgumentList args, List<Index> indexes) {
        return new DefinitionInstance(this, args, indexes);
    }
    
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
    public KeepNode getKeep(String key) {
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

