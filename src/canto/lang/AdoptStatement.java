/* Canto Compiler and Runtime Engine
 * 
 * AdoptStatement.java
 *
 * Copyright (c) 2018-2026 by cantolang.org
 * All rights reserved.
 */

package canto.lang;

/**
 * An adopt statement.
 *
 * @author Michael St. Hippolyte
 * @version $Revision: 1.3 $
 */

public class AdoptStatement extends Directive {

    public AdoptStatement(ComplexName nameRange) {
        super(nameRange);
    }

    public String toString(String prefix) {
        return prefix + "adopt " + getName();
    }
}
