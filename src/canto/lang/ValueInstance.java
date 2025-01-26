/* Canto Compiler and Runtime Engine
 * 
 * Value.java
 *
 * Copyright (c) 2024 by cantolang.org
 * All rights reserved.
 */

package canto.lang;

import java.util.HashMap;

import canto.runtime.Context;

/**
 * 
 */
public class ValueInstance implements Value, ValueSource {

    public static final ValueInstance NULL = new ValueInstance(null);
    public static final ValueInstance TRUE = new ValueInstance(Boolean.TRUE);
    public static final ValueInstance FALSE = new ValueInstance(Boolean.FALSE);
    public static final ValueInstance ZERO = new ValueInstance(0);
    public static final ValueInstance ONE = new ValueInstance(1);
    public static final ValueInstance EMPTY_STRING = new ValueInstance("");
    public static final ValueInstance EMPTY_LIST = new ValueInstance(new Object[0]);
    public static final ValueInstance EMPTY_MAP = new ValueInstance(new HashMap<String,Object>());

    public Object value;

    public ValueInstance(Object value) {
        this.value = value;
    }

    public ValueInstance(boolean value) {
        this.value = (Boolean) value;
    }

    public ValueInstance(byte value) {
        this.value = (Byte) value;
    }

    public ValueInstance(char value) {
        this.value = (Character) value;
    }

    public ValueInstance(int value) {
        this.value = (Integer) value;
    }

    public ValueInstance(long value) {
        this.value = (Long) value;
    }

    public ValueInstance(double value) {
        this.value = (Double) value;
    }

    public String getString() {
        return (value == null ? null : value.toString());
    }

    @Override
    public ValueInstance getValue(Context context) throws Redirection {
        return this;
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
    
    public Object getObject() {
        return value;
    }

    public Class<?> getValueClass() {
        return (value == null ? null : value.getClass());
    }

}
