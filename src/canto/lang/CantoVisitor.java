/* Canto Compiler and Runtime Engine
 * 
 * CantoBuilder.java
 *
 * Copyright (c) 2024-2025 by cantolang.org
 * All rights reserved.
 */

package canto.lang;

import java.util.ArrayList;
import java.util.List;

import org.antlr.v4.runtime.ParserRuleContext;
import org.antlr.v4.runtime.Token;
import org.antlr.v4.runtime.tree.ParseTree;

import canto.parser.CantoParser;
import canto.parser.CantoParserBaseVisitor;
import canto.parser.CantoParser.NameComponentContext;
import canto.runtime.Log;

/**
 * 
 */
public class CantoVisitor extends CantoParserBaseVisitor<CantoNode> { 

    private static final Log LOG = Log.getLogger(CantoVisitor.class);
    
    @Override
    public CantoNode visitCompilationUnit(CantoParser.CompilationUnitContext ctx) {
        CompilationUnit unit = new CompilationUnit();
        int numChildren = ctx.getChildCount();
        for (int i = 0; i < numChildren; i++) {
            Site site = (Site) ctx.getChild(i).accept(this);
            unit.addSite(site);
        }
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
    public CantoNode visitNamedElementDefinition(CantoParser.NamedElementDefinitionContext ctx) {
        NameNode name = (NameNode) ctx.identifier().accept(this);
        ParseTree typeCtx = ctx.simpleType();
        Type superType = (typeCtx == null ? null : (Type) typeCtx.accept(this));
        ParseTree paramsCtx = ctx.params();
        if (paramsCtx != null) {
            ParameterList params = (ParameterList) paramsCtx.accept(this);
            List<ParameterList> paramsList = new ArrayList<ParameterList>(1);
            paramsList.add(params);
            name = new NameWithParams(name.getName(), paramsList);
        }
        CantoNode contents = ctx.expression().accept(this);
        return new NamedDefinition(superType, name, contents);
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
    public CantoNode visitBlock(CantoParser.BlockContext ctx) {
        if (ctx.codeBlock() != null) {
            return ctx.codeBlock().accept(this);
        } else if (ctx.textBlock() != null) {
            return ctx.textBlock().accept(this);
        } else {
            return ctx.literalBlock().accept(this);
        }
    }

    @Override
    public CantoNode visitCodeBlock(CantoParser.CodeBlockContext ctx) {
        ListNode<CantoNode> nodes = new ListNode<CantoNode>();
        for (int i = 0; i < ctx.getChildCount(); i++) {
            CantoNode node = ctx.getChild(i).accept(this);
            if (node != null) {
                nodes.add(node);
            }
        }
        return new CodeBlock(nodes);
    }

    @Override
    public CantoNode visitTextBlock(CantoParser.TextBlockContext ctx) {
        ListNode<CantoNode> nodes = new ListNode<CantoNode>();
        for (int i = 0; i < ctx.getChildCount(); i++) {
            CantoNode node = ctx.getChild(i).accept(this);
            if (node != null) {
                nodes.add(node);
            }
        }
        return new StaticBlock(nodes);
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
    public CantoNode visitConstruction(CantoParser.ConstructionContext ctx) {
        if (ctx.block() != null) {
            return ctx.block().accept(this);
        } else if (ctx.conditional() != null) {
            return ctx.conditional().accept(this);
        } else if (ctx.loop() != null) {
            return ctx.loop().accept(this);
        } else {
            return ctx.expression().accept(this);
        }
    }

    @Override
    public CantoNode visitInstantiationExpression(CantoParser.InstantiationExpressionContext ctx) {
        return ctx.instantiation().accept(this);
    }

    @Override
    public CantoNode visitLiteralExpression(CantoParser.LiteralExpressionContext ctx) {
        Expression expression = new ValueExpression(ctx.literal().accept(this));
        return expression;
    }

    @Override
    public CantoNode visitInstantiation(CantoParser.InstantiationContext ctx) {
        List<NameComponentContext> nameComponents = ctx.nameComponent();
        int numComponents = nameComponents.size();
        List<CantoNode> nodeList = new ArrayList<CantoNode>(numComponents);
        
        for (NameComponentContext nameCtx: nameComponents) {
            NameNode name = (NameNode) nameCtx.accept(this);
            nodeList.add(name);
        }
        NameNode name = new ComplexName(nodeList);
        return new Instantiation(name);
    }

    @Override
    public CantoNode visitLiteral(CantoParser.LiteralContext ctx) {
        String data = ctx.getText();
        if (ctx.integerLiteral() != null) {
            try {
                int value = Integer.parseInt(data);
                return new PrimitiveValue(value);
            } catch (NumberFormatException e) {
                LOG.error("Invalid integer literal: " + data);
                return new PrimitiveValue(0);
            }
        } else if (ctx.floatLiteral() != null) {
            try {
                double value = Double.parseDouble(data);
                return new PrimitiveValue(value);
            } catch (NumberFormatException e) {
                LOG.error("Invalid float literal: " + data);
                return new PrimitiveValue(0.0f);
            }
        } else if (ctx.BOOL_LITERAL() != null) {
            boolean value = data.equals("true");
            return new PrimitiveValue(value);
        } else if (ctx.NULL_LITERAL() != null) {
            return new PrimitiveValue(null);
        }
        // assume it's a string
        return new PrimitiveValue(data);
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
    public CantoNode visitNameComponent(CantoParser.NameComponentContext ctx) {
        int numNodes = ctx.getChildCount();
        if (numNodes > 1) {
            String name = ctx.getChild(0).getText();
            ArgumentList args = null;
            IndexList indexes = null;
            
            CantoNode node = ctx.getChild(1).accept(this);
            if (node instanceof ArgumentList) {
                args = (ArgumentList) node;
                if (numNodes == 2) {
                    return new NameWithArgs(name, args);
                } else {
                    indexes = new IndexList(numNodes - 2);
                }  
            } else {
                indexes = new IndexList(numNodes - 1);
                indexes.add((Index) node);
            }
            
            for (int i = 2; i < numNodes; i++) {
                indexes.add((Index) ctx.getChild(i).accept(this));
            }
            return (args == null ? new NameWithArgs(name, indexes) : new NameWithArgs(name, args, indexes));
        } else {
            return ctx.getChild(0).accept(this);
        }
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

