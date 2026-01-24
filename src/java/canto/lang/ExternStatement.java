/* Canto Compiler and Runtime Engine
 * 
 * ExternStatement.java
 *
 * Copyright (c) 2018-2025 by cantolang.org
 * All rights reserved.
 */

package canto.lang;

/**
 * ExternStatement represents an extern statement, which declares a name to refer
 * to an external object of a particular binding (the language the object is
 * implemented in).
 */

public class ExternStatement extends ComplexName {

    private NameNode binding;

    public ExternStatement(NameNode binding, NameRange nameRange) {
        super(nameRange);
        this.binding = binding;
    }

    public NameNode getBinding() {
        return binding;
    }

    public String toString(String prefix) {
        return prefix + "extern " + binding + " " + getName();
    }
}

