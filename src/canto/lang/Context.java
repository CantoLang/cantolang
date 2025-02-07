/* Canto Compiler and Runtime Engine
 * 
 * Context.java
 *
 * Copyright (c) 2018-2025 by cantolang.org
 * All rights reserved.
 */

package canto.lang;

import java.util.*;

import canto.runtime.CantoLogger;
import canto.runtime.Context;
import canto.runtime.ContextMarker;

/**
 * 
 */
public class Context {
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
    private int size = 0;

    private ArrayDeque<Scope> scopeStack = new ArrayDeque<Scope>();
    private ArrayDeque<Scope> subscopeStack = new ArrayDeque<Scope>();
    
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
        subscopeStack = new ArrayDeque<Scope>(context.subscopeStack);
    }
}

class ContextMarker {
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
                CantoLogger.vlog("comparing context marker loop indices: " + loopIndex + " to " + marker.loopIndex);
            }
            return (marker.rootContext == rootContext && marker.stateCount == stateCount && marker.loopIndex == loopIndex);
        } else {
            return object.equals(this);
        }
    }
    public int hashCode() {
        int n = (rootContext.getRootEntry().hashCode() << 16) + stateCount;
        return n;
    }
}
