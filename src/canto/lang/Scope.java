/* Canto Compiler and Runtime Engine
 * 
 * Scope.java
 *
 * Copyright (c) 2024 by cantolang.org
 * All rights reserved.
 */

package canto.lang;

import java.util.*;

/**
 * 
 */
public class Scope {
    public Definition def;
    public List<Identifier> params;
    public List<Expression> args;
    
    private Map<String, Object> cache = null;
    
    public Scope(Definition def, List<Identifier> params, List<Expression> args) {
        this.def = def;
        this.params = params;
        this.args = args;
    }
    
    public Object get(String key) {
        if (cache == null) {
            return null;
        } else {
            return cache.get(key);
        }
    }
    
    public void put(String key, Object value) {
        if (cache == null) {
            cache = new HashMap<String, Object>();
        }
        cache.put(key, value);
    }
    
}
