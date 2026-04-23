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
import java.util.Map;

import org.antlr.v4.runtime.ParserRuleContext;
import org.antlr.v4.runtime.Token;
import org.antlr.v4.runtime.tree.ParseTree;

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

    private static Map<Integer, AbstractOperator> ops = Map.ofEntries(
            Map.entry(Integer.valueOf(CantoParser.PLUS), new AddOperator()),
            Map.entry(Integer.valueOf(CantoParser.MINUS), new SubtractOperator()),
            Map.entry(Integer.valueOf(CantoParser.STAR), new MultiplyOperator()),
            Map.entry(Integer.valueOf(CantoParser.SLASH), new DivideByOperator()),
            Map.entry(Integer.valueOf(CantoParser.MOD), new ModOperator()),
            Map.entry(Integer.valueOf(CantoParser.STARSTAR), new PowerOperator()),
            Map.entry(Integer.valueOf(CantoParser.EQ), new EqualsOperator()),
            Map.entry(Integer.valueOf(CantoParser.NE), new NotEqualsOperator()),
            Map.entry(Integer.valueOf(CantoParser.LT), new LessThanOperator()),
            Map.entry(Integer.valueOf(CantoParser.LE), new LessThanOrEqualOperator()),
            Map.entry(Integer.valueOf(CantoParser.GT), new GreaterThanOperator()),
            Map.entry(Integer.valueOf(CantoParser.GE), new GreaterThanOrEqualOperator()),
            Map.entry(Integer.valueOf(CantoParser.ANDAND), new LogicalAndOperator()),
            Map.entry(Integer.valueOf(CantoParser.OROR), new LogicalOrOperator()),
            Map.entry(Integer.valueOf(CantoParser.BITAND), new BitwiseAndOperator()),
            Map.entry(Integer.valueOf(CantoParser.BITOR), new BitwiseOrOperator()),
            Map.entry(Integer.valueOf(CantoParser.CARET), new XorOperator()),
            Map.entry(Integer.valueOf(CantoParser.LSHIFT), new LeftShiftOperator()),
            Map.entry(Integer.valueOf(CantoParser.RSHIFT), new RightShiftOperator()),
            Map.entry(Integer.valueOf(CantoParser.RUSHIFT), new RightUnsignedShiftOperator()),
            Map.entry(Integer.valueOf(CantoParser.IN), new InOperator()),
            Map.entry(Integer.valueOf(CantoParser.BANG), new LogicalNotOperator()),
            Map.entry(Integer.valueOf(CantoParser.TILDE), new BitflipOperator())
            );
    
    @Override
    public CantoNode visitCompilationUnit(CantoParser.CompilationUnitContext ctx) {
        CompilationUnit unit = new CompilationUnit();
        int numChildren = ctx.getChildCount();
        for (int i = 0; i < numChildren; i++) {
            Site site = (Site) ctx.getChild(i).accept(this);
            if (site != null) {
                unit.addSite(site);
            }
        }
        return unit;
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
        if (ctx.doc != null && node != null) {
            node.setDocComment(ctx.doc.getText());
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
        Definition def = handleDefinition(ctx.doc, ctx.keep, ctx.access, ctx.dur, ctx);
        return def;
    }
    
    @Override
    public CantoNode visitDefinition(CantoParser.DefinitionContext ctx) {
        Definition def = handleDefinition(ctx.doc, ctx.keep, ctx.access, ctx.dur, ctx);
        return def;
    }

    @Override
    public CantoNode visitTopKeepPrefix(CantoParser.TopKeepPrefixContext ctx) {
        KeepNode keepNode = new KeepNode();
        if (ctx.keepAs() != null) {
            NameNode asName = (NameNode) ctx.keepAs().identifier().accept(this);
            keepNode.setAsName(asName);
        }
        Instantiation tableInstance = (Instantiation) ctx.keepIn().instantiation().accept(this);
        keepNode.setTableInstance(tableInstance);
        return keepNode;
    }

    @Override
    public CantoNode visitKeepPrefix(CantoParser.KeepPrefixContext ctx) {
        KeepNode keepNode = new KeepNode();
        if (ctx.keepAs() != null) {
            NameNode asName = (NameNode) ctx.keepAs().identifier().accept(this);
            keepNode.setAsName(asName);
        }
        if (ctx.keepIn() != null) {
            Instantiation tableInstance = (Instantiation) ctx.keepIn().instantiation().accept(this);
            keepNode.setTableInstance(tableInstance);
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
    
    private Definition handleDefinition(Token doc, ParseTree keep, Token access, ParseTree dur, ParserRuleContext ctx) {
        Definition def = null;
        KeepNode keepNode = null;
        int numNodes = ctx.getChildCount();
        ParseTree child = ctx.getChild(numNodes - 1);
        def = (Definition) child.accept(this);

        if (def != null) {
            if (doc != null) {
                def.setDocComment(doc.getText());
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
    public CantoNode visitNamedElementDefinition(CantoParser.NamedElementDefinitionContext ctx) {
        NameNode name = (NameNode) ctx.identifier().accept(this);
        ParseTree typeCtx = ctx.simpleType();
        Type superType = (typeCtx == null ? null : (Type) typeCtx.accept(this));
        CantoParser.ParamsContext paramsCtx = ctx.params();
        if (paramsCtx != null) {
            ParameterList params = paramsHelper(paramsCtx);
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
    public CantoNode visitTextChunk(CantoParser.TextChunkContext ctx) {
        String name = ctx.getText();
        return new NameNode(name);
    }

    
    @Override
    public CantoNode visitLiteralBlock(CantoParser.LiteralBlockContext ctx) {
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
        CantoParser.ParamsContext paramsCtx = ctx.params();
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
        
        if (paramsCtx != null) {
            ParameterList params = paramsHelper((CantoParser.ParamsContext) paramsCtx);
            paramsList = new ArrayList<>(1);
            paramsList.add(params);
        }
        
        if (type != null) {
            if (typeDims != null && typeDims.size() > 0) {
                type = new ComplexType(type, typeDims, null);
            }
            name = new TypedName(type, name.getName(), paramsList, dims);
        } else if (dims != null && dims.size() > 0) {
            name = new NameWithDims(name.getName(), paramsList, dims);
        } else {
            name = new NameWithParams(name.getName(), paramsList);
        }
        
        return name;
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
    public CantoNode visitBlockDefName(CantoParser.BlockDefNameContext ctx) {
        NameNode name = (NameNode) ctx.identifier().accept(this);
        ParseTree paramsCtx = ctx.params();
        ParseTree multiParamsCtx = ctx.multiParams();

        if (multiParamsCtx != null) {
            // multiParams is: params (COMMA params)+
            List<ParameterList> paramsList = new ArrayList<>();
            CantoParser.MultiParamsContext multiCtx = (CantoParser.MultiParamsContext) multiParamsCtx;

            for (CantoParser.ParamsContext pCtx : multiCtx.params()) {
                ParameterList params = paramsHelper(pCtx);
                paramsList.add(params);
            }
            return new NameWithParams(name.getName(), paramsList);

        } else if (paramsCtx != null) {
            ParameterList params = paramsHelper((CantoParser.ParamsContext) paramsCtx);
            List<ParameterList> paramsList = new ArrayList<>(1);
            paramsList.add(params);
            return new NameWithParams(name.getName(), paramsList);
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
            Type paramType = (typeCtx == null ? null : (Type) typeCtx.accept(this));
            NameNode paramName = (NameNode) paramCtx.identifier().accept(this);
            DefParameter param = new DefParameter(paramType, paramName);
            paramList.add(param);
        }

        return paramList;
    }

    @Override
    public CantoNode visitMultiType(CantoParser.MultiTypeContext ctx) {
        List<Type> types = new ArrayList<>();
        for (CantoParser.SimpleTypeContext simpleTypeCtx : ctx.simpleType()) {
            types.add((Type) simpleTypeCtx.accept(this));
        }
        return new TypeList(types);
    }

    @Override
    public CantoNode visitTypeWithArgs(CantoParser.TypeWithArgsContext ctx) {
        if (ctx.identifier() != null) {
            return new ComplexType((NameNode) ctx.identifier().accept(this));
        } else {
            return new ComplexType((NameNode) ctx.qualifiedName().accept(this));
        }
    }
    
    
    
    @Override
    public CantoNode visitSimpleType(CantoParser.SimpleTypeContext ctx) {
        if (ctx.identifier() != null) {
            return new ComplexType((NameNode) ctx.identifier().accept(this));
        } else if (ctx.qualifiedName() != null) {
            return new ComplexType((NameNode) ctx.qualifiedName().accept(this));
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
        return new ListNode<Object>(elements);
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
        ForStatement.IteratorValues iteratorValues = new ForStatement.IteratorValues(forDef, null, null, null, from, to, through, by);
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
    public CantoNode visitShiftExpression(CantoParser.ShiftExpressionContext ctx) {
        return handleBinaryExpression(ctx, ctx.op);
    }

    @Override
    public CantoNode visitRelExpression(CantoParser.RelExpressionContext ctx) {
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
        UnaryOperator op = (UnaryOperator) ops.get(Integer.valueOf(ctx.op.getType()));
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
        BinaryOperator op = (BinaryOperator) ops.get(Integer.valueOf(opToken.getType()));
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
        CantoNode cond = ctx.getRuleContext(ExpressionContext.class, 0).accept(this);
        CantoNode ifTrue = ctx.getRuleContext(ExpressionContext.class, 1).accept(this);
        CantoNode ifFalse = ctx.getRuleContext(ExpressionContext.class, 2).accept(this);
        Expression expression = new ChoiceExpression(cond, ifTrue, ifFalse);
        return expression;
    }

    @Override
    public CantoNode visitChoiceWithExpression(CantoParser.ChoiceWithExpressionContext ctx) {
        CantoNode cond = new WithPredicate(ctx.getRuleContext(ExpressionContext.class, 0).accept(this), true);
        CantoNode ifTrue = ctx.getRuleContext(ExpressionContext.class, 1).accept(this);
        CantoNode ifFalse = ctx.getRuleContext(ExpressionContext.class, 2).accept(this);
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

