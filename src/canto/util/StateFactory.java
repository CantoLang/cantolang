/* Canto Compiler and Runtime Engine
 * 
 * StateFactory.java
 *
 * Copyright (c) 2024-2025 by cantolang.org
 * All rights reserved.
 */

package canto.util;

/** An object capable of generating unique state id values. Used by the Context
 *  class to generate ids which are globally unique within a context tree
 *  (a root context and all contexts copied and cloned from it).
 *
 *  State id's are nonnegative integers (along with the special value -1 for
 *  an empty context), meaning there are about 2 billion unique states.  The
 *  id generator will roll over safely when it hits the limit and continue
 *  to generate nonnegative id's, but those id's are no longer absolutely
 *  guaranteed to be unique.
 *
 *  To minimize the possibility of nonunique states, the rollover logic skips
 *  low id values, so that id values up to 1000 <i>are</i> guaranteed to remain
 *  unique.
 */
public class StateFactory {
    /** The value to which the state wraps around to.  Global and persistent state
     *  values will most likely have low values, so the wrap around value should
     *  be well above zero.
     */
    private final static int WRAP_AROUND_STATE = 10001;
    private int state = -1;
    public StateFactory() {}

    public StateFactory(StateFactory factory) {
        state = factory.state;
    }

    public int nextState() {
        ++state;
        if (state < 0) {
            // wrap arround
            state = WRAP_AROUND_STATE;
        }
        return state;
    }
    public int lastState() { return state; }
}
