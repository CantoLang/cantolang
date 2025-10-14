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
    @DisplayName("Parser should handle various instantiations")
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
    void testInstantiation(String input) {
        ParseTree tree = parseInput(input, "instantiation");
        
        Assertions.assertThat(tree).isNotNull();
        Assertions.assertThat(parser.getNumberOfSyntaxErrors()).isEqualTo(0);
    }

    @ParameterizedTest
    @DisplayName("Parser should handle various expressions")
    @ValueSource(strings = {
        "(1+x)",
        "(x+1)",
        "1",
        "(x)",
        "x + 5",
        "(x - 5)",
        "x * y - z / 5",
        "a && b",
        "a ^ b",
        "a % b",
        "a >> b",
        "a << b + 2"
    })
    void testExpression(String input) {
        ParseTree tree = parseInput(input, "expression");
        
        Assertions.assertThat(tree).isNotNull();
        Assertions.assertThat(parser.getNumberOfSyntaxErrors()).isEqualTo(0);
    }

    @ParameterizedTest
    @DisplayName("Parser should handle various block types")
    @ValueSource(strings = {

            "{ x = 5 [| nested block |] x; }",
            "{ x = 5;\n \"code block 1\"; }",
            "{= x = 5;\n \"code block 2\"; =}",
            "[| text block 1 |]",
            "[/ text block 2 |]",
            "[| text block 3 /]",
            "[/ text block 4 /]",
            "[`` literal block } { |] \\ [| ``]",
            "[| text {= z; =} text |]",
            "{ q = 50 [| first nested |] x = 100 [| second nested |] q; x; }",
            "{ y = 'z' [| nested {= y; =} nested |] x = y }"
    
    })
    void testBlock(String block) {
        ParseTree tree = parseInput(block, "block");
        Assertions.assertThat(tree).isNotNull();
        Assertions.assertThat(parser.getNumberOfSyntaxErrors()).isEqualTo(0);
    }

    @ParameterizedTest
    @DisplayName("Parser should handle various block definitions")
    @ValueSource(strings = {
            "d1 { int x = 5  x; }",
            "d2 {= float x = 5.0  x;  =}",
            "d3 [| text block 1 |]",
            "d4(x) { x; }",
            "int d5(int x) { x; }",
            "d6 [`` literal block } { |] \\ [| ``]",
            "d7(z) [| text {= z; =} block 2 |]"
    })
    void testBlockDefinition(String input) {
        ParseTree tree = parseInput(input, "blockDefinition");
        
        Assertions.assertThat(tree).isNotNull();
        Assertions.assertThat(parser.getNumberOfSyntaxErrors()).isEqualTo(0);
    }

    @ParameterizedTest
    @DisplayName("Parser should handle various conditionals")
    @ValueSource(strings = {
            "if (x) { x; }",
            "if x { x; } else { z; }",
            "if y > 0 { y; } else [| zero |]",
            "if (y == 'hello') [| x |] else if (y == 'goodbye') [| bye |]",
            "if f(x) > 0 { x; } else if (y + z < 100) { z; } else if x - y + z >= 0 { y; } else { w; }"
    })
    void testConditional(String input) {
        ParseTree tree = parseInput(input, "conditional");
        
        Assertions.assertThat(tree).isNotNull();
        Assertions.assertThat(parser.getNumberOfSyntaxErrors()).isEqualTo(0);
    }

    @ParameterizedTest
    @DisplayName("Parser should handle various element definitions")
    @ValueSource(strings = {
            "x = 42",
            "y(z) = z",
            "w = f(5)",
            "v = \"string\""
    })
    void testNamedElementDefinition(String input) {
        ParseTree tree = parseInput(input, "namedElementDefinition");
        
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