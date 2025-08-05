package com.quip.backend.tool.handler;

import com.quip.backend.tool.model.ToolUpdateMessage;
import com.quip.backend.tool.service.ToolService;
import com.quip.backend.tool.monitoring.ToolSyncMetricsService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.OffsetDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for ToolUpdateMessageHandler.
 * <p>
 * Tests cover message validation, processing logic, error handling,
 * and edge cases for tool update message handling.
 * </p>
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("ToolUpdateMessageHandler Tests")
class ToolUpdateMessageHandlerTest {

    @Mock
    private ToolService toolService;

    @Mock
    private ToolSyncMetricsService metricsService;

    @InjectMocks
    private ToolUpdateMessageHandler handler;

    private ToolUpdateMessage validMessage;

    @BeforeEach
    void setUp() {
        validMessage = ToolUpdateMessage.builder()
                .messageId("test-message-123")
                .timestamp(OffsetDateTime.now().minusMinutes(5))
                .source("agent")
                .addedTools(Arrays.asList("new-tool-1", "new-tool-2"))
                .removedTools(Arrays.asList("old-tool-1"))
                .build();
    }

    @Nested
    @DisplayName("Message Validation Tests")
    class MessageValidationTests {

        @Test
        @DisplayName("Should validate a properly formatted message")
        void shouldValidateValidMessage() {
            boolean result = handler.validateMessage(validMessage);
            assertThat(result).isTrue();
        }

        @Test
        @DisplayName("Should reject null message")
        void shouldRejectNullMessage() {
            boolean result = handler.validateMessage(null);
            assertThat(result).isFalse();
        }

        @Test
        @DisplayName("Should reject message with null messageId")
        void shouldRejectNullMessageId() {
            validMessage.setMessageId(null);
            boolean result = handler.validateMessage(validMessage);
            assertThat(result).isFalse();
        }

        @Test
        @DisplayName("Should reject message with empty messageId")
        void shouldRejectEmptyMessageId() {
            validMessage.setMessageId("   ");
            boolean result = handler.validateMessage(validMessage);
            assertThat(result).isFalse();
        }

        @Test
        @DisplayName("Should reject message with null timestamp")
        void shouldRejectNullTimestamp() {
            validMessage.setTimestamp(null);
            boolean result = handler.validateMessage(validMessage);
            assertThat(result).isFalse();
        }

        @Test
        @DisplayName("Should reject message with null source")
        void shouldRejectNullSource() {
            validMessage.setSource(null);
            boolean result = handler.validateMessage(validMessage);
            assertThat(result).isFalse();
        }

        @Test
        @DisplayName("Should reject message with empty source")
        void shouldRejectEmptySource() {
            validMessage.setSource("  ");
            boolean result = handler.validateMessage(validMessage);
            assertThat(result).isFalse();
        }

        @Test
        @DisplayName("Should reject message with null addedTools list")
        void shouldRejectNullAddedToolsList() {
            validMessage.setAddedTools(null);
            boolean result = handler.validateMessage(validMessage);
            assertThat(result).isFalse();
        }

        @Test
        @DisplayName("Should reject message with null removedTools list")
        void shouldRejectNullRemovedToolsList() {
            validMessage.setRemovedTools(null);
            boolean result = handler.validateMessage(validMessage);
            assertThat(result).isFalse();
        }

        @Test
        @DisplayName("Should reject message with no changes")
        void shouldRejectMessageWithNoChanges() {
            validMessage.setAddedTools(Collections.emptyList());
            validMessage.setRemovedTools(Collections.emptyList());
            boolean result = handler.validateMessage(validMessage);
            assertThat(result).isFalse();
        }

        @Test
        @DisplayName("Should reject message with invalid tool names")
        void shouldRejectInvalidToolNames() {
            validMessage.setAddedTools(Arrays.asList("valid-tool", "invalid tool with spaces"));
            boolean result = handler.validateMessage(validMessage);
            assertThat(result).isFalse();
        }

        @Test
        @DisplayName("Should reject message that is too old")
        void shouldRejectOldMessage() {
            validMessage.setTimestamp(OffsetDateTime.now().minusMinutes(35)); // Older than 30 minutes
            boolean result = handler.validateMessage(validMessage);
            assertThat(result).isFalse();
        }

        @Test
        @DisplayName("Should reject message with future timestamp")
        void shouldRejectFutureMessage() {
            validMessage.setTimestamp(OffsetDateTime.now().plusMinutes(5));
            boolean result = handler.validateMessage(validMessage);
            assertThat(result).isFalse();
        }

        @Test
        @DisplayName("Should accept message with only added tools")
        void shouldAcceptMessageWithOnlyAddedTools() {
            validMessage.setRemovedTools(Collections.emptyList());
            boolean result = handler.validateMessage(validMessage);
            assertThat(result).isTrue();
        }

        @Test
        @DisplayName("Should accept message with only removed tools")
        void shouldAcceptMessageWithOnlyRemovedTools() {
            validMessage.setAddedTools(Collections.emptyList());
            boolean result = handler.validateMessage(validMessage);
            assertThat(result).isTrue();
        }

        @Test
        @DisplayName("Should accept valid tool names with underscores and hyphens")
        void shouldAcceptValidToolNames() {
            validMessage.setAddedTools(Arrays.asList("tool_with_underscores", "tool-with-hyphens", "tool123"));
            validMessage.setRemovedTools(Arrays.asList("UPPERCASE_TOOL", "mixedCase_Tool-123"));
            boolean result = handler.validateMessage(validMessage);
            assertThat(result).isTrue();
        }

        @Test
        @DisplayName("Should reject tool names with special characters")
        void shouldRejectToolNamesWithSpecialCharacters() {
            List<String> invalidToolNames = Arrays.asList(
                    "tool@domain.com",
                    "tool with spaces",
                    "tool.with.dots",
                    "tool/with/slashes",
                    "tool\\with\\backslashes",
                    "tool:with:colons",
                    "tool;with;semicolons",
                    "tool,with,commas",
                    "tool(with)parentheses",
                    "tool[with]brackets",
                    "tool{with}braces",
                    "tool+with+plus",
                    "tool=with=equals",
                    "tool?with?question",
                    "tool*with*asterisk",
                    "tool%with%percent",
                    "tool#with#hash",
                    "tool&with&ampersand",
                    "tool|with|pipe"
            );

            for (String invalidToolName : invalidToolNames) {
                validMessage.setAddedTools(Arrays.asList(invalidToolName));
                boolean result = handler.validateMessage(validMessage);
                assertThat(result)
                        .withFailMessage("Tool name '%s' should be rejected but was accepted", invalidToolName)
                        .isFalse();
            }
        }

        @Test
        @DisplayName("Should reject empty tool names")
        void shouldRejectEmptyToolNames() {
            validMessage.setAddedTools(Arrays.asList("valid-tool", "", "another-valid-tool"));
            boolean result = handler.validateMessage(validMessage);
            assertThat(result).isFalse();
        }

        @Test
        @DisplayName("Should reject null tool names in list")
        void shouldRejectNullToolNamesInList() {
            validMessage.setAddedTools(Arrays.asList("valid-tool", null, "another-valid-tool"));
            boolean result = handler.validateMessage(validMessage);
            assertThat(result).isFalse();
        }
    }

    @Nested
    @DisplayName("Message Processing Tests")
    class MessageProcessingTests {

        @Test
        @DisplayName("Should handle valid message successfully")
        void shouldHandleValidMessage() {
            boolean result = handler.handleToolUpdates(validMessage);
            assertThat(result).isTrue();
        }

        @Test
        @DisplayName("Should return false for null message")
        void shouldReturnFalseForNullMessage() {
            boolean result = handler.handleToolUpdates(null);
            assertThat(result).isFalse();
        }

        @Test
        @DisplayName("Should return false for invalid message")
        void shouldReturnFalseForInvalidMessage() {
            validMessage.setMessageId(null); // Make message invalid
            boolean result = handler.handleToolUpdates(validMessage);
            assertThat(result).isFalse();
        }

        @Test
        @DisplayName("Should process message with only added tools")
        void shouldProcessMessageWithOnlyAddedTools() {
            validMessage.setRemovedTools(Collections.emptyList());
            boolean result = handler.handleToolUpdates(validMessage);
            assertThat(result).isTrue();
        }

        @Test
        @DisplayName("Should process message with only removed tools")
        void shouldProcessMessageWithOnlyRemovedTools() {
            validMessage.setAddedTools(Collections.emptyList());
            boolean result = handler.handleToolUpdates(validMessage);
            assertThat(result).isTrue();
        }

        @Test
        @DisplayName("Should process message with both added and removed tools")
        void shouldProcessMessageWithBothAddedAndRemovedTools() {
            boolean result = handler.handleToolUpdates(validMessage);
            assertThat(result).isTrue();
        }

        @Test
        @DisplayName("Should handle large number of tools")
        void shouldHandleLargeNumberOfTools() {
            // Create lists with many tools
            List<String> manyAddedTools = Arrays.asList(
                    "tool-1", "tool-2", "tool-3", "tool-4", "tool-5",
                    "tool-6", "tool-7", "tool-8", "tool-9", "tool-10"
            );
            List<String> manyRemovedTools = Arrays.asList(
                    "old-tool-1", "old-tool-2", "old-tool-3", "old-tool-4", "old-tool-5"
            );

            validMessage.setAddedTools(manyAddedTools);
            validMessage.setRemovedTools(manyRemovedTools);

            boolean result = handler.handleToolUpdates(validMessage);
            assertThat(result).isTrue();
        }
    }

    @Nested
    @DisplayName("Edge Cases and Error Handling")
    class EdgeCasesAndErrorHandlingTests {

        @Test
        @DisplayName("Should handle message at exactly 30 minutes old")
        void shouldHandleMessageAtExactly30MinutesOld() {
            validMessage.setTimestamp(OffsetDateTime.now().minusMinutes(30));
            boolean result = handler.validateMessage(validMessage);
            assertThat(result).isTrue();
        }

        @Test
        @DisplayName("Should reject message at 31 minutes old")
        void shouldRejectMessageAt31MinutesOld() {
            validMessage.setTimestamp(OffsetDateTime.now().minusMinutes(31));
            boolean result = handler.validateMessage(validMessage);
            assertThat(result).isFalse();
        }

        @Test
        @DisplayName("Should handle message with very recent timestamp")
        void shouldHandleMessageWithVeryRecentTimestamp() {
            validMessage.setTimestamp(OffsetDateTime.now().minusSeconds(1));
            boolean result = handler.validateMessage(validMessage);
            assertThat(result).isTrue();
        }

        @Test
        @DisplayName("Should handle duplicate tool names in same list")
        void shouldHandleDuplicateToolNames() {
            validMessage.setAddedTools(Arrays.asList("duplicate-tool", "duplicate-tool", "unique-tool"));
            boolean result = handler.handleToolUpdates(validMessage);
            assertThat(result).isTrue();
        }

        @Test
        @DisplayName("Should handle same tool in both added and removed lists")
        void shouldHandleSameToolInBothLists() {
            validMessage.setAddedTools(Arrays.asList("conflicting-tool", "added-tool"));
            validMessage.setRemovedTools(Arrays.asList("conflicting-tool", "removed-tool"));
            boolean result = handler.handleToolUpdates(validMessage);
            assertThat(result).isTrue();
        }

        @Test
        @DisplayName("Should handle very long tool names")
        void shouldHandleVeryLongToolNames() {
            String longToolName = "a".repeat(100); // 100 character tool name
            validMessage.setAddedTools(Arrays.asList(longToolName));
            boolean result = handler.validateMessage(validMessage);
            assertThat(result).isTrue();
        }

        @Test
        @DisplayName("Should handle single character tool names")
        void shouldHandleSingleCharacterToolNames() {
            validMessage.setAddedTools(Arrays.asList("a", "b", "1", "2"));
            boolean result = handler.validateMessage(validMessage);
            assertThat(result).isTrue();
        }

        @Test
        @DisplayName("Should handle tool names with only numbers")
        void shouldHandleToolNamesWithOnlyNumbers() {
            validMessage.setAddedTools(Arrays.asList("123", "456789"));
            boolean result = handler.validateMessage(validMessage);
            assertThat(result).isTrue();
        }

        @Test
        @DisplayName("Should handle tool names with only underscores and hyphens")
        void shouldHandleToolNamesWithOnlyUnderscoresAndHyphens() {
            validMessage.setAddedTools(Arrays.asList("_", "-", "__", "--", "_-_", "-_-"));
            boolean result = handler.validateMessage(validMessage);
            assertThat(result).isTrue();
        }
    }

    @Nested
    @DisplayName("Message Content Validation")
    class MessageContentValidationTests {

        @Test
        @DisplayName("Should validate message with different source values")
        void shouldValidateMessageWithDifferentSourceValues() {
            String[] validSources = {"agent", "AGENT", "Agent", "test-agent", "agent_1", "agent-service"};
            
            for (String source : validSources) {
                validMessage.setSource(source);
                boolean result = handler.validateMessage(validMessage);
                assertThat(result)
                        .withFailMessage("Source '%s' should be valid", source)
                        .isTrue();
            }
        }

        @Test
        @DisplayName("Should validate message with different messageId formats")
        void shouldValidateMessageWithDifferentMessageIdFormats() {
            String[] validMessageIds = {
                    "uuid-123-456-789",
                    "message_123",
                    "msg-456",
                    "12345",
                    "abcdef",
                    "UPPERCASE_ID",
                    "mixedCase_ID-123"
            };
            
            for (String messageId : validMessageIds) {
                validMessage.setMessageId(messageId);
                boolean result = handler.validateMessage(validMessage);
                assertThat(result)
                        .withFailMessage("MessageId '%s' should be valid", messageId)
                        .isTrue();
            }
        }
    }
}