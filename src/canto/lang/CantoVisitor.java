/* Canto Compiler and Runtime Engine
 * 
 * CantoBuilder.java
 *
 * Copyright (c) 2024-2026 by cantolang.org
 * All rights reserved.
 */

package canto.lang;

import java.util.ArrayList;
import java.util.List;
import java.util.ListIterator;

import org.antlr.v4.runtime.ParserRuleContext;
import org.antlr.v4.runtime.Token;
import org.antlr.v4.runtime.tree.ParseTree;
import org.antlr.v4.runtime.tree.TerminalNode;

import canto.parser.CantoParser;
import canto.parser.CantoParserBaseVisitor;
import canto.parser.CantoParser.DimContext;
import canto.parser.CantoParser.ExpressionContext;
import canto.parser.CantoParser.NameComponentContext;
import canto.runtime.Log;

/**
 * 
 */
public class CantoVisitor extends CantoParserBaseVisitor<CantoNode> { 

    private static final Log LOG = Log.getLogger(CantoVisitor.class);

    private static AbstractOperator getOp(int opType) {
         switch (opType) {
             case CantoParser.PLUS:
                 return new AddOperator();
             case CantoParser.MINUS:
                 return new SubtractOperator();
             case CantoParser.STAR:
                 return new MultiplyOperator();
             case CantoParser.SLASH:
                 return new DivideByOperator();
             case CantoParser.MOD:
                 return new ModOperator();
             case CantoParser.STARSTAR:
                 return new PowerOperator();
             case CantoParser.EQ:
                 return new EqualsOperator();
             case CantoParser.NE:
                 return new NotEqualsOperator();
             case CantoParser.LT:
                 return new LessThanOperator();
             case CantoParser.LE:
                 return new LessThanOrEqualOperator();
             case CantoParser.GT:
                 return new GreaterThanOperator();
             case CantoParser.GE:
                 return new GreaterThanOrEqualOperator();
             case CantoParser.ANDAND:
                 return new LogicalAndOperator();
             case CantoParser.OROR:
                 return new LogicalOrOperator();
             case CantoParser.BITAND:
                 return new BitwiseAndOperator();
             case CantoParser.BITOR:
                 return new BitwiseOrOperator();
             case CantoParser.CARET:
                 return new XorOperator();
             case CantoParser.LSHIFT:
                 return new LeftShiftOperator();
             case CantoParser.RSHIFT:
                 return new RightShiftOperator();
             case CantoParser.RUSHIFT:
                 return new RightUnsignedShiftOperator();
             case CantoParser.IN:
                 return new InOperator();
             case CantoParser.BANG:
                 return new LogicalNotOperator();
             case CantoParser.TILDE:
                 return new BitflipOperator();
             default:
                 throw new IllegalArgumentException("Unknown operator type: " + opType);
         }
    }


    @Override
    public CantoNode visitCompilationUnit(CantoParser.CompilationUnitContext ctx) {
        int numChildren = ctx.getChildCount();
        Site mainSite = null;
        for (int i = 0; i < numChildren; i++) {
            Site site = (Site) ctx.getChild(i).accept(this);
            if (site != null) {
                ctx.DOC_COMMENT().forEach(comment -> site.addDocComment(comment.getText()));
                if (mainSite == null) {
                    mainSite = site;
                } else if (site.getName().equals(mainSite.getName())) {
                    mainSite.mergeSite(site);
                } else {
                    throw new NameMismatchException("Site name mismatch in compilation unit: " + mainSite.getName() + " and " + site.getName());
                }
            }
        }
        return mainSite;
    }

    private Site handleSiteDefinition(String domain, NameNode name, SiteBlock block) {
        Site site = new Site(domain, name);
        site.setType(site.createType());
        site.setContents(block);
        return site;
    }
    
    @Override
    public CantoNode visitSiteDefinition(CantoParser.SiteDefinitionContext ctx) {
        NameNode name = (NameNode) ctx.identifier().accept(this);
        SiteBlock block = (SiteBlock) ctx.siteBlock().accept(this);
        Site site = handleSiteDefinition(Name.SITE, name, block);
        return site;
    }
    
    @Override
    public CantoNode visitCoreDefinition(CantoParser.CoreDefinitionContext ctx) {
        SiteBlock block = (SiteBlock) ctx.siteBlock().accept(this);
        Core core = new Core();
        core.setContents(block);
        return core;
    }
    
    @Override
    public CantoNode visitCosmosDefinition(CantoParser.CosmosDefinitionContext ctx) {
        SiteBlock block = (SiteBlock) ctx.siteBlock().accept(this);
        Site site = handleSiteDefinition(Name.COSMOS, new NameNode(Name.COSMOS), block);
        return site;
    }
    
    @Override
    public CantoNode visitGlobeDefinition(CantoParser.GlobeDefinitionContext ctx) {
        SiteBlock block = (SiteBlock) ctx.siteBlock().accept(this);
        Site site = handleSiteDefinition(Name.GLOBE, new NameNode(Name.GLOBE), block);
        return site;
    }
    
    @Override
    public CantoNode visitDefaultSiteDefinition(CantoParser.DefaultSiteDefinitionContext ctx) {
        SiteBlock block = (SiteBlock) ctx.siteBlock().accept(this);
        Site site = handleSiteDefinition(Name.SITE, new NameNode(Name.DEFAULT), block);
        return site;
    }
    
    @Override
    public CantoNode visitDomainDefinition(CantoParser.DomainDefinitionContext ctx) {
        SiteBlock block = (SiteBlock) ctx.siteBlock().accept(this);
        NameNode domain = (NameNode) ctx.identifier(0).accept(this);
        NameNode name = (NameNode) ctx.identifier(1).accept(this);
        Site site = handleSiteDefinition(domain.getName(), name, block);
        return site;
    }
    
    @Override
    public CantoNode visitSiteBlock(CantoParser.SiteBlockContext ctx) {
        ListNode<CantoNode> nodes = new ListNode<CantoNode>();
        for (int i = 0; i < ctx.getChildCount(); i++) {
            CantoNode node = ctx.getChild(i).accept(this);
            if (node != null) {
                nodes.add(node);
            }
        }
        return new SiteBlock(nodes);
    }
    
    @Override
    public CantoNode visitDirective(CantoParser.DirectiveContext ctx) {
        CantoNode node;
        if (ctx.externDirective() != null) {
            node = ctx.externDirective().accept(this);
        } else {
            node = ctx.adoptDirective().accept(this);
        }
        if (node != null) {
            ctx.DOC_COMMENT().forEach(comment -> node.addDocComment(comment.getText()));
        }
        return node;
    }

    @Override
    public CantoNode visitExternDirective(CantoParser.ExternDirectiveContext ctx) {
        NameNode binding = (NameNode) ctx.identifier().accept(this);
        ComplexName nameRange = (ComplexName) ctx.nameRange().accept(this);
        ExternStatement externDirective = new ExternStatement(binding, nameRange);
        return externDirective;
    }

    @Override
    public CantoNode visitAdoptDirective(CantoParser.AdoptDirectiveContext ctx) {
        ComplexName nameRange = (ComplexName) ctx.nameRange().accept(this);
        AdoptStatement adoptDirective = new AdoptStatement(nameRange);
        return adoptDirective;
    }

    @Override
    public CantoNode visitTopDefinition(CantoParser.TopDefinitionContext ctx) {
        Definition def = handleDefinition(ctx.DOC_COMMENT(), ctx.keep, ctx.access, ctx.dur, ctx);
        return def;
    }
    
    @Override
    public CantoNode visitDefinition(CantoParser.DefinitionContext ctx) {
        Definition def = handleDefinition(ctx.DOC_COMMENT(), ctx.keep, ctx.access, ctx.dur, ctx);
        return def;
    }

    @Override
    public CantoNode visitTopKeepPrefix(CantoParser.TopKeepPrefixContext ctx) {
        KeepNode keepNode = new KeepNode();
        CantoParser.KeepAsContext keepAsCtx = ctx.keepAs();
        int numChildren = (keepAsCtx != null ? 2 : 1);
        List<CantoNode> children = new ArrayList<>(numChildren);
        
        if (keepAsCtx != null) {
            NameNode asName = (NameNode) keepAsCtx.identifier().accept(this);
            keepNode.setAsName(asName);
            children.add(asName);
        }
        Instantiation tableInstance = (Instantiation) ctx.keepIn().instantiation().accept(this);
        keepNode.setTableInstance(tableInstance);
        children.add(tableInstance);
        keepNode.setChildren(children);
        return keepNode;
    }

    @Override
    public CantoNode visitKeepPrefix(CantoParser.KeepPrefixContext ctx) {
        KeepNode keepNode = new KeepNode();
        CantoParser.KeepAsContext keepAsCtx = ctx.keepAs();
        CantoParser.KeepInContext keepInCtx = ctx.keepIn();
        
        int numChildren = (keepAsCtx != null ? 1 : 0) + (keepInCtx != null ? 1 : 0);
        List<CantoNode> children = new ArrayList<>(numChildren);
        
        if (keepAsCtx != null) {
            NameNode asName;
            if (keepAsCtx.identifier() != null) {
                asName = (NameNode) ctx.keepAs().identifier().accept(this);
            } else {
                asName = new NameNode(Name.THIS);
            }
            keepNode.setAsName(asName);
            children.add(asName);
        }
        if (ctx.keepIn() != null) {
            Instantiation tableInstance = (Instantiation) ctx.keepIn().instantiation().accept(this);
            keepNode.setTableInstance(tableInstance);
            children.add(tableInstance);
        }

        if (numChildren > 0) {
            keepNode.setChildren(children);
        }

        return keepNode;
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
    
    private Definition handleDefinition(List<TerminalNode> comments, ParseTree keep, Token access, ParseTree dur, ParserRuleContext ctx) {
        Definition def = null;
        KeepNode keepNode = null;
        int numNodes = ctx.getChildCount();
        ParseTree child = ctx.getChild(numNodes - 1);
        def = (Definition) child.accept(this);

        if (def != null) {
            if (comments != null) {
                for (TerminalNode comment : comments) {
                    def.addDocComment(comment.getText());
                }
            }
            if (keep != null) {
                keepNode = (KeepNode) keep.accept(this);
                if (keepNode != null) {
                    keepNode.setDefName(def.getNameNode());
                    ((NamedDefinition) def).addKeep(keepNode);
                }
            }
            def.setAccess(getAccess(access));
            def.setDurability(getDurability(dur));
        }
        return def;
    }

    @Override
    public CantoNode visitCollectionElementDefinition(CantoParser.CollectionElementDefinitionContext ctx) {
        CantoParser.CollectionDefNameContext nameCtx = ctx.collectionDefName();
        NameNode name = (NameNode) nameCtx.accept(this);
        ParseTree typeCtx = nameCtx.collectionType();
        if (typeCtx == null) {
            typeCtx = nameCtx.simpleType();
        }
        Type superType = (typeCtx == null ? null : (Type) typeCtx.accept(this));
        CantoNode contents = ctx.expression().accept(this);
        return new CollectionDefinition(superType, name, contents);
    }

    @Override
    public CantoNode visitCollectionDefinition(CantoParser.CollectionDefinitionContext ctx) {
        CantoParser.CollectionDefNameContext nameCtx = ctx.collectionDefName();
        NameNode name = (NameNode) nameCtx.accept(this);
        Type superType = name.getType();
        CantoNode contents = ctx.collectionInitBlock().accept(this);
        return new CollectionDefinition(superType, name, contents);
    }
    
    @Override
    public CantoNode visitDimlessCollectionDefinition(CantoParser.DimlessCollectionDefinitionContext ctx) {
        CantoParser.DimlessCollectionNameContext nameCtx = ctx.dimlessCollectionName();
        NameNode name = (NameNode) nameCtx.accept(this);
        Type superType = name.getType();
        CantoNode contents = ctx.collectionInitBlock().accept(this);
        return new CollectionDefinition(superType, name, contents);
    }
    
    @Override
    public CantoNode visitAbstractCollectionDefinition(CantoParser.AbstractCollectionDefinitionContext ctx) {
        CantoParser.CollectionDefNameContext nameCtx = ctx.collectionDefName();
        NameNode name = (NameNode) nameCtx.accept(this);
        Type superType = name.getType();
        CantoNode contents = ctx.abstractBlock().accept(this);
        return new ExternalCollectionDefinition(superType, name, contents);
    }
    
    @Override
    public CantoNode visitExternalCollectionDefinition(CantoParser.ExternalCollectionDefinitionContext ctx) {
        CantoParser.CollectionDefNameContext nameCtx = ctx.collectionDefName();
        NameNode name = (NameNode) nameCtx.accept(this);
        Type superType = name.getType();
        CantoNode contents = ctx.externalBlock().accept(this);
        return new ExternalCollectionDefinition(superType, name, contents);
    }
    
    @Override
    public CantoNode visitNamedElementDefinition(CantoParser.NamedElementDefinitionContext ctx) {
        CantoParser.DefNameContext nameCtx = ctx.defName();
        NameNode name = (NameNode) nameCtx.accept(this);
        ParseTree typeCtx = nameCtx.simpleType();
        if (typeCtx == null) {
            typeCtx = nameCtx.multiType();
        }
        Type superType = (typeCtx == null ? null : (Type) typeCtx.accept(this));
        
        CantoNode contents = ctx.expression().accept(this);
        return new NamedDefinition(superType, name, contents);
    }

    @Override
    public CantoNode visitExternalDefinition(CantoParser.ExternalDefinitionContext ctx) {
        CantoParser.DefNameContext nameCtx = ctx.defName();
        NameNode name = (NameNode) nameCtx.accept(this);
        ParseTree typeCtx = nameCtx.simpleType();
        if (typeCtx == null) {
            typeCtx = nameCtx.multiType();
        }
        Type superType = (typeCtx == null ? null : (Type) typeCtx.accept(this));
        
        Block block = (Block) ctx.externalBlock().accept(this);
        ComplexDefinition blockDef = new ComplexDefinition(superType, name, block);
        return blockDef;
    }

    @Override
    public CantoNode visitBlockDefinition(CantoParser.BlockDefinitionContext ctx) {
        CantoParser.DefNameContext nameCtx = ctx.defName();
        NameNode name = (NameNode) nameCtx.accept(this);
        ParseTree typeCtx = nameCtx.simpleType();
        if (typeCtx == null) {
            typeCtx = nameCtx.multiType();
            if (typeCtx == null) {
                typeCtx = nameCtx.typeWithArgs();
            }
        }
        Type superType = (typeCtx == null ? null : (Type) typeCtx.accept(this));
        
        ParseTree blockCtx = ctx.block();
        if (blockCtx == null) {
            blockCtx = ctx.abstractBlock();
        }
        Block block = (Block) blockCtx.accept(this);
        ComplexDefinition blockDef = new ComplexDefinition(superType, name, block);
        return blockDef;
    }

    @Override
    public CantoNode visitBlock(CantoParser.BlockContext ctx) {
        Block block;
        if (ctx.codeBlock() != null) {
            block = (Block) ctx.codeBlock().accept(this);
        } else if (ctx.textBlock() != null) {
            block = (Block) ctx.textBlock().accept(this);
        } else if (ctx.literalBlock() != null) {
            return ctx.literalBlock().accept(this);
        } else {
            return ctx.emptyBlock().accept(this);
        }
        if (ctx.catchBlock() != null) {
            CantoParser.CatchBlockContext catchCtx = ctx.catchBlock();
            Block catchBlock = (Block) catchCtx.block().accept(this);
            block.setCatchBlock(catchBlock);
            if (catchCtx.identifier() != null) {
                NameNode catchIdentifier = (NameNode) catchCtx.identifier().accept(this);
                block.setCatchIdentifier(catchIdentifier);
            }
        }
        return block;
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
        CantoBlock block = new CantoBlock(nodes);
        ctx.DOC_COMMENT().forEach(comment -> block.addDocComment(comment.getText()));
        return block;
    }

    @Override
    public CantoNode visitTextBlock(CantoParser.TextBlockContext ctx) {
        int openDelim = ctx.openDelim.getType();
        int closeDelim = ctx.closeDelim.getType();
        boolean trimLeading = (openDelim == CantoParser.TEXT_OPEN || openDelim == CantoParser.TEXT_REOPEN);
        boolean trimTrailing = (closeDelim == CantoParser.TEXT_CLOSE);
        ListNode<CantoNode> nodes = new ListNode<CantoNode>();
        for (int i = 0; i < ctx.getChildCount(); i++) {
            ParseTree child = ctx.getChild(i);
            if (child instanceof CantoParser.TextChunkContext) {
                String text = child.getText();
                if (text.length() > 0) {
                    if (nodes.size() == 0 && trimLeading) {
                        text = text.stripLeading();
                    }
                    if (i == ctx.getChildCount() - 2 && trimTrailing) {
                        text = text.stripTrailing();
                    }
                    StaticText staticText = new StaticText(text);
                    nodes.add(staticText);
                }
                
            } else if (child instanceof CantoParser.BlockContext) {
                CantoNode node = child.accept(this);
                if (node != null) {
                    nodes.add(node);
                }
            }
        }
        return new StaticBlock(nodes);
    }
    @Override

    public CantoNode visitUnnestableTextBlock(CantoParser.UnnestableTextBlockContext ctx) {
        int openDelim = ctx.openDelim.getType();
        int closeDelim = ctx.closeDelim.getType();
        boolean trimLeading = (openDelim == CantoParser.TEXT_OPEN || openDelim == CantoParser.TEXT_REOPEN);
        boolean trimTrailing = (closeDelim == CantoParser.TEXT_CLOSE);
        String text = ctx.textChunk().getText();
        return new StaticText(text);
    }

    @Override
    public CantoNode visitTextChunk(CantoParser.TextChunkContext ctx) {
        String name = ctx.getText();
        return new NameNode(name);
    }

    
    @Override
    public CantoNode visitLiteralBlock(CantoParser.LiteralBlockContext ctx) {
        ListNode<CantoNode> nodes = new ListNode<CantoNode>(1);
        String text = ctx.body != null ? ctx.body.getText() : "";
        nodes.add(new StaticText(text));
        return new StaticBlock(nodes);
    }

    @Override
    public CantoNode visitEmptyBlock(CantoParser.EmptyBlockContext ctx) {
        return new NullValue(NullValue.EMPTY_BLOCK);
    }

    @Override
    public CantoNode visitAbstractBlock(CantoParser.AbstractBlockContext ctx) {
        return new NullValue(NullValue.ABSTRACT_BLOCK);
    }

    @Override
    public CantoNode visitExternalBlock(CantoParser.ExternalBlockContext ctx) {
        return new NullValue(NullValue.EXTERNAL_BLOCK);
    }

    @Override
    public CantoNode visitCollectionDefName(CantoParser.CollectionDefNameContext ctx) {
        NameNode name = (NameNode) ctx.identifier().accept(this);
        Type type = null;
        List<Dim> dims = null;
        List<Dim> typeDims = null;
        List<ParameterList> paramsList = null;

        CantoParser.SimpleTypeContext simpleTypeCtx = ctx.simpleType();
        CantoParser.CollectionTypeContext collectionTypeCtx = ctx.collectionType();
        CantoParser.TypeWithArgsContext typeWithArgsCtx = ctx.typeWithArgs();
        CantoParser.CollectionSuffixContext collectionSuffixCtx = ctx.collectionSuffix();

        if (collectionTypeCtx != null) {
            type = (Type) collectionTypeCtx.simpleType().accept(this);
            typeDims = new ArrayList<Dim>();
            for (CantoParser.DimContext dimCtx : collectionTypeCtx.dim()) {
                Dim dim = dimCtx.arrayDim() != null ? (Dim) dimCtx.arrayDim().accept(this) : (Dim) dimCtx.tableDim().accept(this);
                typeDims.add(dim);
            }
        } else if (typeWithArgsCtx != null) {
            type = (Type) typeWithArgsCtx.accept(this);
        } else if (simpleTypeCtx != null) {
            type = (Type) simpleTypeCtx.accept(this);
        }
        
        if (collectionSuffixCtx != null) {
            dims = dimsHelper(collectionSuffixCtx.dim());
        }
        
        ParseTree paramsCtx = ctx.params();
        ParseTree multiParamsCtx = ctx.multiParams();

        if (multiParamsCtx != null) {
            // multiParams is: params (COMMA params)+
            paramsList = new ArrayList<>();
            CantoParser.MultiParamsContext multiCtx = (CantoParser.MultiParamsContext) multiParamsCtx;

            for (CantoParser.ParamsContext pCtx : multiCtx.params()) {
                ParameterList params = paramsHelper(pCtx);
                paramsList.add(params);
            }

        } else if (paramsCtx != null) {
            ParameterList params = paramsHelper((CantoParser.ParamsContext) paramsCtx);
            paramsList = new ArrayList<>(1);
            paramsList.add(params);
        }
        
        if (type != null) {
            if (typeDims != null && typeDims.size() > 0) {
                type = addDimsToType(type, typeDims);
            }
            name = new TypedName(type, name.getName(), paramsList, dims);
        } else if (dims != null && dims.size() > 0) {
            name = new NameWithDims(name.getName(), paramsList, dims);
        } else {
            name = new NameWithParams(name.getName(), paramsList);
        }

        return name;
    }

    @Override
    public CantoNode visitDimlessCollectionName(CantoParser.DimlessCollectionNameContext ctx) {
        NameNode name = (NameNode) ctx.identifier().accept(this);
        Type type;

        CantoParser.TypeWithArgsContext typeWithArgsCtx = ctx.typeWithArgs();
        if (typeWithArgsCtx != null) {
            type = (Type) typeWithArgsCtx.accept(this);
        } else {
            type = (Type) ctx.simpleType().accept(this);
        }

        ParseTree paramsCtx = ctx.params();
        ParseTree multiParamsCtx = ctx.multiParams();

        List<ParameterList> paramsList = null;

        if (multiParamsCtx != null) {
            paramsList = new ArrayList<>();
            CantoParser.MultiParamsContext multiCtx = (CantoParser.MultiParamsContext) multiParamsCtx;
            for (CantoParser.ParamsContext pCtx : multiCtx.params()) {
                paramsList.add(paramsHelper(pCtx));
            }
        } else if (paramsCtx != null) {
            paramsList = new ArrayList<>(1);
            paramsList.add(paramsHelper((CantoParser.ParamsContext) paramsCtx));
        }

        return new TypedName(type, name.getName(), paramsList, null);
    }

    /**
     * Helper method to construct a list of Dims from a list of DimContexts.
     */
    private List<Dim> dimsHelper(List<DimContext> dimCtxs) {
        List<Dim> dims = new ArrayList<>(dimCtxs.size());

        for (CantoParser.DimContext dimCtx : dimCtxs) {
            if (dimCtx.arrayDim() != null) {
                dims.add((Dim) dimCtx.arrayDim().accept(this));
            } else if (dimCtx.tableDim() != null) {
                dims.add((Dim) dimCtx.tableDim().accept(this));
            }
        }

        return dims;
    }
    
    @Override
    public CantoNode visitDefName(CantoParser.DefNameContext ctx) {
        NameNode name = (NameNode) ctx.identifier().accept(this);
        CantoParser.SimpleTypeContext simpleTypeCtx = ctx.simpleType();
        CantoParser.MultiTypeContext multiTypeCtx = ctx.multiType();
        CantoParser.TypeWithArgsContext typeWithArgsCtx = ctx.typeWithArgs();

        Type type = simpleTypeCtx != null ? (Type) simpleTypeCtx.accept(this)
                                          : (multiTypeCtx != null ? (Type) multiTypeCtx.accept(this)
                                                                  : (typeWithArgsCtx != null ? (Type) typeWithArgsCtx.accept(this) : null));
        
        ParseTree paramsCtx = ctx.params();
        ParseTree multiParamsCtx = ctx.multiParams();

        List<ParameterList> paramsList = null;
        
        if (multiParamsCtx != null) {
            // multiParams is: params (COMMA params)+
            paramsList = new ArrayList<>();
            CantoParser.MultiParamsContext multiCtx = (CantoParser.MultiParamsContext) multiParamsCtx;

            for (CantoParser.ParamsContext pCtx : multiCtx.params()) {
                ParameterList params = paramsHelper(pCtx);
                paramsList.add(params);
            }

        } else if (paramsCtx != null) {
            ParameterList params = paramsHelper((CantoParser.ParamsContext) paramsCtx);
            paramsList = new ArrayList<>(1);
            paramsList.add(params);
        }

        if (type != null) {
            name = new TypedName(type, name.getName(), paramsList, null);
        } else if (paramsList != null) {
            name = new NameWithParams(name.getName(), paramsList);
        }
        
        return name;
    }

    /**
     * Helper method to construct a ParameterList from a ParamsContext.
     * params: LPAREN (param (COMMA param)*)? RPAREN
     */
    private ParameterList paramsHelper(CantoParser.ParamsContext ctx) {
        ParameterList paramList = new ParameterList();

        for (CantoParser.ParamContext paramCtx : ctx.param()) {
            ParseTree typeCtx = paramCtx.simpleType();
            if (typeCtx == null) {
                typeCtx = paramCtx.collectionType();
            }
            Type paramType = (typeCtx == null ? null : (Type) typeCtx.accept(this));
            NameNode paramName;
            List<CantoParser.DimContext> dimCtx = paramCtx.dim();
            if (dimCtx != null && dimCtx.size() > 0) {
                List<Dim> dims = dimsHelper(dimCtx);
                if (paramType != null) {
                    paramType = addDimsToType(paramType, dims);
                }
                paramName = new NameWithDims(paramCtx.identifier().getText(), null, dims);
            } else {
                paramName = (NameNode) paramCtx.identifier().accept(this);
            }
            DefParameter param = new DefParameter(paramType, paramName);
            paramList.add(param);
        }

        return paramList;
    }

    private Type addDimsToType(Type baseType, List<Dim> dims) {
        if (baseType instanceof PrimitiveType) {
            ((PrimitiveType) baseType).setDims(dims);
            return baseType;
        } else {
            return new ComplexType(baseType, dims, null);
        }
    }
    
    @Override
    public CantoNode visitMultiType(CantoParser.MultiTypeContext ctx) {
        List<CantoNode> types = new ArrayList<CantoNode>();
        for (CantoParser.SimpleTypeContext simpleTypeCtx : ctx.simpleType()) {
            types.add(simpleTypeCtx.accept(this));
        }
        return new TypeList(types);
    }

    @Override
    public CantoNode visitTypeWithArgs(CantoParser.TypeWithArgsContext ctx) {
        String name;
        if (ctx.identifier() != null) {
            name = ctx.identifier().getText();
        } else {
            name = ((NameNode) ctx.qualifiedName().accept(this)).getName();
        }
        ConstructionList args = (ctx.typeArgs() != null ? (ConstructionList) ctx.typeArgs().accept(this) : new ConstructionList());
        NameNode nameNode = new NameWithArgs(name, args);
        return new ComplexType(nameNode);
    }   
    
    
    @Override
    public CantoNode visitSimpleType(CantoParser.SimpleTypeContext ctx) {
        if (ctx.identifier() != null) {
            return new ComplexType((NameNode) ctx.identifier().accept(this));
        } else if (ctx.qualifiedName() != null) {
            return new ComplexType((NameNode) ctx.qualifiedName().accept(this));
        } else if (ctx.BOOLEAN() != null) {
            return (NameNode) PrimitiveType.BOOLEAN();
        } else if (ctx.INT() != null) {
            return (NameNode) PrimitiveType.INT();
        } else if (ctx.LONG() != null) {
            return (NameNode) PrimitiveType.LONG();
        } else if (ctx.STRING() != null) {
            return (NameNode) PrimitiveType.STRING();
        } else if (ctx.FLOAT() != null) {
            return (NameNode) PrimitiveType.FLOAT();
        } else if (ctx.DOUBLE() != null) {
            return (NameNode) PrimitiveType.DOUBLE();
        } else if (ctx.CHAR() != null) {
            return (NameNode) PrimitiveType.CHAR();
        } else if (ctx.BYTE() != null) {
            return (NameNode) PrimitiveType.BYTE();
        } else if (ctx.NUMBER() != null) {
            return (NameNode) PrimitiveType.NUMBER();
        } else {
            return null;
        }
    }

    @Override
    public CantoNode visitCollectionType(CantoParser.CollectionTypeContext ctx) {
        Type baseType = (Type) ctx.simpleType().accept(this);
        List<Dim> dims = new ArrayList<Dim>();
        for (CantoParser.DimContext dimCtx : ctx.dim()) {
            Dim dim = dimCtx.arrayDim() != null ? (Dim) dimCtx.arrayDim().accept(this) : (Dim) dimCtx.tableDim().accept(this);
            dims.add(dim);
        }

        return (CantoNode) addDimsToType(baseType, dims);
    }

    @Override
    public CantoNode visitConstruction(CantoParser.ConstructionContext ctx) {
        CantoNode node;
        if (ctx.block() != null) {
            node = ctx.block().accept(this);
        } else if (ctx.conditional() != null) {
            node = ctx.conditional().accept(this);
        } else if (ctx.loop() != null) {
            node = ctx.loop().accept(this);
        } else if (ctx.redirect() != null) {
            node = ctx.redirect().accept(this);
        } else {
            node = convertSpecialStatement(ctx.expression().accept(this));
        }
        final CantoNode result = node;
        ctx.DOC_COMMENT().forEach(comment -> result.addDocComment(comment.getText()));
        return result;
    }

    /** If the node is a bare-name Instantiation of sub/super/next (no args, no
     *  indexes, single name component), replace it with the corresponding
     *  Statement so Context.construct's special handling fires. */
    private static CantoNode convertSpecialStatement(CantoNode node) {
        if (!(node instanceof Instantiation)) return node;
        Instantiation inst = (Instantiation) node;
        CantoNode ref = inst.getReference();
        if (!(ref instanceof NameNode) || ref instanceof ComplexName) return node;
        if (inst.getArguments() != null) return node;
        IndexList idx = inst.getIndexes();
        if (idx != null && !idx.isEmpty()) return node;
        String name = ((NameNode) ref).getName();
        if (Name.SUB.equals(name)) return new SubStatement();
        if (Name.SUPER.equals(name)) return new SuperStatement();
        if (Name.NEXT.equals(name)) return new NextStatement();
        return node;
    }
    
    @Override
    public CantoNode visitRedirect(CantoParser.RedirectContext ctx) {
        CantoNode target = (CantoNode) ctx.instantiation().accept(this);
        return new RedirectStatement(target);
    }

    @Override
    public CantoNode visitConditional(CantoParser.ConditionalContext ctx) {
        ValueSource condition = buildCondition(ctx.cond, ctx.expression(), ctx.identifier());
        Block body = (Block) ctx.block().accept(this);
        Block elseBody = (ctx.elsePart() == null ? null : (Block) ctx.elsePart().accept(this));
        List<ValueSource> elseIfConditions = new ArrayList<>();
        List<Block> elseIfBodies = new ArrayList<>();
        for (CantoParser.ElseIfPartContext eic : ctx.elseIfPart()) {
            elseIfConditions.add(buildCondition(eic.cond, eic.expression(), eic.identifier()));
            elseIfBodies.add((Block) eic.block().accept(this));
        }
        return buildConditionalChain(condition, body, elseIfConditions, elseIfBodies, elseBody);
    }

    @Override
    public CantoNode visitLoop(CantoParser.LoopContext ctx) {
        return buildForStatement(ctx.iterator(), (Block) ctx.block().accept(this));
    }

    @Override
    public CantoNode visitArrayDim(CantoParser.ArrayDimContext ctx) {
        Dim dim;
        
        if (ctx.expression() != null) {
            dim = new Dim(Dim.TYPE.DEFINITE, (Construction) ctx.expression().accept(this));
        } else {
            dim = new Dim();
        }
        
        return dim;
    }
    
    @Override
    public CantoNode visitTableDim(CantoParser.TableDimContext ctx) {
        Dim dim = new Dim();
        dim.setTable(true);
        
        return dim;
    }
    
    
    @Override
    public CantoNode visitArrayInitBlock(CantoParser.ArrayInitBlockContext ctx) {
        if (ctx.EMPTY_ARRAY() != null) {
            return null;
        }
        ConstructionList elements = new ConstructionList();
        CantoParser.ArrayElementListContext listCtx = ctx.arrayElementList();
        if (listCtx != null) {
            for (CantoParser.ArrayElementContext elemCtx : listCtx.arrayElement()) {
                CantoNode element = elemCtx.accept(this);
                if (element instanceof Construction) {
                    elements.add((Construction) element);
                } else if (element != null) {
                    elements.add(new Instantiation(element));
                }
            }
        }
        return elements;
    }

    @Override
    public CantoNode visitArrayElement(CantoParser.ArrayElementContext ctx) {
        if (ctx.expression() != null) {
            return ctx.expression().accept(this);
        } else if (ctx.arrayDynamicInitExpression() != null) {
            return ctx.arrayDynamicInitExpression().accept(this);
        } else if (ctx.codeBlock() != null) {
            return ctx.codeBlock().accept(this);
        } else if (ctx.textBlock() != null) {
            return ctx.textBlock().accept(this);
        } else if (ctx.literalBlock() != null) {
            return ctx.literalBlock().accept(this);
        } else {
            // nested collectionInitBlock — wrap in anonymous CollectionDefinition
            CantoNode contents = ctx.collectionInitBlock().accept(this);
            return new CollectionDefinition(null, new NameNode(""), contents);
        }
    }

    @Override
    public CantoNode visitArrayBlock(CantoParser.ArrayBlockContext ctx) {
        ConstructionList elements = new ConstructionList();
        CantoParser.ArrayElementListContext listCtx = ctx.arrayElementList();
        if (listCtx != null) {
            for (CantoParser.ArrayElementContext elemCtx : listCtx.arrayElement()) {
                CantoNode element = elemCtx.accept(this);
                if (element instanceof Construction) {
                    elements.add((Construction) element);
                } else if (element != null) {
                    elements.add(new Instantiation(element));
                }
            }
        }
        return new ArrayBlock(elements);
    }

    @Override
    public CantoNode visitAnonymousArray(CantoParser.AnonymousArrayContext ctx) {
        ConstructionList elements = new ConstructionList();
        CantoParser.ArrayElementListContext listCtx = ctx.arrayElementList();
        for (CantoParser.ArrayElementContext elemCtx : listCtx.arrayElement()) {
            CantoNode element = elemCtx.accept(this);
            if (element instanceof Construction) {
                elements.add((Construction) element);
            } else if (element != null) {
                elements.add(new Instantiation(element));
            }
        }
        return new ArrayBlock(elements);
    }

    @Override
    public CantoNode visitArrayConditional(CantoParser.ArrayConditionalContext ctx) {
        ValueSource condition = buildCondition(ctx.cond, ctx.expression(), ctx.identifier());
        ArrayBlock body = (ArrayBlock) ctx.arrayBlock().accept(this);
        ArrayBlock elseBody = (ctx.arrayElsePart() == null ? null
                : (ArrayBlock) ctx.arrayElsePart().arrayBlock().accept(this));
        List<ValueSource> elseIfConditions = new ArrayList<>();
        List<Block> elseIfBodies = new ArrayList<>();
        for (CantoParser.ArrayElseIfPartContext eic : ctx.arrayElseIfPart()) {
            elseIfConditions.add(buildCondition(eic.cond, eic.expression(), eic.identifier()));
            elseIfBodies.add((Block) eic.arrayBlock().accept(this));
        }
        return buildConditionalChain(condition, body, elseIfConditions, elseIfBodies, elseBody);
    }

    @Override
    public CantoNode visitArrayLoop(CantoParser.ArrayLoopContext ctx) {
        return buildForStatement(ctx.iterator(), (ArrayBlock) ctx.arrayBlock().accept(this));
    }

    @Override
    public CantoNode visitTableInitBlock(CantoParser.TableInitBlockContext ctx) {
        if (ctx.EMPTY_TABLE() != null) {
            return null;
        }
        List<CantoNode> elements = new ArrayList<>();
        CantoParser.TableElementListContext listCtx = ctx.tableElementList();
        if (listCtx != null) {
            for (CantoParser.TableElementContext elemCtx : listCtx.tableElement()) {
                CantoNode element = elemCtx.accept(this);
                if (element != null) {
                    elements.add(element);
                }
            }
        }
        return new ListNode<CantoNode>(elements);
    }

    @Override
    public CantoNode visitTableElement(CantoParser.TableElementContext ctx) {
        if (ctx.tableDynamicInitExpression() != null) {
            return ctx.tableDynamicInitExpression().accept(this);
        }
        CantoNode keyNode = ctx.expression(0).accept(this);
        CantoNode valueNode = (ctx.collectionInitBlock() != null)
                ? ctx.collectionInitBlock().accept(this)
                : ctx.expression(1).accept(this);
        // For nested collection values, wrap in anonymous CollectionDefinition
        if (valueNode instanceof ConstructionList || valueNode instanceof ListNode) {
            valueNode = new CollectionDefinition(null, new NameNode(""), valueNode);
        }
        TableElement element = new TableElement();
        if (keyNode instanceof Value) {
            element.setKey((Value) keyNode);
        } else if (keyNode instanceof ValueGenerator) {
            element.setDynamicKey((ValueGenerator) keyNode);
        } else {
            element.setKey(new PrimitiveValue(keyNode.toString()));
        }
        element.setElement(valueNode);
        return element;
    }

    @Override
    public CantoNode visitTableBlock(CantoParser.TableBlockContext ctx) {
        List<Object> elements = new ArrayList<>();
        CantoParser.TableElementListContext listCtx = ctx.tableElementList();
        if (listCtx != null) {
            for (CantoParser.TableElementContext elemCtx : listCtx.tableElement()) {
                CantoNode element = elemCtx.accept(this);
                if (element != null) {
                    elements.add(element);
                }
            }
        }
        return new TableBlock(elements);
    }

    @Override
    public CantoNode visitTableConditional(CantoParser.TableConditionalContext ctx) {
        ValueSource condition = buildCondition(ctx.cond, ctx.expression(), ctx.identifier());
        TableBlock body = (TableBlock) ctx.tableBlock().accept(this);
        TableBlock elseBody = (ctx.tableElsePart() == null ? null
                : (TableBlock) ctx.tableElsePart().tableBlock().accept(this));
        List<ValueSource> elseIfConditions = new ArrayList<>();
        List<Block> elseIfBodies = new ArrayList<>();
        for (CantoParser.TableElseIfPartContext eic : ctx.tableElseIfPart()) {
            elseIfConditions.add(buildCondition(eic.cond, eic.expression(), eic.identifier()));
            elseIfBodies.add((Block) eic.tableBlock().accept(this));
        }
        return buildConditionalChain(condition, body, elseIfConditions, elseIfBodies, elseBody);
    }

    @Override
    public CantoNode visitTableLoop(CantoParser.TableLoopContext ctx) {
        return buildForStatement(ctx.iterator(), (TableBlock) ctx.tableBlock().accept(this));
    }

    @Override
    public CantoNode visitCollectionIterator(CantoParser.CollectionIteratorContext ctx) {
        NameNode name = (NameNode) ctx.identifier().accept(this);
        ParseTree typeCtx = ctx.simpleType();
        Type supertype = (typeCtx == null ? null : (Type) typeCtx.accept(this));
        DefParameter forDef = new DefParameter(supertype, name);
        Construction collection = (Construction) ctx.expression(0).accept(this);
        int i = 1;
        Construction where = (ctx.WHERE() != null ? (Construction) ctx.expression(i++).accept(this) : null);
        Construction until = (ctx.UNTIL() != null ? (Construction) ctx.expression(i++).accept(this) : null);
        ForStatement.IteratorValues iteratorValues = new ForStatement.IteratorValues(forDef, collection, where, until, null, null, null, null);
        return iteratorValues;
    }
    
    @Override
    public CantoNode visitStepIterator(CantoParser.StepIteratorContext ctx) {
        NameNode name = (NameNode) ctx.identifier().accept(this);
        ParseTree typeCtx = ctx.simpleType();
        Type supertype = (typeCtx == null ? null : (Type) typeCtx.accept(this));
        DefParameter forDef = new DefParameter(supertype, name);
        Construction from = (Construction) ctx.expression(0).accept(this);
        int i = 1;
        Construction to = (ctx.TO() != null ? (Construction) ctx.expression(i++).accept(this) : null);
        Construction through = (ctx.THROUGH() != null ? (Construction) ctx.expression(i++).accept(this) : null);
        Construction by = (ctx.BY() != null ? (Construction) ctx.expression(i++).accept(this) : null);
        Construction where = (ctx.WHERE() != null ? (Construction) ctx.expression(i++).accept(this) : null);
        Construction until = (ctx.UNTIL() != null ? (Construction) ctx.expression(i++).accept(this) : null);
        ForStatement.IteratorValues iteratorValues = new ForStatement.IteratorValues(forDef, null, where, until, from, to, through, by);
        return iteratorValues;
    }

    @Override
    public CantoNode visitAddSubExpression(CantoParser.AddSubExpressionContext ctx) {
        return handleBinaryExpression(ctx, ctx.op);
    }

    @Override
    public CantoNode visitMulDivExpression(CantoParser.MulDivExpressionContext ctx) {
        return handleBinaryExpression(ctx, ctx.op);
    }

    @Override
    public CantoNode visitPowerExpression(CantoParser.PowerExpressionContext ctx) {
        return handleBinaryExpression(ctx, ctx.op);
    }

    @Override
    public CantoNode visitShiftExpression(CantoParser.ShiftExpressionContext ctx) {
        return handleBinaryExpression(ctx, ctx.op);
    }

    @Override
    public CantoNode visitRelExpression(CantoParser.RelExpressionContext ctx) {
        return handleBinaryExpression(ctx, ctx.op);
    }

    @Override
    public CantoNode visitInExpression(CantoParser.InExpressionContext ctx) {
        return handleBinaryExpression(ctx, ctx.op);
    }

    @Override
    public CantoNode visitEqExpression(CantoParser.EqExpressionContext ctx) {
        return handleBinaryExpression(ctx, ctx.op);
    }

    @Override
    public CantoNode visitBitAndExpression(CantoParser.BitAndExpressionContext ctx) {
        return handleBinaryExpression(ctx, ctx.op);
    }

    @Override
    public CantoNode visitBitXorExpression(CantoParser.BitXorExpressionContext ctx) {
        return handleBinaryExpression(ctx, ctx.op);
    }

    @Override
    public CantoNode visitBitOrExpression(CantoParser.BitOrExpressionContext ctx) {
        return handleBinaryExpression(ctx, ctx.op);
    }

    @Override
    public CantoNode visitLogicalAndExpression(CantoParser.LogicalAndExpressionContext ctx) {
        return handleBinaryExpression(ctx, ctx.op);
    }

    @Override
    public CantoNode visitLogicalOrExpression(CantoParser.LogicalOrExpressionContext ctx) {
        return handleBinaryExpression(ctx, ctx.op);
    }

    @Override
    public CantoNode visitUnaryExpression(CantoParser.UnaryExpressionContext ctx) {
        CantoNode operand = ctx.expression().accept(this);
        UnaryOperator op;
        int opType = ctx.op.getType();
        if (opType == CantoParser.PLUS) {
            // unary plus is an empty operator, return the operand directly
            return operand;
            
        } else if (opType == CantoParser.MINUS) {
            // can't look up unary minus because the ops table holds the subtract
            // operator for this token, so create a new NegateOperator instance directly
            op = new NegateOperator();
        } else {
            op = (UnaryOperator) getOp(Integer.valueOf(ctx.op.getType()));
        }
        Expression expression = new UnaryExpression(op, operand);
        return expression;
    }

    @Override
    public CantoNode visitIsaExpression(CantoParser.IsaExpressionContext ctx) {
        CantoNode operand = ctx.expression().accept(this);
        Type type = (Type) ctx.simpleType().accept(this);
        Expression expression = new IsaExpression(operand, type);
        return expression;
    }

    private CantoNode handleBinaryExpression(CantoParser.ExpressionContext ctx, Token opToken) {
        CantoNode left = ctx.getRuleContext(ExpressionContext.class, 0).accept(this);
        CantoNode right = ctx.getRuleContext(ExpressionContext.class, 1).accept(this);
        BinaryOperator op = (BinaryOperator) getOp(Integer.valueOf(opToken.getType()));
        Expression expression = new BinaryExpression(left, op, right);
        return expression;
    }

    @Override
    public CantoNode visitTypeExpression(CantoParser.TypeExpressionContext ctx) {
        Type type = (Type) ctx.simpleType().accept(this);
        CantoNode operand = ctx.expression().accept(this);
        Expression expression = new UnaryExpression(new TypeOperator(type), operand);
        return expression;
    }

    @Override
    public CantoNode visitChoiceExpression(CantoParser.ChoiceExpressionContext ctx) {
        CantoNode cond = ctx.expression(0).accept(this);
        CantoNode ifTrue = ctx.expression(1).accept(this);
        CantoNode ifFalse = ctx.expression(2).accept(this);
        Expression expression = new ChoiceExpression(cond, ifTrue, ifFalse);
        return expression;
    }

    @Override
    public CantoNode visitChoiceWithExpression(CantoParser.ChoiceWithExpressionContext ctx) {
        CantoNode cond = new WithPredicate(ctx.identifier().accept(this), true);
        CantoNode ifTrue = ctx.expression(0).accept(this);
        CantoNode ifFalse = ctx.expression(1).accept(this);
        Expression expression = new ChoiceExpression(cond, ifTrue, ifFalse);
        return expression;
    }

    @Override
    public CantoNode visitNestedExpression(CantoParser.NestedExpressionContext ctx) {
        return ctx.expression().accept(this);
    }

    @Override
    public CantoNode visitLiteralExpression(CantoParser.LiteralExpressionContext ctx) {
        Expression expression = new ValueExpression(ctx.literal().accept(this));
        return expression;
    }

    @Override
    public CantoNode visitAnonymousArrayExpression(CantoParser.AnonymousArrayExpressionContext ctx) {
        ArrayBlock arrayBlock = (ArrayBlock) ctx.anonymousArray().accept(this);
        return arrayBlock;
    }

    @Override
    public CantoNode visitInstantiationExpression(CantoParser.InstantiationExpressionContext ctx) {
        Instantiation instantiation = (Instantiation) ctx.instantiation().accept(this);
        return instantiation;
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
        NameNode name = nodeList.size() == 1 ? (NameNode) nodeList.get(0) : new ComplexName(nodeList);
        return new Instantiation(name);
    }

    @Override
    public CantoNode visitLiteral(CantoParser.LiteralContext ctx) {
        String data = ctx.getText();
        if (ctx.integerLiteral() != null) {
            try {
                int radix = 10;
                if (data.startsWith("0x") || data.startsWith("0X")) {
                    radix = 16;
                    data = data.substring(2);
                } else if (data.startsWith("#") && data.length() > 1) {
                    radix = 16;
                    data = data.substring(1);
                } else if (data.startsWith("0b") || data.startsWith("0B")) {
                    radix = 2;
                    data = data.substring(2);
                }
                if (data.endsWith("L") || data.endsWith("l")) {
                    data = data.substring(0, data.length() - 1);
                    return new PrimitiveValue(Long.parseLong(data, radix));
                } else {
                    return new PrimitiveValue(Integer.parseInt(data, radix));
                }
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

        } else if (ctx.textBlock() != null) {
            return ctx.textBlock().accept(this);

        } else if (ctx.literalBlock() != null) {
            // a literal block contains a single StaticText child with the literal value 
            Block block = (Block) ctx.literalBlock().accept(this);
            return block.children[0];
       
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
    public CantoNode visitSpecialName(CantoParser.SpecialNameContext ctx) {
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
    public CantoNode visitArgs(CantoParser.ArgsContext ctx) {
        ConstructionList args = new ConstructionList(ctx.expression().size());
        for (CantoParser.ExpressionContext exprCtx : ctx.expression()) {
            args.add((Construction) exprCtx.accept(this));
        }
        return args;
    }

    @Override
    public CantoNode visitTypeArgs(CantoParser.TypeArgsContext ctx) {
        ConstructionList args;
        if (ctx.any() != null) {
            args = new ConstructionList(1);
            args.add(ConstructionList.ANY_ARG());
        } else {
            args = new ConstructionList(ctx.expression().size());
            for (CantoParser.ExpressionContext exprCtx : ctx.expression()) {
                args.add((Construction) exprCtx.accept(this));
            }
        }
        return args;
    }

    @Override
    public CantoNode visitDynamicArgs(CantoParser.DynamicArgsContext ctx) {
        ConstructionList args = new ConstructionList(ctx.expression().size());
        args.setDynamic(true);
        for (CantoParser.ExpressionContext exprCtx : ctx.expression()) {
            args.add((Construction) exprCtx.accept(this));
        }
        return args;
    }

    @Override
    public CantoNode visitIndex(CantoParser.IndexContext ctx) {
        if (ctx.STREAM_ARRAY() != null) {
            return new Index();
        }
        Index index = new Index();
        index.setChild(0, ctx.expression().accept(this));
        return index;
    }

    @Override
    public CantoNode visitNameComponent(CantoParser.NameComponentContext ctx) {
        String name = ctx.getChild(0).getText();
        ConstructionList args = null;
        IndexList indexes = null;
        
        if (ctx.args() != null) {
            args = (ConstructionList) ctx.args().accept(this);
        } else if (ctx.dynamicArgs() != null) {
            args = (ConstructionList) ctx.dynamicArgs().accept(this);
        }
        
        List<CantoParser.IndexContext> indexCtxs = ctx.index();
        if (indexCtxs != null && !indexCtxs.isEmpty()) {
            indexes = new IndexList(indexCtxs.size());
            for (CantoParser.IndexContext indexCtx : indexCtxs) {
                indexes.add((Index) indexCtx.accept(this));
            }
        }
        return new NameWithArgs(name, args, indexes);
    }

    @Override
    public CantoNode visitComplexNameComponent(CantoParser.ComplexNameComponentContext ctx) {
        String name = ctx.getChild(0).getText();
        ConstructionList args = null;
        IndexList indexes = null;
        
        if (ctx.args() != null) {
            args = (ConstructionList) ctx.args().accept(this);
        }
        
        List<CantoParser.IndexContext> indexCtxs = ctx.index();
        if (indexCtxs != null && !indexCtxs.isEmpty()) {
            indexes = new IndexList(indexCtxs.size());
            for (CantoParser.IndexContext indexCtx : indexCtxs) {
                indexes.add((Index) indexCtx.accept(this));
            }
        }
        return new NameWithArgs(name, args, indexes);
    }

    @Override
    public CantoNode visitNameRange(CantoParser.NameRangeContext ctx) {
        int numNodes = ctx.getChildCount();
        List<CantoNode> nodeList = new ArrayList<CantoNode>(numNodes);
        for (int i = 0; i < numNodes; i++) {
            ParseTree child = ctx.getChild(i);
            if (child instanceof ParserRuleContext) {
                nodeList.add(ctx.getChild(i).accept(this));
            }
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

    @Override
    public CantoNode visitComplexName(CantoParser.ComplexNameContext ctx) {
        int numNodes = ctx.getChildCount();
        List<CantoNode> nodeList = new ArrayList<CantoNode>(numNodes);
        for (CantoParser.ComplexNameComponentContext node : ctx.complexNameComponent()) {
            nodeList.add(node.accept(this));
        }
        return new ComplexName(nodeList);
    }

    // --- Shared helpers for conditional and loop building ---

    private ValueSource buildCondition(Token condToken,
            CantoParser.ExpressionContext exprCtx,
            CantoParser.IdentifierContext idCtx) {
        if (condToken.getType() == CantoParser.IF) {
            return (ValueSource) exprCtx.accept(this);
        } else {
            return new WithPredicate(idCtx.accept(this), condToken.getType() == CantoParser.WITH);
        }
    }

    private ConditionalStatement buildConditionalChain(
            ValueSource condition, Block body,
            List<ValueSource> elseIfConditions, List<Block> elseIfBodies,
            Block elseBody) {
        if (!elseIfConditions.isEmpty()) {
            ListIterator<ValueSource> condIter = elseIfConditions.listIterator(elseIfConditions.size());
            ListIterator<Block> bodyIter = elseIfBodies.listIterator(elseIfBodies.size());
            ConditionalStatement chain = null;
            while (condIter.hasPrevious()) {
                ValueSource eic = condIter.previous();
                Block eib = bodyIter.previous();
                chain = (chain == null)
                        ? new ConditionalStatement(eic, eib, elseBody)
                        : new ConditionalStatement(eic, eib, chain);
            }
            return new ConditionalStatement(condition, body, chain);
        } else {
            return new ConditionalStatement(condition, body, elseBody);
        }
    }

    private ForStatement buildForStatement(List<CantoParser.IteratorContext> iteratorCtxs, Block body) {
        ForStatement loop = new ForStatement();
        for (CantoParser.IteratorContext iterCtx : iteratorCtxs) {
            loop.addIteratorValues((ForStatement.IteratorValues) iterCtx.accept(this));
        }
        loop.setBody(body);
        return loop;
    }
}  

