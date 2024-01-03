/* Canto Compiler and Runtime Engine
 * 
 * Context.java
 *
 * Copyright (c) 2018-2024 by cantolang.org
 * All rights reserved.
 */

package canto.runtime;

import java.lang.reflect.Array;
import java.util.*;

import canto.lang.*;

/**
 * A Context contains a stack of definitions representing the state of a construction
 * operation.  When a page is instantiated, its definition is pushed onto the stack.  As
 * each construction called for by the page definition is executed, the definition of the
 * object being constructed is pushed onto the stack; if that definition istself contains
 * constructions the process continues recursively.  When the construction of an object is
 * complete, its definition is popped from the stack.  Thus, at any point in the process,
 * the stack contains the nested definitions of the objects-within-objects being
 * constructed at that point.
 *
 * Definition parameters and instantiation arguments, if present, are pushed on the
 * stack along with the definition.
 *
 * A context is not just a stack, however; it is also a tree.  At any point, a context
 * may be cloned, and the clone may subsequently follow a separate history.  When two
 * different definitions are pushed on to a context and its clone, the context branches.
 * Indeed, the tree is created on the first push, at which point the two copies of the
 * context are no longer identical.
 *
 * @author Michael St. Hippolyte
 */

public class Context {

    /** Value to pass to setErrorThreshhold to redirect everything. */
    public final static int EVERYTHING = 0;

    /** Value to pass to setErrorThreshhold to redirect all warnings and errors. */
    public final static int WARNINGS = 1;

    /**
     *  Value to pass to setErrorThreshhold to ignore warnings and redirect all
     *  errors, including ignorable ones such as undefined instances.
     */
    public final static int IGNORABLE_ERRORS = 2;

    /**
     *  Value to pass to setErrorThreshhold to ignore warnings and ignorable errors
     *  and redirect all functional and fatal errors.
     */
    public final static int FUNCTIONAL_ERRORS = 3;

    /** Value to pass to setErrorThreshhold to ignore everything except for fatal errors. */
    public final static int FATAL_ERRORS = 4;

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

    private int errorThreshhold = EVERYTHING;


    /** Constructs a context beginning with the specified definition */
    public Context(Site site) throws Redirection {
        instanceCount++;
        rootContext = this;

        if (site != null) {
            try {
                push(site, null, null);
        
            } catch (Redirection r) {
                vlog("Error creating context: " + r.getMessage());
                throw r;
            }
        }

        popLimit = topEntry;
    }

    /** Constructs a context which is a copy of the passed context.
     */
    public Context(Context context) {
        instanceCount++;
        rootContext = context.rootContext;
    }
}
