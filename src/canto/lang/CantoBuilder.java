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
import org.antlr.v4.runtime.tree.ParseTree;

import canto.parser.CantoLexer;
import canto.parser.CantoParser;
import canto.parser.CantoParser.AnyanyContext;
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
            NameNode binding = (NameNode) ctx.identifier().accept(this);
            NameRange nameRange = (NameRange) ctx.nameRange().accept(this);
            ExternStatement externDirective = new ExternStatement(binding, nameRange);
            return externDirective;
        }
    
        @Override
        public CantoNode visitAdoptDirective(CantoParser.AdoptDirectiveContext ctx) {
            NameRange nameRange = (NameRange) ctx.nameRange().accept(this);
            AdoptStatement adoptDirective = new AdoptStatement(nameRange);
            return adoptDirective;
        }
    
        @Override
        public CantoNode visitTopDefinition(CantoParser.TopDefinitionContext ctx) {
            Definition def = null;
            String docComment = null;
            KeepNode keepNode = null;
            int numNodes = ctx.getChildCount();
            for (int i = 0; i < numNodes; i++) {
                ParseTree child = ctx.getChild(i);
                if (child == ctx.doc) {
                    docComment = ctx.doc.getText();
                } else if (child == ctx.keep) {
                    keepNode = (KeepNode) ctx.keep.accept(this);
                } else {
                    def = (Definition) child.accept(this);
                }
            }
            return def;
        }
    
        @Override
        public CantoNode visitCollectionElementDefinition(CantoParser.CollectionElementDefinitionContext ctx) {
            return ctx.accept(this);
        }

        @Override
        public CantoNode visitCollectionDefinition(CantoParser.CollectionDefinitionContext ctx) {
            return ctx.accept(this);
        }
        
        @Override
        public CantoNode visitElementDefinition(CantoParser.ElementDefinitionContext ctx) {
            return ctx.accept(this);
        }

        @Override
        public CantoNode visitBlockDefinition(CantoParser.BlockDefinitionContext ctx) {
            return ctx.accept(this);
        }

        @Override
        public CantoNode visitCollectionDefName(CantoParser.CollectionDefNameContext ctx) {
            //   : collectionType identifier paramSuffix?
            //   | simpleType? identifier paramSuffix? collectionSuffix
            return ctx.accept(this);
        }
        
        @Override
        public CantoNode visitElementDefName(CantoParser.ElementDefNameContext ctx) {
            //: simpleType? identifier paramSuffix? 
            return ctx.accept(this);
        }

        @Override
        public CantoNode visitBlockDefName(CantoParser.BlockDefNameContext ctx) {
            //: multiType identifier (paramSuffix | multiParamSuffix)?
            //| simpleType? identifier (paramSuffix | multiParamSuffix)?
            return ctx.accept(this);
        }

        @Override
        public CantoNode visitIdentifier(CantoParser.IdentifierContext ctx) {
            String name = ctx.getText();
            return new NameNode(name);
        }

        @Override
        public CantoNode visitAny(CantoParser.AnyContext ctx) {
            return new Any();
        }

        @Override
        public CantoNode visitAnyany(CantoParser.AnyanyContext ctx) {
            return new AnyAny();
        }

        @Override
        public CantoNode visitNameRange(CantoParser.NameRangeContext ctx) {
            int numNodes = ctx.getChildCount();
            List<CantoNode> nodeList = new ArrayList<CantoNode>(numNodes);
            for (int i = 0; i < numNodes; i++) {
                nodeList.add(ctx.getChild(i).accept(this));
            }
            return new ComplexName(nodeList);
        }

        @Override
        public CantoNode visitQualifiedName(CantoParser.QualifiedNameContext ctx) {
            int numNodes = ctx.getChildCount();
            List<CantoNode> nodeList = new ArrayList<CantoNode>(numNodes);
            for (CantoParser.IdentifierContext identifier : ctx.identifier()) {
                nodeList.add(identifier.accept(this));
            }
            return new ComplexName(nodeList);
        }
    }  
}
