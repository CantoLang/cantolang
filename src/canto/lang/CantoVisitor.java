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
import java.util.ListIterator;
import java.util.Map;

import org.antlr.v4.runtime.ParserRuleContext;
import org.antlr.v4.runtime.Token;
import org.antlr.v4.runtime.tree.ParseTree;

import canto.parser.CantoParser;
import canto.parser.CantoParserBaseVisitor;
import canto.parser.CantoParser.ExpressionContext;
import canto.parser.CantoParser.IteratorContext;
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
    public CantoNode visitCollectionDefName(CantoParser.CollectionDefNameContext ctx) {
        NameNode name = (NameNode) ctx.identifier().accept(this);
        ParseTree paramsCtx = ctx.params();

        if (paramsCtx != null) {
            ParameterList params = visitParamsHelper((CantoParser.ParamsContext) paramsCtx);
            List<ParameterList> paramsList = new ArrayList<>(1);
            paramsList.add(params);
            return new NameWithParams(name.getName(), paramsList);
        }

        return name;
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
                ParameterList params = visitParamsHelper(pCtx);
                paramsList.add(params);
            }
            return new NameWithParams(name.getName(), paramsList);

        } else if (paramsCtx != null) {
            ParameterList params = visitParamsHelper((CantoParser.ParamsContext) paramsCtx);
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
    private ParameterList visitParamsHelper(CantoParser.ParamsContext ctx) {
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
    public CantoNode visitConditional(CantoParser.ConditionalContext ctx) {
        int cond = ctx.cond.getType();
        ValueSource condition = (ValueSource) (cond == CantoParser.IF ? ctx.expression().accept(this) : new WithPredicate(ctx.identifier().accept(this), cond == CantoParser.WITH));
        Block body = (Block) ctx.block().accept(this);
        List<CantoParser.ElseIfPartContext> elseIfParts = ctx.elseIfPart();
        CantoParser.ElsePartContext elsePart = ctx.elsePart();
        
        ConditionalStatement conditional = null;
        Block elseBody = (elsePart == null ? null : (Block) elsePart.accept(this));

        if (elseIfParts != null) {
            ListIterator<CantoParser.ElseIfPartContext> iter = elseIfParts.listIterator(elseIfParts.size());
            ConditionalStatement elseIfConditional = null;
            while (iter.hasPrevious()) {
                CantoParser.ElseIfPartContext elseIfCtx = iter.previous();
                int elseIfCond = elseIfCtx.cond.getType();
                ValueSource elseIfCondition = (ValueSource) (elseIfCond == CantoParser.IF ? elseIfCtx.expression().accept(this) : new WithPredicate(elseIfCtx.identifier().accept(this), elseIfCond == CantoParser.WITH));
                Block elseIfBody = (Block) elseIfCtx.block().accept(this);
                if (elseIfConditional == null) {
                    elseIfConditional = new ConditionalStatement(elseIfCondition, elseIfBody, elseBody);
                } else {
                    elseIfConditional = new ConditionalStatement(elseIfCondition, elseIfBody, elseIfConditional);
                }
            }
            conditional = new ConditionalStatement(condition, body, elseIfConditional);
        } else {
            conditional = new ConditionalStatement(condition, body, elseBody);
        }
        
        return conditional;
    }

    @Override
    public CantoNode visitLoop(CantoParser.LoopContext ctx) {
        ForStatement loop = new ForStatement();
        List<CantoParser.IteratorContext> iteratorCtxs = ctx.iterator();
        for (CantoParser.IteratorContext iteratorCtx : iteratorCtxs) {
            ForStatement.IteratorValues iteratorValues = (ForStatement.IteratorValues) iteratorCtx.accept(this);
            loop.addIteratorValues(iteratorValues);
        }
        loop.setBody((Block) ctx.block().accept(this));
        return loop;
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

