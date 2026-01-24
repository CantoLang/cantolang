/* Canto Compiler and Runtime Engine
 *
 * CantoBuilderTest.java
 *
 * Copyright (c) 2025 by cantolang.org
 * All rights reserved.
 */

package canto.lang;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.tree.ParseTree;
import org.assertj.core.api.Assertions;
import org.assertj.core.api.WithAssertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import canto.parser.CantoLexer;
import canto.parser.CantoParser;
import canto.parser.CantoParser.NamedElementDefinitionContext;

public class CantoVisitorTest {

    private CantoVisitor visitor = new CantoVisitor();

    private class TypedParser<T extends ParseTree> {
        private Method method;
        public TypedParser(String rule) {
            try {
                this.method = CantoParser.class.getMethod(rule);
            } catch (NoSuchMethodException e) {
                e.printStackTrace();
                throw new IllegalArgumentException("Unknown parser rule " + rule);
            }
        }
        
        @SuppressWarnings("unchecked")
        private T parseInput(String input) {
            CantoLexer lexer = new CantoLexer(CharStreams.fromString(input));
            CommonTokenStream tokens = new CommonTokenStream(lexer);
            CantoParser parser = new CantoParser(tokens);
            try {
                return (T) method.invoke(parser);
            } catch (SecurityException | IllegalAccessException | InvocationTargetException e) {
                e.printStackTrace();
                throw new RuntimeException("Exception parsing input: " + e.toString());
            }
        }
    }

    @Test
    public void testVisitCompilationUnit() {

    }

    @Test
    public void testVisitSiteDefinition() {

    }

    @Test
    public void testVisitSiteBlock() {

    }

    @Test
    public void testVisitExternDirective() {

    }

    @Test
    public void testVisitAdoptDirective() {

    }

    @Test
    public void testVisitTopDefinition() {

    }

    @Test
    public void testVisitDefinition() {

    }

    @Test
    public void testVisitCollectionElementDefinition() {

    }

    @Test
    public void testVisitCollectionDefinition() {

    }

    @ParameterizedTest
    @DisplayName("Visitor should build various named element definitions")
    @ValueSource(strings = {
            "x = 42",
            "y(z) = z",
            "w = f(5)",
            "v = \"string\""
    })
    public void testVisitNamedElementDefinition(String input) {
        TypedParser<CantoParser.NamedElementDefinitionContext> parser = new TypedParser<CantoParser.NamedElementDefinitionContext>("namedElementDefinition");
        NamedElementDefinitionContext ctx = parser.parseInput(input);
        CantoNode node = visitor.visitNamedElementDefinition(ctx);
            
        Assertions.assertThat(node).isInstanceOf(NamedDefinition.class);

    }

    @ParameterizedTest
    @DisplayName("Visitor should build various block definitions")
    @ValueSource(strings = {
            "d1 { int x = 5  x; }",
            "d2 {= float x = 5.0  x;  =}",
            "d3 [| text block 1 |]",
            "d4(x) { x; }",
            "int d5(int x) { x; }",
            "d6 [`` literal block } { |] \\ [| ``]",
            "d7(z) [| text {= z; =} block 2 |]"
    })
    public void testVisitBlockDefinition(String input) {
        TypedParser<CantoParser.BlockDefinitionContext> parser = new TypedParser<CantoParser.BlockDefinitionContext>("blockDefinition");
        CantoParser.BlockDefinitionContext ctx = parser.parseInput(input);
        CantoNode node = visitor.visitBlockDefinition(ctx);
            
        Assertions.assertThat(node).isInstanceOf(ComplexDefinition.class);
    }

    @ParameterizedTest
    @DisplayName("Visitor should build various conditionals")
    @ValueSource(strings = {
            "if (x) { x; }",
            "if x { x; } else { z; }",
            "if y > 0 { y; } else [| zero |]",
            "if (y == 'hello') [| x |] else if (y == 'goodbye') [| bye |]",
            "if f(x) > 0 { x; } else if (y + z < 100) { z; } else if x - y + z >= 0 { y; } else { w; }"
    })
    void testVisitConditional(String input) {
        TypedParser<CantoParser.ConditionalContext> parser = new TypedParser<CantoParser.ConditionalContext>("conditional");
        CantoParser.ConditionalContext ctx = parser.parseInput(input);
        CantoNode node = visitor.visitConditional(ctx);
            
        Assertions.assertThat(node).isInstanceOf(ConditionalStatement.class);
    }

    
    @ParameterizedTest
    @DisplayName("Visitor should build various loops")
    @ValueSource(strings = {
            "for x in a { x; }",
            "for char c in b [| c = {= c; =} |]",
            "for int x from 1 to 10 by 2 { x; }",
            "for x in a[1] { x; }",
            "for y in a.b.c { for z in d { y; z; } }",
            "for x in a and y in b { x; y; }",
            "for x in a until x == 'X' { x; }",
            "for int y in b where y > 0 { y; }"
    })
    void testVisitLoop(String input) {
        TypedParser<CantoParser.LoopContext> parser = new TypedParser<CantoParser.LoopContext>("loop");
        CantoParser.LoopContext ctx = parser.parseInput(input);
        CantoNode node = visitor.visitLoop(ctx);
            
        Assertions.assertThat(node).isInstanceOf(ForStatement.class);
    }

    @ParameterizedTest
    @DisplayName("Visitor should build various literals")
    @ValueSource(strings = {
        "42",           // integer
        "3.14",         // float  
        "true",         // boolean
        "false",        // boolean
        "'hello'",      // string
        "\"world\""     // string
    })
    void testVisitLiteral(String input) {
        TypedParser<CantoParser.LiteralContext> parser = new TypedParser<CantoParser.LiteralContext>("literal");
        CantoParser.LiteralContext ctx = parser.parseInput(input);
        CantoNode node = visitor.visitLiteral(ctx);
            
        Assertions.assertThat(node).isInstanceOf(PrimitiveValue.class);
    }

    @ParameterizedTest
    @DisplayName("Visitor should build various instantiations")
    @ValueSource(strings = {
        "x;",
        "x(1);",
        "x(y);",
        "x(y,z);",
        "w(x,y,z);",
        "f(5,'test',true);",
        "a.b.c;",
        "a(x).b;",
        "m[0];",
        "m[0][1];",
        "m[0](x);"
    })
    void testVisitInstantiation(String input) {
        TypedParser<CantoParser.InstantiationContext> parser = new TypedParser<CantoParser.InstantiationContext>("instantiation");
        CantoParser.InstantiationContext ctx = parser.parseInput(input);
        CantoNode node = visitor.visitInstantiation(ctx);
            
        Assertions.assertThat(node).isInstanceOf(Instantiation.class);
    }

    
    @Test
    public void testVisitCollectionDefName() {

    }

    @Test
    public void testVisitBlockDefName() {

    }

    @Test
    public void testVisitSimpleType() {

    }

    @Test
    public void testVisitIdentifier() {

    }

    @Test
    public void testVisitAny() {

    }

    @Test
    public void testVisitAnyany() {

    }

    @Test
    public void testVisitNameRange() {

    }

    @Test
    public void testVisitQualifiedName() {

    }
}