/* Canto Compiler and Runtime Engine
 * 
 * DefinitionFlavor.java
 *
 * Copyright (c) 2018-2026 by cantolang.org
 * All rights reserved.
 */

package canto.lang;

import java.util.*;

/**
 * A DefinitionFlavor is a definition with a specific parameter list.
 */

public class DefinitionFlavor extends ComplexDefinition {

    public Definition def;
    public ParameterList params;

    public DefinitionFlavor(Definition def, ParameterList params) {
        super(def.getNameNode());
        this.def = def;
        this.params = params;
    }

    public ParameterList getParameters() {
        return params;
    }

    /** Overridden to always return the parameter list associated with this definition flavor,
     *  regardless of the arguments.
     */
    @Override
    protected ParameterList getMatch(ConstructionList args, Context argContext) {
        return getParameters();
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof DefinitionFlavor) {
            DefinitionFlavor defFlavor = (DefinitionFlavor) obj;
            return (def.equals(defFlavor.def) && params.equals(defFlavor.params));
        } else {
            return def.equals(obj);
        }
    }

    // ComplexDefinition methods are overridden to call the delegate.  They will throw
    // ClassCastExceptions if the delegate definition is not a ComplexDefinition.

    /** Calls the delegate.  Throws a ClassCastException if the delegate definition
     *  is not a ComplexDefinition.
     */
    @Override
    protected Definition getExplicitDefinition(NameNode name, ConstructionList args, Context argContext) {
        return ((ComplexDefinition) def).getExplicitDefinition(name, args, argContext);
    }

    /** Calls the delegate.  Throws a ClassCastException if the delegate definition
     *  is not a ComplexDefinition.
     */
    @Override
    public Definition getExplicitChildDefinition(NameNode name) {
        return ((NamedDefinition) def).getExplicitChildDefinition(name);
    }

    @Override
    DefinitionTable getDefinitionTable() {
        return def.getDefinitionTable();
    }

    // Definition interface methods are all handled by delegation

    /** Returns the name. */
    @Override
    public String getName() {
        return def.getName();
    }

    /** Returns true if the definition this is a flavor of is abstract, i.e.,
     *  contains an abstract construction.
     */
    @Override
    public boolean isAbstract(Context context) {
        return def.isAbstract(context);
    }

    /** Returns true if the definition this is a flavor of is primitive.
     */
    @Override
    public boolean isPrimitive() {
        return def.isPrimitive();
    }

    /** Returns the access modifier. */
    @Override
    public Access getAccess() {
        return def.getAccess();
    }

    /** Returns the durability modifier. */
    @Override
    public Durability getDurability() {
        return def.getDurability();
    }

    /** Returns the associated type object. */
    @Override
    public Type getType() {
        return def.getType();
    }

    /** Returns true if this definition contains a <code>next</code> statement.
     */
    @Override
    public boolean hasNext(Context context) {
    	return def.hasNext(context);
    }

    /** Returns a linked list of lateral superdefinitions -- superdefinitions that contain
     *  a <code>next</code> statement, or null if there are no such superdefinitions.
     */
    @Override
    public LinkedList<Definition> getNextList(Context context) {
    	return def.getNextList(context);
    }; 


    /** Returns the supertype, or null if unspecified. */
    @Override
    public Type getSuper() {
        return def.getSuper();
    }

    @Override
    public Type getSuper(Context context) {
        return def.getSuper(context);
    }
   
    /** Returns the supertype for the given context.  If the supertype is a list of types,
     *  returns the type in the list that best matches the parameters in the context.
     */
    @Override
    public Type getSuper(Context context, ParameterList params) {
        return def.getSuper(context, params);
    }

    /** Returns the supertype of this definition which corresponds to the owner of the
     *  childDef, or is a subtype of the owner of childDef.
     * 
     *  This method enables the identification of the specific chain of supertypes within a 
     *  multiple inheritance tree of supertypes which leads to the parent  
     */
    @Override
    public Type getSuperForChild(Context context, Definition childDef) {
        return def.getSuperForChild(context, childDef);
    }
    
    /** Returns true if <code>name</code> is the name of an ancestor of this
     *  definition.
     */
    @Override
    public boolean isSuperType(String name) {
        return def.isSuperType(name);
    }

    /** Returns the superdefinition, or null if unspecified.  The context does not affect
     *  how the superdefinition is resolved; rather, it allows the supertype arguments to
     *  be resolved, and a DefinitionFlavor to be returned if the superdefinition has
     *  multiple parameter lists.
     */
    @Override
    public NamedDefinition getSuperDefinition(Context context) {
        return def.getSuperDefinition(context);
    }

    /** Returns the superdefinition, or null if unspecified. */
    @Override
    public NamedDefinition getSuperDefinition() {
        return def.getSuperDefinition();
    }

    /** Returns true if this definition contains a <code>sub</code> statement,
     *  or is empty and <code>hasSub</code> called on its superdefinition (if any)
     *  returns true.
     */
    @Override
    public boolean hasSub(Context context) {
        return def.hasSub(context);
    }

    public boolean isSubDefinition(NamedDefinition subDef) {
        return (def instanceof NamedDefinition && ((NamedDefinition) def).isSubDefinition(subDef));
    }

    /** Returns the full name, including the ownership chain, in dot notation */
    @Override
    public String getFullName() {
        return def.getFullName();
    }

    /** Returns the full name, with the ownership chain adjusted to reflect the
     *  actual subclasses, in dot notation.
     */
    @Override
    public String getFullNameInContext(Context context) {
        return def.getFullNameInContext(context);
    }
    
    /** Returns the context of this definition, or null if none. */
    @Override
    public Definition getOwner() {
        return def.getOwner();
    }

    @Override
    public Object getChild(NameNode name, ConstructionList args, IndexList indexes, ConstructionList parentArgs, Context argContext, boolean generate, boolean trySuper, Object parentObj, Definition resolver) throws Redirection {
        return def.getChild(name, args, indexes, parentArgs, argContext, generate, trySuper, parentObj, resolver);
    }

    /** Returns the child definition by the specified name.
     */
    @Override
    public Definition getChildDefinition(NameNode name, Context context) {
        return def.getChildDefinition(name, context);
    }


    /** Instantiates a child definition of the specified name in the specified context and
     *   returns the result.
     */
    @Override
    public Object getChildData(NameNode childName, Type type, Context context, ConstructionList args) throws Redirection {
        return def.getChildData(childName, type, context, args);
    }


    /** Returns a list of formal parameter lists associated with this definition, or
     *  null if none are defined.
     */
    @Override
    public List<ParameterList> getParamLists() {
        return def.getParamLists();
    }

    /** Gets a list of constructions comprising this definition */
    @Override
    public List<Construction> getConstructions(Context context) {
        return def.getConstructions(context);
    }

    /** Gets the contents of the definition as a single node, which may be a block,
     *  an array or a single value.
     */
    @Override
    public CantoNode getContents() {
        return def.getContents();
    }
}
