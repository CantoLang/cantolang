/* Canto Compiler and Runtime Engine
 * 
 * ValueSource.java
 *
 * Copyright (c) 2018-2025 by cantolang.org
 * All rights reserved.
 */

package canto.lang;


/**
 * Common base interface for the Value and ValueGenerator classes.  This
 * interface specifies one method, getValue(Context context).  Value
 * implements this by returning itself.
 * 
 * The ValueSource interface also specifies type order.  Most operators with 
 * multiple inputs of different types will return a value with of the same type as 
 * the input with the highest type order.  So, for example, an operation between an
 * <code>int</code> and a <code>byte</code> yields an <code>int</code>, between an 
 * <code>int</code> and a <code>char</code> yields a <code>char</code>, and between
 * anything and a <code>string</code> yields a <code>string</code>.
 */

public interface ValueSource {

    // type order values

    public final static int VOID = 0;
    public final static int BOOLEAN = 1;
    public final static int BYTE = 2;
    public final static int INT = 3;
    public final static int LONG = 4;
    public final static int DOUBLE = 5;
    public final static int CHAR = 6;
    public final static int STRING = 7;

    public Value getValue(Context context);

}
