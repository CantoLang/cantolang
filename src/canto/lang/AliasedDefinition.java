/* Canto Compiler and Runtime Engine
 *
 * AliasedDefinition.java
 *
 * Copyright (c) 2018-2026 by cantolang.org
 * All rights reserved.
 */

package canto.lang;

import java.util.LinkedList;
import java.util.List;


/**
 * An AliasedDefinition is a lightweight wrapper around another Definition.
 * All calls are delegated to the wrapped definition except calls that
 * retrieve the definition's name, which return the alias's name instead.
 *
 * Structurally modeled on DefinitionFlavor: not an ExternalDefinition, not
 * an owner of its own AST children. Its own inherited `name` field holds
 * the alias; the wrapped definition is stored in `def`.
 */
public class AliasedDefinition extends ComplexDefinition {

    Definition def;

    public AliasedDefinition(Definition def, NameNode alias) {
        super(alias);
        this.def = def;
    }

    /** Returns the wrapped definition. */
    public Definition getAliasedDefinition() {
        return def;
    }

    // Name-related: use the alias (stored via super(alias) in our own name field).
    // - getName() and getNameNode() are inherited and return alias's name.
    // - getFullName() delegates to the wrapped def; using the alias's own name
    //   would produce a full name relative to whatever owner happens to be set
    //   on this wrapper (which is fragile since the wrapper may be reparented
    //   by setContents cascades).

    @Override
    public String getFullName() {
        return def == null ? "" : def.getFullName();
    }

    // Aliases signal a name whose meaning may change in context.

    @Override
    public boolean isDynamic() {
        return true;
    }

    @Override
    public Durability getDurability() {
        return Durability.IN_CONTEXT;
    }

    // Delegation — everything else goes to the wrapped definition.

    @Override
    public Definition getOwner() {
        return def == null ? null : def.getOwner();
    }

    @Override
    public Access getAccess() {
        return def.getAccess();
    }

    @Override
    public Type getType() {
        return def.getType();
    }

    @Override
    public Type getSuper() {
        return def.getSuper();
    }

    @Override
    public Type getSuper(Context context) {
        return def.getSuper(context);
    }

    @Override
    public NamedDefinition getSuperDefinition() {
        return def.getSuperDefinition();
    }

    @Override
    public NamedDefinition getSuperDefinition(Context context) {
        return def.getSuperDefinition(context);
    }

    @Override
    public boolean isSuperType(String name) {
        return def.isSuperType(name);
    }

    @Override
    public boolean isPrimitive() {
        return ((CantoNode) def).isPrimitive();
    }

    @Override
    public boolean isAbstract(Context context) {
        return def.isAbstract(context);
    }

    @Override
    public boolean hasSub(Context context) {
        return def.hasSub(context);
    }

    @Override
    public boolean hasNext(Context context) {
        return def.hasNext(context);
    }

    @Override
    public LinkedList<Definition> getNextList(Context context) {
        return def.getNextList(context);
    }

    @Override
    public List<ParameterList> getParamLists() {
        return def.getParamLists();
    }

    @Override
    public List<Construction> getConstructions(Context context) {
        return def.getConstructions(context);
    }

    @Override
    public CantoNode getContents() {
        return def.getContents();
    }

    @Override
    public String getFullNameInContext(Context context) {
        return def.getFullNameInContext(context);
    }

    @Override
    public boolean isSubDefinition(Definition subDef) {
        return def.isSubDefinition(subDef);
    }

    @Override
    public Object getChildData(NameNode childName, Type type, Context context, ConstructionList args) throws Redirection {
        return def.getChildData(childName, type, context, args);
    }

    @Override
    public Definition getChildDefinition(NameNode name, Context context) {
        return def.getChildDefinition(name, context);
    }

    @Override
    public Object getChild(NameNode node, ConstructionList args, IndexList indexes, ConstructionList parentArgs, Context context, boolean generate, boolean trySuper, Object parentObj, Definition resolver) throws Redirection {
        return def.getChild(node, args, indexes, parentArgs, context, generate, trySuper, parentObj, resolver);
    }

    /** Construct this definition with the specified arguments in the specified context. */
    @Override
    public Object instantiate(ConstructionList args, IndexList indexes, Context context) throws Redirection {
        return def.instantiate(args, indexes, context);
    }

    @Override
    public Definition getUltimateDefinition(Context context) {
        if (Name.THIS.equals(getName())) {
            // this is to make sure an object wrapper is created
            // when this definition is instantiated
            return this;
        } else if (def instanceof AliasedDefinition) {
            return def.getUltimateDefinition(context);
        } else {
            return def;
        }
    }

    @Override
    public Site getSite() {
        if (def instanceof Site) {
            return (Site) def;
        } else {
            return def.getSite();
        }
    }

    @Override
    DefinitionTable getDefinitionTable() {
        return def.getDefinitionTable();
    }

    /** Kept for callers that previously received an AliasedDefinition through an
     *  ExternalDefinition-typed variable and called getDefForContext. Returns
     *  this (the aliased view).
     */
    public Definition getDefForContext(Context context, ConstructionList args) {
        return this;
    }
}
