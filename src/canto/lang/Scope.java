/* Canto Compiler and Runtime Engine
 * 
 * Scope.java
 *
 * Copyright (c) 2024-2025 by cantolang.org
 * All rights reserved.
 */

package canto.lang;

import java.util.*;

import canto.runtime.Log;
import canto.util.StateFactory;
import canto.util.Holder;

/**
 * 
 */
public class Scope {
    private static final Log LOG = Log.getLogger(Scope.class);
    
    protected static int scopesCreated = 0;
    public static int getNumEntriesCreated() {
        return scopesCreated;
    }
    protected static int scopesCloned = 0;
    public static int getNumEntriesCloned() {
        return scopesCloned;
    }

    private final static int MAX_POINTER_CHAIN_LENGTH = 10; 
    
    public Definition def;
    public Definition superdef;
    public ParameterList params;
    public ArgumentList args;
    public Scope superscope = null;
    public Scope root = null;
    public Scope previous = null;
    
    Map<String, Object> cache = null;
    private Map<String, Object> globalKeep = null;

    // Objects that are persisted through keep directives are cached here
    public Map<String, Pointer> keepMap = null;
    private Map<String, Object> keepKeep = null;
 
    
    int refCount = 0;   // number of previouss by other scopes to this one
    private int contextState = -1;
    private int loopIx = -1;
    private StateFactory loopIndexFactory;
    int origParamsSize;
    int origArgsSize;

    
    public Scope(Definition def, ParameterList params, ArgumentList args) {
        scopesCreated++;

        this.def = def;
        this.superdef = null;
        this.params = params;
        this.args = args;
        origParamsSize = params.size();
        origArgsSize = args.size();
        loopIndexFactory = new StateFactory();
    }
    

    public Scope(Definition def, Definition superdef, ParameterList params, ArgumentList args, Map<String, Object> cache, Map<String, Object> globalKeep) {
        scopesCreated++;

        this.def = def;
        this.superdef = superdef;
        this.params = (params != null ? (ParameterList) params.clone() : new ParameterList(Context.newArrayList(0, DefParameter.class)));
        this.args = (args != null ? (ArgumentList) args.clone() : new ArgumentList(Context.newArrayList(0, Construction.class)));
        origParamsSize = this.params.size();
        origArgsSize = this.args.size();

        // fill out the argument list with nulls if it's shorter than the parameter list
        while (origArgsSize < origParamsSize) {
            this.args.add(ArgumentList.MISSING_ARG);
            origArgsSize++;
        }
        loopIndexFactory = new StateFactory();

        this.cache = cache;
        this.globalKeep = globalKeep;
    }

    protected Scope(Scope scope, boolean copyKeep) {
        scopesCreated++;
        scopesCloned++;

        def = scope.def;
        params = (scope.params != null ? (ParameterList) scope.params.clone() : new ParameterList(Context.newArrayList(0, DefParameter.class)));
        args = (scope.args != null ? (ArgumentList) scope.args.clone() : new ArgumentList(Context.newArrayList(0, Construction.class)));

        // don't clone the previous to avoid duplicating references.  If the
        // clone needs to point somewhere, it has to be done explicitly.

        contextState = scope.contextState;
        loopIx = scope.loopIx;
        loopIndexFactory = scope.loopIndexFactory;

        // the keep map is always shared
        keepMap = scope.keepMap;
        globalKeep = scope.globalKeep;

        // make shallow copy of cache if copyKeep flag is true, otherwise they
        // will be null;
        if (copyKeep) {
            // for now, no read only cache, let everybody write (yikes!)
            cache = scope.getKeep();
            keepKeep = scope.getKeepKeep();
        }
    }

    void init(Definition def, Definition superdef, ParameterList params, ArgumentList args, Map<String, Object> cache, Map<String, Object> globalKeep) {
        this.def = def;
        this.superdef = superdef;
        this.params = (params != null ? (ParameterList) params.clone() : new ParameterList(Context.newArrayList(0, DefParameter.class)));
        this.args = (args != null ? (ArgumentList) args.clone() : new ArgumentList(Context.newArrayList(0, Construction.class)));

        origParamsSize = this.params.size();
        origArgsSize = this.args.size();

        // fill out the argument list with nulls if it's shorter than the parameter list
        while (origArgsSize < origParamsSize) {
            this.args.add(ArgumentList.MISSING_ARG);
            origArgsSize++;
        }

        this.cache = cache;
        this.keepKeep = null;
    }

    public Object get(String key) {
        if (cache == null) {
            return null;
        } else {
            return cache.get(key);
        }
    }
    
    public void put(String key, Object value) {
        if (cache == null) {
            cache = new HashMap<String, Object>();
        }
        cache.put(key, value);
    }

    void copy(Scope scope, boolean copyKeep) {
        if (refCount > 0) {
            throw new RuntimeException("Attempt to copy over scope with non-zero refCount");
        }

        def = scope.def;
        superdef = scope.superdef;
        if (params != null) {
            params.clear();
            if (scope.params != null) {
                params.addAll(scope.params);
            }
        } else {
            params = (scope.params != null ? (ParameterList) scope.params.clone() : new ParameterList(Context.newArrayList(0, DefParameter.class)));
        }

        if (args != null) {
            args.clear();
            if (scope.args != null) {
                args.addAll(scope.args);
            }
        } else {
            args = (scope.args != null ? (ArgumentList) scope.args.clone() : new ArgumentList(Context.newArrayList(0, Construction.class)));
        }

        contextState = scope.contextState;
        loopIx = scope.loopIx;
        loopIndexFactory = scope.loopIndexFactory;

        // for now, let everybody write (yikes!)
        //scope.readOnlyKeep = (cache != null ? cache : readOnlyKeep);
        if (copyKeep) {
            copyKeeps(scope);
        } else {
            cache = null;
            keepKeep = null;

            // keepMap is shared everywhere
            keepMap = scope.keepMap;
        }

    }

    void copyKeeps(Scope scope) {
        // Calling getKeep allocates the cache if it doesn't exist; this is
        // wasteful if the cache never gets used, but it guarantees that if the
        // cache is used, it's the same cache for every scope that wants to use
        // the same cache.
        //
        // To eliminate the waste, we could wait till the cache is allocated,
        // then backfill previous scopes as appropriate.  For now, we do the
        // allocation on the first copy, trading greater waste for less risk.
        cache = scope.getKeep();
        keepKeep = scope.keepKeep;
        keepMap = scope.keepMap;
    }

    void addKeeps(Scope scope) {
        if (cache != null) {
            if (scope.cache != null && scope.cache != cache) {
                synchronized (cache) {
                    cache.putAll(scope.cache);
                }
            }
        } else {
            cache = scope.cache;
        }

        if (keepKeep != null) {
            if (scope.keepKeep != null && scope.keepKeep != keepKeep) {
                synchronized (keepKeep) {
                    keepKeep.putAll(scope.keepKeep);
                }
            }
        } else {
            keepKeep = scope.keepKeep;
        }

        if (keepMap != null) {
            if (scope.keepMap != null && scope.keepMap != keepMap) {
                synchronized (keepMap) {
                    keepMap.putAll(scope.keepMap);
                }
            }
        } else {
            keepMap = scope.keepMap;
        }

    }

    public void clear() {
        if (refCount > 0) {
            throw new RuntimeException("Attempt to clear scope with non-zero refCount");
        }

        def = null;
        superdef = null;
        if (params != null) {
            params.clear();
        }
        if (args != null) {
            args.clear();
        }
        origParamsSize = 0;
        origArgsSize = 0;
        
        setPrevious(null);

        keepMap = null;
        cache = null;
        keepKeep = null;
    }

    private Map<String, Object> getGlobalKeep() {
        return globalKeep;
    }

    public void addKeep(ResolvedInstance ri, ResolvedInstance riAs, Object keyObj, Map<String, Object> table, Map<String, Pointer> contextKeepMap, Map<String, Object> contextKeep) {
        if (def.isGlobal()) {
            contextKeep = getGlobalKeep();
        }

        if (keepMap == null) {
            keepMap = Context.newHashMap(Pointer.class);
        }

        synchronized (keepMap) {
            String key = (keyObj == null ? null : (keyObj instanceof Value ? ((Value) keyObj).getString() : keyObj.toString())); 
            if (key != null) {
                if (!key.endsWith(".")) {
                    if (ri != null) {
                        Pointer p = new Pointer(ri, riAs, keyObj, table);
                        String keepKey = ri.getName();
                        keepMap.put(keepKey, p);
                        keepMap.put(key, p);

                        String contextKey = def.getFullName() + '.' + keepKey;
                        contextKey = contextKey.substring(contextKey.indexOf('.') + 1);
                        Pointer contextp = new Pointer(ri, riAs, contextKey, contextKeep);
                        contextKeepMap.put(contextKey, contextp);
                    }
                } else {
                    if (ri != null) {
                        String name = ri.getName();
                        String keepKey = key + name;
                        Pointer p = new Pointer(ri, keepKey, table);
                        keepMap.put(keepKey, p);

                        String contextKey = def.getFullName() + '.' + keepKey;
                        contextKey = contextKey.substring(contextKey.indexOf('.') + 1);
                        Pointer contextp = new Pointer(ri, contextKey, contextKeep);
                        contextKeepMap.put(contextKey, contextp);
                    }
                }

            }
        }
    }

    public void removeKeep(String name) {
        synchronized (keepMap) {
            keepMap.remove(name);
        }
    }

    /** Returns true if a parameter of the specified name is present in this scope. */
    public boolean paramIsPresent(NameNode nameNode, boolean checkForArg) {
        boolean isPresent = false;
        String name = nameNode.getName();
        int n = name.indexOf('.');
        if (n > 0) {
            name = name.substring(0, n);
        }
        int numParams = params.size();
        for (int i = 0; i < numParams; i++) {
            DefParameter param = params.get(i);
            if (name.equals(param.getName()) && (!checkForArg || args.get(i) != ArgumentList.MISSING_ARG)) {
                isPresent = true;
                break;
            }
        }
        return isPresent;
    }

    /** Return the parameter by a given name, or null if there isn't one. */
    public DefParameter getParam(String name) {
        int n = name.indexOf('.');
        if (n > 0) {
            name = name.substring(0, n);
        }
        int numParams = params.size();
        for (int i = 0; i < numParams; i++) {
            DefParameter param = params.get(i);
            if (name.equals(param.getName()) && args.get(i) != ArgumentList.MISSING_ARG) {
                return param;
            }
        }
        return null;
    }


    public Object get(String key, String globalKey, ArgumentList args, boolean local) {
        return get(key, globalKey, args, false, local, true);
    }

    public Definition getDefinition(String key, String globalKey, ArgumentList args) {
        Holder holder = (Holder) get(key, globalKey, args, true, false, true);
        if (holder != null) {
            return holder.def;
        } else {
            return null;
        }
    }

    public Holder getDefHolder(String key, String globalKey, ArgumentList args, boolean local) {
        return (Holder) get(key, globalKey, args, true, local, true);
    }

    private Object get(String key, String globalKey, ArgumentList args, boolean getDefHolder, boolean local, boolean localAllowed) {
        Holder holder = null;
        Map<String, Object> c = cache;
        Map<String, Object> globalKeep = getGlobalKeep();
        
        if (globalKeep != null && globalKey != null && globalKeep.get(globalKey) != null) {
            c = globalKeep;
        }
        if (c == null && (keepMap == null || keepMap.get(key) == null)) {
            if (this.def == null) {
                return null;
            }
            if (!local && previous != null && !this.def.hasChildDefinition(key, localAllowed) && !NameNode.isSpecialName(key)) {
                return previous.get(key, globalKey, args, getDefHolder, false, (localAllowed && previous.def.equalsOrExtends(this.def.getOwner())));
            } else {
                return null;
            }
        }

        Object data = null;
        Definition def = null;
        ResolvedInstance ri = null;

        // If there is a keep scope here but no value was retrieved from the cache above
        // then it means the value has not been instantiated yet in this context.  If
        // this is not a keep statement, or there is no value for this key in the
        // out-of-context cache, or if the key is accompanied by a non-null modifier,
        // return null rather than continue the search up the context chain in order to
        // force instantiation and avoid bypassing the designated cache.
        if (data == null && keepMap != null && keepMap.get(key) != null) {
            Pointer p = keepMap.get(key);
            ri = p.ri;

            Map<String, Object> keepTable = p.cache;
            data = keepTable.get(p.getKey());

            if (data instanceof Pointer) {
                int i = 0;
                do {
                    p = (Pointer) data;
                    data = p.cache.get(p.getKey());
                    if (data instanceof Holder) {
                        holder = (Holder) data;
                        if (isCompatibleHolder(holder, localAllowed)) {
                            data = (holder.data == CantoNode.UNINSTANTIATED ? null : holder.data);
                            def = holder.def;
                            args = holder.args;
                            ri = holder.resolvedInstance;
                        } else {
                            data = null;
                        }
                    }
                    i++;
                    if (i >= MAX_POINTER_CHAIN_LENGTH) {
                        throw new IndexOutOfBoundsException("Pointer chain in cache exceeds limit");
                    }
                } while (data instanceof Pointer);
            } else if (data instanceof Holder) {
                holder = (Holder) data;
                if (isCompatibleHolder(holder, localAllowed)) {
                    data = (holder.data == CantoNode.UNINSTANTIATED ? null : holder.data);
                    def = p.riAs.getDefinition();
                    args = holder.args;
                    ri = holder.resolvedInstance;
                } else {
                    data = null;
                }
            } else if (data instanceof ElementDefinition) {
                def = (Definition) data;
                data = ((ElementDefinition) data).getElement();
                data = CantoNode.getObjectValue(null, data);
            }
        }

        if (data == null && c != null) {
            String ckey = (c == globalKeep ? globalKey : key);
            data = Context.getKeepData(c, ckey, globalKey);
            if (data != null) {

                if (data instanceof ElementDefinition) {
                    def = (Definition) data;
                    data = ((ElementDefinition) def).getElement();
                } else if (data instanceof Holder) {
                    holder = (Holder) data;
                    if (isCompatibleHolder(holder, localAllowed)) {
                        data = (holder.data == CantoNode.UNINSTANTIATED ? null : holder.data);
                        def = holder.def;
                        args = holder.args;
                        ri = holder.resolvedInstance;
                    } else {
                        data = null;
                    }
                } else if (data instanceof Pointer) {
                    def = ((Pointer) data).riAs.getDefinition();
                    ckey = baseKey(((Pointer) data).getKey());
                }

                String loopModifier = getLoopModifier();
                if (loopModifier != null) {
                    if (data instanceof Pointer && ((Pointer) data).cache == c) {
                        data = c.get(ckey + loopModifier);
                    }
                }
            }

            if (data != null) {
                // to prevent an infinite loop arising from a circular list
                // of pointers, abort after reaching a maximum
                int i = 0;
                if (data instanceof Pointer) {
                    do {
                        Pointer p = (Pointer) data;
                        data = p.cache.get(p.getKey());
                        if (data instanceof Holder) {
                            holder = (Holder) data;
                            if (isCompatibleHolder(holder, localAllowed)) {
                                holder = (Holder) data;
                                data = (holder.data == CantoNode.UNINSTANTIATED ? null : holder.data);
                                def = holder.def;
                                args = holder.args;
                                ri = holder.resolvedInstance;
                            } else {
                                data = null;
                            }
                        }
                        i++;
                        if (i >= MAX_POINTER_CHAIN_LENGTH) {
                            throw new IndexOutOfBoundsException("Pointer chain in cache exceeds limit");
                        }
                    } while (data instanceof Pointer);
                } else if (data instanceof Holder) {
                    holder = (Holder) data;
                    if (isCompatibleHolder(holder, localAllowed)) {
                        holder = (Holder) data;
                        data = (holder.data == CantoNode.UNINSTANTIATED ? null : holder.data);
                        def = holder.def;
                        args = holder.args;
                        ri = holder.resolvedInstance;
                    } else {
                        data = null;
                    }
                }
            }
        }

        // continue up the context chain
        if (data == null && (def == null || !getDefHolder) && !local && previous != null && !this.def.hasChildDefinition(key, localAllowed) && !NameNode.isSpecialName(key)) {
            return previous.get(key, globalKey, args, getDefHolder, false, (localAllowed && previous.def.getOwner().equals(this.def)));
        }

        // return either the definition or the data, depending on the passed flag
        if (getDefHolder) {
            if (holder == null) {
        
                if (def == null && ri != null) {
                    def = ri.getDefinition();
                }
        
                // if def is null, this might be an scope resulting from an "as" clause
                // in a keep statement, so look in the keep table for an scope.
                if (def == null && keepMap != null && keepMap.get(key) != null) {
                    Pointer p = keepMap.get(key);
                    def = p.riAs.getDefinition();
                }
                if (def != null) {
                    Definition nominalDef = def;
                    ArgumentList nominalArgs = args;
                    holder = new Holder(nominalDef, nominalArgs, def, args, null, data, ri);
                } else if (data != null) {
                    holder = new Holder(null, null, null, null, null, data, null);
                }
            }
            return holder;
        } else {
            return data;
        }
    }

    private boolean isCompatibleHolder(Holder holder, boolean localAllowed) {
        return (localAllowed || !holder.def.isLocal());
    }

    /** If the current cache contains a Pointer under the specified key, stores the
     *  data in the cache pointed to by the Pointer; otherwise stores the data in the
     *  current cache.  Then traverses back up the context tree, storing the data in
     *  any other context level where data for that key is explicitly stored (i.e. the
     *  cache contains a Pointer for that key, or the scope definition is the owner or
     *  a subdefinition of the owner of a child whose name is the key).
     *
     *  If the definition parameter is non-null, the data and the definition are
     *  wrapped in a Holder, which is cached.
     */
    public void put(String key, Holder holder, Context context, int maxLevels) {
        boolean kept = false;
        Definition nominalDef = holder.nominalDef;
        Map<String, Object> localKeep = getKeep();
        synchronized (localKeep) {
            if (localPut(localKeep, keepMap, key, holder, true)) {
                kept = true;
            }
        }

        if (nominalDef != null) {
            if (nominalDef.isGlobal() && nominalDef.getName().equals(key)) {
                if (globalKeep != null) {
                    synchronized (globalKeep) {
                        String globalKey = makeGlobalKey(nominalDef.getFullNameInContext(context));
                        localPut(globalKeep, null, globalKey, holder, true);
                    }
                } else {
                    throw new NullPointerException("global cache not found");
                }
            }
        }

        Definition.Access access = (nominalDef != null ? nominalDef.getAccess() : Definition.Access.LOCAL);
        if (previous != null && access != Definition.Access.LOCAL) {
            KeepNode keep = this.def.getKeep(key);
            String ownerName = this.def.getName();
            if (ownerName != null && nominalDef != null && !nominalDef.isFormalParam()) { 
                // should this be def or nominalDef?
                Definition defOwner = nominalDef.getOwner();
                for (Scope nextScope = previous; nextScope != null; nextScope = nextScope.previous) {
                    if (nextScope.def.equalsOrExtends(defOwner)) {
                        // get the subbest subclass
                        do {
                            defOwner = nextScope.def;
                            nextScope = nextScope.previous;
                        } while (nextScope != null && nextScope.def.equalsOrExtends(defOwner));

                        break;
                    }
                }
                for (String k = key; defOwner != null && k.indexOf('.') > 0; k = k.substring(k.indexOf('.') + 1)) {
                    defOwner = defOwner.getOwner();
                }

                if (defOwner != null && defOwner.getDurability() != Definition.Durability.DYNAMIC && this.def.equalsOrExtends(defOwner)) {
                    Definition defOwnerOwner = defOwner.getOwner();
                    boolean isSite = (defOwnerOwner instanceof Site);
                    Map<String, Object> ownerKeep = null;
                    Scope scope = previous;
                    while (scope != null) {
                        if (scope.def.equalsOrExtends(defOwnerOwner)) {
                            ownerKeep = scope.getKeep();
                            break;
                        }
                        scope = scope.previous;
                    }
                    if (scope != null && ownerKeep != null) {
                        String ownerKey = ownerName + "." + key;
                        if (keep != null || ownerKeep.containsKey(ownerKey)) {
                            synchronized (ownerKeep) {
                                scope.localPut(ownerKeep, null, ownerName + "." + key, holder, false);
                            }
                        }
                    }
                }
            }
    
            // keep directives intercept cache updates
            if (maxLevels > 0) maxLevels--;
            if (!kept && maxLevels != 0) {
                if (this.def.hasChildDefinition(key, true)) {
                    if (keep == null || keep.getTableInstance() == null) {
                        return;
                    }
                }
                previous.checkForPut(key, holder, context, maxLevels);
            }
        }
    }

    private Scope getOwnerContainerScope(Definition def) {
        if (def != null) {
            Definition ownerDef = def.getOwner();
            if (ownerDef != null) {
                boolean isSite = (ownerDef instanceof Site); // && !(ownerDef instanceof Core);
                Scope scope = this;
                while (scope != null) {
                    if (scope.def.equalsOrExtends(ownerDef)) {
                        break;
                    } else if (scope.def != def && scope.def.equalsOrExtends(def)) {
                        Definition scopeDefOwner = scope.def.getOwner();
                        if (scopeDefOwner != null) {
                            ownerDef = scopeDefOwner;
                        }
                    } else if (isSite && scope.previous == null) {
                        break;
                    }
                    scope = scope.previous;
                }
                return scope;
            }
        }
        return null;
    }


    private boolean localPut(Map<String, Object> cache, Map<String, Pointer> keepMap, String key, Holder holder, boolean updateContainerChild) {
        boolean kept = false;
        Pointer p = null;
        Object oldData = cache.get(key);

        // if this is the first scope, set up any required pointers for keep tables
        // and modifiers, save the data and return
        if (oldData == null || (oldData instanceof Holder && (((Holder) oldData).data == CantoNode.UNINSTANTIATED || ((Holder) oldData).data == null))) {

            Object newData = holder;
            if (keepMap != null) {
                p = keepMap.get(key);
                if (p != null) {
                    Map<String, Object> keepTable = p.cache;
                    if (keepTable != cache || !key.equals(p.getKey())) {
                        synchronized (keepTable) {
                            p = new Pointer(p.ri, p.riAs, p.getKey(), keepTable);

                            // two scenarios: keep as and cached identity.  With keep as, we want the
                            // pointer def; with cached identity, we want the holder def.  We can tell
                            // cached identities because the def and nominalDef in the holder are different.
                            Definition newDef;
                            if (holder.def != null && !holder.def.equals(holder.nominalDef)) {
                                newDef = holder.def; 
                            } else {
                                newDef = p.riAs.getDefinition();
                            }
                            ArgumentList newArgs = (newDef == holder.def ? holder.args : null);
                            Holder newHolder = new Holder(holder.nominalDef, holder.nominalArgs, newDef, newArgs, null, holder.data, holder.resolvedInstance);
                            keepTable.put(p.getKey(), newHolder);
                            newData = p;
                        }
                    }

                    kept = true;
                }
            }
            cache.put(key, newData);

        } else {

            // There is an existing scope.  See if it's a pointer
            if (oldData instanceof Pointer) {
                p = (Pointer) oldData;
                Map<String, Object> keepTable = p.cache;
                if (keepTable != cache || !key.equals(p.getKey())) {
                    p = new Pointer(p.ri, p.riAs, p.getKey(), keepTable);

                    // two scenarios: keep as and cached identity.  With keep as, we want the
                    // pointer def; with cached identity, we want the holder def.  We can tell
                    // cached identities either from the definition's identity flag or because 
                    // the def and nominalDef in the holder are different.
                    Definition newDef = (!holder.def.equals(holder.nominalDef) || holder.def.isIdentity() ? holder.def : p.riAs.getDefinition()); 
                    ArgumentList newArgs = (newDef == holder.def ? holder.args : null);
                    holder = new Holder(holder.nominalDef, holder.nominalArgs, newDef, newArgs, null, holder.data, holder.resolvedInstance);
                    kept = true;
                }
            }

            // Follow the pointer chain to update the data
            Map<String, Object> nextKeep = cache;
            String nextKey = key;
            Object nextData = oldData;
            while (nextData != null && nextData instanceof Pointer) {
                p = (Pointer) nextData;
                nextKeep = p.cache;
                nextKey = p.getKey();
                nextData = nextKeep.get(nextKey);
            }
            synchronized (nextKeep) {
                //if (def != null) {
                //    data = new Holder(def, args, context, data);
                //}
                nextKeep.put(nextKey, holder);
            }
    
        }
        // if the key has multiple parts, it represents a container and child.  If we 
        // need to update container children (specified by a boolean parameter to this 
        // function), and we are caching a container child, check to see if there is a
        // keep map cached for the definition corresponding to the prefix (i.e., the
        // container).  If so, check to see if that keep map has an scope for the 
        // child being cached.  If so, update that scope. 
        if (updateContainerChild) {
            int ix = key.indexOf('.');
            if (ix > 0) {
                String prefix = key.substring(0, ix);
                String childKey = key.substring(ix + 1);
                Object keepObj = cache.get(prefix + ".keep");
                if (keepObj instanceof Holder) {
                    keepObj = ((Holder) keepObj).data;
                }
                try {
                    @SuppressWarnings("unchecked")
                    Map<String, Object> parentKeepKeep = (Map<String, Object>) keepObj;
                    if (parentKeepKeep != null) {
                        @SuppressWarnings("unchecked")
                        Map<String, Pointer> parentKeepMap = (Map<String, Pointer>) parentKeepKeep.get("from");
                        if (parentKeepMap != null && parentKeepMap.containsKey(childKey)) {
                            localPut(parentKeepKeep, parentKeepMap, childKey, holder, false);
                        }
                    }
                } catch (Exception e) {
                    System.err.println("Exception getting parent keep map: " + e);
                    e.printStackTrace();
                }
            }
        }
        return kept;
    }

    private void checkForPut(String key, Holder holder, Context context, int maxLevels) {

        if ((cache != null && cache.get(key) != null) ||
                (keepMap != null && keepMap.get(key) != null) ||
                this.def.hasChildDefinition(key, true)) {

            put(key, holder, context, maxLevels);
        } else {
            if (maxLevels > 0) maxLevels--;
            if (previous != null && maxLevels != 0) {
                previous.checkForPut(key, holder, context, maxLevels);
            }
        }
    }

    private String getLoopModifier() {
        int loopIx = getLoopIndex();
        if (loopIx >= 0) {
            return "#" + String.valueOf(loopIx);
        } else {
            return null;
        }
    }

    Map<String, Object> getKeep() {
        if (cache == null) {
            cache = Context.newHashMap(Object.class);
        }
        return cache;
    }

    @SuppressWarnings("unchecked")
    Map<String, Object> getKeepKeep() {
        if (keepMap == null) {
            keepMap = Context.newHashMap(Pointer.class);
        }

        if (keepKeep == null) {
            // cache the keep cache in the owner scope in the context
            Scope containerScope = getOwnerContainerScope(def);
            if (containerScope != null && containerScope != this) {
                String key = def.getName() + ".keep";
                Map<String, Object> containerKeep = containerScope.getKeep();
                synchronized (containerKeep) {
                    keepKeep = (Map<String, Object>) containerKeep.get(key);
                    if (keepKeep == null) {
                        Map<String, Object> containerKeepKeep = containerScope.getKeepKeep();
                        keepKeep = (Map<String, Object>) containerKeepKeep.get(key);
                        if (keepKeep == null) {
                            keepKeep = Context.newHashMap(Object.class);
                            if (def.getDurability() != Definition.Durability.DYNAMIC) {
                                containerKeep.put(key, keepKeep);
                                containerKeepKeep.put(key, keepKeep);
                            }
                            keepKeep.put("from", keepMap);
                        }
                    }
                }
            } else {
                keepKeep = Context.newHashMap(Object.class);
            }            
        } else {
            // make sure the keep cache is cached in the owner scope 
            // in the context
            Scope containerScope = getOwnerContainerScope(def);
            if (containerScope != null) {
                String key = def.getName() + ".keep";
                Map<String, Object> containerKeep = containerScope.getKeep();
                Map<String, Object> containerKeepKeep = (Map<String, Object>) containerKeep.get(key);
                if (containerKeepKeep == null) {
                    keepKeep.put("from", keepMap);
                    if (def.getDurability() != Definition.Durability.DYNAMIC) {
                        containerKeep.put(key, keepKeep);
                    }
                }        
            }
        }
        return keepKeep;
    }

    @SuppressWarnings("unchecked")
    void addKeepKeep(Map<String, Object> cache) {
        if (keepKeep == null) {
            keepKeep = Context.newHashMap(Object.class);
        }
        Set<Map.Entry<String, Object>> entrySet = cache.entrySet();
        Iterator<Map.Entry<String, Object>> it = entrySet.iterator();
        while (it.hasNext()) {
            Map.Entry<String, Object> scope = it.next();
            String key = scope.getKey();
            if (!key.equals("from")) {
                keepKeep.put(scope.getKey(), scope.getValue());
            }
        }
        if (keepMap == null) {
            keepMap = Context.newHashMap(Pointer.class);
            keepKeep.put("from", keepMap);
        }
        Map<String, Pointer> map = (Map<String, Pointer>) cache.get("from");
        if (map != null) {
            keepMap.putAll(map);
        }
    }

    public boolean equals(Object obj) {
        if (obj instanceof Scope) {
            Scope scope = (Scope) obj;
            if (def.equals(scope.def) && args.equals(scope.args)) {
                if (superdef == null) {
                    return (scope.superdef == null);
                } else {
                    return superdef.equals(scope.superdef); 
                }
            }
        }
        return false;
    }

    public boolean covers(Definition def) {
        if (def.equals(this.def) || def.equals(superdef)) {
            return true;
        } else {
            Definition sdef = this.def.getSuperDefinition();
            return sdef == null ? false : def.equalsOrExtends(sdef);
        }
    }

    public String toString() {
        StringBuffer sb = new StringBuffer();
        sb.append(def.getName());

        sb.append(" <");
        if (superdef != null) {
            sb.append(superdef.getName());
        }

        sb.append("> A:");
        if (args != null && args.size() > 0) {
            sb.append(args.toString());
        } else {
            sb.append("()");
        }

        sb.append(" P:");
        if (params != null && params.size() > 0) {
            sb.append(params.toString());
        } else {
            sb.append("()");
        }

        if (loopIx > -1) {
            sb.append("@" + loopIx);
        }
        return sb.toString();
    }

    int getState() {
        return contextState;
    }

    void setState(int state) {
        contextState = state;
    }

    int getLoopIndex() {
        return loopIx;
    }

    void setLoopIndex(int ix) {
        loopIx = ix;
    }

    void advanceLoopIndex() {
        loopIx = loopIndexFactory.nextState();
    }

    void resetLoopIndex() {
        loopIx = -1;
    }

    public boolean isInLoop() {
        return loopIx != -1;
    }

    public Scope getPrevious() {
        return previous;
    }

    void setPrevious(Scope scope) {
        // decrement the ref count in the old previous
        if (previous != null) {
            previous.refCount--;
        }
        previous = scope;
        if (previous != null) {
            previous.refCount++;
        }
    }
    void incRefCount() {
        refCount++;
    }

    void decRefCount() {
        refCount--;
    }

    static String makeGlobalKey(String fullName) {
        return fullName;
    }
    
    /** Takes a cache key and strips off modifiers indicating arguments or loop index. */
    private static String baseKey(String key) {
        // argument modifier
        int ix = key.indexOf('(');
        if (ix > 0) {
            key = key.substring(0, ix);
        } else {
            // loop modifier
            ix = key.indexOf('#');
            if (ix > 0) {
                key = key.substring(0, ix);
            }
        }
        return key;
    }

}


