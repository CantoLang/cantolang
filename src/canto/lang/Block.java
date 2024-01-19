/* Canto Compiler and Runtime Engine
 * 
 * Block.java
 *
 * Copyright (c) 2024 by cantolang.org
 * All rights reserved.
 */

package canto.lang;

import java.util.Iterator;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 
 */
public abstract class Block extends Construction {

    private List<Construction> constructions;
    
    protected Block(List<Construction> constructions) {
        super();
        CantoNode[] nodes = new CantoNode[constructions.size()];
        this.children = constructions.toArray(nodes);
        this.constructions = constructions;
    }

    @Override
    public Value construct(Context context) {
        Value val = null;
        StringBuffer sb = null;
        Iterator<Construction> it = constructions.iterator();
        while (it.hasNext()) {
            Construction construction = it.next();
            Value v = construction.construct(context);
            if (v != null) {
                if (val == null) {
                    val = v;
                } else {
                    if (sb == null) {
                        sb = new StringBuffer(val.toString());
                    }
                    sb.append(v.toString());
                }
            }
        }
        if (sb != null) {
            val = new Value(sb.toString());
        }
        return val;
    }

    @Override
    public boolean isPrimitive() {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public boolean isStatic() {
        Iterator<Construction> it = constructions.iterator();
        while (it.hasNext()) {
            Construction construction = it.next();
            if (!construction.isStatic()) {
                return false;
            }
        }
        return true;
    }

    @Override
    public boolean isDynamic() {
        Iterator<Construction> it = constructions.iterator();
        while (it.hasNext()) {
            Construction construction = it.next();
            if (construction.isDynamic()) {
                return true;
            }
        }
        return false;
    }

}
