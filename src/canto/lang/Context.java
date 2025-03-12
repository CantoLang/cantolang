/* Canto Compiler and Runtime Engine
 * 
 * Context.java
 *
 * Copyright (c) 2018-2025 by cantolang.org
 * All rights reserved.
 */

package canto.lang;

import java.util.*;

import canto.runtime.CantoObjectWrapper;
import canto.runtime.CantoSession;
import canto.util.Holder;
import canto.runtime.Log;
import canto.util.StateFactory;

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

    private static int instanceCount = 0;
    public static int getNumContextsCreated() {
        return instanceCount;
    }
    public static void resetNumContextsCreated() {
        instanceCount = 0;
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

    private ArrayDeque<Scope> scopeStack = new ArrayDeque<Scope>();
    private ArrayDeque<Scope> unpushedScopes = new ArrayDeque<Scope>();
    private Scope abandonedScopes = null;

    
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
        scopeStack.push(new Scope(site, null, null));        
    }
   
    public Context(Context context) {
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
        scopeStack = new ArrayDeque<Scope>();
        if (context.scopeStack.size() > 0) {
            scopeStack.push(context.scopeStack.peek());
        }
    }
    // -------------------------------------------
    // push, pop, peek, etc.
    
    public void push(Definition def, ParameterList params, ArgumentList args) throws Redirection {
        DefinitionInstance defInstance = getContextDefInstance(def, args);
        Scope scope = newScope(defInstance.def, defInstance.def, params, defInstance.args);
        push(scope);
    }

    public void push(Definition def, ParameterList params, ArgumentList args, boolean newFrame) throws Redirection {
        DefinitionInstance defInstance = getContextDefInstance(def, args);
        if (defInstance.args != null && (defInstance.args != args || params == null)) {
            args = defInstance.args;
            params = defInstance.def.getParamsForArgs(args, this);
        }
        Definition superdef = (newFrame ? null : defInstance.def);
        Scope scope = newScope(defInstance.def, superdef, params, args);
        push(scope);
    }

    public void push(Definition instantiatedDef, Definition superdef, ParameterList params, ArgumentList args) throws Redirection {
        DefinitionInstance defInstance = getContextDefInstance(instantiatedDef, args);
        if (defInstance.args != null && defInstance.args != args) {
            args = defInstance.args;
            params = defInstance.def.getParamsForArgs(args, this);
        }
        Scope scope = newScope(defInstance.def, getContextDefinition(superdef), params, args);
        push(scope);
    }

    private void push(Scope scope) throws Redirection {
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
                    List<KeepStatement> keeps = scopedef.getKeeps();
                    if (keeps != null) {
                        Iterator<KeepStatement> it = keeps.iterator();
                        while (it.hasNext()) {
                            KeepStatement k = it.next();
                            try {
                                keep(k);
                            } catch (Redirection r) {
                                vlog("Error in keep statement: " + r.getMessage());
                                throw r;
                            }
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
                List<KeepStatement> keeps = scopedef.getKeeps();
                if (keeps != null) {
                    Iterator<KeepStatement> it = keeps.iterator();
                    while (it.hasNext()) {
                        KeepStatement k = it.next();
                        try {
                            keep(k);
                        } catch (Redirection r) {
                            vlog("Error in keep statement: " + r.getMessage());
                            throw r;
                        }
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
            setRootScope(scope);
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
            if (topScope == popLimit) {
                vlog("popping context beyond popLimit");
                throw new IndexOutOfBoundsException("Illegal pop attempt; can only pop scopes pushed after this copy was made.");
            }
            Scope scope = topScope;
            setTop(scope.getPrevious());
            return scope;
        } else {
            return null;
        }
    }

    public synchronized Scope unpush() {
        if (size <= 1) {
            throw new IndexOutOfBoundsException("Attempt to unpush root scope in context");
        }
        Scope scope = _pop();
        unpushedEntries.push(scope);
        return scope;
    }

    public synchronized void repush() {
        Scope scope = unpushedEntries.pop();
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
            try {
                Definition elementDef = ((ElementReference) definition).getElementDefinition(this);
                if (elementDef != null) {
                    definition = elementDef;
                }
            } catch (Redirection r) {
                throw new IllegalStateException("Redirection on attempt to get element definition: " + r);
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
            try {
                Definition elementDef = ((ElementReference) definition).getElementDefinition(this);
                if (elementDef != null) {
                    definition = elementDef;
                }
            } catch (Redirection r) {
                throw new IllegalStateException("Redirection on attempt to get element definition: " + r);
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

    public Object getParameter(NameNode name, boolean inContainer, Class<?> returnClass) throws Redirection {
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
            List<Index> paramIndexes = paramNameNode.getIndexes();
            if ((paramArgs != null && paramArgs.size() > 0) || (paramIndexes != null && paramIndexes.size() > 0)) {
                Context argContext = this;
                if (mustUnpush) {
                    argContext = clone(false);
                    Scope clonedScope = newScope(unpushedEntries.peek(), true);
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
                    CantoObjectWrapper wrapper = (CantoObjectWrapper) ((Value) arg).getValue();
                    Context argContext = wrapper.context;
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

    public Object getRootScope() {
        return rootScope;
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
