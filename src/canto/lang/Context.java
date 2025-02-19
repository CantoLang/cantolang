/* Canto Compiler and Runtime Engine
 * 
 * Context.java
 *
 * Copyright (c) 2018-2025 by cantolang.org
 * All rights reserved.
 */

package canto.lang;

import java.util.*;

import canto.runtime.CantoSession;
import canto.util.Holder;
import canto.runtime.Log;

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
    
    private int stateCount;

    private int size = 0;

    private ArrayDeque<Scope> scopeStack = new ArrayDeque<Scope>();
    private ArrayDeque<Scope> unpushedScopes = new ArrayDeque<Scope>();
    private Scope abandonedScopes = null;

    
    public Context() {
        instanceCount++;
        rootContext = this;
    }

    public Context(Site site) {
        instanceCount++;
        rootContext = this;
        scopeStack.push(new Scope(site, null, null));        
    }
   
    public Context(Context context) {
        instanceCount++;
        rootContext = context.rootContext;
        scopeStack = new ArrayDeque<Scope>();
        if (context.scopeStack.size() > 0) {
            scopeStack.push(context.scopeStack.peek());
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
    private static Object getKeepData(Map<String, Object> cache, String key, String fullKey) {
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

    protected static int entriesCreated = 0;
    public static int getNumEntriesCreated() {
        return entriesCreated;
    }
    protected static int entriesCloned = 0;
    public static int getNumEntriesCloned() {
        return entriesCloned;
    }


    public Scope newScope(Definition def, Definition superdef, ParameterList params, ArgumentList args) {

        Map<String, Object> entryKeep = null;
        if (def instanceof Site) {
            entryKeep = siteKeeps.get(def.getName());
            if (entryKeep == null) {
                entryKeep = newHashMap(Object.class);
                siteKeeps.put(def.getName(), entryKeep);
            }
        }
        // ENTRY REUSE HAS BEEN DISABLED FOR NOW
        // getAbandonedScope will always return null
        Scope entry = getAbandonedScope();
        if (entry != null) {
            entry.init(def, superdef, params, args, entryKeep, globalKeep);
        } else {
            entry = new Scope(def, superdef, params, args, entryKeep, globalKeep);
        }
        return entry;
    }

    public Scope newScope(Scope copyScope, boolean copyKeep) {
        // ENTRY REUSE HAS BEEN DISABLED FOR NOW
        // getAbandonedScope will always return null
        Scope entry = getAbandonedScope();
        if (entry != null) {
            entry.copy(copyScope, copyKeep);
        } else {
            entry = new Scope(copyScope, copyKeep);
        }
        return entry;
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
                LOG.debug(" !!! popped an entry with ref count of " + scope.refCount);
            }
        }

    }
    // DISABLE ENTRY REUSE FOR NOW
    // see addAbandonedEntry
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
