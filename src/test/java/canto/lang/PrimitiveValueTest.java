package canto.lang;

import org.assertj.core.api.Assertions;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

/**
 * Tests for PrimitiveValue class.
 * Tests basic value handling and type operations.
 */
class PrimitiveValueTest {

    @Test
    @DisplayName("PrimitiveValue should handle string values")
    void testStringValue() {
        PrimitiveValue value = new PrimitiveValue("hello");
        
        Assertions.assertThat(value.getData()).isEqualTo("hello");
        Assertions.assertThat(value.getString()).isEqualTo("hello");
        Assertions.assertThat(value.getValueClass()).isEqualTo(String.class);
    }

    @Test
    @DisplayName("PrimitiveValue should handle integer values")
    void testIntegerValue() {
        PrimitiveValue value = new PrimitiveValue(42);
        
        Assertions.assertThat(value.getData()).isEqualTo(42);
        Assertions.assertThat(value.getInt()).isEqualTo(42);
        Assertions.assertThat(value.getValueClass()).isEqualTo(int.class);
    }

    @Test
    @DisplayName("PrimitiveValue should handle double values")
    void testDoubleValue() {
        PrimitiveValue value = new PrimitiveValue(3.14);
        
        Assertions.assertThat(value.getData()).isEqualTo(3.14);
        Assertions.assertThat(value.getDouble()).isEqualTo(3.14);
        Assertions.assertThat(value.getValueClass()).isEqualTo(double.class);
    }

    @Test
    @DisplayName("PrimitiveValue should handle boolean values")
    void testBooleanValue() {
        PrimitiveValue value = new PrimitiveValue(true);
        
        Assertions.assertThat(value.getData()).isEqualTo(true);
        Assertions.assertThat(value.getBoolean()).isTrue();
        Assertions.assertThat(value.getValueClass()).isEqualTo(boolean.class);
    }

    @ParameterizedTest
    @DisplayName("PrimitiveValue should handle various numeric types")
    @ValueSource(ints = {-100, -1, 0, 1, 42, 100, Integer.MAX_VALUE})
    void testVariousIntegers(int testValue) {
        PrimitiveValue value = new PrimitiveValue(testValue);
        
        Assertions.assertThat(value.getInt()).isEqualTo(testValue);
        Assertions.assertThat(value.getData()).isEqualTo(testValue);
    }

    @Test
    @DisplayName("PrimitiveValue should handle null values")
    void testNullValue() {
        PrimitiveValue value = new PrimitiveValue(null);
        
        Assertions.assertThat(value.getData()).isNull();
        Assertions.assertThat(value.getValueClass()).isEqualTo(void.class);
    }

    @Test
    @DisplayName("PrimitiveValue toString should work correctly")
    void testToString() {
        PrimitiveValue stringValue = new PrimitiveValue("test");
        PrimitiveValue intValue = new PrimitiveValue(123);
        
        Assertions.assertThat(stringValue.toString()).contains("test");
        Assertions.assertThat(intValue.toString()).contains("123");
    }
}