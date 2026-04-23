/* Canto Compiler and Runtime Engine
 * 
 * Block.java
 *
 * Copyright (c) 2025 by cantolang.org
 * All rights reserved.
 */

package canto.lang;

import java.util.List;
import java.util.stream.Collectors;

import canto.util.EmptyList;

/**
 * 
 */
public class SiteBlock extends CodeBlock {

    private List<AdoptStatement> adopts;
    private List<ExternStatement> externs;
    
    protected SiteBlock() {
        super();
        this.adopts = new EmptyList<AdoptStatement>();
        this.externs = new EmptyList<ExternStatement>();
    }
    
    public SiteBlock(List<CantoNode> children) {
        super(children);

         this.adopts = ExtractAdopts(children);
         this.externs = ExtractExterns(children);
    }

    private static List<AdoptStatement> ExtractAdopts(List<CantoNode> children) {
        List<AdoptStatement> adopts = children.stream().filter(c -> c instanceof AdoptStatement).map(c -> (AdoptStatement) c).collect(Collectors.toList());        
        return adopts;
    }

    private static List<ExternStatement> ExtractExterns(List<CantoNode> children) {
        List<ExternStatement> externs = children.stream().filter(c -> c instanceof ExternStatement)
                .map(c -> (ExternStatement) c).collect(Collectors.toList());
        return externs;
    }   
    
    public List<AdoptStatement> getAdopts() {
        return adopts;
    }

    public List<ExternStatement> getExterns() {
        return externs;
    }


}
