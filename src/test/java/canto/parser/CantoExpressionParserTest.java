package canto.parser;

import org.assertj.core.api.Assertions;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.tree.ParseTree;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

/**
 * Tests for the ANTLR4 Canto parser.
 * Tests basic parsing functionality and grammar correctness.
 */
class CantoExpressionParserTest {

    private CantoLexer lexer;
    private CantoParser parser;

    @BeforeEach
    void setUp() {
        // Setup will be done per test since input varies
    }

    private ParseTree parseInput(String input) {
        return parseInput(input, "compilationUnit");
    }

    private ParseTree parseInput(String input, String rule) {
        lexer = new CantoLexer(CharStreams.fromString(input));
        CommonTokenStream tokens = new CommonTokenStream(lexer);
        parser = new CantoParser(tokens);
        parser.setTrace(true);
        Method method;
        try {
            method = CantoParser.class.getMethod(rule);
            return (ParseTree) method.invoke(parser);
        } catch (NoSuchMethodException | SecurityException | IllegalAccessException | IllegalArgumentException | InvocationTargetException e) {
            e.printStackTrace();
        }
        return null;       
    }


    @ParameterizedTest
    @DisplayName("Parser should handle various expressions")
    @ValueSource(strings = {
        "(1)",
        "(x)"
    })
    void testExpression(String input) {
        ParseTree tree = parseInput(input, "expression");
        
        Assertions.assertThat(tree).isNotNull();
        Assertions.assertThat(parser.getNumberOfSyntaxErrors()).isEqualTo(0);
    }

}