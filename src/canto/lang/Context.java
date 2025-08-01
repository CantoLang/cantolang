/* Canto Compiler and Runtime Engine
 * 
 * Context.java
 *
 * Copyright (c) 2018-2025 by cantolang.org
 * All rights reserved.
 */

package canto.lang;

import java.lang.reflect.Array;
import java.util.*;

import canto.runtime.CantoObjectWrapper;
import canto.runtime.CantoSession;
import canto.util.Holder;
import canto.runtime.Log;
import canto.util.StateFactory;
import canto.util.MappedArray;

/**
 * 
 */
public class Context {
    private static final Log LOG = Log.getLogger(Context.class);

    /** Anything bigger than this will be treated as runaway recursion.  The default value
     *  is DEFAULT_MAX_CONTEXT_SIZE.
     **/
    private final static int DEFAULT_MAX_CONTEXT_SIZE = 400;
    private static int maxSize = DEFAULT_MAX_CONTEXT_SIZE;

    /** Set the maximum size for any context.  The maximum number of levels of nesting
     *  may be slightly less than this number, since some constructions require temporary
     *  pushing of additional definitions onto the stack during instantiation.
     */
    public static void setGlobalMaxSize(int max) {
        maxSize = max;
    }

    private final static int MAX_POINTER_CHAIN_LENGTH = 10;
    
    private static int instanceCount = 0;
    public static int getNumContextsCreated() {
        return instanceCount;
    }
    public static void resetNumContextsCreated() {
        instanceCount = 0;
    }

    private static int numClonedContexts = 0;
    public static int getNumClonedContexts() {
        return numClonedContexts;
    }

    // -------------------------------------------
    // Context properties
    // -------------------------------------------

    private Context rootContext;
    private Scope rootScope = null;
    private Scope topScope = null;
    private Scope pinnedScope = null;
    private Definition definingDef = null;
    private NamedDefinition instantiatedDef = null;

    private Map<String, Object> cache = null;
    private Map<String, Pointer> keepMap = null;
    private Map<String, Map<String,Object>> siteKeeps = null;
    private Map<String, Object> globalKeep = null;
    private CantoSession session = null;
    
    private StateFactory stateFactory;
    private int stateCount;

    private int size = 0;

    private Scope abandonedScopes = null;

    private Stack<Scope> unpushedScopes = new Stack<Scope>();

    
    public Context() {
        instanceCount++;
        rootContext = this;
        stateFactory = new StateFactory();
        stateCount = stateFactory.lastState();
    }

    public Context(Site site) {
        instanceCount++;
        rootContext = this;
        stateFactory = new StateFactory();
        stateCount = stateFactory.lastState();
    }
   
    public Context(Context context) {
        this(context, false);
    }
  
    public Context(Context context, boolean clearKeep) {
        instanceCount++;
        rootContext = context.rootContext;
        // this is a copy, so don't pop past the current top

        // copy the global state variables
        stateCount = context.stateCount;
        stateFactory = new StateFactory(context.stateFactory);
        size = context.size;
        
        // copy the session
        session = context.session;

        keepMap = context.keepMap;
        if (!clearKeep) {
            rootScope = context.rootScope;
            // share the cache
            cache = context.cache;
            siteKeeps = context.siteKeeps;

            if (context.topScope != null) {
                if (context.topScope == context.rootScope) {
                    rootScope = newScope(context.rootScope, true);
                    setTop(rootScope);

                } else {
                    // clone the top scope only.  This assumes that scopes from the root
                    // up to just below the top will not be modified in the new context,
                    // because those scopes are shared with the original context.
                    Scope top = newScope(context.topScope, true);
                    top.setPrevious(context.topScope.getPrevious());
                    setTop(top);
                }
            }

        } else {
            cache = newHashMap(Object.class);
            siteKeeps = newHashMapOfMaps(Object.class);
            rootScope = newScope(context.rootScope, false);
            setTop(rootScope);
        }
    }

    public Object clone() {
        Context context = new Context(this, false);
        numClonedContexts++;
        return context;
    }
    
    public Context clone(boolean clearKeep) {
        Context context = new Context(this, clearKeep);
        numClonedContexts++;
        return context;
    }

    public int size() {
        return size;
    }
    
    // -------------------------------------------
    // push, pop, peek, etc.
    
    public void push(Definition def, ParameterList params, ArgumentList args) {
        DefinitionInstance defInstance = getContextDefInstance(def, args);
        Scope scope = newScope(defInstance.def, defInstance.def, params, defInstance.args);
        push(scope);
    }

    public void push(Definition def, ParameterList params, ArgumentList args, boolean newFrame) {
        DefinitionInstance defInstance = getContextDefInstance(def, args);
        if (defInstance.args != null && (defInstance.args != args || params == null)) {
            args = defInstance.args;
            params = defInstance.def.getParamsForArgs(args, this);
        }
        Definition superdef = (newFrame ? null : defInstance.def);
        Scope scope = newScope(defInstance.def, superdef, params, args);
        push(scope);
    }

    public void push(Definition instantiatedDef, Definition superdef, ParameterList params, ArgumentList args) {
        DefinitionInstance defInstance = getContextDefInstance(instantiatedDef, args);
        if (defInstance.args != null && defInstance.args != args) {
            args = defInstance.args;
            params = defInstance.def.getParamsForArgs(args, this);
        }
        Scope scope = newScope(defInstance.def, getContextDefinition(superdef), params, args);
        push(scope);
    }

    private void push(Scope scope) {
        boolean newFrame = (scope.superdef == null);
        boolean newScope = (scope.def != scope.superdef);

        if (scope.def == null) {
            throw new NullPointerException("attempt to push null definition onto context stack");
        }
        if (scope.def instanceof Site) {
            // if we are pushing a site, share the cache from the
            // root scope
            if (rootScope != null && !scope.equals(rootScope)) {
                scope.cache = rootScope.cache;
            }
    
        } else {
            Site scopeSite = scope.def.getSite();
            if (scopeSite != null && !(scopeSite instanceof Core) && topScope != null) {
                Site currentSite = topScope.def.getSite();
                if (!scopeSite.equals(currentSite)) {
                    Map<String, Object> siteKeep = siteKeeps.get(scopeSite.getName());
                    if (siteKeep == null) {
                        siteKeep = newHashMap(Object.class);
                        siteKeeps.put(scopeSite.getName(), siteKeep);
                    }
                    //scope.setSiteKeep(siteKeep);
                }
            }
        }

        stateCount = stateFactory.nextState();
        scope.setState(stateCount);
        _push(scope);
        if (scope.def instanceof NamedDefinition) {
            definingDef = scope.def;
    
            // add keep directives to this scope's list.
            NamedDefinition scopedef = (NamedDefinition) ((scope.superdef == null && scope.def instanceof NamedDefinition) ? scope.def : scope.superdef);
            Scope prev = scope.previous;
            if (!newFrame && prev != null) {
                if (sharesKeeps(scope, prev, true)) {
                    setKeepsFromScope(prev);
                } else {
                    List<KeepNode> keeps = scopedef.getKeeps();
                    if (keeps != null) {
                        Iterator<KeepNode> it = keeps.iterator();
                        while (it.hasNext()) {
                            KeepNode k = it.next();
                            keep(k);
                        }
                
                        String keepKeepKey = scopedef.getName() + ".keep";
                        String globalKeepKeepKey = Scope.makeGlobalKey(scopedef.getFullNameInContext(this)) + ".keep";
                        while (prev != null) {
                            @SuppressWarnings("unchecked")
                            Map<String, Object> keepKeep = (Map<String, Object>) prev.get(keepKeepKey, globalKeepKeepKey, null, true);
                            if (keepKeep != null) {
                                topScope.addKeepKeep(keepKeep);
                                break;
                            }
                            prev = prev.previous;
                        }
                    }
                }

            } else if (newScope) {
                List<KeepNode> keeps = scopedef.getKeeps();
                if (keeps != null) {
                    Iterator<KeepNode> it = keeps.iterator();
                    while (it.hasNext()) {
                        KeepNode k = it.next();
                        keep(k);
                    }
                }
                // Don't cache the keep map if the def owning the keeps is dynamic or the current instantiation
                // of the owning def is dynamic.
                //
                // Not sure if scope.args is right -- maybe should be the args for the scope where
                // scopedef shows up (assuming scopedef is right -- maybe should be scope.def) 
                if (scopedef.getDurability() != Definition.Durability.DYNAMIC && (scope.args == null || !scope.args.isDynamic())) {
                    String keepKeepKey = scopedef.getName() + ".keep";
                    String globalKeepKeepKey = Scope.makeGlobalKey(scopedef.getFullNameInContext(this)) + ".keep";
                    while (prev != null) {
                        @SuppressWarnings("unchecked")
                        Map<String, Object> keepKeep = (Map<String, Object>) prev.get(keepKeepKey, globalKeepKeepKey, null, true);
                        if (keepKeep != null) {
                            topScope.addKeepKeep( keepKeep);
                            break;
                        }
                        prev = prev.previous;
                    }
                }
            }
        }
    }

    private boolean sharesKeeps(Scope scope, Scope prev, boolean forward) {
        boolean shares = false;

        if (forward && prev.def.equals(scope.def) && (prev.superdef == null || scope.superdef == null || prev.superdef.equalsOrExtends(scope.superdef) || scope.superdef.equalsOrExtends(prev.superdef))) {
            shares = true;
        } else if (!forward && prev.def.isAlias()) {
            Definition prevSuperdef = prev.def.getSuperDefinition();
            Definition thisSuperdef = scope.def.getSuperDefinition();
            if (prevSuperdef != null && thisSuperdef != null && (prevSuperdef.equalsOrExtends(thisSuperdef) || thisSuperdef.equalsOrExtends(prevSuperdef))) {
                shares = true;
            }
        }
        return shares;
    }
    
    private synchronized void _push(Scope scope) {
        if (scope.def == null) {
            throw new NullPointerException("attempt to push null definition on context");
        }

        if (size >= maxSize) {
            throw new RuntimeException("blown context");
        } else if (size == 300) {
            System.err.println("**** context exceeding 300 ****");
        } else if (size == 200) {
            System.err.println("**** context exceeding 200 ****");
        } else if (size == 100) {
            System.err.println("**** context exceeding 100 ****");
        }

        if (rootScope == null) {
            if (scope.getPrevious() != null) {
                scope = newScope(scope, true);
            }
            rootScope = scope;
        } else {
            if (scope.getPrevious() != topScope) {
                if (scope.getPrevious() != null) {
                    scope = newScope(scope, true);
                }
                scope.setPrevious(topScope);
            }
        }
        setTop(scope);
    }

    public synchronized void pop() {
        Scope scope = _pop();

        if (topScope != null) {
            stateCount = topScope.getState();
            definingDef = topScope.def;
            if (sharesKeeps(scope, topScope, false)) {
                addKeepsFromScope(scope);
            } else if (scope.keepMap != null) {
                //topScope.cache.put
            }
        } else {
            stateCount = -1;
            definingDef = null;
        }
        oldScope(scope);
    }

    private Scope _pop() {
        if (size > 0) {
            if (topScope == pinnedScope) {
                LOG.error("attempt to pop context beyond limit");
                throw new IndexOutOfBoundsException("Illegal pop attempt; can only pop scopes pushed after this copy was made.");
            }
            Scope scope = topScope;
            setTop(scope.getPrevious());
            return scope;
        } else {
            return null;
        }
    }

    public void pushParam(DefParameter param, Construction arg) {
        if (size >= maxSize) {
            throw new RuntimeException("blown context");
        } else if (size < 1) {
            throw new NoSuchElementException("Cannot push a parameter onto an empty context");
        }

        Scope scope = topScope;
        if (scope.params == null || scope.params.size() == 0) {
            scope.params = new ParameterList(newArrayList(1, DefParameter.class));
        } else if (scope.params.size() == scope.origParamsSize) {
            scope.params = new ParameterList(newArrayList(scope.params));
        }
        if (scope.args == null || scope.args.size() == 0) {
            scope.args = new ArgumentList(newArrayList(1, Construction.class));
        } else if (scope.args.size() == scope.origArgsSize) {
            scope.args = new ArgumentList(newArrayList(scope.args));
        }

        scope.params.add(param);
        scope.args.add(arg);
    }

    public void popParam() {
        Scope scope = topScope;
        int n = scope.params.size();
        if (n >  0) {
            scope.params.remove(n - 1);
            // this scope may have started with fewer args than params
            scope.args.remove(scope.args.size() - 1);
        }
    }

    public synchronized Scope unpush() {
        if (size <= 1) {
            throw new IndexOutOfBoundsException("Attempt to unpush root scope in context");
        }
        Scope scope = _pop();
        unpushedScopes.push(scope);
        return scope;
    }

    public synchronized void repush() {
        Scope scope = unpushedScopes.pop();
        // by pre-setting the previous to topScope, we avoid the logic in _push that clones
        // the scope being pushed
        //scope.setPrevious(topScope);
        _push(scope);
    }


    public synchronized void unpop(Definition def, ParameterList params, ArgumentList args) {
        DefinitionInstance defInstance = getContextDefInstance(def, args);
        Definition contextDef = defInstance.def;
        if (defInstance.args != null && defInstance.args != args) {
            args = defInstance.args;
            params = contextDef.getParamsForArgs(args, this);
        }
        _push(newScope(contextDef, contextDef, params, args));
    }

    public synchronized void unpop(Scope scope) {
        _push(scope);
    }

    public synchronized Scope repop() {
        Scope scope = _pop();
        return scope;
    }

    public Scope peek() {
        return topScope;
    }

    public Scope doublePeek() {
        if (topScope != null) {
            return topScope.previous;
        } else {
            return null;
        }
    }

    /** Returns true if the passed definition is on the stack. **/
    public boolean contains(Definition def) {
        for (Scope scope = topScope; scope != null; scope = scope.previous) {
            if (scope.def.equalsOrExtends(def)) {
                return true;
            }
        }
        return false;
    }
    
    public Definition getDefiningDef() {
        return definingDef;
    }

    public void setTop(Scope scope) {
        if (topScope != null) {
            topScope.decRefCount();
        }
        topScope = scope;
        if (topScope != null) {
            topScope.incRefCount();
        }
        int calcSize = 0;
        Scope e = topScope;
        while (e != null) {
            calcSize++;
            e = e.previous;
        }
        size = calcSize;
    }

    
    // other operations

    /** Dereferences the passed definition if necessary to return the proper
     *  definition to push on the context.
     * @throws Redirection 
     */
    private Definition getContextDefinition(Definition definition) {
        Definition contextDef = null;

        // first resolve element references to element definitions,
        // which are handled below
        if (definition instanceof ElementReference) {
            Definition elementDef = ((ElementReference) definition).getElementDefinition(this);
            if (elementDef != null) {
                definition = elementDef;
            }
        }


        if (definition instanceof DefinitionFlavor) {
            contextDef = ((DefinitionFlavor) definition).def;
        } else if (definition instanceof TypeDefinition) {
            contextDef = ((TypeDefinition) definition).def;
        //} else if (definition instanceof ElementReference) {
        //    // array and table references defer to their collections for context.
        //    contextDef = ((ElementReference) definition).getCollectionDefinition();
        } else if (definition instanceof ElementDefinition) {
            Object element = ((ElementDefinition) definition).getElement(this);
            if (element instanceof Definition) {
                contextDef = getContextDefinition((Definition) element);
            } else if (element instanceof Instantiation) {
                Instantiation instance = (Instantiation) element;
                contextDef = getContextDefinition(instance.getDefinition(this));
            }
        }

        if (contextDef != null) {
            return contextDef;

        } else {
        // anything else has to be a named definition
           return definition;
        }
    }

    /** Dereferences the passed definition if necessary to return the proper
     *  definition to push on the context.
     * @throws Redirection 
     */
    private DefinitionInstance getContextDefInstance(Definition definition, ArgumentList args) {
        DefinitionInstance contextDefInstance = null;

        if (definition instanceof AliasedDefinition) {
            definition = definition.getUltimateDefinition(this);
        }
        
        // first resolve element references to element definitions,
        // which are handled below
        if (definition instanceof ElementReference) {
            Definition elementDef = ((ElementReference) definition).getElementDefinition(this);
            if (elementDef != null) {
                definition = elementDef;
            }
        }

        if (definition instanceof DefinitionFlavor) {
            definition = ((DefinitionFlavor) definition).def;
        } else if (definition instanceof TypeDefinition) {
            definition = ((TypeDefinition) definition).def;
        //} else if (definition instanceof ElementReference) {
        //    // array and table references defer to their collections for context.
        //    contextDef = ((ElementReference) definition).getCollectionDefinition();
        } else if (definition instanceof ElementDefinition) {
            Object element = ((ElementDefinition) definition).getElement(this);
            if (element instanceof Definition) {
                contextDefInstance = getContextDefInstance((Definition) element, args);
            } else if (element instanceof Instantiation) {
                Instantiation instance = (Instantiation) element;
                Definition instanceDef = instance.getDefinition(this);
                if (instanceDef != null) {
                    contextDefInstance = getContextDefInstance(instanceDef, instance.getArguments());
                }
            }
        }

        if (contextDefInstance != null) {
            return contextDefInstance;

        } else {
        // anything else has to be a named definition
           return new DefinitionInstance(definition, args, null);
        }
    }

    /** Checks to see if a name corresponds to a parameter, and if so returns
     *  the definition associated with it (i.e., the argument passed as the
     *  parameter's value).
     */
    public Definition getParameterDefinition(NameNode name, boolean inContainer) {
        return (Definition) getParameter(name, inContainer, Definition.class);
    }

    public Scope getParameterScope(NameNode name, boolean inContainer) {
        return (Scope) getParameter(name, inContainer, Scope.class);
    }
    
   public Object getParameter(NameNode name, boolean inContainer, Class<?> returnClass) {
        if (topScope == null) {
            return null;
        }
        Scope scope = topScope;

        if (inContainer) {
            Object paramObj = null;
            synchronized (this) {
                int i = 0;
                try {
                    //unpush();
                    //i++;
                    while (topScope != null) {
                        paramObj = getParameter(name, false, returnClass);
                        if (paramObj != null || topScope.getPrevious() == null) {
                            break;
                        }
                        unpush();
                        i++;
                    }
                } finally {
                    while (i > 0) {
                        repush();
                        i--;
                    }
                }
            }
            return paramObj;
        }

        boolean checkForChild = (name.numParts() > 1);
        String checkName  = name.getName();
        ArgumentList args = scope.args;
        int numArgs = args.size();
        ParameterList params = scope.params;
        int numParams = params.size();

        ArgumentList argArgs = null;
        ParameterList argParams = null;

        Object arg = null;
        int n = (numParams > numArgs ? numArgs : numParams);
        boolean mustUnpush = false;
        DefParameter param = null;
        Type paramType = null;

        int i;
        for (i = n - 1; i >= 0; i--) {
            param = params.get(i);
            String paramName = param.getName();
            if ((!checkForChild && checkName.equals(paramName)) || (checkForChild && checkName.startsWith(paramName + '.'))) {
                arg = args.get(i);
                break;
            }
        }

        if (arg == null) {
            return null;
        }

        Definition argDef = null;

        // for loop arguments are in the same context, not the next higher context
        if (!param.isInFor() && size > 1) {
            mustUnpush = true;
            unpush();
        }

        int numPushes = 0;
        Instantiation argInstance = null;
        try {
            if (arg instanceof Definition) {
                argDef = (Definition) arg;
            } else if (arg != ArgumentList.MISSING_ARG) {
                argDef = param.getDefinitionFor(this, arg);
                if (arg instanceof Instantiation && argDef != null) {
                    argInstance = (Instantiation) arg;
                    argArgs = argInstance.getArguments();
                    if (argInstance.isSuper() && argArgs == null) {
                        argArgs = topScope.args;
                    }
                    argParams = argDef.getParamsForArgs(argArgs, this);

                    //numPushes += pushParts(argInstance);
                }
            }
    
            if (argDef == null) {
                return null;
            }

            push(argDef, argParams, argArgs);
            numPushes++;
    
            paramType = param.getType();

            // dereference the argument definition if the reference includes indexes
            NameNode paramNameNode = (checkForChild ? (NameNode) name.getChild(0) : name);
            ArgumentList paramArgs = paramNameNode.getArguments();
            IndexList paramIndexes = paramNameNode.getIndexes();
            if ((paramArgs != null && paramArgs.size() > 0) || (paramIndexes != null && paramIndexes.size() > 0)) {
                Context argContext = this;
                if (mustUnpush) {
                    argContext = clone(false);
                    Scope clonedScope = newScope(unpushedScopes.peek(), true);
                    argContext.push(clonedScope);
                }
                argDef = argContext.initDef(argDef, paramArgs, paramIndexes);
            }
    
            // if this is a child of a parameter, resolve it.
            if (checkForChild && argDef != null) {

                // the child consists of everything past the first dot, which is the
                // same as a complex name consisting of every node in the name
                // except for the first
                ComplexName childName = new ComplexName(name, 1, name.getNumChildren());

                // see if the argument definition has a child definition by that name
                ArgumentList childArgs = childName.getArguments();
                Definition childDef = argDef.getChildDefinition(childName, childArgs, childName.getIndexes(), args, this, null);

                // if not, then look for an aliased external definition
                if (childDef == null) {
                    if (argDef.isAlias()) {
                        NameNode aliasName = argDef.getAlias();
                        childName = new ComplexName(aliasName, childName);
                        Definition ndef = peek().def;
                        childDef = ExternalDefinition.createForName(ndef, childName, param.getType(), argDef.getAccess(), argDef.getDurability(), this);
                    }

                    // if that didn't work, look for a special definition child
                    if (childDef == null) {
                        if (paramType != null && paramType.getName().equals("definition")) {
                            childDef = argDef.getDefinitionChild(childName, this, args);
                        }
                    }
            
                } else {
                    childDef = childDef.getUltimateDefinition(this);
                }
                argDef = childDef;

                if (arg instanceof PrimitiveValue && CantoObjectWrapper.class.equals(((PrimitiveValue) arg).getValueClass())) {
                    CantoObjectWrapper wrapper = (CantoObjectWrapper) ((Value) arg).getData();
                    Context argContext = wrapper.getContext();
                    argDef = new BoundDefinition(argDef, argContext);
                }
            }

            if (returnClass == Scope.class) {
                if (argDef == null) {
                    return null;
                } else {
                    return newScope(argDef, argDef, argParams, argArgs);
                }
            } else if (returnClass == Definition.class) {
                return argDef;
            } else if (returnClass == ResolvedInstance.class) {
                return new ResolvedInstance(argDef, this, argArgs, null);
            } else {
                Object data = (argDef == null ? null : construct(argDef, argArgs));
                if (data == null) {
                    return NullValue.NULL_VALUE;
                } else {
                    return data;
                }
            }

        } finally {
            // restore the stack
            while (numPushes-- > 0) {
                pop();
            }
            if (mustUnpush) {
                repush();
            }
            //validateSize();
        }
    }

    public Definition initDef(Definition def, ArgumentList args, IndexList indexes) {
        ExternalDefinition externalDef = null;
        
        if (def instanceof ExternalDefinition) {
            externalDef = (ExternalDefinition) def;
        } else if (def instanceof DefinitionFlavor && ((DefinitionFlavor) def).def instanceof ExternalDefinition) {
            externalDef = (ExternalDefinition) ((DefinitionFlavor) def).def;
        }
        
        if (externalDef != null) {
            def = externalDef.getDefForContext(this, args);
        }

        // if the reference has one or more indexes, and the definition is a
        // collection definition, get the appropriate element in the collection.
        // Note: this fails when there is an index on an aliased collection definition
        if (indexes != null && indexes.size() > 0 && def.isCollection()) {
            CollectionDefinition collectionDef = def.getCollectionDefinition(this, args);
            if (collectionDef != null) {
                if (def instanceof ElementReference) {
                    IndexList combinedIndexes = new IndexList(((ElementReference) def).getIndexes());
                    combinedIndexes.addAll(indexes);
                    indexes = combinedIndexes;
                }
                
                indexes = resolveIndexes(indexes);
                def = collectionDef.getElementReference(this, args, indexes);
            } else {
                def = null;
            }
        }

        return def;
    }

    public Definition dereference(Definition def, ArgumentList args, IndexList indexes) {
        if (indexes != null) {
            CollectionDefinition collectionDef = null;

            if (def instanceof CollectionDefinition) {
                collectionDef = (CollectionDefinition) def;

            } 
    
            Definition checkDef = def;
            ArgumentList checkArgs = args;
            while (collectionDef == null && checkDef != null) {
                int numAliasPushes = 0;
                ParameterList aliasParams = checkDef.getParamsForArgs(checkArgs, this);
                try {
                    Instantiation checkInstance = null;
                    while (checkDef != null && checkDef.isAliasInContext(this)) {
                        checkInstance = checkDef.getAliasInstanceInContext(this);
                        checkArgs = checkInstance.getArguments();    // getUltimateInstance(this).getArguments();
                        aliasParams = (checkDef != null ? checkDef.getParamsForArgs(checkArgs, this) : null);
                        push(checkDef, aliasParams, checkArgs, false);
                        numAliasPushes++;
                        checkDef = (Definition) checkInstance.getDefinition(this);  // lookup(context, false);
                    }
                    if (checkDef != null) {
                        if (checkDef instanceof CollectionDefinition) {
                            collectionDef = (CollectionDefinition) checkDef;
                            return collectionDef.getElementReference(this, args, indexes);
                        } else if (!(checkDef instanceof IndexedMethodDefinition)) {
                            ResolvedInstance instance = new ResolvedInstance(checkDef, this, checkArgs, null);
                            return new IndexedInstanceReference(instance, indexes);
                        } else {
                            checkDef = null;
                        }
                    }
                } finally {
                    while (numAliasPushes-- > 0) {
                        pop();
                    }
                }
            }
    
            if (collectionDef != null) {
                def = collectionDef.getElementReference(this, args, indexes);
            }
        }
        return def;
    }

    public Object dereference(Object data, IndexList indexes) {
        // dereference collections represented as values
        if (data instanceof Value) {
            data = ((Value) data).getData();
            if (data == null) {
                return null;
            }
        }

        // dereference collections represented as CantoArray objects
        if (data instanceof CantoArray) {
            data = ((CantoArray) data).getArrayObject();
        }
        Iterator<Index> it = indexes.iterator();
        while (it.hasNext() && data != null) {
            Index index = it.next();
            data = getElement(data, index);
        }
        return data;
    }


    
    /** Checks to see if a name corresponds to a parameter, and if so returns
     *  the parameter type, otherwise null.
     */
    public Type getParameterType(NameNode node, boolean inContainer) {
        if (topScope == null) {
            return null;
        }
        Type paramType = null;
        if (inContainer) {
            synchronized (this) {
                int i = 0;
                try {
                    while (topScope != null) {
                        paramType = getParameterType(node, false);
                        if (paramType != null || topScope.getPrevious() == null) {
                            break;
                        }
                        unpush();
                        i++;
                    }
                } finally {
                    while (i > 0) {
                        repush();
                        i--;
                    }
                }
            }
        } else if (node.numParts() > 1) {
            Definition paramDef = getParameterDefinition(node, false);
            if (paramDef != null) {
                paramType = paramDef.getType();
            }
    
        } else {
            String name = node.getName();
            int numParams = topScope.params.size();
            for (int i = numParams - 1; i >= 0; i--) {
                DefParameter param = topScope.params.get(i);
                String paramName = param.getName();
                if (name.equals(paramName)) {
                    paramType = param.getType();
                }
            }
        }
        // Warning: this might not work on multidimensional arrays that have fewer
        // indexes than dimenstions
        if (paramType != null && paramType.isCollection() && node.hasIndexes()) {
            Definition def = paramType.getDefinition();
            if (def instanceof CollectionDefinition) {
                paramType = ((CollectionDefinition) def).getElementType();
            }
        }
        return paramType;
    }

    /** Returns true if a parameter of the specified name is present at the top of the
     *  context stack.
     */
    public boolean paramIsPresent(NameNode nameNode) {
        if (topScope != null) {
            return topScope.paramIsPresent(nameNode, true);
        } else {
            return false;
        }
    }


    /** Returns true if this is the instantiation of a child of a parameter at the
     *  top of the context stack.
     */
    public boolean isParameterChildDefinition(NameNode node) {
        if (node == null) {
            return false;
        }
        String name  = node.getName();
        if (topScope == null) {
            return false;
        }
        int numParams = topScope.params.size();
        for (int i = numParams - 1; i >= 0; i--) {
            DefParameter param = topScope.params.get(i);
            String paramName = param.getName();
            if (name.startsWith(paramName + '.')) {
                return true;
            }
        }
        return false;
    }

    public Object getArgumentForParameter(NameNode name, boolean checkForChild, boolean inContainer) {
        if (topScope == null || topScope == rootScope) {
            return null;
        }

        Scope scope = topScope;

        if (inContainer) {
            while (!scope.paramIsPresent(name, true)) {
                if (scope.previous == null || scope.previous == rootScope) {
                    break;
                }
                scope = scope.previous;
            }
        }

        String checkName  = name.getName();
        ArgumentList args = scope.args;
        int numArgs = args.size();
        ParameterList params = scope.params;
        int numParams = params.size();

        DefParameter param = null;
        Construction arg = null;
        int i;
        int n = (numParams > numArgs ? numArgs : numParams);
        for (i = n - 1; i >= 0; i--) {
            param = params.get(i);
            String paramName = param.getName();
            if ((!checkForChild && checkName.equals(paramName)) || (checkForChild && checkName.startsWith(paramName + '.'))) {
                arg = args.get(i);
                break;
            }
        }

        if (arg != null && scope.args.isDynamic()) {
            ArgumentList argHolder = new ArgumentList(true);
            argHolder.add(arg);
            return argHolder;
        } else {
            return arg;
        }
    }

    
    public Object getParameterInstance(NameNode name, boolean checkForChild, boolean inContainer, Definition argOwner) throws Redirection {
        Scope scope = topScope;
        int numUnpushes = 0;
        while (!scope.def.equalsOrExtends(argOwner)) {
            if (scope.previous == null || scope.previous == rootScope) {
                numUnpushes = 0;
                break;
            }
            numUnpushes++;
            scope = scope.previous;
        }

        try {
            for (int i = 0; i < numUnpushes; i++) {
                unpush();
            }
            return getParameterInstance(name, checkForChild, inContainer);    
        } finally {
            while (numUnpushes-- > 0) {
                repush();
            }
    
            //validateSize();
        }
    }
    
    
    /** Checks to see if a name corresponds to a parameter, and if so returns the instance
     *  or, if the instantiate flag is true, instantiates it and returns the generated data.
     */
    public Object getParameterInstance(NameNode name, boolean checkForChild, boolean inContainer) throws Redirection {
//        if (checkForChild) {
//            return getParameter(name, inContainer, Object.class);
//        }    

        if (topScope == null || topScope == rootScope) {
            return null;
        }

        Scope scope = topScope;
        int numUnpushes = 0;

        if (inContainer) {
            while (!scope.paramIsPresent(name, false)) {
                if (scope.previous == null || scope.previous == rootScope) {
                    numUnpushes = 0;
                    break;
                }
                numUnpushes++;
                scope = scope.previous;
            }
        }

        String checkName  = name.getName();
        ArgumentList args = scope.args;
        int numArgs = args.size();
        ParameterList params = scope.params;
        int numParams = params.size();

        DefParameter param = null;
        Object arg = null;
        int i;
        int n = (numParams > numArgs ? numArgs : numParams);
        for (i = n - 1; i >= 0; i--) {
            param = params.get(i);
            String paramName = param.getName();
            if ((!checkForChild && checkName.equals(paramName)) || (checkForChild && checkName.startsWith(paramName + '.'))) {
                arg = args.get(i);
                break;
            }
        }

        if (arg == null || arg == ArgumentList.MISSING_ARG) {
            return null;

        } else {
            try {
                Object data = null;

                for (i = 0; i < numUnpushes; i++) {
                    unpush();
                }

                Context resolutionContext = this; //(arg instanceof ResolvedInstance ? ((ResolvedInstance) arg).getResolutionContext() : this);
        
                if (checkForChild) {
                    // the child consists of everything past the first dot, which is the
                    // same as a complex name consisting of every node in the name
                    // except for the first
                    IndexList indexes = ((NameNode) name.getChild(0)).getIndexes();
                    ComplexName childName = new ComplexName(name, 1, name.getNumChildren());
                    data = resolutionContext.instantiateParameterChild(childName, param, arg, indexes);

                } else {
                    data = resolutionContext.instantiateParameter(param, arg, name);
                }
                return data;
    
            } finally {
                while (numUnpushes-- > 0) {
                    repush();
                }
                //validateSize();
            }
        }
    }


    private Object instantiateParameter(DefParameter param, Object arg, NameNode argName) throws Redirection {
        Object data = null;
        ArgumentList argArgs = argName.getArguments();
        IndexList indexes = argName.getIndexes();
        int numUnpushes = 0;
        boolean pushedOwner = false;

        Context resolutionContext = this;
        if (arg instanceof ResolvedInstance) {
            resolutionContext = ((ResolvedInstance) arg).getResolutionContext();
            //if (resolutionContext != this) {
            //    return resolutionContext.instantiateParameter(param, arg, argName);
            //}
        }

        if (arg instanceof CantoNode) {
            Definition argOwner = ((CantoNode) arg).getOwner();
            Scope scope = topScope;
            while (!scope.def.equalsOrExtends(argOwner)) {
                if (scope.previous == null || scope.previous == rootScope) {
                    numUnpushes = (size > 1 && !param.isInFor() ? 1 : 0);
                    break;
                }
                numUnpushes++;
                scope = scope.previous;
            }
        }
        numUnpushes = Math.max((size > 1 && !param.isInFor() ? 1 : 0), numUnpushes);

        if (arg instanceof Instantiation) {
            Instantiation argInstance = (Instantiation) arg;
            boolean isParam = argInstance.isParameterKind();
            CantoNode argRef = argInstance.getReference();
            Definition argDef = null;

            // handle parameters which reference parameters in their containers
            if (argRef instanceof NameNode && isParam) {
                NameNode refName = (NameNode) argRef;
                for (int i = 0; i < numUnpushes; i++) {
                    unpush();
                }
                try {
                    argDef = argInstance.getDefinition(this);
                    if (!refName.hasArguments() && argArgs != null) {
                        while (numUnpushes > 0) {
                            repush();
                            numUnpushes--;
                        }
                        data = argDef.instantiate(argArgs, indexes, this);

                    } else {
                        boolean inContainer = argInstance.isContainerParameter(resolutionContext);
                        data = resolutionContext.getParameterInstance(refName, argInstance.isParamChild, inContainer);
                        if (argDef != null) {
                            String key = argInstance.getName();
                            // too expensive for a large loop
                            //if (argInstance.isForParameter()) {
                            //    key = key + addLoopModifier();
                            //}
                            putData(argDef, argArgs, argDef, argArgs, indexes, key, data, null);
                        }
                    }
            
                } finally {
                    for (int i = 0; i < numUnpushes; i++) {
                        repush();
                    }
                }
                if (data != null) {
                    if (indexes != null) {
                        data = dereference(data, indexes);
                    }
                    return data;
                } else {
                    return NullValue.NULL_VALUE;
                }
            }
        }

        Definition argDef = null;
        boolean unpoppedArgDef = false;
        try {
            if (arg instanceof Definition) {
                if (arg instanceof ElementDefinition) {
                    Object element = ((ElementDefinition) arg).getElement();
                    if (element instanceof Definition) {
                        argDef = (Definition) element;
    
                    } else if (element instanceof Instantiation) {
                        Instantiation instance = (Instantiation) element;
                        arg = instance;
                        argDef = instance.getDefinition(this);
                        if (argDef != null && argArgs == null) {
                            argArgs = instance.getArguments();
                            if (instance.isSuper() && argArgs == null) {
                                argArgs = topScope.args;
                            }
                        }
    
                    } else {
                        if (element instanceof Value) {
                            data = ((Value) element).getData();
                        } else if (element instanceof Construction) {
                            data = ((Construction) element).getData(this);
                        } else if (element instanceof ValueGenerator) {
                            data = ((ValueGenerator) element).getValue(this).getData();
                        }  else {
                            data = element;
                            //throw new Redirection(Redirection.STANDARD_ERROR, "unrecognized element class: " + element.getClass().getName());
                        }
    
                        arg = element;
                    }
                } else {  // if (arg instanceof CollectionDefinition) {
                    argDef = (Definition) arg;
                }
                for (int i = 0; i < numUnpushes; i++) {
                    unpush();
                }
    
            } else {
                for (int i = 0; i < numUnpushes; i++) {
                    unpush();
                }
                try {
                    if (data == null) {
                        if (arg instanceof Instantiation) {
                            if (argArgs != null || indexes != null) {
                                for (int i = 0; i < numUnpushes; i++) {
                                    repush();
                                }
                                if (indexes != null) {
                                    indexes = instantiateIndexes(indexes);
                                }
                                if (argArgs != null) {
                                    argArgs = resolveArguments(argArgs);
                                }
                                for (int i = 0; i < numUnpushes; i++) {
                                    unpush();
                                }
                            }
                            if (arg instanceof ResolvedInstance) {
                                ResolvedInstance ri = (ResolvedInstance) arg;
                                if (argArgs != null && argArgs.size() > 0) {
                                    ri.setArguments(argArgs);
                                }
                                data = ri.getData(ri.getResolutionContext());
                            } else {
                                Instantiation instance = (Instantiation) arg;
                                if (instance.isSuper() && argArgs == null) {
                                    argArgs = topScope.args;
                                }
                                if (argArgs != null) {
                                    if (indexes == null || instance.getIndexes() == null) {
                                        instance = new Instantiation(instance, argArgs, indexes);
                                        indexes = null;
                                    } else {
                                        instance = new Instantiation(instance, argArgs, instance.getIndexes());
                                    }
                                }
                                data = instance.getData(this);
                            }
                    
                        } else if (arg instanceof PrimitiveValue) {
                            data = arg; //((PrimitiveValue) arg).getData();
                    
                        } else if (arg instanceof Expression) {
                            data = ((ValueGenerator) arg).getValue(this).getData();
                
                        } else if (arg instanceof Construction) { 
                            argDef = param.getDefinitionFor(this, (Construction) arg);
                            // there must be a better way to avoid this, but for now...
                            if (argDef == null && numUnpushes > 0) {
                                for (int i = 0; i < numUnpushes; i++) {
                                    repush();
                                }
                                numUnpushes = 0;
                                argDef = param.getDefinitionFor(this, (Construction) arg);
                            }
                            if (argDef != null && arg instanceof Instantiation) {
                                Instantiation instance = (Instantiation) arg;
                                argArgs = instance.getArguments();
                                if (instance.isSuper() && argArgs == null) {
                                    argArgs = topScope.args;
                                }
                            }

                        } else if (arg instanceof ValueGenerator) {
                            data = ((ValueGenerator) arg).getValue(this).getData();
    
                        } else {
                            data = arg;
                        }
    
                        if (data != null) {
                            // if the name is indexed, and the argument is raw data, then get
                            // the appropriate item in the collection.  In such a case, the data
                            // must be of the appropriate type for the indexes.
                            if (indexes != null) {
                                data = dereference(data, indexes);
                            }
                        } else {
                            data = NullValue.NULL_VALUE;
                        }
    
                    }
                } finally {
                    ;
                }
            }

            if (argDef != null) {
                // this is to partly handle definitions returned from out of context, e.g. the
                // ones returned by descendants_of_type
                if (arg instanceof Definition) {
                    Definition argOwner = ((CantoNode) arg).getOwner();
                    if (!topScope.def.equalsOrExtends(argOwner)) {
                        push(argOwner, null, null, true);
                        pushedOwner = true;
                    }
                }

                if (!(argDef.isFormalParam())) {
                    unpop(argDef, argDef.getParamsForArgs(argArgs, this, false), argArgs);
                    unpoppedArgDef = true;
                }

                // if the name has one or more indexes, and the argument definition is a
                // collection definition, get the appropriate element in the collection.
                if (argDef instanceof CollectionDefinition && indexes != null) {
                    CollectionDefinition collectionDef = (CollectionDefinition) argDef;
                    argDef = collectionDef.getElementReference(this, argArgs, indexes);
                }
                data = constructDef(argDef, argArgs, indexes);
            }
        } finally {
            if (unpoppedArgDef) {
                repop();
            }

            if (pushedOwner) {
                pop();
            }
    
            for (int i = 0; i < numUnpushes; i++) {
                repush();
            }
    
            //validateSize();
        }
        return data;
    }

    private IndexList instantiateIndexes(IndexList indexes) {
        if (indexes == null || indexes.size() == 0) {
            return indexes;
        }
        ListNode<Index> instantiatedIndexes = new ListNode<Index>(indexes.size());
        Iterator<Index> it = indexes.iterator();
        while (it.hasNext()) {
            Index index = it.next();
            Index instantiatedIndex = index.instantiateIndex(this);
            instantiatedIndexes.add(instantiatedIndex);
        }

        return new IndexList(instantiatedIndexes);
    }
    
    public IndexList resolveIndexes(IndexList indexes) {
        if (indexes == null || indexes.size() == 0) {
            return indexes;
        }
        ListNode<Index> resolvedIndexes = new ListNode<Index>(indexes.size());
        Iterator<Index> it = indexes.iterator();
        while (it.hasNext()) {
            Index index = it.next();
            Index resolvedIndex = index.resolveIndex(this);
            resolvedIndexes.add(resolvedIndex);
        }

        return new IndexList(resolvedIndexes);
    }
    
    private ArgumentList resolveArguments(ArgumentList args) {
        if (args == null || args.size() == 0) {
            return args;
        }
        ArgumentList resolvedArgs = args;
        for (int i = 0; i < args.size(); i++) {
            Construction arg = args.get(i);
            if (arg instanceof Instantiation && !(arg instanceof ResolvedInstance)) {
                Instantiation argInstance = (Instantiation) arg;
                if (resolvedArgs == args) {
                    resolvedArgs = new ArgumentList(args);
                }
                ResolvedInstance ri = new ResolvedInstance(argInstance, this, false);
                resolvedArgs.set(i, ri);
            }
        }

        return resolvedArgs;
    }
    
    private Object instantiateParameterChild(ComplexName childName, DefParameter param, Object arg, IndexList indexes) {
        if (arg instanceof Value && !(arg instanceof Instantiation)) {
            Object val = ((Value) arg).getData();
            if (val instanceof CantoObjectWrapper) {
                arg = val;
            }
        }
        Object data = null;
        Instantiation instance = (arg instanceof Instantiation ? (Instantiation) arg : null);
        int numUnpushes = 0;
        if (arg instanceof CantoNode) {
            Definition argOwner = ((CantoNode) arg).getOwner();
            Definition argOwnerOwner = (argOwner != null ? argOwner.getOwner() : null);
            Scope scope = topScope;
            while (!scope.def.equalsOrExtends(argOwner) && !scope.def.equalsOrExtends(argOwnerOwner)) {
                if (scope.previous == null || scope.previous == rootScope) {
                    numUnpushes = 0;
                    break;
                }
                numUnpushes++;
                scope = scope.previous;
            }
        }
        numUnpushes = Math.max((size > 1 && !param.isInFor() ? 1 : 0), numUnpushes);

        Context fallbackContext = this;
        Definition argDef = null;
        ArgumentList args = null;
        
        try {
            for (int i = 0; i < numUnpushes; i++) {
                unpush();
            }
    
            if (arg instanceof Definition) {
                if (arg instanceof ElementDefinition) {
                    Object contents = ((ElementDefinition) arg).getContents();
                    if (contents instanceof Definition) {
                        argDef = (Definition) contents;
    
                    } else if (contents instanceof Instantiation) {
                        instance = (Instantiation) contents;
                        arg = instance;
                        argDef = instance.getDefinition(this);
                        if (instance instanceof ResolvedInstance) {
                            fallbackContext = ((ResolvedInstance) instance).getResolutionContext();
                        }
    
                    } else {
                        argDef = (Definition) arg;
                        arg = contents;
                    }
                } else {
                    argDef = (Definition) arg;
                }
            } else if (arg instanceof CantoObjectWrapper) {
                CantoObjectWrapper wrapper = (CantoObjectWrapper) arg;
                // TODO: this doesn't handle children of children
                data = wrapper.getChildData(childName);
        
            } else {
                if (arg instanceof Construction) {
                    argDef = param.getDefinitionFor(this, (Construction) arg);
        
                    if (arg instanceof ResolvedInstance) {
                        fallbackContext = ((ResolvedInstance) arg).getResolutionContext();
                    }
            
                } else if (arg instanceof Map<?,?> && arg != null) {
                    String nm = childName.getName();
                    if (nm.equals("keys")) {
                        Set<?> keySet = ((Map<?,?>) arg).keySet();
                        List<String> keys = new ArrayList<String>(keySet.size());
                        Iterator<?> it = keySet.iterator();
                        while (it.hasNext()) {
                            keys.add(it.next().toString());
                        }
                        data = keys;
                
                    } else {
                        data = ((Map<?,?>) arg).get(childName.getName());
                    }
    
                } else {
                    data = arg;
                }

                if (instance != null && instance.isParameterKind()) {
                    Context resolutionContext = this;
                    while (instance.isParameterKind()) {
                        if (instance instanceof ResolvedInstance) {
                            resolutionContext = ((ResolvedInstance) instance).getResolutionContext();
                        }
                        String checkName = instance.getName();
                        NameNode instanceName = instance.getReferenceName();
                        Scope scope = resolutionContext.topScope;
                        if (instance.isContainerParameter(resolutionContext)) {
                            while (scope != null) {
                                if (scope.paramIsPresent(instanceName, true)) {
                                    break;
                                }
                                scope = scope.previous;
                            }
                            if (scope == null) {
                                return null;
                            }
                        }
                
                        args = scope.args;
                        int numArgs = args.size();
                        ParameterList params = scope.params;
                        int numParams = params.size();
      
                        DefParameter p = null;
                        Object a = null;
                        int i;
                        int n = (numParams > numArgs ? numArgs : numParams);
                        for (i = n - 1; i >= 0; i--) {
                            p = params.get(i);
                            String paramName = p.getName();
                            if (checkName.equals(paramName)) {
                                a = args.get(i);
                                break;
                            }
                        }
                        if (a == null) {
                            break;
                        }
                
                        if (a instanceof Value && !(a instanceof Instantiation)) {
                            Object o = ((Value) a).getData();
                            a = o;
                        }
                
                        if (a instanceof Definition) {
                            if (a instanceof NamedDefinition && p.getType().isTypeOf("definition")) {
                                argDef = new AliasedDefinition((NamedDefinition) a, instance.getReferenceName());
                            } else {
                                argDef = (Definition) a;
                            }
                            break;
                        } else if (!(a instanceof Instantiation)) {
                            break;
                        }

                        instance = (Instantiation) a;
                        arg = a;
                        param = p;
                        if (!p.isInFor()) {
                            unpush();
                            numUnpushes++;
                        }
                    }
                    if (instance instanceof ResolvedInstance) {
                        resolutionContext = ((ResolvedInstance) instance).getResolutionContext();
                    }
                    //if (instance.getIndexes() == null && argIndexes != null) {
                    //    instance.setIndexes(argIndexes);
                    //}
                    if (instance.isParameterChild()) {
                        NameNode compName = new ComplexName(instance.getReferenceName(), childName);
                        data = resolutionContext.getParameter(compName, instance.isContainerParameter(resolutionContext), Object.class);
                    } else {
                        fallbackContext = resolutionContext;
                    }
                }
            }        

            if (data != null) {
                // if the name is indexed, and the argument is raw data, then get
                // the appropriate item in the collection.  In such a case, the data
                // must be of the appropriate type for the indexes.
                if (indexes != null) {
                    data = dereference(data, indexes);
                }
                return data;
            } 

            if (argDef != null) {
                args = (instance != null ? instance.getArguments() : null);
                IndexList argIndexes = (instance != null ? instance.getIndexes() : null);
                if (argDef.isIdentity() && (instance == null || !(instance instanceof ResolvedInstance))) {
                    Holder holder = getDefHolder(argDef.getName(), argDef.getFullNameInContext(this), args, argIndexes, false);
                    if (holder != null) {
                        if (holder.data instanceof CantoObjectWrapper) {
                            CantoObjectWrapper wrapper = (CantoObjectWrapper) holder.data;
                            data = wrapper.getChildData(childName);
                            if (indexes != null) {
                                data = dereference(data, indexes);
                            }
                            return data;
                    
                        } else if (holder.def != null && !holder.def.isIdentity()) {
                            argDef = holder.def;
                            args = holder.args;
                        }
                    }
                }
            }
    
        } finally {
            // un-unpush if necessary
            for (int i = 0; i < numUnpushes; i++) {
                repush();
            }
        }

        if (argDef != null) {
            argDef = initDef(argDef, args, indexes);

//    
//           The following line replaces the commented out section following it.  The commented
//           out section in some cases tries twice to instantiate the child, first with this
//           context and second with the fallbackContext, in cases where the argument resolves  
//           to a ResolvedInstance, in which case the fallbackContext gets the value of  
//           the ResolvedInstance's resolution context.  The problem with this is that often
//           the null return value is intentional, and not caused by being unable to resolve    
//           the child reference.  So, now we are trying something different.  We will use the
//           ResolvedInstance's resolution context first and only, when it exists.  Since
//           fallbackContext is initialized to this, we can achieve this by simply using
//           falllbackContext (no longer aptly named), and not falling back to anything.    
//
    
            data = fallbackContext.instantiateArgChild(childName, param.getType(), argDef, args, null);
    
//          // commented out to fix Array Element Child Array test, because of
//          // a reference to a loop parameter.
//    
//          //  for (int i = 0; i < numUnpushes; i++) {
//          //      unpush();
//          //  }
//
//            data = instantiateArgChild(childName, param.getType(), argDef, args, null);
//          //  for (int i = 0; i < numUnpushes; i++) {
//          //      repush();
//          //  }
//
//            if ((data == null || data == NullValue.NULL_VALUE) && fallbackContext != this) {
//                data = fallbackContext.instantiateArgChild(childName, param.getType(), argDef, args, null);
//            }
        }

        return data;
    }


    private Object instantiateArgChild(ComplexName name, Type paramType, Definition def, ArgumentList args, IndexList indexes) {

        int n = name.numParts();

        NameNode childName = name.getFirstPart();
        int numPushes = 0;

        try {
            // Keep track of intermediate definitions during alias dereferencing
            // by pushing them onto the context stack in case their parameters are
            // referenced in the child being instantiated.  Ensure however that
            // the original definition remains on top
            for (int i = 0; i < n - 1; i++) {
                if (def == null) {
                    break;
                }
                ParameterList params = def.getParamsForArgs(args, this);

                Definition childDef = null;

                if (def.isExternal()) {
                    push(def, params, args, false);
                    numPushes++;
                    childDef = def.getChildDefinition(childName, childName.getArguments(), childName.getIndexes(), null, this, null);

                } else {
                    push(def, params, args, false);
                    numPushes++;
                    childDef = def.getChildDefinition(childName, childName.getArguments(), childName.getIndexes(), null, this, null);
                    //pop();
                    //numPushes--;
                }
                numPushes += pushSupersAndAliases(def, args, childDef);
                def = childDef;
                if (def != null && childName != null) {
                    args = childName.getArguments();
                    def = initDef(def, childName.getArguments(), childName.getIndexes());
                }

                childName = (NameNode) name.getChild(i + 1);
            }

            return _instantiateArgChild(childName, paramType, def, args, indexes);

        } finally {
            while (numPushes-- > 0) {
                pop();
            }
            //validateSize();
        }
    }

    public Object getDescendant(Definition parentDef, ArgumentList args, NameNode name, boolean generate, Object parentObj) throws Redirection {
        Definition def = parentDef;

        // if this is a reference to a collection element, forward to its definition
        if (def instanceof ElementReference) {
            Definition elementDef = ((ElementReference) def).getElementDefinition(this);
            if (elementDef instanceof ElementDefinition) {
                // might have to fix the args and parentArgs here
                return ((ElementDefinition) elementDef).getChild(name, name.getArguments(), null, null, this, generate, true, parentObj, null); 
            }
        }

        Definition childDef = null;
        NameNode childName = name.getFirstPart();
        ArgumentList childArgs = childName.getArguments();
        boolean dynamicChild = (childArgs != null && childArgs.isDynamic());
        IndexList childIndexes = childName.getIndexes();
        int numPushes = 0;
        int numSuperPushes = 0;
        ComplexName restOfName = null;
        int numNameParts = name.numParts();
        if (numNameParts > 1) {
            restOfName = new ComplexName(name, 1, numNameParts);
        }

        // if parentObj is a CantoObjectWrapper and we are generating data, delegate to the object
        //if (generate && !dynamicChild && numNameParts == 1 && parentObj != null && parentObj instanceof CantoObjectWrapper) {
        //    CantoObjectWrapper obj = (CantoObjectWrapper) parentObj;
        //    return obj.getChildData(name);
        //}

        try {
            // Keep track of intermediate definitions during alias dereferencing
            // by pushing them onto the context stack in case their parameters are
            // referenced in the child being instantiated.  Look for cached
            // definitions and arguments

            if ((args == null || !args.isDynamic()) && !(def instanceof AliasedDefinition)) {
                String nm = def.getName();
                String fullNm = def.getFullNameInContext(this);
                Holder holder = getDefHolder(nm, fullNm, null, null, false);
                if (holder != null && holder.nominalDef != null && holder.nominalDef.getDurability() != Definition.Durability.DYNAMIC && !((CantoNode) holder.nominalDef).isDynamic()) {
                    def = holder.nominalDef;
                    args = holder.nominalArgs;
                    if (generate && def.isIdentity() && holder.data != null && holder.data instanceof CantoObjectWrapper && numNameParts == 1) {
                        CantoObjectWrapper obj = (CantoObjectWrapper) holder.data;
                        return obj.getChildData(resolveArgsIndexes(childName));
                    }
                }
            }
    
            if (!def.isExternal() && (!def.isCollection() || parentObj == null) && !childName.isSpecial()) {
                ParameterList params = def.getParamsForArgs(args, this);
                if (!def.isIdentity() && !topScope.def.equals(def)) {
                    boolean newFrame = !topScope.def.equalsOrExtends(def);
                    push(def, params, args, newFrame);
                    numPushes++;
                }

                // put in loop to push supers
        
                Definition superDef = def.getSuperDefinition(this);
                Definition nextDef = def;
                while (nextDef.isAliasInContext(this) && !nextDef.isCollection()) {
                    Instantiation aliasInstance = nextDef.getAliasInstanceInContext(this);
                    if (nextDef.isParamAlias() && aliasInstance != null) {
                        aliasInstance = aliasInstance.getUltimateInstance(this);
                    }
                    if (aliasInstance == null) {
                        break;
                    }
                    NameNode aliasName = aliasInstance.getReferenceName();
                    if (aliasName.isComplex()) {
                        numPushes += pushParts(aliasInstance);
                    }
            
                    ArgumentList aliasArgs = aliasInstance.getArguments();
                    IndexList aliasIndexes = aliasInstance.getIndexes();
                    Definition aliasDef = aliasInstance.getDefinition(this, def, false);  // def or nextDef?
                    if (aliasDef == null) {
                        break;
                    }
            
                    // we are only interested in aliases in the same hierarchy
                    if (superDef != null && !aliasDef.equalsOrExtends(superDef)) {
                        break;
                    }
            
                    nextDef = aliasDef;
                    args = aliasArgs;
                    if ((args == null || !args.isDynamic()) && aliasIndexes == null) {
                        String nm = aliasInstance.getName();
                        String fullNm = parentDef.getFullNameInContext(this) + "." + nm;
                        Holder holder = getDefHolder(nm, fullNm, null, null, false);
                        if (holder == null && aliasInstance instanceof ResolvedInstance) {
                            ResolvedInstance ri = (ResolvedInstance) aliasInstance;
                            if (!equals(ri.getResolutionContext())) {
                                holder = ri.getResolutionContext().getDefHolder(nm, fullNm, null, null, false);
                            }
                        }
                        if (holder != null && holder.nominalDef != null && holder.nominalDef.getDurability() != Definition.Durability.DYNAMIC && !((CantoNode) holder.nominalDef).isDynamic() && (nextDef.equals(holder.nominalDef) || nextDef.equals(holder.def))) {
                            nextDef = holder.nominalDef;
                            args = holder.nominalArgs;
                            if (generate && holder.data != null && holder.data instanceof CantoObjectWrapper) {
                                CantoObjectWrapper obj = (CantoObjectWrapper) holder.data;
                                if (numNameParts == 1) {
                                    try {
                                        unpush();
                                        return obj.getChildData(resolveArgsIndexes(childName));
                                    } finally {
                                        repush();
                                    }
                                } else {
                                    Definition objDef = obj.getDefinition();
                                    Context resolutionContext = obj.getResolutionContext();
                                    return resolutionContext.getDescendant(objDef, childArgs, name, generate, obj);
                                }
                            }
                        }
                    }
                    params = nextDef.getParamsForArgs(args, this);
                    push(nextDef, params, args, true);
                    numPushes++;
                }
                if (def != nextDef) {
                    def = nextDef;
                    superDef = def.getSuperDefinition(this);
                }
        
                if (superDef != null && topScope.def.equalsOrExtends(superDef)) {
                    numSuperPushes = pushSupers(def, superDef);
                }
            }

            //if (childIndexes == null) {
                String nm = childName.getName();
                String fullNm = parentDef.getFullNameInContext(this) + "." + nm;
                Holder holder = getDefHolder(nm, fullNm, childArgs, childIndexes, false);
                if (holder != null) {
                    Definition nominalDef = holder.nominalDef;
                    if (nominalDef != null && !nominalDef.isCollection() && nominalDef.getDurability() != Definition.Durability.DYNAMIC) { 
                        if (nominalDef.isIdentity()) {
                            childDef = holder.def;
                            if (childArgs == null) {
                                childArgs = holder.args;
                            }
                        } else {
                            childDef = nominalDef;
                            if (childArgs == null) {
                                childArgs = holder.nominalArgs;
                            }
                        }
                        if (childDef != null && childDef.getDurability() == Definition.Durability.DYNAMIC) {
                            dynamicChild = true;    
                        }

                        if (generate && !dynamicChild) { // && fullNm.equals(childDef.getFullNameInContext(this))) {
                            if (holder.data != null && !holder.data.equals(NullValue.NULL_VALUE)) {
                                if (numNameParts == 1) {
                                    return holder.data;
                                } else if (holder.data instanceof CantoObjectWrapper) {
                                    CantoObjectWrapper obj = (CantoObjectWrapper) holder.data;
                                    return obj.getChildData(resolveArgsIndexes(restOfName));
                                }
                            } else if (holder.resolvedInstance != null) {
                                ResolvedInstance ri = holder.resolvedInstance;
                                if (numNameParts == 1) {
                                    Object data = ri.getData(this, childDef);
                                    if (data != null && !data.equals(NullValue.NULL_VALUE)) {
                                        return data;
                                    }
                                }
                            }
                        }
                    }
                }
            //}
    
            if (childDef == null) {
                return def.getChild(name, name.getArguments(), name.getIndexes(), args, this, generate, true, parentObj, null);
            }

            // if parentObj is a CantoObjectWrapper and we are generating data, delegate to the object
            if (generate && childDef.getDurability() != Definition.Durability.DYNAMIC && !dynamicChild && numNameParts == 1 && parentObj != null && parentObj instanceof CantoObjectWrapper) {
                CantoObjectWrapper obj = (CantoObjectWrapper) parentObj;
                return obj.getChildData(resolveArgsIndexes(name));
            }
    
    
            DefinitionInstance childDefInstance = null;
            if (childDef != null) {
                if (generate && childName != null) {
                    childDef = initDef(childDef, childArgs, childName.getIndexes());
                }

                if (restOfName != null) {
                    if (generate) {
                        return getDescendant(childDef, childArgs, restOfName, generate, parentObj);
                    } else {
                        childDefInstance = (DefinitionInstance) getDescendant(childDef, childArgs, restOfName, generate, parentObj);
                    }
                }
            }    
    
            if (!generate) {
                if (childDefInstance != null) {
                    return childDefInstance;
                } else if (childDef != null) {
                    return childDef.getDefInstance(childArgs, childIndexes);
                } else {
                    return null;
                }
            }
    
            if (childDefInstance != null) {
                childDef = childDefInstance.def;
            }

            if (childDef == null) {
                return CantoNode.UNDEFINED;
        
            } else {
                return childDef.instantiate(childArgs, childName.getIndexes(), this);
            }

        } finally {
            if (numSuperPushes > 0) {
                unpushSupers(numSuperPushes);
            }
            while (numPushes-- > 0) {
                pop();
            }
            //validateSize();
        }
    }

    private NameNode resolveArgsIndexes(NameNode name) {
        ArgumentList args = name.getArguments();
        IndexList indexes = name.getIndexes();

        if ((args != null && args.size() > 0) || (indexes != null && indexes.size() < 0)) {
            if (args != null && args.size() > 0) {
                args = ResolvedInstance.resolveArguments(args, this);
            }
            if (indexes != null && indexes.size() < 0) {
                indexes = resolveIndexes(indexes);
            }
            name = new NameWithArgs(name.getName(), args, indexes);
        }
        return name;
    }
    
    public int pushParts(Instantiation instance) {
        if (instance != null) {
            NameNode nameNode = instance.getReferenceName();
            if (nameNode != null) {
                if (nameNode.isComplex()) {
                    Context resolutionContext = this;
                    //if (instance instanceof ResolvedInstance) {
                    //    resolutionContext = ((ResolvedInstance) instance).getResolutionContext();
                    //}
                    int num = nameNode.numParts();
                    return resolutionContext.pushParts(nameNode, num - 1, instance.getOwner());
                }
            }
        }
        return 0;
    }
    
    public int pushParts(NameNode nameNode, int numParts, Definition owner) {
        int numPushes = 0;
        try {
            for (int part = 0; part < numParts; part++) {
                NameNode partName = nameNode.getPart(part);
                Definition partDef = null;
        
                IndexList partIndexes = partName.getIndexes();
                ArgumentList partArgs = partName.getArguments();
                if (partIndexes == null || partIndexes.size() == 0) {
                    String nm = partName.getName();
                    String fullNm = owner.getFullNameInContext(this) + "." + nm;
                    Holder holder = getDefHolder(nm, fullNm, partArgs, partIndexes, false);
                    if (holder != null) {
                        Definition nominalDef = holder.nominalDef;
                        if (nominalDef != null && !nominalDef.isCollection() && nominalDef.getDurability() != Definition.Durability.DYNAMIC) { 
                            if (nominalDef.isIdentity()) {
                                partDef = holder.def;
                                if (partArgs == null) {
                                    partArgs = holder.args;
                                }
                            } else {
                                partDef = nominalDef;
                                if (partArgs == null) {
                                    partArgs = holder.nominalArgs;
                                }
                            }
                        }
                    }
                }
        
                Instantiation partInstance = new Instantiation(partName, owner);
                partInstance.setKind(getParameterKind(partName.getName()));
        
                if (partDef == null) {
                    partDef = partInstance.getDefinition(this);
                    if (partDef == null) {
                        break;
                    }
                }
                if (partInstance.isParameterKind()) {
                    Scope partScope = getParameterScope(partInstance.getReferenceName(), partInstance.isContainerParameter(this));
                    push(partScope);
                } else {
                    ParameterList partParams = partDef.getParamsForArgs(partArgs, this, false);
                    push(partDef, partParams, partArgs, false);
                }
                numPushes++;
                while (partDef.isAliasInContext(this) && !partDef.isIdentity()) {
                    partInstance = partDef.getAliasInstanceInContext(this);
                    if (partInstance == null) {
                        break;
                    }
                    if (partInstance.isParameterKind()) {
                        Scope partScope = getParameterScope(partInstance.getReferenceName(), partInstance.isContainerParameter(this));
                        if (partScope == null) {
                            break;
                        }
                        partDef = partScope.def;
                        if (partDef == null) {
                            break;
                        }
                        push(partScope);
                    } else {
                        partDef = partInstance.getDefinition(this);
                        if (partDef == null) {
                            break;
                        }
                        partArgs = partInstance.getArguments();
                        ParameterList partParams = partDef.getParamsForArgs(partArgs, this);
                        push(partDef, partParams, partArgs, false);
                    }
                    numPushes++;
                }
            }
        } finally {
            //validateSize();
        }
        return numPushes;
    }

    public int getParameterKind(String name) {
        int kind = Instantiation.UNRESOLVED;
        boolean isChild = (name.indexOf('.') > 0);
        DefParameter param = topScope.getParam(name);

        if (param != null) {
            if (param.isInFor()) {
                return (isChild ? Instantiation.FOR_PARAMETER_CHILD : Instantiation.FOR_PARAMETER);
            } else {
                return (isChild ? Instantiation.PARAMETER_CHILD : Instantiation.PARAMETER);
            }
        }

        for (Scope entry = topScope.getPrevious(); entry != null; entry = entry.getPrevious()) {
            param = entry.getParam(name);
            if (param != null) {
                return (isChild ? Instantiation.CONTAINER_PARAMETER_CHILD : Instantiation.CONTAINER_PARAMETER);
            }
        }
        return kind;
    }

    /** Push superdefinitions of the passed definition on the stack, from most super to least super with
     *  the passed definition remaining on top.  This is designed to accommodate instantiation of children
     *  which reference parameters. 
     */
    private int pushSupers(Definition def, Definition superDef) {
        int numPushes = 0;
        Definition contextDef = def;

        // remember the current top scope before we do all the pushing
        Scope oldTop = topScope;

        while (superDef != null) {
            Type st = def.getSuper(this);
            //if (superDef != topScope.superdef) {
                ArgumentList args = st.getArguments(this);
                ParameterList params = superDef.getParamsForArgs(args, this);
                Scope scope = newScope(contextDef, superDef, params, args);
                push(scope);
                numPushes++;
            //}
            def = superDef;
            superDef = def.getSuperDefinition(this);
        }

        if (numPushes > 0) {
            Scope top = topScope;
            Scope nextLink = oldTop;

            for (int i = 0; i < numPushes - 1; i++) {
                // now reverse the order of the just pushed entries
                Scope nextTop = top.previous;
                top.previous = nextLink;
                nextLink = top;
                top = nextTop;
            }
    
            top.previous = nextLink;
            topScope = top;
            push(newScope(oldTop, true));
            numPushes++;
            //validateSize();
        }
        // get the top scope after all the pushing (not the same as before)
        return numPushes;
    }
    
    private void unpushSupers(int numPushes) {
        for (int i = 0; i < numPushes; i++) {
            pop();
        }
        //validateSize();
    }
    
    
    public int pushSupersAndAliases(Definition def, ArgumentList args, Definition childDef) {
        //validateSize();
        // track back through superdefinitions and aliases to push intermediate definitions
        if (childDef != null  /* && !isSpecialDefinition(childDef) */ ) {

            // find the complex owner of the child
            Definition childOwner = childDef.getOwner();
            while (childOwner != null && !(childOwner instanceof ComplexDefinition)) {
                childOwner = childOwner.getOwner();
            }
            if (childOwner == null) {
                throw new NullPointerException("Improperly initialized definition tree");
            }

            return pushSupersAndAliases((ComplexDefinition) childOwner, def, args);
        } else {
            return 0;
        }
    }

    public int pushSupersAndAliases(ComplexDefinition owner, Definition def, ArgumentList args) {
        Definition instantiatedDef = def;
        DefinitionInstance defInstance = getContextDefInstance(instantiatedDef, args);
        def = defInstance.def;
        if (defInstance.args != null) {
            args = defInstance.args;
        }
        int numPushes = 0;
        ParameterList params = def.getParamsForArgs(args, this);
        Definition superdef = null;
        while (!def.equals(owner)) {
            push(instantiatedDef, params, args, false);
            numPushes++;

            Type st = def.getSuper(this);
            superdef = def.getSuperDefinition(this);

            // this doesn't completely work, because it misses
            // superclasses of intermediate aliases.  To really
            // handle this right, we need a flag for getChildDefinition
            // which prevents it from restoring the context, so
            // that none of the pushing here would be necessary.
            // Instead we would use a clone of the context, which
            // we could just throw away when we're done.
            if (def.isAliasInContext(this)) {
                Definition aliasDef = def;
                int numAliasPushes = 0;
                ArgumentList aliasArgs = args;
                ParameterList aliasParams = def.getParamsForArgs(args, this);
                while (aliasDef != null && aliasDef.isAliasInContext(this)) {
                    push(instantiatedDef, aliasParams, aliasArgs, false);
                    numAliasPushes++;
                    Instantiation aliasInstance = aliasDef.getAliasInstanceInContext(this);
                    aliasDef = (Definition) aliasInstance.getDefinition(this);  // lookup(this, false);
                    aliasArgs = aliasInstance.getArguments();  // getUltimateInstance(this).getArguments();
                    if (aliasDef != null) {
                        aliasParams = aliasDef.getParamsForArgs(aliasArgs, this);
                    }
                }
                if (aliasDef != null && aliasDef.equals(owner)) {
                    def = aliasDef;
                    args = aliasArgs;
                    params = aliasParams;
                    numPushes += numAliasPushes;
                    continue;
                } else {
                    while (numAliasPushes-- > 0) {
                        pop();
                    }
                }
            }
            if (st == null || superdef == null) {
                break;
            }
            def = superdef;
            args = st.getArguments(this);
            params = def.getParamsForArgs(args, this);
        }
        if (superdef != null) {
            push(instantiatedDef, superdef, params, args);
            numPushes++;
        }
  
        //validateSize();
        return numPushes;
    }

    synchronized private Object _instantiateArgChild(NameNode childName, Type paramType, Definition argDef, ArgumentList argArgs, IndexList argIndexes) {
        Object data = null;
        int numPushes = 0;
        int numUnpushes = 0;

        // initialization dynamic objects such as collections initialized with
        // comprehensions or external methods
        if (argDef instanceof DynamicObject) {
            argDef = (Definition) ((DynamicObject) argDef).initForContext(this, argArgs, argIndexes);
        }

        try {
            if (!childName.isSpecial()) {
                while (argDef.isAliasInContext(this)) {
                    ParameterList params = argDef.getParamsForArgs(argArgs, this);
                    push(argDef, params, argArgs, false);
                    numPushes++;
                    Instantiation aliasInstance = argDef.getAliasInstanceInContext(this);  //.getUltimateInstance(this);
                    if (aliasInstance == null) {
                        pop();
                        numPushes--;
                        break;
                    }
                    Definition newDef = (Definition) aliasInstance.getDefinition(this);  // lookup(this, false);
                    if (newDef == null) {
                        pop();
                        numPushes--;
                        break;
                    } else {
                        argDef = newDef;
                    }
                    argArgs = aliasInstance.getArguments();
                }
            }
            if (argDef != null) {
                if (argDef instanceof ElementReference) {
                    unpush();
                    numUnpushes++;
                    argDef = ((ElementReference) argDef).getElementDefinition(this);
                    repush();
                    numUnpushes--;
                    if (argDef == null) {
                        return null;
                    }
                }

                // if it's a NamedDefinition, but not an external definition, push the 
                // definition of the parameter onto the context in order to properly resolve 
                // any of its children which may be instantiated
                if (argDef instanceof NamedDefinition) { // && !argDef.isExternal()) {
    
                    // unpop the stack since the child's arguments have to be
                    // resolved where they are, not up at the level of its parent's
                    // referenced parameter.
                    push(argDef, argDef.getParamsForArgs(argArgs, this, false), argArgs);
                    numPushes++;
                }
        
                data = argDef.getChildData(childName, paramType, this, argArgs);
            }
             
        } finally {
            while (numPushes-- > 0) {
                pop();
            }
            while (numUnpushes-- > 0) {
                repush();
            }
            //validateSize();
        }
        return data;
    }

    
    /** Looks through the context for the immediate subdefinition of the superdefinition at the
     *  top of the stack.
     */
    private synchronized NamedDefinition getSubdefinition() {

        // if there is no superdef in the top context scope, then there is
        // no subdefinition
        if (topScope.superdef == null) {
            return null;
        }

        int numUnpushes = 0;
        Definition superdef = topScope.superdef;
        try {
            while (topScope != null) { 
                Definition subdef = (topScope.superdef != null ? topScope.superdef : topScope.def);
                NamedDefinition sd = subdef.getSuperDefinition(this);
                if (sd != null && sd.includes(superdef)) {
                    return (NamedDefinition) subdef;
                }
                if (topScope.previous == null) {
                    break;
                }
                unpush();
                numUnpushes++;
            }
        } finally {
            while (numUnpushes > 0) {
                repush();
                numUnpushes--;
            }
        }
        return null;
    }

    private Object constructSuper(Definition def, ArgumentList args, Definition instantiatedDef) throws Redirection {
        return constructSuper(def, args, instantiatedDef, null);
    }

    
    private Object constructSuper(Definition def, ArgumentList args, Definition instantiatedDef, LinkedList<Definition> nextList) throws Redirection {
        Object data = null;
        boolean pushed = false;
        boolean hasMore = (nextList != null && nextList.size() > 0);

        if (!hasMore) {
            ParameterList params = def.getParamsForArgs(args, this, false);
            push(instantiatedDef, def, params, args);
            pushed = true;
        }

        try {
            List<Construction> constructions = def.getConstructions(this);
            int numConstructions = (constructions == null ? 0 : constructions.size());
            NamedDefinition superDef = def.getSuperDefinition(this);

            if (!hasMore && superDef != null && (superDef.hasSub(this) || numConstructions == 0)) {
                Type st = def.getSuper(this);
                ArgumentList superArgs = (st != null ? st.getArguments(this) : null);
                NamedDefinition superFlavor = (NamedDefinition) superDef.getDefinitionForArgs(superArgs, this);
                data = constructSuper(superFlavor, superArgs, instantiatedDef);

            } else {
        
                if (hasMore) {
                    Definition nextDef = nextList.removeFirst();
                    constructions = nextDef.getConstructions(this);
                    numConstructions = (constructions == null ? 0 : constructions.size());
                }
        
                if (numConstructions == 1) {
                    Construction object = constructions.get(0);
                    if (object instanceof SubStatement) {
                        NamedDefinition sub = (NamedDefinition) peek().def;
                        data = constructSub(sub, instantiatedDef);

                    } else if (object instanceof SuperStatement) {
                        Type st = def.getSuper(this);
                        ArgumentList superArgs = (st != null ? st.getArguments(this) : null);
                        NamedDefinition superFlavor = (NamedDefinition) superDef.getDefinitionForArgs(superArgs, this);
                        data = constructSuper(superFlavor, superArgs, instantiatedDef);

                    } else if (object instanceof NextStatement) {
                        data = constructSuper(def, args, instantiatedDef, nextList);
                
                    } else if (object instanceof RedirectStatement) {
                        RedirectStatement redir = (RedirectStatement) object;
                        throw redir.getRedirection(this);

                    } else if (object instanceof Value) {
                        data = object;

                    } else if (object instanceof ValueGenerator) {
                        data = ((ValueGenerator) object).getValue(this).getData();

                    } else {
                        data = object.getData(this);
                    }
            
                    if (data instanceof Value) {
                        data = ((Value) data).getData();
                    } else if (data instanceof CantoNode) {
                        ((CantoNode) instantiatedDef).initNode((CantoNode) data);
                    }

                } else if (numConstructions > 1) {
                    Iterator<Construction> it = constructions.iterator();
                    while (it.hasNext()) {
                        Construction chunk = it.next();
                        if (chunk == null) {
                            continue;

                        } else if (chunk instanceof RedirectStatement) {
                            RedirectStatement redir = (RedirectStatement) chunk;
                            throw redir.getRedirection(this);

                        } else {
                            Object chunkData = null;

                            if (chunk instanceof SubStatement) {
                                NamedDefinition sub = getSubdefinition();
                                Object subData = (sub == null ? null : constructSub(sub, instantiatedDef));
                                if (subData != null) {
                                    if (subData instanceof Value) {
                                        chunkData = ((Value) subData).getData();
                                    } else if (subData instanceof ValueGenerator) {
                                        chunkData = ((ValueGenerator) subData).getValue(this).getData();
                                    } else {
                                        chunkData = subData;
                                    }
                                }

                            } else if (chunk instanceof SuperStatement) {
                                Type st = def.getSuper(this);
                                ArgumentList superArgs = (st != null ? st.getArguments(this) : null);
                                NamedDefinition superFlavor = (NamedDefinition) superDef.getDefinitionForArgs(superArgs, this);
                                Object superData = constructSuper(superFlavor, superArgs, instantiatedDef);
                                if (superData != null) {
                                    if (superData instanceof Value) {
                                        chunkData = ((Value) superData).getData();
                                    } else if (superData instanceof ValueGenerator) {
                                        chunkData = ((ValueGenerator) superData).getValue(this).getData();
                                    } else {
                                        chunkData = superData;
                                    }
                                }

                            } else if (chunk instanceof NextStatement) {
                                chunkData = constructSuper(def, args, instantiatedDef, nextList);
                        
                            } else {
                                chunkData = chunk.getData(this);
                                if (chunkData != null && chunkData instanceof Value) {
                                    chunkData = ((Value) chunkData).getData();
                                }
                            }
                            if (chunkData != null) {
                                if (data == null) {
                                    data = chunkData;
                                } else {
                                    data = PrimitiveValue.getStringFor(data) + PrimitiveValue.getStringFor(chunkData);
                                }
                            }
                        }
                    }
                }
            }
        } finally {
            if (pushed) {
                pop();
            }
            //validateSize();
        }
        return data;
    }

    public Object constructSub(Definition def, Definition instantiatedDef) throws Redirection {
        Object data = getLocalData("sub", null, null); // getData(null, "sub", null, null, null);
        if (data != null) {
            return data;
        }

        LinkedList<Definition> nextList = null;

        // call the form of getSuperDefinition that does not take
        // a context parameter, because we want the multidefinition
        // if there is one.
        Definition superDef = def.getSuperDefinition();

        if (superDef != null && superDef.hasNext(this)) {
              nextList = superDef.getNextList(this);
        }

        if (nextList != null && nextList.size() > 0) {
            Definition nextDef = nextList.removeFirst();
            data = constructSub(nextDef, instantiatedDef, nextList);

        } else {
            try {
                unpush();
                data = constructSub(def, instantiatedDef, null);
            } finally {
                repush();
            }
        }
    
        if (data == null) {
            data = NullValue.NULL_VALUE;
        }
        putData(def, null, null, "sub", data);
        return data;
    }
    

    private Object constructSub(Definition def, Definition instantiatedDef, LinkedList<Definition> nextList) throws Redirection {
        Object data = null;    
        List<Construction> constructions = def.getConstructions(this);
        boolean hasMoreNext = (nextList != null && nextList.size() > 0);
    
        if (constructions == null || constructions.size() == 0) {
            NamedDefinition sub = getSubdefinition();
            data = (sub == null ? null : constructSub(sub, instantiatedDef));
        } else {
            if (def.equals(instantiatedDef)) {
                data = construct(constructions);
            } else {
                int n = constructions.size();
                if (n == 1) {
                    Construction object = constructions.get(0);
                    if (object instanceof NextStatement) {
                        if (hasMoreNext) {
                            Definition nextDef = nextList.removeFirst();
                            data = constructSub(nextDef, instantiatedDef, nextList);
                        } else {
                            NamedDefinition sub = getSubdefinition();
                            if (sub == null) {
                                data = null;
                            } else {
                                try {
                                    unpush();
                                    data = constructSub(sub, instantiatedDef, null);
                                } finally {
                                    repush();
                                }
                            }
                        }
                
                    } else if (object instanceof SubStatement) {
                        NamedDefinition sub = getSubdefinition();
                        data = (sub == null ? null : constructSub(sub, instantiatedDef));

                    } else if (object instanceof SuperStatement) {
                        Definition superDef = def.getSuperDefinition(this);
                        if (superDef == null) {
                            throw new Redirection(Redirection.STANDARD_ERROR, "Undefined superdefinition reference in " + def.getFullName());
                        } else {
                            Type st = def.getSuper(this);
                            ArgumentList superArgs = (st != null ? st.getArguments(this) : null);
                            NamedDefinition superFlavor = (NamedDefinition) superDef.getDefinitionForArgs(superArgs, this);
                            data = constructSuper(superFlavor, superArgs, instantiatedDef);
                        }
                
                    } else if (object instanceof RedirectStatement) {
                        RedirectStatement redir = (RedirectStatement) object;
                        throw redir.getRedirection(this);

                    } else if (object instanceof Value) {
                        data =  object;
                    } else if (object instanceof ValueGenerator) {
                        data = ((ValueGenerator) object).getValue(this).getData();
                    } else {
                        data = object.getData(this);
                    }
                    if (data instanceof Value) {
                        data = ((Value) data).getData();
                    } else if (data instanceof CantoNode) {
                        instantiatedDef.initNode((CantoNode) data);
                    }


                } else if (n > 1) {
                    Iterator<Construction> it = constructions.iterator();
                    while (it.hasNext()) {
                        Construction chunk = it.next();
                        if (chunk == null) {
                            continue;

                        } else if (chunk instanceof RedirectStatement) {
                            RedirectStatement redir = (RedirectStatement) chunk;
                            throw redir.getRedirection(this);

                        } else {
                            Object chunkData = null;
                            if  (chunk instanceof NextStatement) {
                                Object subData = null;
                                if (hasMoreNext) {
                                    Definition nextDef = nextList.removeFirst();
                                    subData = constructSub(nextDef, instantiatedDef, nextList);
                                } else { 
                                    NamedDefinition sub = getSubdefinition();
                                    if (sub == null) {
                                        subData = null;
                                    } else {
                                        try {
                                            unpush();
                                            subData = constructSub(sub, instantiatedDef, null);
                                        } finally {
                                            repush();
                                        }
                                    }
                                }
                                if (subData != null) {
                                    if (subData instanceof Value) {
                                        chunkData = ((Value) subData).getData();
                                    } else if (subData instanceof ValueGenerator) {
                                        chunkData = ((ValueGenerator) subData).getValue(this).getData();
                                    } else {
                                        chunkData = subData;
                                    }
                                }
                        
                            } else if (chunk instanceof SubStatement) {
                                NamedDefinition sub = getSubdefinition();
                                Object subData = (sub == null ? null : constructSub(sub, instantiatedDef));
                                if (subData != null) {
                                    if (subData instanceof Value) {
                                        chunkData = ((Value) subData).getData();
                                    } else if (subData instanceof ValueGenerator) {
                                        chunkData = ((ValueGenerator) subData).getValue(this).getData();
                                    } else {
                                        chunkData = subData;
                                    }
                                }

                            } else if (chunk instanceof SuperStatement) {
                                Definition superDef = def.getSuperDefinition(this);
                                if (superDef == null) {
                                    throw new Redirection(Redirection.STANDARD_ERROR, "Undefined superdefinition reference in " + def.getFullName());
                                } else {
                                    Type st = def.getSuper(this);
                                    ArgumentList superArgs = (st != null ? st.getArguments(this) : null);
                                    NamedDefinition superFlavor = (NamedDefinition) superDef.getDefinitionForArgs(superArgs, this);
                                    Object superData = constructSuper(superFlavor, superArgs, instantiatedDef);
                                    if (superData != null) {
                                        if (superData instanceof Value) {
                                            chunkData = ((Value) superData).getData();
                                        } else if (superData instanceof ValueGenerator) {
                                            chunkData = ((ValueGenerator) superData).getValue(this).getData();
                                        } else {
                                            chunkData = superData;
                                        }
                                    }
                                }
                            } else {
                                chunkData = chunk.getData(this);
                                if (chunkData != null && chunkData instanceof Value) {
                                    chunkData = ((Value) chunkData).getData();
                                }
                            }
                            if (chunkData != null) {
                                if (data == null) {
                                    data = chunkData;
                                } else {
                                    data = PrimitiveValue.getStringFor(data) + PrimitiveValue.getStringFor(chunkData);
                                }
                            }
                        }
                    }
                }
            }
        }
        return data;
    }


    public Object construct(Definition definition, ArgumentList args) {
        Object data = null;

        boolean pushedSuperDef = false;
        boolean pushedParamDef = false;
        boolean pushedContext = false;
        int pushedParams = 0;

        Block catchBlock = definition.getCatchBlock();

        NamedDefinition oldInstantiatedDef = instantiatedDef;

        if (definition instanceof NamedDefinition) {
            instantiatedDef = (NamedDefinition) definition;
        }
        ParameterList params = null;

        // determine if this defines a namespace and therefore a new context level.
        // No need to push external definitions, because external names are
        // resolved externally
        if (!definition.isAnonymous() && !definition.isExternal()) {
            // get the arguments and parameters, if any, to push on the
            // context stack with the definition
            params = definition.getParamsForArgs(args, this);

            // if there are args but this definition has no params, check to see if it's an
            // alias and if so look for params there
            if (params == null && args != null && definition.isAliasInContext(this)) {
                Definition aliasDef = definition;
                while (params == null && aliasDef.isAlias() && (aliasDef.getDurability() == Definition.Durability.DYNAMIC || getData(aliasDef, aliasDef.getName(), args, null) == null)) {
                    Instantiation aliasInstance = aliasDef.getAliasInstanceInContext(this);
                    aliasDef = aliasInstance.getUltimateDefinition(this);
                    if (aliasDef == null) {
                        break;
                    }
                    params = aliasDef.getParamsForArgs(args, this);
                    definition = aliasDef;
                }
            }
            push(definition, params, args, true);
            pushedContext = true;
        }

        try {
            List<Construction> constructions = definition.getConstructions(this);
            boolean constructed = false;
            Definition aliasDef = null;
            Instantiation aliasInstance = null;
            NamedDefinition superDef = definition.getSuperDefinition(this);
            Type st = definition.getSuper(this);
    
            if (!constructed && superDef != null && !superDef.isPrimitive() && definition.getName() != Name.SUB) {

                // check to see if this is an alias, and the alias definition extends or equals the
                // superdefinition, in which case we shouldn't bother constructing the superdef here,
                // it will get constructed when the alias is constructed
                if (definition.isAliasInContext(this)) {
                    aliasInstance = definition.getAliasInstanceInContext(this);
                    if (aliasInstance != null) {
                        if (definition.isParamAlias()) {
                            aliasInstance = aliasInstance.getUltimateInstance(this);
                        }
                        aliasDef = aliasInstance.getDefinition(this, definition, false);
                        //if (aliasDef != null) {
                        //    return construct(aliasDef, aliasInstance.getArguments());
                        //}
                    }
                }
                CantoNode contents = definition.getContents();
                Definition constructedDef = null;
                if (contents instanceof Construction) {
                    Type constructedType = ((Construction) contents).getType(this, true);
                    if (constructedType != null) {
                        constructedDef = constructedType.getDefinition();
                    }
                }
                if ((aliasDef == null || !aliasDef.equalsOrExtends(superDef))
                        && (constructedDef == null || !constructedDef.equalsOrExtends(superDef))) {
                    ArgumentList superArgs = (st != null ? st.getArguments(this) : null);
                    NamedDefinition superFlavor = (NamedDefinition) superDef.getDefinitionForArgs(superArgs, this);
                    if (superFlavor != null && (superFlavor.hasSub(this) || (constructions == null || constructions.size() == 0))) {
                        NamedDefinition ndef = (NamedDefinition) peek().def;
                        data = constructSuper(superFlavor, superArgs, ndef);
                        constructed = true;
                    }
                }
            }

            // not handled by one of the above cases
            if (!constructed) {
                if (definition.isAlias()) {
                    Construction construction = constructions.get(0);
                    if (construction instanceof Value) {
                        data = construction;
                    } else if (aliasInstance != null) {
                        data = aliasInstance.getData(this, aliasDef);
                    } else if (construction instanceof ValueGenerator) {
                        data = ((ValueGenerator) construction).getValue(this).getData();
                    } else {
                        data = construction.getData(this);
                    }
                } else {
                    data = construct(constructions);
                }
            }
    
            if (data instanceof Value) {
                data = ((Value) data).getData();

            } else if (data instanceof CantoNode) {
                instantiatedDef.initNode((CantoNode) data);
            }
    
            return data;

        } catch (Redirection r) {

            if (catchBlock != null) {
                String location = r.getLocation();
                String catchIdentifier = catchBlock.getCatchIdentifier();
                while (catchIdentifier != null && catchIdentifier.length() > 0) {
                    if (catchIdentifier.equals(location)) {
                        return catchBlock.getData(this);
                    } else {
                        catchBlock = catchBlock.getCatchBlock();
                        if (catchBlock == null) {
                            throw r;
                        }
                        catchIdentifier = catchBlock.getCatchIdentifier();
                    }
                }
                return catchBlock.getData(this);

            } else {
                throw r;
            }
        } catch (Throwable t) {
            if (catchBlock != null && catchBlock.getCatchIdentifier() == null) {
                return catchBlock.getData(this);
            } else {
                String className = t.getClass().getName();
                String message = t.getMessage();
                if (message == null) {
                    message = className;
                } else {
                    message = className + ": " + message;
                }
                t.printStackTrace();
                throw new RuntimeException(message);
            }

        } finally {
            if (pushedParams > 0) {
                for (int i = 0; i < pushedParams; i++) {
                    popParam();
                }

            } else if (pushedContext) {
                pop();
            }

            if (pushedSuperDef) {
                pop();
            }

            if (pushedParamDef) {
                pop();
            }
            instantiatedDef = oldInstantiatedDef;

            //validateSize();
        }
    }


    public Object construct(List<Construction> constructions) {
        Object data = null;
        if (constructions != null) {
            StringBuffer sb = null;
            try {
                int n = constructions.size();
                for (int i = 0; i < n; i++) {
                    Construction object = constructions.get(i);
                        
                    if (object instanceof RedirectStatement) {
                        RedirectStatement redir = (RedirectStatement) object;
                        throw redir.getRedirection(this);

                    } else if (data == null) {
                        if (object instanceof SubStatement) {
                            NamedDefinition sub = getSubdefinition();
                            data = (sub == null ? null : constructSub(sub, instantiatedDef));

                        } else if (object instanceof SuperStatement) {
                            Definition def = peek().def;
                            NamedDefinition superDef = def.getSuperDefinition();
                            if (superDef == null) {
                                throw new Redirection(Redirection.STANDARD_ERROR, "Undefined superdefinition reference in " + def.getFullName());
                            } else {
                                LinkedList<Definition> nextList = null;
                                if (superDef.hasNext(this)) {
                                      nextList = superDef.getNextList(this);
                                }
                        
                                // get the specific definition for this context
                                superDef = def.getSuperDefinition(this);
                                if (superDef == null) {
                                    throw new Redirection(Redirection.STANDARD_ERROR, "Undefined superdefinition reference in " + def.getFullName());
                                } else {
                                    Type st = def.getSuper(this);
                                    ArgumentList superArgs = (st != null ? st.getArguments(this) : null);
                                    NamedDefinition superFlavor = (NamedDefinition) superDef.getDefinitionForArgs(superArgs, this);
                                    data = constructSuper(superFlavor, superArgs, instantiatedDef, nextList);
                                }
                            }

                        } else if (object instanceof Value) {
                            data = object;
                        } else {
                            data = object.getData(this);
                        }
                
                        if (data instanceof Value) {
                            data = ((Value) data).getData();
                        } else if (data instanceof CantoNode) {
                            if (instantiatedDef != null) {
                                instantiatedDef.initNode((CantoNode) data);
                            } else {
                                LOG.debug("Null instantiatedDef in constructions for " + peek().def.getFullName());
                            }
                        }

                    } else {
                        String str = null;
                        if (object instanceof SubStatement) {
                            NamedDefinition sub = getSubdefinition();
                            if (sub != null) {
                                Object obj = constructSub(sub, instantiatedDef);
                                if (obj != null && !obj.equals(NullValue.NULL_VALUE)) {
                                    str = obj.toString();
                                }
                            }
                        
                        } else if (object instanceof SuperStatement) {
                            Definition def = peek().def;
                            NamedDefinition superDef = def.getSuperDefinition();
                            if (superDef == null) {
                                throw new Redirection(Redirection.STANDARD_ERROR, "Undefined superdefinition reference in " + def.getFullName());
                            } else {
                                LinkedList<Definition> nextList = null;
                                if (superDef.hasNext(this)) {
                                      nextList = superDef.getNextList(this);
                                }
                        
                                // get the specific definition for this context
                                superDef = def.getSuperDefinition(this);
                                if (superDef == null) {
                                    throw new Redirection(Redirection.STANDARD_ERROR, "Undefined superdefinition reference in " + def.getFullName());
                                } else {
                                    Type st = def.getSuper(this);
                                    ArgumentList superArgs = (st != null ? st.getArguments(this) : null);
                                    NamedDefinition superFlavor = (NamedDefinition) superDef.getDefinitionForArgs(superArgs, this);
                                    Object obj = constructSuper(superFlavor, superArgs, instantiatedDef, nextList);
                                    if (obj != null && !obj.equals(NullValue.NULL_VALUE)) {
                                        str = obj.toString();
                                    }
                                }
                            }
                        } else if (object instanceof Value) {
                            if (!object.equals(NullValue.NULL_VALUE)) {
                                str = ((Value) object).getString();
                            }
                        } else if (object instanceof ValueGenerator) {
                            str = ((ValueGenerator) object).getValue(this).getString();
                        } else if (object != null) {
                            str = object.getText(this);
                        }
                        if (str != null && str.length() > 0) {
                            if (sb == null) {
                                sb = new StringBuffer(PrimitiveValue.getStringFor(data));
                                data = sb;
                            }
                            sb.append(str);
                        }
                    }
                }

            } catch (ScriptExit se) {
                String textOut = null;
                if (sb != null) {
                    textOut = sb.toString();
                } else if (data != null) {
                    textOut = data.toString();
                }
                if (textOut != null) {
                    se.setTextOut(textOut);
                }
                throw se;
            }

            if (sb != null && data == sb) {
                data = sb.toString();
            }
        }
        return data;
    }

    /** Returns data cached in the context via a keep statement.  
     */
    public Object getContextData(String name) {
        return getContextData(name, false);
    }
    
    public Holder getContextHolder(String name) {
        return (Holder) getContextData(name, true);
    }

    private Object getContextData(String name, boolean getHolder) {
        if (name == null || cache == null || keepMap == null) {
            return null;
        }
        Object data = null;
        Holder holder = null;
        String key = name;
        if (keepMap != null && keepMap.get(key) != null) {
            Pointer p = keepMap.get(key);

            // Problem: the cache map stored in the pointer might no longer be
            // valid, depending on its scope and what has happened since the pointer
            // was created.
            //
            // So, this is what we have to do: Instead of storing the map
            // directly, we store the def name of the scope where it's locally
            // cached and the key it's cached under.  
    
            Map<String, Object> keepTable = p.cache;
            data = keepTable.get(p.getKey());

            if (data instanceof Pointer) {
                int i = 0;
                do {
                    p = (Pointer) data;
                    data = p.cache.get(p.getKey());
                    if (data instanceof Holder) {
                        holder = (Holder) data;
                        data = (holder.data == CantoNode.UNINSTANTIATED ? null : holder.data);
                    } else {
                        holder = null;
                    }
                    i++;
                    if (i >= MAX_POINTER_CHAIN_LENGTH) {
                        throw new IndexOutOfBoundsException("Pointer chain in cache exceeds limit");
                    }
                } while (data instanceof Pointer);
            } else if (data instanceof Holder) {
                holder = (Holder) data;
                data = (holder.data == CantoNode.UNINSTANTIATED ? null : holder.data);
            }
        }
    
        return getHolder ? holder : data;
    }

    /** Returns any cached data for a definition with the specified name
     *  in the current frame of the current context, or null if there is none.
     */
    public Object getLocalData(String name, ArgumentList args, IndexList indexes) {
        return getData(null, name, args, indexes, true);
    }    

    /** Returns any cached data for a definition with the specified name
     *  in the current context, or null if there is none.
     */
    public Object getData(Definition def, String name, ArgumentList args, IndexList indexes) {
        Object data = getData(def, name, args, indexes, false);
        LOG.debug(" - - - getting " + name + " from cache: - - - ");
        if (data == null) {
            LOG.debug(" - - - (no data)");
        } else {
            LOG.debug(" - - - " + data.toString());
        }
        return data;
    }    

    synchronized private Object getData(Definition def, String name, ArgumentList args, IndexList indexes, boolean local) {
        if (name == null || name.length() == 0) {
            return null;
        }
        String fullName = (def == null ? name : def.getFullNameInContext(this));

        Object data = null;

        if (topScope != null) {
            // use indexes as part of the key otherwise a cached element may be confused with a cached array 
            String key = addIndexesToKey(name, indexes);
            data = topScope.get(key, fullName, args, local);
        }

        if (data == null) {

            // TODO: modify fullName to match name if name is multipart
            //
    
            // use indexes as part of the key otherwise a cached element may be confused with a cached array 
            String key = addIndexesToKey(fullName, indexes);
            data = getContextData(key);
        }
        return data;
    }
    

    /** Returns the definition associated with cached data which is the same or the 
     *  equivalent of the specified definition in the current context, or null if there is none.
     */
    
    public Definition getKeepDefinition(Definition def, ArgumentList args) {
        String name = def.getName();
        String fullName = def.getFullNameInContext(this);
        Definition defInKeep = getDefinition(name, fullName, args);
        if (defInKeep == null) {
            Definition defOwner = def.getOwner();
            int numUnpushes = 0;
            try {
                for (Definition topDef = topScope.def; topDef != defOwner && size() > 1; topDef = topScope.def) {
                    unpush();
                    numUnpushes++;
                }
                if (numUnpushes > 0) {
                    defInKeep = getDefinition(name, fullName, args);
                }
            } catch (Throwable t) {
                String message = "Unable to find definition in cache for array " + name + ": " + t.toString();
                LOG.debug(message);
       
            } finally {
                while (numUnpushes-- > 0) {
                    repush();
                }
            }
        }
        return defInKeep;
    }
    
    
    /** Returns the cached definition holder associated with the specified definition in the current context,
     *  or null if there is none.
     */
    
    public Holder getKeepdHolderForDef(Definition def, ArgumentList args, IndexList indexes) {
        String name = def.getName();
        String fullName = def.getFullNameInContext(this);
        Holder holder = getDefHolder(name, fullName, args, indexes, false);
        if (holder == null) {
            Definition defOwner = def.getOwner();
            int numUnpushes = 0;
            try {
                for (Definition topDef = topScope.def; topDef != defOwner && size() > 1; topDef = topScope.def) {
                    unpush();
                    numUnpushes++;
                }
                if (numUnpushes > 0) {
                    holder = getDefHolder(name, fullName, args, indexes, false);
                }
            } catch (Throwable t) {
                String message = "Unable to find holder in cache for array " + name + ": " + t.toString();
                LOG.debug(message);
       
            } finally {
                while (numUnpushes-- > 0) {
                    repush();
                }
            }
        }
        return holder;
    }

    /** Returns the definition associated with cached data for a specified name
     *  in the current context, or null if there is none.
     */
    public Definition getDefinition(String name, String fullName, ArgumentList args) {
        if (topScope == null || name == null || name.length() == 0) {
            return null;
        }
        Definition def = topScope.getDefinition(name, Scope.makeGlobalKey(fullName), args);

        return def;
    }
    
    
    /** Returns a Holder containing the definition and arguments associated with cached data for a 
     *  specified name in the current context, or null if there is none.
     */
    synchronized public Holder getDefHolder(String name, String fullName, ArgumentList args, IndexList indexes, boolean local) {
        if (topScope == null || name == null || name.length() == 0) {
            return null;
        }

        // use indexes as part of the key otherwise a cached element may be confused with a cached array 
        String key = addIndexesToKey(name, indexes);
        Holder holder = topScope.getDefHolder(key, Scope.makeGlobalKey(fullName), args, local);

        // if we get back a global definition and we weren't passed a full name, we
        // need to call getDefHolder again for it to check the global cache
        if (fullName == null && holder != null && holder.nominalDef != null && holder.nominalDef.isGlobal()) {
            fullName = holder.nominalDef.getFullNameInContext(this);
            holder = topScope.getDefHolder(key, Scope.makeGlobalKey(fullName), args, local);
        }

        if (holder == null) {
            holder = getContextHolder(name);
        }

        // if this is an identity, then use the definition of the passed argument, if available,
        // else the superdefinition instead so children etc. resolve to it
        if (holder != null && holder.nominalDef != null && holder.nominalDef.isIdentity()) {
            Construction arg = (args != null && args.size() > 0 ? args.get(0) : null);
            if (arg != null && arg instanceof Instantiation) {
                Instantiation argInstance = ((Instantiation) arg).getUltimateInstance(this);
                Definition argDef = argInstance.getDefinition(this);
                if (argDef != null && !argDef.getType().isPrimitive()) {
                    holder.def = argDef;
                    holder.args = argInstance.getArguments();
                }
            }
        }
        return holder;
    }

    public void putDefinition(Definition def, String name, ArgumentList args, IndexList indexes) {
        putData(def, args, indexes, name, null); //CantoNode.UNINSTANTIATED);
    }
    
    /** Keeps data associated with the specified name
     *  in the current context.
     */
    public void putData(Definition def, ArgumentList args, IndexList indexes, String name, Object data) {
        putData(def, args, def, args, indexes, name, data, null);
    }    
    /** Keeps data associated with the specified name
     *  in the current context.
     */
    public void putData(Definition nominalDef, ArgumentList nominalArgs, Definition def, ArgumentList args, IndexList indexes, String name, Object data, ResolvedInstance resolvedInstance) {
        Holder holder = new Holder(nominalDef, nominalArgs, def, args, this, data, resolvedInstance);
        putData(name, holder, indexes);
    }
        
    synchronized public void putData(String name, Holder holder, IndexList indexes) {
        if (holder.data != null || holder.resolvedInstance != null) {
            LOG.debug(" - - - storing " + name + " in cache - - - ");
        }
        if (name.endsWith(".keep")) {
            System.out.println(" ----- direct put of " + name + "," + " value is " + (holder.data == null ? "null" : (" a " + holder.data.getClass().getName())));
            (new Throwable()).printStackTrace();
        }
        if (topScope != null && name != null && name.length() > 0) {
            int maxKeepLevels = getMaxKeepLevels(holder.nominalDef);

            // use indexes as part of the key otherwise a cached element may be confused with a cached array 
            String key = addIndexesToKey(name, indexes);
            topScope.put(key, holder, this, maxKeepLevels);
        }
    }

    
    public Object getMarker(Object obj) {
        if (obj == null) {
            return new ContextMarker(this);
        } else {
            mark(obj);
            return obj;
        }
    }

    public Object getMarker() {
        return new ContextMarker(this);
    }

    public void mark(Object obj) {
        if (obj instanceof ContextMarker) {
            synchronized (obj) {
                ContextMarker marker = (ContextMarker) obj;
                marker.rootContext = rootContext;
                marker.stateCount = stateCount;
                marker.loopIndex = getLoopIndex();
            }
        } else {
            throw new IllegalArgumentException("attempt to mark a strange object");
        }
    }

    /** Advances the current loop index to a new unique integer value, i.e. a value
     *  not used by any other pass in this loop or any other loop in the same context,
     *  and returns this value.
     *
     *  @throws NullPointerException if the context is uninitialized.
     */
    public int nextLoopIndex() {
        topScope.advanceLoopIndex();
        return topScope.getLoopIndex();
    }

    public void resetLoopIndex() {
        topScope.resetLoopIndex();
    }

    public int getLoopIndex() {
        if (topScope != null) {
            return topScope.getLoopIndex();
        }
        return -1;
    }

    public void setLoopIndex(int index) {
        if (topScope != null) {
            topScope.setLoopIndex(index);
        }
    }

    public ArgumentList getArguments() {
        if (topScope != null) {
            return topScope.args;
        }
        return null;
    }

    public ParameterList getParameters() {
        if (topScope != null) {
            return topScope.params;
        }
        return null;
    }

    public Iterator<Scope> iterator() {
        return new ContextIterator();
    }


    /** Modify the name used to cache a value with indexes, to discriminate
     *  cached collections from cached elements.
     */
    private String addIndexesToKey(String key, IndexList indexes) {
        if (indexes != null && indexes.size() > 0) {
            Iterator<Index> it = indexes.iterator();
            while (it.hasNext()) {
                key = key + it.next().getModifierString(this);
            }
        }
        return key;
    }

    private Object getElement(Object collection, Index index) {
        Object data = null;

        // this occurs with anonymous collections in the input
        if (collection instanceof CollectionDefinition) {
            collection = ((CollectionDefinition) collection).getCollectionInstance(this, null, null);
        }

        if (collection instanceof CollectionInstance) {
            collection = ((CollectionInstance) collection).getCollectionObject();
        }

        if (collection instanceof Value) {
            collection = ((Value) collection).getData();

        } else if (collection instanceof ValueGenerator) {
            collection = ((ValueGenerator) collection).getValue(this).getData();
        }

        if (collection instanceof CantoArray) {
            collection = ((CantoArray) collection).getArrayObject();
        }

        boolean isArray = collection.getClass().isArray();
        boolean isList = (collection instanceof List<?>);
        if (!index.isNumericIndex(this)) {
            String key = index.getIndexValue(this).getString();
            if (isArray || isList) {
                // NOTE: the following is the comment accompanying the related logic
                // in the method getElement in ArrayDefinition:
                //
                //     retrieve the element which matches the index key value.  There
                //     are two ways an element can match the key:
                //
                //     -- if the element is a definition which owns a child named "key"
                //        compare its instantiated string value to the index key
                //
                //     -- if the element doesn't have such a "key" field, compare the
                //        string value of the element itself to the index key.
                //
                // The logic below is operating on an instantiated array, so it contains
                // instantiated elements, and as a result the element definitions are
                // not available.  Therefore only the second of the two methods
                // described above can be implemented.
                //
                // This inconsistency between the logic here and in ArrayDefinition
                // is a bug and needs to be corrected, preferably by finding a way to
                // put the logic in one place.

                if (key == null) {
                    return null;
                }
                int size = (isArray ? Array.getLength(collection) : ((List<?>) collection).size());
                int ix = -1;
                for (int i = 0; i < size; i++) {
                    Object element = (isArray ? Array.get(collection, i) : ((List<?>) collection).get(i));
                    try {
                        String elementKey;
                        if (element instanceof String) {
                            elementKey = (String) element;
                        } else if (element instanceof Value) {
                            elementKey = ((Value) element).getString();
                        } else if (element instanceof Construction) {
                            elementKey = ((Construction) element).getText(this);
                        } else if (element instanceof ValueGenerator) {
                            elementKey = ((ValueGenerator) element).getValue(this).getString();
                        } else {
                            elementKey = element.toString();
                        }

                        if (key.equals(elementKey)) {
                            ix = i;
                            break;
                        }

                    } catch (Redirection r) {
                        // don't redirect, we're only checking
                        continue;
                    }
                }
                data = new PrimitiveValue(ix);

            } else if (collection instanceof Map<?,?>) {
                data = ((Map<?,?>) collection).get(key);
            }
        } else {    // must be an array
            int i = index.getIndexValue(this).getInt();
            if (collection.getClass().isArray()) {
                data = Array.get(collection, i);

            } else if (collection instanceof List<?>) {
                data = ((List<?>) collection).get(i);

            } else if (collection instanceof Map<?,?>) {
                Object[] keys = ((Map<?,?>) collection).keySet().toArray();
                Arrays.sort(keys);
                data = ((Map<?,?>) collection).get(keys[i]);
            }
        }
        while (data instanceof Holder) {
            data = ((Holder) data).data;
        }
        if (data instanceof ElementDefinition) {
            data = ((ElementDefinition) data).getElement();
        }
        return data;
    }

    public Object constructDef(Definition definition, ArgumentList args, IndexList indexes) throws Redirection {
        // initialization expressions
        if (definition instanceof DynamicObject) {
            definition = (Definition) ((DynamicObject) definition).initForContext(this, args, indexes);
        }

        if (definition instanceof CollectionDefinition) {
            CollectionInstance collection = ((CollectionDefinition) definition).getCollectionInstance(this, args, indexes);
            return collection.getCollectionObject();

        } else {
            return construct(definition, args);
        }
    }

    
    public void addKeeps(Definition def) {
        if (def != null && def instanceof NamedDefinition) {
            List<KeepNode> keeps = ((NamedDefinition) def).getKeeps();
            if (keeps != null) {
                Iterator<KeepNode> it = keeps.iterator();
                while (it.hasNext()) {
                    KeepNode k = it.next();
                    keep(k);
                }
            }
        }
    }

    @SuppressWarnings("unchecked")
    private void keep(KeepNode k) {
        
        Map<String, Object> table = null;
        Instantiation instance = k.getTableInstance();

        if (instance == null) {
            table = topScope.getKeepKeep();

        } else {
            NamedDefinition def = (NamedDefinition) instance.getDefinition(this);
            if (def instanceof CollectionDefinition) {
        
                CollectionDefinition collectionDef = (CollectionDefinition) def; // ((TableDefinition) def).initCollection(this);
                CollectionInstance collection = collectionDef.getCollectionInstance(this, instance.getArguments(), instance.getIndexes());
                if (collectionDef.isTable()) {
                    table = (Map<String, Object>) collection.getCollectionObject();
                } else {    
                    table = new MappedArray(collection.getCollectionObject(), this);
                }
        
            } else {
                Object tableObject = instance.getData(this);
                if (tableObject == null || tableObject.equals(NullValue.NULL_VALUE)) {
                    throw new NullPointerException("error in keep: table " + instance.getName() + " not found");
                }
        
                if (tableObject instanceof CantoNode) {
                    def.initNode((CantoNode) tableObject);
                }
                if (tableObject instanceof Map<?,?>) {
                    table = (Map<String, Object>) tableObject;
                } else if (tableObject instanceof List || tableObject.getClass().isArray()) {
                    table = new MappedArray(tableObject, this);
                } else if (tableObject instanceof CollectionInstance) {
                    Object obj = ((CollectionInstance) tableObject).getCollectionObject();
                    if (obj instanceof Map<?,?>) {
                        table = (Map<String, Object>) obj;
                    } else {
                        table = new MappedArray(obj, this);
                    }
                } else if (tableObject instanceof CollectionDefinition) {
                    CollectionInstance collection = ((CollectionDefinition) tableObject).getCollectionInstance(this, instance.getArguments(), instance.getIndexes());
                    if (((CollectionDefinition) tableObject).isTable()) {
                        table = (Map<String, Object>) collection.getCollectionObject();
                    } else {    
                        table = new MappedArray(collection.getCollectionObject(), this);
                    }
                }
            }
        }
        if (table != null) {
            ResolvedInstance ri = k.getResolvedDefInstance(this);
            ResolvedInstance riAs = k.getResolvedAsInstance(this);
            Name asName = k.getAsName();
            String key = null;
            if (asName != null) {
                key = asName.getName();
                if (key == Name.THIS) {
                    key = definingDef.getName();
                }
            }

            topScope.addKeep(ri, riAs, key, table, keepMap, cache);
        }
    }

    private void setKeepsFromScope(Scope scope) {
        topScope.copyKeeps(scope);
    }

    private void addKeepsFromScope(Scope scope) {
        topScope.addKeeps(scope);
    }

    /** Determine how far down the context stack to go looking to see if a value should
     *  be cached, whether by a keep statement or because the definition resides at
     *  that level.
     */
    private int getMaxKeepLevels(Definition def) {
        int levels = 0;
        if (def == null) {
            return -1;
        }
        Definition owner = def.getOwner();
        while (owner != null) {
            if (owner instanceof ComplexDefinition) {
                break;
            }
            owner = owner.getOwner();
        }
        ComplexDefinition scopeOwner = owner instanceof ComplexDefinition ? (ComplexDefinition) owner : null;
        Scope scope = topScope;
        Definition scopeDef = scope.def;
        boolean reachedScope = (scopeOwner == null || scopeOwner.equals(scopeDef) || scopeOwner.isSubDefinition(scopeDef));
        while (true) {
            levels++;
            scope = scope.previous;
            if (scope == null) {
                // may have been obtained by reflection or some other out-of-scope mechanism; don't try to
                // cache beyond local level
                if (!reachedScope) {
                    levels = 0;
                }
                break;
            }
            if (reachedScope) {
                if (!scope.def.equals(scopeDef)) {
                    break;
                }
            } else {
                scopeDef = scope.def;
                reachedScope = (scopeOwner.equals(scopeDef) || 
                                scopeOwner.isSubDefinition(scopeDef));
            }
        }

        return levels;
    }
    

    @SuppressWarnings("unchecked")
    static Object getKeepData(Map<String, Object> cache, String key, String fullKey) {
        Object data = null;

        data = cache.get(key);

        if (data == null && fullKey != null) {
            int ix = fullKey.indexOf('.');
            if (ix > 0) {
                String firstKey = fullKey.substring(0, ix);
                String restOfKey = fullKey.substring(ix + 1);
                Object obj = cache.get(firstKey);
                if (obj != null && obj instanceof Holder) {
                    Holder holder = (Holder) obj;
                    if (holder.data != null && holder.data instanceof Map<?,?>) {
                        data = getKeepData((Map<String, Object>) holder.data, key, restOfKey); 
                    }
                }
                if (data == null) {
                    String keepKeepKey = firstKey + ".keep";
                    Map<String, Object> keepKeep = (Map<String, Object>) cache.get(keepKeepKey);
                    if (keepKeep != null) {
                        data = getKeepData(keepKeep, key, restOfKey);
                    }
                }
            }
        }
        if (data == null) {
            int ix = key.indexOf('.');
            if (ix > 0) {
                String firstKey = key.substring(0, ix);
                String restOfKey = key.substring(ix + 1);
                Object obj = cache.get(firstKey);
                if (obj != null && obj instanceof Holder) {
                    Holder holder = (Holder) obj;
                    if (holder.data != null && holder.data instanceof Map<?,?>) {
                        data = getKeepData((Map<String, Object>) holder.data, key, restOfKey); 
                    }
                }
                if (data == null) {
                    String keepKeepKey = firstKey + ".keep";
                    Map<String, Object> keepKeep = (Map<String, Object>) cache.get(keepKeepKey);
                    if (keepKeep != null) {
                        data = getKeepData(keepKeep, restOfKey, null);
                    }
                }
            }
        }
        return data;
    }
    
    static class KeepHolder {
        public NameNode keepName;
        public Definition owner;
        public ResolvedInstance resolvedDefInstance;
        public ResolvedInstance resolvedAsInstance;
        public NameNode byName;
        public Map<String, Object> table;
        public boolean inContainer;
        public boolean asThis;

        KeepHolder(NameNode keepName, Definition owner, ResolvedInstance ri, ResolvedInstance riAs, NameNode byName, Map<String, Object> table, boolean inContainer, boolean asThis) {
            this.keepName = keepName;
            this.owner = owner;
            this.resolvedDefInstance = ri;
            this.resolvedAsInstance = riAs;
            this.byName = byName;
            this.table = table;
            this.inContainer = inContainer;
            this.asThis = asThis;
        }
    }
    
    
    private static int hashMapsCreated = 0;
    public static int getNumHashMapsCreated() {
        return hashMapsCreated;
    }

    public static <E> HashMap<String, E> newHashMap(Class<E> c) {
        hashMapsCreated++;
        return new HashMap<String, E>();
    }

    public static <E> HashMap<String, E> newHashMap(Map<String, E> map) {
        hashMapsCreated++;
        return new HashMap<String, E>(map);
    }

    public static <E> HashMap<String, Map<String,E>> newHashMapOfMaps(Class<E> c) {
        hashMapsCreated++;
        return new HashMap<String, Map<String, E>>();
    }

    private static int arrayListsCreated = 0;
    private static long totalListSize = 0L;
    public static int getNumArrayListsCreated() {
        return arrayListsCreated;
    }
    public static long getTotalListSize() {
        return totalListSize;
    }

    public static <E> ArrayList<E> newArrayList(int size, Class<E> c) {
        arrayListsCreated++;
        totalListSize += size;
        return new ArrayList<E>(size);
    }

    public static <E> ArrayList<E> newArrayList(int size, List<E> list) {
        arrayListsCreated++;
        totalListSize += size;
        return new ArrayList<E>(size);
    }

    public static <E> ArrayList<E> newArrayList(List<E> list) {
        arrayListsCreated++;
        totalListSize += list.size();
        return new ArrayList<E>(list);
    }

    protected static int scopesCreated = 0;
    public static int getNumEntriesCreated() {
        return scopesCreated;
    }
    protected static int scopesCloned = 0;
    public static int getNumEntriesCloned() {
        return scopesCloned;
    }


    public Scope newScope(Definition def, Definition superdef, ParameterList params, ArgumentList args) {

        Map<String, Object> scopeKeep = null;
        if (def instanceof Site) {
            scopeKeep = siteKeeps.get(def.getName());
            if (scopeKeep == null) {
                scopeKeep = newHashMap(Object.class);
                siteKeeps.put(def.getName(), scopeKeep);
            }
        }
        // ENTRY REUSE HAS BEEN DISABLED FOR NOW
        // getAbandonedScope will always return null
        Scope scope = getAbandonedScope();
        if (scope != null) {
            scope.init(def, superdef, params, args, scopeKeep, globalKeep);
        } else {
            scope = new Scope(def, superdef, params, args, scopeKeep, globalKeep);
        }
        return scope;
    }

    public Scope newScope(Scope copyScope, boolean copyKeep) {
        // ENTRY REUSE HAS BEEN DISABLED FOR NOW
        // getAbandonedScope will always return null
        Scope scope = getAbandonedScope();
        if (scope != null) {
            scope.copy(copyScope, copyKeep);
        } else {
            scope = new Scope(copyScope, copyKeep);
        }
        return scope;
    }

    private void oldScope(Scope scope) {
        // recycle if the refCount has dropped to zero, unless it's the top of the
        // unpushedEntries stack, which may have a refCount of 0 but should
        // definitely not be abandoned.

        synchronized (scope) {
            if (scope.refCount == 0 && !unpushedScopes.contains(scope)) {
                scope.clear();
                addAbandonedScope(scope);
            } else {
                LOG.debug(" !!! popped an scope with ref count of " + scope.refCount);
            }
        }

    }
    // DISABLE ENTRY REUSE FOR NOW
    // see addAbandonedScope
    private Scope getAbandonedScope() {
        Scope scope = rootContext.abandonedScopes;
        if (scope != null) {
            synchronized (rootContext.abandonedScopes) {
                Scope next = scope.getPrevious();
                scope.setPrevious(null);
                rootContext.abandonedScopes = next;
            }
        }
        return scope;
    }

    // DISABLE ENTRY REUSE FOR NOW
    // by disabling this function
    private void addAbandonedScope(Scope scope) {
        //scope.setPrevious(rootContext.abandonedScopes);
        //rootContext.abandonedScopes = scope;
    }

    public Scope getRootScope() {
        return rootScope;
    }

    class ContextIterator implements Iterator<Scope> {
        private Scope nextScope;

        public ContextIterator() {
            nextScope = topScope;
        }

        public boolean hasNext() {
            return (nextScope != null);
        }

        public Scope next() {
            if (nextScope == null) {
                throw new NoSuchElementException();
            }
            Scope scope = nextScope;
            nextScope = nextScope.getPrevious();
            return scope;
        }

        public void remove() {
            throw new UnsupportedOperationException("ReverseIterator does not support remove");
        }
    }

}

class ContextMarker {
    private static final Log LOG = Log.getLogger(ContextMarker.class);

    Context rootContext = null;
    int stateCount = -1;
    int loopIndex = -1;

    public ContextMarker() {}

    public ContextMarker(Context context) {
        context.mark(this);
    }

    public boolean equals(Object object) {
        if (object instanceof ContextMarker) {
            ContextMarker marker = (ContextMarker) object;
            if (loopIndex >= 0) {
                LOG.debug("comparing context marker loop indices: " + loopIndex + " to " + marker.loopIndex);
            }
            return (marker.rootContext == rootContext && marker.stateCount == stateCount && marker.loopIndex == loopIndex);
        } else {
            return object.equals(this);
        }
    }
    public int hashCode() {
        int n = (rootContext.getRootScope().hashCode() << 16) + stateCount;
        return n;
    }
}
