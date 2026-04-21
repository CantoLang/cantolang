/* Canto Compiler and Runtime Engine
 * 
 * Holder.java
 *
 * Copyright (c) 2018-2026 by cantolang.org
 * All rights reserved.
 */

package canto.util;

import canto.lang.ConstructionList;
import canto.lang.Context;
import canto.lang.Definition;
import canto.lang.ResolvedInstance;

/**
 *  A simple container for associating a definition with an instantiated
 *  object.  A Holder is useful for lazy instantiation (avoiding instantiation
 *  till the last possible moment) and for providing name, type, source code
 *  and other metadata about the object that can be obtained from the 
 *  definition   
 */

public class Holder {
    public Definition nominalDef;
    public ConstructionList nominalArgs;
    public Definition def;
    public ConstructionList args;
    public Object data;
    public ResolvedInstance resolvedInstance;
    
    public Holder() {
        this(null, null, null, null, null, null, null);
    }
    

    public Holder(Definition nominalDef, ConstructionList nominalArgs, Definition def, ConstructionList args, Context context, Object data, ResolvedInstance resolvedInstance) {
        this.nominalDef = nominalDef;
        this.nominalArgs = nominalArgs;
        this.def = def;
        this.args = args;
        this.data = data;
        this.resolvedInstance = resolvedInstance;
    }

    public String toString() {
        return "{ nominalDef: "
             + (nominalDef == null ? "(null)" : nominalDef.getName())
             + "\n  nominalArgs: "
             + (nominalArgs == null ? "(null)" : nominalArgs.toString())
             + "\n  def: "
             + (def == null ? "(null)" : def.getName())
             + "\n  args: "
             + (args == null ? "(null)" : args.toString())
             + "\n  data: "
             + (data == null ? "(null)" : data.toString())
             + "\n  resolvedInstance: "
             + (resolvedInstance == null ? "(null)" : resolvedInstance.getName())
             + "\n}";
    }

}
