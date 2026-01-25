/* Canto Compiler and Runtime Engine
 * 
 * BoundDefinition.java
 *
 * Copyright (c) 2018, 2019 by cantolang.org
 * All rights reserved.
 */

package canto.lang;

import java.util.*;

import canto.runtime.Log;

/**
 * A Definition is an object which describes a class of objects.
 *
 * @author Michael St. Hippolyte
 * @version $Revision: 1.5 $
 */

public class BoundDefinition extends NamedDefinition {
    private static final Log LOG = Log.getLogger(BoundDefinition.class);

    private Definition def;
    private Context boundContext;   
   
    public BoundDefinition(Definition def, Context context) {
        super(def, context);
        this.def = def;
        boundContext = context.clone(false);
    }

    public Context getBoundContext() {
    	return boundContext;
    }
    
    public boolean isAbstract(Context context) { return def.isAbstract(boundContext); }
    public Access getAccess()                  { return def.getAccess(); }
    public Durability getDurability()          { return def.getDurability(); }
    public boolean isGlobal()                  { return def.isGlobal(); }
    public Type getType()                      { return def.getType(); }
    
    public Definition getDefinitionForArgs(ArgumentList args, Context argContext) {
        return def.getDefinitionForArgs(args, boundContext);    
    }

    public boolean hasSub(Context context)     { return def.hasSub(boundContext); }
    public boolean hasNext(Context context)    { return def.hasNext(boundContext); }
    public LinkedList<Definition> getNextList(Context context) {
        return def.getNextList(boundContext);
    }

    public Type getSuper()                     { return def.getSuper(); }
    public Type getSuper(Context context)      { return def.getSuper(boundContext); }

    public Type getSuperForChild(Context context, Definition childDef) {
        return def.getSuperForChild(boundContext, childDef);
    }

    public NamedDefinition getSuperDefinition(Context context) {
        return def.getSuperDefinition(boundContext);
    }

    public NamedDefinition getSuperDefinition() {
        return def.getSuperDefinition();
    }

    public Definition getSubdefInContext(Context context) {
        return def.getSubdefInContext(boundContext);
    }

    public boolean isSuperType(String name)    { return def.isSuperType(name); }

    public boolean equalsOrExtends(Definition definition) {
        return def.equalsOrExtends(definition);
    }

    public boolean equals(Definition definition, Context context) {
    	if (definition != null && definition instanceof BoundDefinition) {
    		BoundDefinition boundDef = (BoundDefinition) definition;
    		return (def.equals(boundDef.def) && boundContext.equals(boundDef.boundContext));
    	} else {
            return def.equals(definition, boundContext);
    	}
    }

    public String getFullName()                { return def.getFullName(); }

    public String getFullNameInContext(Context context) {
        return def.getFullNameInContext(boundContext);
    }

    public Definition getOwner()               { return def.getOwner(); }
    public Site getSite()                      { return def.getSite(); }
    public NameNode getNameNode()              { return def.getNameNode(); }
    public boolean isAnonymous()               { return def.isAnonymous(); }

    public Object instantiate(ArgumentList args, IndexList indexes, Context context) throws Redirection {
        return def.instantiate(args, indexes, boundContext);
    }

    public Definition getUltimateDefinition(Context context) {
    	Definition ultimateDef = def.getUltimateDefinition(boundContext);
    	if (ultimateDef == def) {
    		return this;
    	} else {
    	    return new BoundDefinition(ultimateDef, boundContext);
    	}
    }

    public Object getChild(NameNode name, ArgumentList args, IndexList indexes, ArgumentList parentArgs, Context argContext, boolean generate, boolean trySuper, Object parentObj, Definition resolver) throws Redirection {
        return def.getChild(name, args, indexes, parentArgs, boundContext, generate, trySuper, parentObj, resolver);
    }

    public Definition getChildDefinition(NameNode name, ArgumentList args, IndexList indexes, ArgumentList parentArgs, Context argContext, Definition resolver) {
        return def.getChildDefinition(name, args, indexes, parentArgs, boundContext, resolver);
    }

    public Definition getChildDefinition(NameNode name, Context context) {
        return def.getChildDefinition(name, boundContext);
    }

    public boolean hasChildDefinition(String name, boolean localAllowed) {
        return def.hasChildDefinition(name, localAllowed);
    }

    public KeepNode getKeep(String key)   { return def.getKeep(key); }

    public Object getChildData(NameNode childName, Type type, Context context, ArgumentList args) throws Redirection {
        return def.getChildData(childName, type, boundContext, args);
    }

    public ParameterList getParamsForArgs(ArgumentList args, Context argContext, boolean validate) {
        return def.getParamsForArgs(args, boundContext, validate);
    }

    public ParameterList getParamsForArgs(ArgumentList args, Context argContext) {
        return def.getParamsForArgs(args, boundContext);
    }

    public List<ParameterList> getParamLists() { return def.getParamLists(); }

    public List<Construction> getConstructions(Context context) {
        return def.getConstructions(boundContext);
    }

    public CantoNode getContents()             { return def.getContents(); }
    public boolean canHaveChildDefinitions()   { return def.canHaveChildDefinitions(); }
    public boolean isIdentity()                { return def.isIdentity(); }
    public boolean isFormalParam()             { return def.isFormalParam(); }
    public boolean isAlias()                   { return def.isAlias(); }
    public NameNode getAlias()                 { return def.getAlias(); }
    public boolean isParamAlias()              { return def.isParamAlias(); }
    public NameNode getParamAlias()            { return def.getParamAlias(); }
    public Instantiation getAliasInstance()    { return def.getAliasInstance(); }
    
    public boolean isAliasInContext(Context context) {
        return def.isAliasInContext(boundContext);
    }

    public NameNode getAliasInContext(Context context) {
        return def.getAliasInContext(boundContext);
    }

    public Instantiation getAliasInstanceInContext(Context context) {
        Instantiation instance = def.getAliasInstanceInContext(boundContext);
        instance = new ResolvedInstance(this, boundContext, instance.getArguments(), instance.getIndexes());
		return instance;
    }
    
    public boolean isReference()               { return def.isReference(); }
    public NameNode getReference()             { return def.getReference(); }
    public boolean isExternal()                { return def.isExternal(); }
    public boolean isCollection()              { return def.isCollection(); }
    
    public CollectionDefinition getCollectionDefinition(Context context, ArgumentList args) {
        return def.getCollectionDefinition(boundContext, args);
    }

     public DefinitionInstance getDefInstance(ArgumentList args, IndexList indexes) {
        return def.getDefInstance(args, indexes);
   
    }

    public Definition getOwnerInContext(Context context) {
        return def.getOwnerInContext(boundContext);
    }

    public Object instantiate(Context context) throws Redirection {
        return def.instantiate(boundContext);
    }
    
    public ListNode<?>[] getMatch(ArgumentList[] argLists, Context argContext) {
        return def.getMatch(argLists, boundContext);
    }

    public String getName() {
        return def.getName();
    }
}
