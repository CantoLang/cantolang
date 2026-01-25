/* Canto Compiler and Runtime Engine
 * 
 * SubStatement.java
 *
 * Copyright (c) 2018-2025 by cantolang.org
 * All rights reserved.
 */

package canto.lang;


/**
 * A sub statement.
 */

public class SubStatement extends Construction {

    public SubStatement() {
        super();
    }

    public boolean isDynamic() {
        return true;
    }

    /** Returns true. **/
    public boolean hasSub() {
        return true;
    }

    public String toString(String prefix) {
        return prefix + "sub;\n";
    }

    public Object generateData(Context context, Definition def) {
        return "";
    }
}
