/* Canto Compiler and Runtime Engine
 * 
 * CantoBuilder.java
 *
 * Copyright (c) 2024-2025 by cantolang.org
 * All rights reserved.
 */

package canto.lang;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import org.antlr.v4.runtime.CharStream;
import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;

import canto.parser.CantoLexer;
import canto.parser.CantoParser;
import canto.parser.CantoParserBaseVisitor;
import canto.runtime.Log;

/**
 * 
 */
public class CantoBuilder {

    private static final Log LOG = Log.getLogger(CantoBuilder.class);
    
    private CantoParser parser;
    private CantoVisitor visitor;
    private Exception exception = null;

    public CantoBuilder(Object source) throws IOException {
        this.parser = getCantoParser(source);
        this.visitor = new CantoVisitor();
    }
    
    private CantoParser getCantoParser(Object source) throws IOException {
        CharStream cs = null;
        if (source instanceof Reader) {
            cs = CharStreams.fromReader((Reader) source);
        } else if (source instanceof String) {
            cs = CharStreams.fromString((String) source);
        } else if (source instanceof InputStream) {
            cs = CharStreams.fromStream((InputStream) source);
        } else if (source instanceof File) {
            cs = CharStreams.fromStream(new FileInputStream((File) source));
        } else if (source instanceof URL) {
            cs = CharStreams.fromStream(((URL) source).openStream());
        } else {
            throw new IOException("Invalid source type: " + source.getClass());
        }
        CantoLexer lexer = new CantoLexer(cs);
        CommonTokenStream tokens = new CommonTokenStream(lexer);
        return new CantoParser(tokens);
    }

    public Exception getException() {
        return exception;
    }

    public CompilationUnit buildSite() {
        CompilationUnit unit = null;
        try {
            unit = (CompilationUnit) parser.compilationUnit().accept(visitor);
        } catch (Exception e) {
            exception = e;
            LOG.error("Error building site", e);
        }
        return unit;
    }

    public ComplexName buildComplexName() {
        ComplexName name = null;
        try {
            name = (ComplexName) parser.compilationUnit().accept(visitor);
        } catch (Exception e) {
            exception = e;
            LOG.error("Error building site", e);
        }
        return name;
    }

    private class CantoVisitor extends CantoParserBaseVisitor<CantoNode> { 

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
            
            Block block = new SiteBlock(elements);
            return block;
        }
        
        @Override
        public CantoNode visitExternDirective(CantoParser.ExternDirectiveContext ctx) {
            return ctx.accept(this);
        }
    
        @Override
        public CantoNode visitAdoptDirective(CantoParser.AdoptDirectiveContext ctx) {
            return ctx.accept(this);
        }
    
        @Override
        public CantoNode visitTopDefinition(CantoParser.TopDefinitionContext ctx) {
            return ctx.accept(this);
        }
    
        @Override
        public CantoNode visitIdentifier(CantoParser.IdentifierContext ctx) {
            String name = ctx.getText();
            return new NameNode(name);
        }
    }  
}
