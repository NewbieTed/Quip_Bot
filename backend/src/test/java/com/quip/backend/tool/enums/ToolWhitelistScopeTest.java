package com.quip.backend.tool.enums;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Test class for ToolWhitelistScope enum, focusing on comprehensive coverage
 * of the fromValue method to ensure all mutation tests are killed.
 */
class ToolWhitelistScopeTest {

    @Test
    void fromValue_shouldReturnGlobal_whenValueIsGlobal() {
        // Test exact match for GLOBAL
        ToolWhitelistScope result = ToolWhitelistScope.fromValue("global");
        assertEquals(ToolWhitelistScope.GLOBAL, result);
    }

    @Test
    void fromValue_shouldReturnServer_whenValueIsServer() {
        // Test exact match for SERVER
        ToolWhitelistScope result = ToolWhitelistScope.fromValue("server");
        assertEquals(ToolWhitelistScope.SERVER, result);
    }

    @Test
    void fromValue_shouldReturnConversation_whenValueIsConversation() {
        // Test exact match for CONVERSATION
        ToolWhitelistScope result = ToolWhitelistScope.fromValue("conversation");
        assertEquals(ToolWhitelistScope.CONVERSATION, result);
    }

    @Test
    void fromValue_shouldThrowIllegalArgumentException_whenValueIsNull() {
        // Test null input - this kills mutations that might skip null checks
        IllegalArgumentException exception = assertThrows(
            IllegalArgumentException.class,
            () -> ToolWhitelistScope.fromValue(null)
        );
        assertEquals("Unknown ToolWhitelistScope value: null", exception.getMessage());
    }

    @Test
    void fromValue_shouldThrowIllegalArgumentException_whenValueIsEmpty() {
        // Test empty string - kills mutations that might treat empty as valid
        IllegalArgumentException exception = assertThrows(
            IllegalArgumentException.class,
            () -> ToolWhitelistScope.fromValue("")
        );
        assertEquals("Unknown ToolWhitelistScope value: ", exception.getMessage());
    }

    @Test
    void fromValue_shouldThrowIllegalArgumentException_whenValueIsWhitespace() {
        // Test whitespace-only string - kills mutations that might trim input
        IllegalArgumentException exception = assertThrows(
            IllegalArgumentException.class,
            () -> ToolWhitelistScope.fromValue("   ")
        );
        assertEquals("Unknown ToolWhitelistScope value:    ", exception.getMessage());
    }

    @ParameterizedTest
    @ValueSource(strings = {
        "GLOBAL",      // Wrong case
        "Global",      // Wrong case
        "SERVER",      // Wrong case
        "Server",      // Wrong case
        "CONVERSATION", // Wrong case
        "Conversation", // Wrong case
        "invalid",     // Invalid value
        "unknown",     // Invalid value
        "globall",     // Typo
        "serve",       // Partial match
        "conversatio", // Partial match
        "global ",     // Trailing space
        " global",     // Leading space
        "glob al"      // Space in middle
    })
    void fromValue_shouldThrowIllegalArgumentException_whenValueIsInvalid(String invalidValue) {
        // Parameterized test for various invalid inputs
        // This kills mutations that might accept partial matches or case variations
        IllegalArgumentException exception = assertThrows(
            IllegalArgumentException.class,
            () -> ToolWhitelistScope.fromValue(invalidValue)
        );
        assertEquals("Unknown ToolWhitelistScope value: " + invalidValue, exception.getMessage());
    }

    @Test
    void fromValue_shouldUseExactStringComparison() {
        // Test that ensures exact string comparison is used
        // This kills mutations that might use contains() or startsWith()
        assertThrows(IllegalArgumentException.class, 
            () -> ToolWhitelistScope.fromValue("globalx"));
        assertThrows(IllegalArgumentException.class, 
            () -> ToolWhitelistScope.fromValue("xglobal"));
        assertThrows(IllegalArgumentException.class, 
            () -> ToolWhitelistScope.fromValue("serverx"));
        assertThrows(IllegalArgumentException.class, 
            () -> ToolWhitelistScope.fromValue("xserver"));
        assertThrows(IllegalArgumentException.class, 
            () -> ToolWhitelistScope.fromValue("conversationx"));
        assertThrows(IllegalArgumentException.class, 
            () -> ToolWhitelistScope.fromValue("xconversation"));
    }

    @Test
    void fromValue_shouldIterateThroughAllValues() {
        // Test that verifies all enum values are checked
        // This kills mutations that might short-circuit the loop
        
        // Verify each valid value works
        assertNotNull(ToolWhitelistScope.fromValue("global"));
        assertNotNull(ToolWhitelistScope.fromValue("server"));
        assertNotNull(ToolWhitelistScope.fromValue("conversation"));
        
        // Verify that an invalid value after checking all valid ones still throws
        assertThrows(IllegalArgumentException.class, 
            () -> ToolWhitelistScope.fromValue("afterall"));
    }

    @Test
    void fromValue_shouldReturnCorrectEnumConstant() {
        // Verify that the correct enum constant is returned, not just any enum
        // This kills mutations that might return wrong enum values
        
        ToolWhitelistScope global = ToolWhitelistScope.fromValue("global");
        ToolWhitelistScope server = ToolWhitelistScope.fromValue("server");
        ToolWhitelistScope conversation = ToolWhitelistScope.fromValue("conversation");
        
        // Verify they are different instances
        assertNotEquals(global, server);
        assertNotEquals(global, conversation);
        assertNotEquals(server, conversation);
        
        // Verify correct values
        assertEquals("global", global.getValue());
        assertEquals("server", server.getValue());
        assertEquals("conversation", conversation.getValue());
    }

    @Test
    void fromValue_shouldThrowExceptionWithCorrectMessage() {
        // Test that the exception message is exactly as expected
        // This kills mutations that might change the exception message format
        
        String invalidValue = "invalidScope";
        IllegalArgumentException exception = assertThrows(
            IllegalArgumentException.class,
            () -> ToolWhitelistScope.fromValue(invalidValue)
        );
        
        String expectedMessage = "Unknown ToolWhitelistScope value: " + invalidValue;
        assertEquals(expectedMessage, exception.getMessage());
        
        // Test with different invalid values to ensure message format is consistent
        String anotherInvalid = "anotherInvalid";
        IllegalArgumentException anotherException = assertThrows(
            IllegalArgumentException.class,
            () -> ToolWhitelistScope.fromValue(anotherInvalid)
        );
        
        String expectedMessage2 = "Unknown ToolWhitelistScope value: " + anotherInvalid;
        assertEquals(expectedMessage2, anotherException.getMessage());
    }
}