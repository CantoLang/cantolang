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
class CantoParserTest {

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
        Method method;
        try {
            method = CantoParser.class.getMethod(rule);
            return (ParseTree) method.invoke(parser);
        } catch (NoSuchMethodException | SecurityException | IllegalAccessException | IllegalArgumentException | InvocationTargetException e) {
            e.printStackTrace();
        }
        return null;       
    }

    @Test
    @DisplayName("Parser should handle empty input")
    void testEmptyInput() {
        ParseTree tree = parseInput("");
        
        Assertions.assertThat(tree).isNotNull();
        Assertions.assertThat(parser.getNumberOfSyntaxErrors()).isEqualTo(0);
    }

    @Test
    @DisplayName("Parser should parse simple identifier")
    void testSimpleIdentifier() {
        ParseTree tree = parseInput("hello", "identifier");
        
        Assertions.assertThat(tree).isNotNull();
        Assertions.assertThat(parser.getNumberOfSyntaxErrors()).isEqualTo(0);
    }

    @ParameterizedTest
    @DisplayName("Parser should handle various literals")
    @ValueSource(strings = {
        "42",           // integer
        "3.14",         // float  
        "true",         // boolean
        "false",        // boolean
        "'hello'",      // string
        "\"world\""     // string
    })
    void testLiterals(String literal) {
        ParseTree tree = parseInput(literal, "literal");
        
        Assertions.assertThat(tree).isNotNull();
        Assertions.assertThat(parser.getNumberOfSyntaxErrors()).isEqualTo(0);
    }

    @ParameterizedTest
    @DisplayName("Parser should handle various block types")
    @ValueSource(strings = {
            "{ x = 5;\n \"code block 1\"; }",
            "{= x = 5;\n \"code block 2\"; =}",
            "[| text block 1 |]",
            "[/ text block 2 |]",
            "[| text block 3 /]",
            "[/ text block 4 /]",
            "[`` literal block } { |] \\ [| ``]"
    })
    void testBlock(String block) {
        ParseTree tree = parseInput(block, "block");
        Assertions.assertThat(tree).isNotNull();
        Assertions.assertThat(parser.getNumberOfSyntaxErrors()).isEqualTo(0);
    }

    @Test
    @DisplayName("Parser should handle element definition")
    void testElementDefinition() {
        String input = "x = 42";
        ParseTree tree = parseInput(input, "elementDefinition");
        
        Assertions.assertThat(tree).isNotNull();
        Assertions.assertThat(parser.getNumberOfSyntaxErrors()).isEqualTo(0);
    }

    @Test
    @DisplayName("Parser should report syntax errors for invalid input")
    void testSyntaxError() {
        // Use clearly invalid syntax
        ParseTree tree = parseInput("@#$%^&*");
        
        Assertions.assertThat(tree).isNotNull(); // Tree is still created
        Assertions.assertThat(parser.getNumberOfSyntaxErrors()).isGreaterThan(0);
    }
}