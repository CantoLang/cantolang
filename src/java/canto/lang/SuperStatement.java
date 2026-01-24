/* Canto Compiler and Runtime Engine
 * 
 * SuperStatement.java
 *
 * Copyright (c) 2018-2025 by cantolang.org
 * All rights reserved.
 */

package canto.lang;


/**
 * A super statement.
 */

public class SuperStatement extends Construction {

    public SuperStatement() {
        super();
    }

    public boolean isDynamic() {
        return true;
    }

    public String toString(String prefix) {
        return prefix + "super;\n";
    }

    public Object generateData(Context context, Definition def) {
        return "";
    }
}
