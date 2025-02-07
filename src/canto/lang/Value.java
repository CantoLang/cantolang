/* Canto Compiler and Runtime Engine
 * 
 * Value.java
 *
 * Copyright (c) 2024-2025 by cantolang.org
 * All rights reserved.
 */

package canto.lang;


/**
 * 
 */
public interface Value extends ValueSource {

    public static Value createValue(Object value) {
        return new Value() {
            @Override
            public Value getValue() {
                return this;
            }

            @Override
            public Value getValue(Context context) {
                return this;
            }

            @Override
            public Object getObject() {
                return value;
            }
        };
    }

    public Object getObject();

    default public String getString() {
        Object value = getObject();
        return (value == null ? null : value.toString());
    }

    default public boolean getBoolean() {
        Object value = getObject();
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

    default public byte getByte() {
        Object value = getObject();
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

    default public char getChar() {
        Object value = getObject();
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

    default public int getInt() {
        Object value = getObject();
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

    default public long getLong() {
        Object value = getObject();
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

    default public double getDouble() {
        Object value = getObject();
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
    
    default public Class<?> getValueClass() {
        Object value = getObject();
        return (value == null ? null : value.getClass());
    }

    default public Value getValue(Context context) {
        return this;
    }

    default public Object getValue() {
        return getObject();
    }

}
