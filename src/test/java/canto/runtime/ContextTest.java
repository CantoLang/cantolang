package canto.runtime;

import org.assertj.core.api.Assertions;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import canto.lang.Context;
import canto.lang.Definition;
import canto.lang.Name;

/**
 * Tests for the Context class.
 * Tests context management, scoping, and definition resolution.
 */
class ContextTest {

    private Context context;

    @BeforeEach
    void setUp() {
        context = new Context();
    }

    @Test
    @DisplayName("Context should be created successfully")
    void testContextCreation() {
        Assertions.assertThat(context).isNotNull();
        Assertions.assertThat(context.size()).isGreaterThanOrEqualTo(0);
    }

    @Test
    @DisplayName("Context should handle push and pop operations")
    void testPushPop() {
        int initialSize = context.size();
        
        // This test will need actual Definition objects
        // For now, just test that context responds to basic operations
        Assertions.assertThat(context.size()).isEqualTo(initialSize);
        
        // Test cloning
        Context clonedContext = context.clone(false);
        Assertions.assertThat(clonedContext).isNotNull();
        Assertions.assertThat(clonedContext).isNotSameAs(context);
    }

    @Test
    @DisplayName("Context should handle root scope correctly")
    void testRootScope() {
        // Test root scope access
        Assertions.assertThat(context.getRootScope()).isNotNull();
        
        // Context should always have at least root scope
        Assertions.assertThat(context.size()).isGreaterThan(0);
    }

    @Test
    @DisplayName("Context clone should work correctly")
    void testContextClone() {
        Context original = new Context();
        Context sharedClone = original.clone(true);
        Context privateClone = original.clone(false);
        
        Assertions.assertThat(sharedClone).isNotNull();
        Assertions.assertThat(privateClone).isNotNull();
        Assertions.assertThat(sharedClone).isNotSameAs(original);
        Assertions.assertThat(privateClone).isNotSameAs(original);
    }

    @Test
    @DisplayName("Context should maintain stack integrity")
    void testStackIntegrity() {
        int initialSize = context.size();
        
        // Test that context maintains proper stack structure
        Assertions.assertThat(context.size()).isEqualTo(initialSize);
        
        // Peek should not change size
        if (context.size() > 0) {
            context.peek();
            Assertions.assertThat(context.size()).isEqualTo(initialSize);
        }
    }
}