package canto.lang;

import org.assertj.core.api.Assertions;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.junit.jupiter.params.provider.NullAndEmptySource;

/**
 * Tests for the NameNode class.
 * Tests name handling, validation, and comparison.
 */
class NameTest {

    @Test
    @DisplayName("NameNode should handle simple names")
    void testSimpleName() {
        NameNode name = new NameNode("hello");
        
        Assertions.assertThat(name.getName()).isEqualTo("hello");
        Assertions.assertThat(name.toString()).contains("hello");
    }

    @ParameterizedTest
    @DisplayName("NameNode should handle various valid identifiers")
    @ValueSource(strings = {
        "x", 
        "variable", 
        "myVariable", 
        "var123", 
        "a_b_c",
        "_private",
        "CONSTANT"
    })
    void testValidNames(String nameString) {
        NameNode name = new NameNode(nameString);
        
        Assertions.assertThat(name.getName()).isEqualTo(nameString);
        Assertions.assertThat(name).isNotNull();
    }

    @Test
    @DisplayName("NameNode should handle anonymous names")
    void testAnonymousName() {
        NameNode anonymousName = new NameNode(Name.ANONYMOUS);
        
        Assertions.assertThat(anonymousName.getName()).isEqualTo(Name.ANONYMOUS);
    }

    @Test
    @DisplayName("NameNode equality should work correctly")
    void testNameEquality() {
        NameNode name1 = new NameNode("test");
        NameNode name2 = new NameNode("test");
        NameNode name3 = new NameNode("different");
        
        Assertions.assertThat(name1).isEqualTo(name2);
        Assertions.assertThat(name1).isNotEqualTo(name3);
        Assertions.assertThat(name1.hashCode()).isEqualTo(name2.hashCode());
    }

    @Test
    @DisplayName("NameNode should handle special names correctly")
    void testSpecialNames() {
        // Test built-in special names if they exist
        Assertions.assertThat(Name.ANONYMOUS).isNotNull();
        Assertions.assertThat(Name.COUNT).isNotNull();
        Assertions.assertThat(Name.KEYS).isNotNull();
    }

    @ParameterizedTest
    @NullAndEmptySource
    @DisplayName("NameNode should handle null and empty strings appropriately")
    void testNullAndEmpty(String nameString) {
        // This test verifies how NameNode handles edge cases
        // Behavior will depend on the actual implementation
        if (nameString == null) {
            // Test null handling - might throw exception or handle gracefully
            Assertions.assertThatThrownBy(() -> new NameNode(nameString))
                .isInstanceOf(IllegalArgumentException.class);
        } else {
            // Empty string handling
            NameNode name = new NameNode(nameString);
            Assertions.assertThat(name.getName()).isEqualTo(nameString);
        }
    }
}