/* Canto Compiler and Runtime Engine
 * 
 * Any.java
 *
 * Copyright (c) 2018-2025 by cantolang.org
 * All rights reserved.
 */

package canto.lang;


/**
 * The regular expression which matches anything not containing a
 * dot ("*").
 */
public class Any extends RegExp {
    public Any() {
        super("*");
    }

    public boolean matches(String str) {
        if (str != null && str.indexOf('.') < 0) {
            return true;
        } else {
            return false;
        }
    }

    public String toString() {
        return "*";
    }

}
