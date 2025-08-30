package com.quip.backend.assistant.handler;

import com.quip.backend.assistant.model.InterruptMessage;
import com.quip.backend.assistant.service.AssistantConversationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.OffsetDateTime;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class InterruptMessageHandlerTest {

    @Mock
    private AssistantConversationService assistantConversationService;

    @InjectMocks
    private InterruptMessageHandler handler;

    private InterruptMessage validMessage;

    @BeforeEach
    void setUp() {
        validMessage = InterruptMessage.builder()
                .messageId("test-message-id")
                .timestamp(OffsetDateTime.now())
                .memberId(123L)
                .serverId(456L)
                .source("agent")
                .interruptedToolName("test-tool")
                .reason("user_message")
                .build();
    }

    @Test
    void handleInterrupt_WithValidMessage_ShouldReturnTrue() {
        // Arrange
        when(assistantConversationService.markAsInterrupted(123L, 456L, 123L))
                .thenReturn(true);

        // Act
        boolean result = handler.handleInterrupt(validMessage);

        // Assert
        assertTrue(result);
        verify(assistantConversationService).markAsInterrupted(123L, 456L, 123L);
    }

    @Test
    void handleInterrupt_WithNullMessage_ShouldReturnFalse() {
        // Act
        boolean result = handler.handleInterrupt(null);

        // Assert
        assertFalse(result);
        verifyNoInteractions(assistantConversationService);
    }

    @Test
    void handleInterrupt_WithInvalidMessage_ShouldReturnFalse() {
        // Arrange
        InterruptMessage invalidMessage = InterruptMessage.builder()
                .messageId(null) // Invalid - null messageId
                .timestamp(OffsetDateTime.now())
                .memberId(123L)
                .serverId(456L)
                .source("agent")
                .build();

        // Act
        boolean result = handler.handleInterrupt(invalidMessage);

        // Assert
        assertFalse(result);
        verifyNoInteractions(assistantConversationService);
    }

    @Test
    void handleInterrupt_WhenServiceFails_ShouldReturnFalse() {
        // Arrange
        when(assistantConversationService.markAsInterrupted(123L, 456L, 123L))
                .thenReturn(false);

        // Act
        boolean result = handler.handleInterrupt(validMessage);

        // Assert
        assertFalse(result);
        verify(assistantConversationService).markAsInterrupted(123L, 456L, 123L);
    }

    @Test
    void handleInterrupt_WhenServiceThrowsException_ShouldReturnFalse() {
        // Arrange
        when(assistantConversationService.markAsInterrupted(123L, 456L, 123L))
                .thenThrow(new RuntimeException("Database error"));

        // Act
        boolean result = handler.handleInterrupt(validMessage);

        // Assert
        assertFalse(result);
        verify(assistantConversationService).markAsInterrupted(123L, 456L, 123L);
    }
}