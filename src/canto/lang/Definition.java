/* Canto Compiler and Runtime Engine
 * 
 * Definition.java
 *
 * Copyright (c) 2018-2024 by cantolang.org
 * All rights reserved.
 */

package canto.lang;

/**
 * 
 */
abstract public class Definition extends Node {

    // The modifier values are such that for groups of definitions, the lowest value
    // governs.  For example, if a group of five definitions includes one dynamic one,
    // the group as a whole is considered dynamic.  Definition groups arise when a
    // definition has multiple supertypes; the superdefinition of such a definition is
    // a definition group.


    // access modifiers

    /** Corresponds to the <code>local</code> keyword. */
    public final static int LOCAL_ACCESS = 0;

    /** Corresponds to no access modifer keyword. */
    public final static int SITE_ACCESS = 1;

    /** Corresponds to the <code>public</code> keyword. */
    public final static int PUBLIC_ACCESS = 2;


    // durability modifiers

    /** Instances of this definition should be reconstructed every time they are referenced.
     *  Corresponds to the <code>dynamic</code> keyword.
     */
    public final static int DYNAMIC = 0;

    /** Instances of this definition should be retrieved from the cache when possible, else
     *  reconstructed.  Corresponds to no durability modifer keyword.
     */
    public final static int IN_CONTEXT = 1;

    /** Instances of this definition should only be constructed if they have not been
     *  constructed before or construction is forced via a dynamic instantiation.
     *  Corresponds to the <code>global</code> keyword.
     */
    public final static int GLOBAL = 2;

    /** Instances of this definition should only be constructed once.  Corresponds to 
     *  the <code>static</code> keyword.
     */
    public final static int STATIC = 3;
    

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

    private Construction construction;


    protected Definition(Name name, Construction construction) {
        super(name.toString() + " " + construction.getSource());
        this.construction = construction;
    }

    protected Definition(Name name, Construction construction, String docComment) {
        super(name.toString() + " " + construction.getSource(), docComment);
        this.construction = construction;
    }


    public boolean isElementDefinition() {
        return false;
    }

    public boolean isBlockDefinition() {
        return false;
    }
    
    @Override
    public boolean isDefinition() {
        return true;
    }
}

class ExpressionDefinition extends Definition {

    protected ExpressionDefinition(Name name, Expression expression) {
        super(name, expression);
    }

    @Override
    public int getNumChildren() {
        // TODO Auto-generated method stub
        return 0;
    }

    @Override
    public boolean isPrimitive() {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public boolean isStatic() {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public boolean isDynamic() {
        // TODO Auto-generated method stub
        return false;
    }
    
}

