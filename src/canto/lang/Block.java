/* Canto Compiler and Runtime Engine
 * 
 * Block.java
 *
 * Copyright (c) 2024-2025 by cantolang.org
 * All rights reserved.
 */

package canto.lang;

import java.util.Iterator;
import java.util.List;
import java.util.stream.Collectors;

import canto.runtime.Log;
import canto.util.EmptyList;

/**
 * 
 */
public class Block extends Construction {
    private static final Log LOG = Log.getLogger(Block.class);

    private List<Construction> constructions;
    private Block catchBlock = null;
    private String catchIdentifier = null;
    
    protected Block() {
        super(new EmptyList<CantoNode>());
        this.constructions = new EmptyList<Construction>();
    }
    
    protected Block(List<CantoNode> children) {
        super(children);
        this.constructions = ExtractConstructions(children);
    }

    private static List<Construction> ExtractConstructions(List<CantoNode> children) {
        List<Construction> constructions = children.stream().filter(c -> c instanceof Construction).map(c -> (Construction) c).collect(Collectors.toList());        
        return constructions;
    }

    public Object generateData(Context context, Definition def) throws Redirection {
        return context.construct(constructions);
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

    public Block getCatchBlock() {
        return catchBlock;
    }

    protected void setCatchBlock(Block block) {
        catchBlock = block;
    }

    public String getCatchIdentifier() {
        return catchIdentifier;
    }

    public void setCatchIdentifier(Name catchName) {
        catchIdentifier = catchName.getName();
    }


}
