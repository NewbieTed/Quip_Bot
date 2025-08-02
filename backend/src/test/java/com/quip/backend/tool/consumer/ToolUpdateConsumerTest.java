package com.quip.backend.tool.consumer;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.quip.backend.redis.service.RedisService;
import com.quip.backend.tool.model.ToolUpdateMessage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * Unit tests for ToolUpdateConsumer.
 * Tests message consumption, validation, and error handling with full mutation coverage.
 */
@ExtendWith(MockitoExtension.class)
class ToolUpdateConsumerTest {

    @Mock
    private RedisService redisService;

    @Mock
    private ObjectMapper objectMapper;

    private ToolUpdateConsumer consumer;

    @BeforeEach
    void setUp() {
        consumer = new ToolUpdateConsumer(redisService, objectMapper);
        
        // Set configuration properties
        ReflectionTestUtils.setField(consumer, "consumerEnabled", true);
        ReflectionTestUtils.setField(consumer, "pollingTimeoutSeconds", 5L);
    }

    @Test
    void testConsumerInitialization() {
        assertFalse(consumer.isRunning());
        assertTrue(consumer.isEnabled());
    }

    @Test
    void testConsumerDisabled() {
        ReflectionTestUtils.setField(consumer, "consumerEnabled", false);
        
        consumer.startConsumer();
        
        assertFalse(consumer.isRunning());
        assertFalse(consumer.isEnabled());
    }

    @Test
    void testConsumerEnabledStartsSuccessfully() {
        consumer.startConsumer();
        
        assertTrue(consumer.isRunning());
        assertTrue(consumer.isEnabled());
        
        // Verify executor service and task are created
        ExecutorService executorService = (ExecutorService) ReflectionTestUtils.getField(consumer, "executorService");
        CompletableFuture<Void> consumerTask = (CompletableFuture<Void>) ReflectionTestUtils.getField(consumer, "consumerTask");
        
        assertNotNull(executorService);
        assertNotNull(consumerTask);
        
        consumer.stopConsumer();
    }

    @Test
    void testValidMessageProcessing() throws Exception {
        // Create a valid test message
        ToolUpdateMessage testMessage = ToolUpdateMessage.builder()
                .messageId("test-message-123")
                .timestamp(OffsetDateTime.now())
                .addedTools(List.of("new-tool"))
                .removedTools(List.of("old-tool"))
                .source("agent")
                .build();

        String messageJson = "{\"messageId\":\"test-message-123\",\"timestamp\":\"2025-01-28T10:30:00Z\",\"addedTools\":[\"new-tool\"],\"removedTools\":[\"old-tool\"],\"source\":\"agent\"}";

        when(objectMapper.readValue(messageJson, ToolUpdateMessage.class))
                .thenReturn(testMessage);

        // Use reflection to call the private processMessage method
        ReflectionTestUtils.invokeMethod(consumer, "processMessage", messageJson);

        verify(objectMapper).readValue(messageJson, ToolUpdateMessage.class);
    }

    @Test
    void testValidMessageWithOnlyAddedTools() throws Exception {
        ToolUpdateMessage testMessage = ToolUpdateMessage.builder()
                .messageId("test-message-456")
                .timestamp(OffsetDateTime.now())
                .addedTools(List.of("tool1", "tool2"))
                .removedTools(List.of())
                .source("agent")
                .build();

        String messageJson = "{\"messageId\":\"test-message-456\"}";

        when(objectMapper.readValue(messageJson, ToolUpdateMessage.class))
                .thenReturn(testMessage);

        ReflectionTestUtils.invokeMethod(consumer, "processMessage", messageJson);

        verify(objectMapper).readValue(messageJson, ToolUpdateMessage.class);
    }

    @Test
    void testValidMessageWithOnlyRemovedTools() throws Exception {
        ToolUpdateMessage testMessage = ToolUpdateMessage.builder()
                .messageId("test-message-789")
                .timestamp(OffsetDateTime.now())
                .addedTools(List.of())
                .removedTools(List.of("old-tool1", "old-tool2"))
                .source("agent")
                .build();

        String messageJson = "{\"messageId\":\"test-message-789\"}";

        when(objectMapper.readValue(messageJson, ToolUpdateMessage.class))
                .thenReturn(testMessage);

        ReflectionTestUtils.invokeMethod(consumer, "processMessage", messageJson);

        verify(objectMapper).readValue(messageJson, ToolUpdateMessage.class);
    }

    @Test
    void testInvalidJsonHandling() throws Exception {
        String invalidJson = "invalid-json";

        when(objectMapper.readValue(invalidJson, ToolUpdateMessage.class))
                .thenThrow(new JsonProcessingException("Invalid JSON") {});

        // Use reflection to call the private processMessage method
        ReflectionTestUtils.invokeMethod(consumer, "processMessage", invalidJson);

        verify(objectMapper).readValue(invalidJson, ToolUpdateMessage.class);
    }

    @Test
    void testUnexpectedExceptionInProcessMessage() throws Exception {
        String messageJson = "{\"messageId\":\"test\"}";

        when(objectMapper.readValue(messageJson, ToolUpdateMessage.class))
                .thenThrow(new RuntimeException("Unexpected error"));

        ReflectionTestUtils.invokeMethod(consumer, "processMessage", messageJson);

        verify(objectMapper).readValue(messageJson, ToolUpdateMessage.class);
    }

    @Test
    void testInvalidMessageSkipped() throws Exception {
        ToolUpdateMessage invalidMessage = ToolUpdateMessage.builder()
                .messageId(null)
                .timestamp(OffsetDateTime.now())
                .addedTools(List.of())
                .removedTools(List.of())
                .source("agent")
                .build();

        String messageJson = "{\"messageId\":null}";

        when(objectMapper.readValue(messageJson, ToolUpdateMessage.class))
                .thenReturn(invalidMessage);

        ReflectionTestUtils.invokeMethod(consumer, "processMessage", messageJson);

        verify(objectMapper).readValue(messageJson, ToolUpdateMessage.class);
    }

    @Test
    void testMessageValidation_NullMessage() {
        boolean result = (Boolean) ReflectionTestUtils.invokeMethod(consumer, "isValidMessage", (Object) null);
        assertFalse(result);
    }

    @Test
    void testMessageValidation_NullMessageId() {
        ToolUpdateMessage message = ToolUpdateMessage.builder()
                .messageId(null)
                .timestamp(OffsetDateTime.now())
                .addedTools(List.of())
                .removedTools(List.of())
                .source("agent")
                .build();

        boolean result = (Boolean) ReflectionTestUtils.invokeMethod(consumer, "isValidMessage", message);
        assertFalse(result);
    }

    @Test
    void testMessageValidation_EmptyMessageId() {
        ToolUpdateMessage message = ToolUpdateMessage.builder()
                .messageId("")
                .timestamp(OffsetDateTime.now())
                .addedTools(List.of())
                .removedTools(List.of())
                .source("agent")
                .build();

        boolean result = (Boolean) ReflectionTestUtils.invokeMethod(consumer, "isValidMessage", message);
        assertFalse(result);
    }

    @Test
    void testMessageValidation_BlankMessageId() {
        ToolUpdateMessage message = ToolUpdateMessage.builder()
                .messageId("   ")
                .timestamp(OffsetDateTime.now())
                .addedTools(List.of())
                .removedTools(List.of())
                .source("agent")
                .build();

        boolean result = (Boolean) ReflectionTestUtils.invokeMethod(consumer, "isValidMessage", message);
        assertFalse(result);
    }

    @Test
    void testMessageValidation_NullTimestamp() {
        ToolUpdateMessage message = ToolUpdateMessage.builder()
                .messageId("test-123")
                .timestamp(null)
                .addedTools(List.of())
                .removedTools(List.of())
                .source("agent")
                .build();

        boolean result = (Boolean) ReflectionTestUtils.invokeMethod(consumer, "isValidMessage", message);
        assertFalse(result);
    }

    @Test
    void testMessageValidation_NullSource() {
        ToolUpdateMessage message = ToolUpdateMessage.builder()
                .messageId("test-123")
                .timestamp(OffsetDateTime.now())
                .addedTools(List.of())
                .removedTools(List.of())
                .source(null)
                .build();

        boolean result = (Boolean) ReflectionTestUtils.invokeMethod(consumer, "isValidMessage", message);
        assertFalse(result);
    }

    @Test
    void testMessageValidation_EmptySource() {
        ToolUpdateMessage message = ToolUpdateMessage.builder()
                .messageId("test-123")
                .timestamp(OffsetDateTime.now())
                .addedTools(List.of())
                .removedTools(List.of())
                .source("")
                .build();

        boolean result = (Boolean) ReflectionTestUtils.invokeMethod(consumer, "isValidMessage", message);
        assertFalse(result);
    }

    @Test
    void testMessageValidation_BlankSource() {
        ToolUpdateMessage message = ToolUpdateMessage.builder()
                .messageId("test-123")
                .timestamp(OffsetDateTime.now())
                .addedTools(List.of())
                .removedTools(List.of())
                .source("   ")
                .build();

        boolean result = (Boolean) ReflectionTestUtils.invokeMethod(consumer, "isValidMessage", message);
        assertFalse(result);
    }

    @Test
    void testMessageValidation_NullAddedTools() {
        ToolUpdateMessage message = ToolUpdateMessage.builder()
                .messageId("test-123")
                .timestamp(OffsetDateTime.now())
                .addedTools(null)
                .removedTools(List.of())
                .source("agent")
                .build();

        boolean result = (Boolean) ReflectionTestUtils.invokeMethod(consumer, "isValidMessage", message);
        assertFalse(result);
    }

    @Test
    void testMessageValidation_NullRemovedTools() {
        ToolUpdateMessage message = ToolUpdateMessage.builder()
                .messageId("test-123")
                .timestamp(OffsetDateTime.now())
                .addedTools(List.of())
                .removedTools(null)
                .source("agent")
                .build();

        boolean result = (Boolean) ReflectionTestUtils.invokeMethod(consumer, "isValidMessage", message);
        assertFalse(result);
    }

    @Test
    void testMessageValidation_NoChanges() {
        ToolUpdateMessage message = ToolUpdateMessage.builder()
                .messageId("test-123")
                .timestamp(OffsetDateTime.now())
                .addedTools(List.of())
                .removedTools(List.of())
                .source("agent")
                .build();

        boolean result = (Boolean) ReflectionTestUtils.invokeMethod(consumer, "isValidMessage", message);
        assertFalse(result);
    }

    @Test
    void testMessageValidation_InvalidToolNames() {
        ToolUpdateMessage message = ToolUpdateMessage.builder()
                .messageId("test-123")
                .timestamp(OffsetDateTime.now())
                .addedTools(List.of("invalid tool name!"))
                .removedTools(List.of())
                .source("agent")
                .build();

        boolean result = (Boolean) ReflectionTestUtils.invokeMethod(consumer, "isValidMessage", message);
        assertFalse(result);
    }

    @Test
    void testMessageValidation_ValidMessage() {
        ToolUpdateMessage message = ToolUpdateMessage.builder()
                .messageId("test-123")
                .timestamp(OffsetDateTime.now())
                .addedTools(List.of("new-tool"))
                .removedTools(List.of())
                .source("agent")
                .build();

        boolean result = (Boolean) ReflectionTestUtils.invokeMethod(consumer, "isValidMessage", message);
        assertTrue(result);
    }

    @Test
    void testConsumerLifecycle() {
        // Test starting the consumer
        consumer.startConsumer();
        assertTrue(consumer.isRunning());

        // Test stopping the consumer
        consumer.stopConsumer();
        // The running flag should be set to false
        assertFalse(consumer.isRunning());
    }

    @Test
    void testStopConsumerWhenDisabled() {
        ReflectionTestUtils.setField(consumer, "consumerEnabled", false);
        
        // Should not throw exception when stopping disabled consumer
        consumer.stopConsumer();
        
        assertFalse(consumer.isRunning());
    }

    @Test
    void testStopConsumerWhenNotRunning() {
        // Consumer is not started, so running should be false
        assertFalse(consumer.isRunning());
        
        // Should not throw exception when stopping non-running consumer
        consumer.stopConsumer();
        
        assertFalse(consumer.isRunning());
    }

    @Test
    void testStopConsumerWithNullTask() {
        consumer.startConsumer();
        
        // Set consumerTask to null to test null check
        ReflectionTestUtils.setField(consumer, "consumerTask", null);
        
        // Should not throw exception
        consumer.stopConsumer();
        
        assertFalse(consumer.isRunning());
    }

    @Test
    void testStopConsumerWithNullExecutorService() {
        consumer.startConsumer();
        
        // Set executorService to null to test null check
        ReflectionTestUtils.setField(consumer, "executorService", null);
        
        // Should not throw exception
        consumer.stopConsumer();
        
        assertFalse(consumer.isRunning());
    }

    @Test
    void testStopConsumerWithInterruptedException() throws InterruptedException {
        consumer.startConsumer();
        
        // Mock executor service that throws InterruptedException
        ExecutorService mockExecutorService = mock(ExecutorService.class);
        when(mockExecutorService.awaitTermination(anyLong(), any(TimeUnit.class)))
                .thenThrow(new InterruptedException("Test interruption"));
        
        ReflectionTestUtils.setField(consumer, "executorService", mockExecutorService);
        
        // Should handle InterruptedException gracefully
        consumer.stopConsumer();
        
        verify(mockExecutorService).shutdownNow();
        assertFalse(consumer.isRunning());
    }

    @Test
    void testStopConsumerWithExecutorServiceThatDoesNotTerminate() throws InterruptedException {
        consumer.startConsumer();
        
        // Mock executor service that doesn't terminate within timeout
        ExecutorService mockExecutorService = mock(ExecutorService.class);
        when(mockExecutorService.awaitTermination(anyLong(), any(TimeUnit.class)))
                .thenReturn(false);
        
        ReflectionTestUtils.setField(consumer, "executorService", mockExecutorService);
        
        // Should call shutdownNow when awaitTermination returns false
        consumer.stopConsumer();
        
        verify(mockExecutorService).shutdownNow();
        assertFalse(consumer.isRunning());
    }

    @Test
    void testConsumeMessagesLoop() throws Exception {
        // This test verifies the consumeMessages method behavior
        // We'll simulate the polling loop by mocking Redis responses
        
        when(redisService.rpop(eq("tools:updates"), eq(String.class)))
                .thenReturn(null) // First call returns null (no message)
                .thenReturn("test-message") // Second call returns a message
                .thenReturn(null); // Third call returns null again
        
        ToolUpdateMessage testMessage = ToolUpdateMessage.builder()
                .messageId("test-123")
                .timestamp(OffsetDateTime.now())
                .addedTools(List.of("tool1"))
                .removedTools(List.of())
                .source("agent")
                .build();
        
        when(objectMapper.readValue("test-message", ToolUpdateMessage.class))
                .thenReturn(testMessage);
        
        // Start consumer and let it run briefly
        consumer.startConsumer();
        
        // Give it a moment to process
        Thread.sleep(100);
        
        consumer.stopConsumer();
        
        // Verify Redis was called
        verify(redisService, atLeastOnce()).rpop(eq("tools:updates"), eq(String.class));
    }

    @Test
    void testConsumeMessagesWithException() throws Exception {
        // Test exception handling in consumeMessages loop
        when(redisService.rpop(eq("tools:updates"), eq(String.class)))
                .thenThrow(new RuntimeException("Redis error"));
        
        consumer.startConsumer();
        
        // Give it a moment to process and handle the exception
        Thread.sleep(100);
        
        consumer.stopConsumer();
        
        // Verify Redis was called and exception was handled
        verify(redisService, atLeastOnce()).rpop(eq("tools:updates"), eq(String.class));
    }
}