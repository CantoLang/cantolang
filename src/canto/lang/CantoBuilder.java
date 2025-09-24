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
import org.antlr.v4.runtime.ParserRuleContext;
import org.antlr.v4.runtime.Token;
import org.antlr.v4.runtime.tree.ParseTree;

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

    public Site buildSite() {
        CompilationUnit unit = null;
        try {
            unit = (CompilationUnit) parser.compilationUnit().accept(visitor);
        } catch (Exception e) {
            exception = e;
            LOG.error("Error building site", e);
        }
        return unit.getSite();
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
        public CantoNode visitCompilationUnit(CantoParser.CompilationUnitContext ctx) {
            Site site = (Site) ctx.getChild(0).accept(this);
            CompilationUnit unit = new CompilationUnit(site);
            return unit;
        }
        
        @Override
        public CantoNode visitSiteDefinition(CantoParser.SiteDefinitionContext ctx) {
            NameNode name = (NameNode) ctx.identifier().accept(this);
            Block block = (Block) ctx.siteBlock().accept(this);
            Site site = new Site(name);
            site.setType(site.createType());
            site.setContents(block);
            return site;
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
            Definition def = handleDefinition(ctx.doc, ctx.keep, ctx.access, ctx.dur, ctx);
            return def;
        }
        
        @Override
        public CantoNode visitDefinition(CantoParser.DefinitionContext ctx) {
            Definition def = handleDefinition(ctx.doc, ctx.keep, ctx.access, ctx.dur, ctx);
            return def;
        }
        
        private Definition.Access getAccess(Token access) {
            if (access == null) {
                return Definition.Access.SITE;
            }
            String s = access.getText();
            if (s.equals("public")) {
                return Definition.Access.PUBLIC;
            } else if (s.equals("local")) {
                return Definition.Access.LOCAL;
            } else {
                return Definition.Access.SITE;
            }
        }

        private Definition.Durability getDurability(ParseTree dur) {
            if (dur == null) {
                return Definition.Durability.IN_CONTEXT;
            }
            String s = dur.getText();
            if (s.equals("dynamic")) {
                return Definition.Durability.DYNAMIC;
            } else if (s.equals("global")) {
                return Definition.Durability.GLOBAL;
            } else if (s.equals("cosmic")) {
                return Definition.Durability.COSMIC;
            } else if (s.equals("static")) {
                return Definition.Durability.STATIC;
            } else {
                return Definition.Durability.IN_CONTEXT;
            }
        }
        
        private Definition handleDefinition(Token doc, ParseTree keep, Token access, ParseTree dur, ParserRuleContext ctx) {
            Definition def = null;
            KeepNode keepNode = null;
            int numNodes = ctx.getChildCount();
            for (int i = 0; i < numNodes; i++) {
                ParseTree child = ctx.getChild(i);
                if (child == keep) {
                    keepNode = (KeepNode) keep.accept(this);
                } else {
                    def = (Definition) child.accept(this);
                }
            }
            if (def != null) {
                if (doc != null) {
                    def.setDocComment(doc.getText());
                }
                if (keepNode != null) {
                    ((NamedDefinition) def).addKeep(keepNode);
                }
                def.setAccess(getAccess(access));
                def.setDurability(getDurability(dur));
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
            CantoParser.BlockDefNameContext nameCtx = ctx.blockDefName();
            NameNode name = (NameNode) nameCtx.identifier().accept(this);
            ParseTree typeCtx = nameCtx.simpleType();
            if (typeCtx == null) {
                typeCtx = nameCtx.multiType();
            }
            Type superType = (typeCtx == null ? null : (Type) typeCtx.accept(this));
            
            ParseTree blockCtx = ctx.block(0);
            if (blockCtx == null) {
                blockCtx = ctx.emptyBlock();
                if (blockCtx == null) {
                    blockCtx = ctx.abstractBlock();
                    if (blockCtx == null) {
                        blockCtx = ctx.externalBlock();
                    }
                }
            }
            Block block = (Block) blockCtx.accept(this);
            ParseTree catchCtx = ctx.block(1);
            if (catchCtx != null) {
                Block catchBlock = (Block) catchCtx.accept(this);
                block.setCatchBlock(catchBlock);
            }
            ComplexDefinition blockDef = new ComplexDefinition(superType, name, block);
            return blockDef;
        }

        @Override
        public CantoNode visitCollectionDefName(CantoParser.CollectionDefNameContext ctx) {
            //   : collectionType identifier paramSuffix?
            //   | simpleType? identifier paramSuffix? collectionSuffix
            return ctx.accept(this);
        }
        
        @Override
        public CantoNode visitBlockDefName(CantoParser.BlockDefNameContext ctx) {
            //: multiType identifier (paramSuffix | multiParamSuffix)?
            //| simpleType? identifier (paramSuffix | multiParamSuffix)?
            return ctx.accept(this);
        }

        @Override
        public CantoNode visitSimpleType(CantoParser.SimpleTypeContext ctx) {
            if (ctx.identifier() != null) {
                return ctx.identifier().accept(this);
            } else if (ctx.qualifiedName() != null) {
                return ctx.qualifiedName().accept(this);
            } else if (ctx.BOOLEAN() != null) {
                return (NameNode) PrimitiveType.BOOLEAN;
            } else if (ctx.INT() != null) {
                return (NameNode) PrimitiveType.INT;
            } else if (ctx.STRING() != null) {
                return (NameNode) PrimitiveType.STRING;
            } else if (ctx.FLOAT() != null) {
                return (NameNode) PrimitiveType.FLOAT;
            } else if (ctx.CHAR() != null) {
                return (NameNode) PrimitiveType.CHAR;
            } else if (ctx.BYTE() != null) {
                return (NameNode) PrimitiveType.BYTE;
            } else {
                return null;
            }
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
