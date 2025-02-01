/* Canto Compiler and Runtime Engine
 * 
 * SiteBuilder.java
 *
 * Copyright (c) 2024-2025 by cantolang.org
 * All rights reserved.
 */

package canto.runtime;

import java.util.ArrayList;
import java.util.List;

import canto.lang.Block;
import canto.lang.CantoNode;
import canto.lang.CompilationUnit;
import canto.lang.Core;
import canto.lang.Name;
import canto.parser.CantoParser;
import canto.parser.CantoParserBaseVisitor;
import canto.parser.NotNull;

/**
 * 
 */
public class SiteBuilder extends CantoParserBaseVisitor<CantoNode> {

    private static final Log LOG = Log.getLogger(SiteBuilder.class);
    
    private Exception exception = null;
    protected Core core;

    public SiteBuilder(Core core) {
        this.core = core;
    }

    public Exception getException() {
        return exception;
    }

    public CompilationUnit build(CantoParser parser) {

        CompilationUnit unit = null;

        try {
            unit = (CompilationUnit) parser.compilationUnit().accept(this);
            core.addCompilationUnit(unit);
            
        
        } catch (Exception e) {
            exception = e;
            LOG.error("Error building site", e);
        }
        
        return unit;
    }

    @Override
    public CantoNode visitSiteDefinition(CantoParser.SiteDefinitionContext ctx) {
        
        Name name = (Name) ctx.identifier().accept(this);
        Block block = (Block) ctx.siteBlock().accept(this);
        CompilationUnit unit = new CompilationUnit(name, block);
        return unit;
    }
    
    @Override
    public CantoNode visitSiteBlock(CantoParser.SiteBlockContext ctx) {
        List<CantoNode> elements = new ArrayList<>();
        for (CantoParser.DirectiveContext directive : ctx.directive()) {
            elements.add(directive.accept(this));
        }
        
        for (CantoParser.TopDefinitionContext def : ctx.topDefinition()) {
            elements.add(def.accept(this));
        }
        
        Block block = new Block(elements);
        return block;
    }
    
    

}
