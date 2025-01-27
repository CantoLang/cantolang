/* Canto Compiler and Runtime Engine
 * 
 * MultiBlockDefinition.java
 *
 * Copyright (c) 2024 by cantolang.org
 * All rights reserved.
 */

package canto.lang;

import java.util.ArrayList;
import java.util.List;

/**
 * 
 */
public class MultiUnitDefinition extends Definition {
    
    List<CompilationUnit> units;

    protected MultiUnitDefinition(Name name) {
        super(name);
        units = new ArrayList<CompilationUnit>();
    }

    protected void addUnit(CompilationUnit unit) {
        units.add(unit);
    }
 
    @Override
    public Value instantiate(Context context) {
        return null;
    }

    @Override
    public boolean isPrimitive() {
        return false;
    }

    @Override
    public boolean isStatic() {
        return false;
    }

    @Override
    public boolean isDynamic() {
        return false;
    }

}
