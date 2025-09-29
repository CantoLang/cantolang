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
            "d6 [`` literal block } { |] \\ [| ``]"
    })
    public void testVisitBlockDefinition(String input) {
        TypedParser<CantoParser.BlockDefinitionContext> parser = new TypedParser<CantoParser.BlockDefinitionContext>("blockDefinition");
        CantoParser.BlockDefinitionContext ctx = parser.parseInput(input);
        CantoNode node = visitor.visitBlockDefinition(ctx);
            
        Assertions.assertThat(node).isInstanceOf(ComplexDefinition.class);
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