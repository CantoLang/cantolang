/* Canto Compiler and Runtime Engine
 * 
 * canto_definition.java
 *
 * Copyright (c) 2026 by cantolang.org
 * All rights reserved.
 */

package canto.lang;

import java.util.List;
import java.util.Map;



/**
 * This interface corresponds to definition, defined in core and representing the external functions
 * that can be called on canto definitions.
 */
public interface canto_definition {

    public Map defs();
    public canto_definition[] children_of_type(String typeName);
    public canto_definition[] descendants_of_type(String typeName);
    public String full_name();
    public canto_definition ancestor_of_type(String typeName);
    public boolean is_array();
    public boolean is_table();
    public boolean is_a(String typeName);

    public Object instantiate(Context context); 
    public List<Object> instantiate_array(Context context);
    public Map instantiate_table(Context context);
}


