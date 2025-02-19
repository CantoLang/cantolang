/* Canto Compiler and Runtime Engine
 * 
 * Pointer.java
 *
 * Copyright (c) 2024-2025 by cantolang.org
 * All rights reserved.
 */

package canto.lang;

import java.util.Map;

public class Pointer {
    ResolvedInstance ri;
    ResolvedInstance riAs;
    Object key;
    Object containerKey;
    Map<String, Object> cache;

    public Pointer(ResolvedInstance ri, Object key, Object containerKey, Map<String, Object> cache) {
        this(ri, ri, key, containerKey, cache);
    }

    public Pointer(ResolvedInstance ri, ResolvedInstance riAs, Object key, Object containerKey, Map<String, Object> cache) {
        this.ri = ri;
        this.riAs = (riAs == null ? ri : riAs);
        this.key = key;
        this.containerKey = containerKey;
        this.cache = cache;
    }

    public String getKey() {
        if (key instanceof Value) {
            return ((Value) key).getString();
        } else {
            return key.toString();
        }
    }

    public String getContainerKey() {
        if (containerKey instanceof Value) {
            return ((Value) containerKey).getString();
        } else {
            return containerKey.toString();
        }
    }

    public String toString() {
        return toString("");    
    }

    public String toString(String prefix) {
        StringBuffer sb = new StringBuffer(prefix);
        prefix += "    ";
        sb.append("===> ");
        sb.append(key == null ? "(null)" : key);
        sb.append(": ");
        if (cache != null) {
            Object obj = cache.get(key);
            if (obj != null) {
                if (obj instanceof CantoNode) {
                    sb.append("\n");
                    sb.append(((CantoNode) obj).toString(prefix));
                } else if (obj instanceof Pointer) {
                    sb.append("\n");
                    sb.append(((Pointer) obj).toString(prefix));
                } else {
                    sb.append(obj.toString());
                }
            } else {
                sb.append("(null)");
            }
        }
        return sb.toString();
    }
}
