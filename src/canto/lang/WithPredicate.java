/* Canto Compiler and Runtime Engine
 * 
 * WithPredicate.java
 *
 * Copyright (c) 2018-2025 by cantolang.org
 * All rights reserved.
 */

package canto.lang;

/**
 * The predicate of a <code>with</code> expression in a conditional
 * statement.  Tests for the presence of a parameter or existence and concreteness
 * of a definition
 */

public class WithPredicate extends Instantiation implements ValueGenerator {

    boolean with = true;

    public WithPredicate(boolean with) {
        super();
        this.with = with;
    }

    protected void setWithout() {
        with = false;
    }
    
    public boolean isPrimitive() {
        return false;
    }

    public String getString(Context context) {
        return getValue(context).getString();
    }

    public boolean getBoolean(Context context) {
        return getValue(context).getBoolean();
    }

    public byte getByte(Context context) {
        return getValue(context).getByte();
    }

    public char getChar(Context context) {
        return getValue(context).getChar();
    }

    public int getInt(Context context) {
        return getValue(context).getInt();
    }

    public long getLong(Context context) {
        return getValue(context).getLong();
    }

    public double getDouble(Context context) {
        return getValue(context).getDouble();
    }

    public Object getData(Context context) {
        return getValue(context).getData();
    }

    public Value getValue(Context context) {
        boolean isPresent = context.paramIsPresent(getReferenceName());
        if (!isPresent) {
            Definition def = getDefinition(context);
            isPresent = (def != null && !def.isAbstract(context));
        }
        return new PrimitiveValue(with ? isPresent : !isPresent);
    }

    /** for source dumping */
    public String toString(String prefix) {
        String str = prefix + "(" + super.toString("") + ")";
        return str;
    }
}
