/* Canto Compiler and Runtime Engine
 * 
 * Index.java
 *
 * Copyright (c) 2018-2025 by cantolang.org
 * All rights reserved.
 */

package canto.lang;


/**
 * A reference to a location (offset or key) in a collection.
 */
public class Index extends CantoNode {

    private boolean dynamic = false;

    public Index() {
        super();
    }

    public Index(Value value) {
        super();
        setChild(0, (CantoNode) value);
    }

    /** Returns <code>false</code> */
    public boolean isPrimitive() {
        return false;
    }

    /** Returns <code>false</code> */
    public boolean isStatic() {
        return false;
    }

    /** Returns <code>false</code> */
    public boolean isDynamic() {
        return dynamic;
    }

    protected void setDynamic(boolean dynamic) {
        this.dynamic = dynamic;
    }

    /** Returns <code>false</code> */
    public boolean isDefinition() {
        return false;
    }

    public boolean isNumericIndex(Context context) {
        Value val = getIndexValue(context);
        Class<?> type = val.getValueClass();
        if (type == Byte.TYPE || type == Short.TYPE || type == Integer.TYPE || type == Long.TYPE || type == Character.TYPE ||
              type == Byte.class || type == Short.class || type == Integer.class || type == Long.class || type == Character.class) {

        	return true;
        } else {
        	return false;
        }
    }

    public Value getIndexValue(Context context) {
        CantoNode node = getChild(0);
        if (node instanceof Value) {
            return (Value) node;
        } else {
            try {
                return ((ValueGenerator) node).getValue(context);
            } catch (Redirection r) {
                return new PrimitiveValue();
            }
        }
    }
    
    public Index instantiateIndex(Context context) {
        Index instantiatedIndex = createIndex();
        instantiatedIndex.setChild(0, (CantoNode) getIndexValue(context));
        return instantiatedIndex;
    }
    
    public Index resolveIndex(Context context) {
        Index resolvedIndex = (Index) clone();
        CantoNode node = (CantoNode) resolvedIndex.getChild(0);
        if (node instanceof Instantiation) {
            try {
                node = Construction.resolveInstance((Instantiation) node, context, false);
            } catch (Redirection r) {
                ;
            }
        }
        resolvedIndex.setChild(0, node);
        return resolvedIndex;
    }
    
    public String getModifierString(Context context) {
        String str = "[" + getKey(context) + "]";
        
        // find loop parameters
        CantoNode node = (CantoNode) getChild(0);
        if (node instanceof Instantiation) {
            int kind = ((Instantiation) node).getKind();
            if (kind == Instantiation.FOR_PARAMETER || kind == Instantiation.FOR_PARAMETER_CHILD) {
                Context resolutionContext = ((node instanceof ResolvedInstance) ? ((ResolvedInstance)node).getResolutionContext() : context);
                str = str + "#" + resolutionContext.getLoopIndex();                  
            }
        }
        
        return str;
    }
    public int getIndex(Context context) {
        return getIndexValue(context).getInt();
    }

    public String getKey(Context context) {
        return getIndexValue(context).getString();
    }

    public String toString() {
        return "[" + getChild(0).toString() + "]";
    }

    protected Index createIndex() {
        return new Index();
    }

}
