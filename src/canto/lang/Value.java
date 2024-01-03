/* Canto Compiler and Runtime Engine
 * 
 * Value.java
 *
 * Copyright (c) 2024 by cantolang.org
 * All rights reserved.
 */

package canto.lang;

import java.util.HashMap;

/**
 * 
 */
public class Value {

    public static final Value NULL = new Value(null);
    public static final Value TRUE = new Value(Boolean.TRUE);
    public static final Value FALSE = new Value(Boolean.FALSE);
    public static final Value ZERO = new Value(0);
    public static final Value ONE = new Value(1);
    public static final Value EMPTY_STRING = new Value("");
    public static final Value EMPTY_LIST = new Value(new Object[0]);
    public static final Value EMPTY_MAP = new Value(new HashMap<String,Object>());

    public Object value;

    public Value(Object value) {
        this.value = value;
    }

    public Value(boolean value) {
        this.value = (Boolean) value;
    }

    public Value(byte value) {
        this.value = (Byte) value;
    }

    public Value(char value) {
        this.value = (Character) value;
    }

    public Value(int value) {
        this.value = (Integer) value;
    }

    public Value(long value) {
        this.value = (Long) value;
    }

    public Value(double value) {
        this.value = (Double) value;
    }

    public String getString() {
        return (value == null ? null : value.toString());
    }

    public boolean getBoolean() {
        if (value == null) {
            return false;
        } else if (value instanceof Boolean) {
            return ((Boolean) value).booleanValue();
        } else if (value instanceof Number) {
            return ((Number) value).doubleValue() != 0.0;
        } else {
            return true;
        }
    }

    public byte getByte() {
        if (value == null) {
            return 0;
        } else if (value instanceof Number) {
            return ((Number) value).byteValue();
        } else if (value instanceof String) {
            return Byte.parseByte((String) value);
        } else {
            return 1;
        }
    }

    public char getChar() {
        if (value == null) {
            return 0;
        } else if (value instanceof Character) {
            return ((Character) value).charValue();
        } else if (value instanceof Number) {
            return (char) ((Number) value).intValue();
        } else if (value instanceof String) {
            return ((String) value).charAt(0);
        } else {
            return 1;
        }
    }

    public int getInt() {
        if (value == null) {
            return 0;
        } else if (value instanceof Number) {
            return ((Number) value).intValue();
        } else if (value instanceof String) {
            return Integer.parseInt((String) value);
        } else {
            return 1;
        }
    }

    public long getLong() {
        if (value == null) {
            return 0;
        } else if (value instanceof Number) {
            return ((Number) value).longValue();
        } else if (value instanceof String) {
            return Long.parseLong((String) value);
        } else {
            return 1;
        }
    }

    public double getDouble() {
        if (value == null) {
            return 0;
        } else if (value instanceof Number) {
            return ((Number) value).doubleValue();
        } else if (value instanceof String) {
            return Double.parseDouble((String) value);
        } else {
            return 1;
        }
    }
}
