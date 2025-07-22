package com.quip.backend.common.exception;

import com.quip.backend.common.BaseTest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for {@link EntityNotFoundException}.
 * <p>
 * This test class validates the custom entity not found exception functionality.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("EntityNotFoundException Tests")
public class EntityNotFoundExceptionTest extends BaseTest {

    /**
     * Tests for the EntityNotFoundException constructors.
     * This nested class validates that the exception is properly created with
     * different message and cause combinations.
     */
    @Nested
    @DisplayName("Constructor Tests")
    class ConstructorTests {

        @Test
        @DisplayName("Should create exception with message")
        void shouldCreateException_WithMessage() {
            // Given
            String errorMessage = "Entity not found";

            // When
            EntityNotFoundException exception = new EntityNotFoundException(errorMessage);

            // Then
            assertNotNull(exception);
            assertEquals(errorMessage, exception.getMessage());
            assertNull(exception.getCause());
        }

        @Test
        @DisplayName("Should create exception with null message")
        void shouldCreateException_WithNullMessage() {
            // When
            EntityNotFoundException exception = new EntityNotFoundException(null);

            // Then
            assertNotNull(exception);
            assertNull(exception.getMessage());
            assertNull(exception.getCause());
        }

        @Test
        @DisplayName("Should create exception with empty message")
        void shouldCreateException_WithEmptyMessage() {
            // Given
            String emptyMessage = "";

            // When
            EntityNotFoundException exception = new EntityNotFoundException(emptyMessage);

            // Then
            assertNotNull(exception);
            assertEquals(emptyMessage, exception.getMessage());
            assertNull(exception.getCause());
        }

        @Test
        @DisplayName("Should create exception with message and cause")
        void shouldCreateException_WithMessageAndCause() {
            // Given
            String errorMessage = "Entity not found";
            Throwable cause = new RuntimeException("Database connection failed");

            // When
            EntityNotFoundException exception = new EntityNotFoundException(errorMessage, cause);

            // Then
            assertNotNull(exception);
            assertEquals(errorMessage, exception.getMessage());
            assertEquals(cause, exception.getCause());
        }

        @Test
        @DisplayName("Should create exception with null message and cause")
        void shouldCreateException_WithNullMessageAndCause() {
            // Given
            Throwable cause = new RuntimeException("Database error");

            // When
            EntityNotFoundException exception = new EntityNotFoundException(null, cause);

            // Then
            assertNotNull(exception);
            assertNull(exception.getMessage());
            assertEquals(cause, exception.getCause());
        }

        @Test
        @DisplayName("Should be instance of RuntimeException")
        void shouldBeInstanceOf_RuntimeException() {
            // Given
            EntityNotFoundException exception = new EntityNotFoundException("Test message");

            // When & Then
            assertInstanceOf(RuntimeException.class, exception);
        }
    }

    /**
     * Tests for the runtime behavior of EntityNotFoundException.
     * This nested class validates that the exception can be thrown and caught properly,
     * and that message and cause information is preserved during this process.
     */
    @Nested
    @DisplayName("Exception Behavior Tests")
    class ExceptionBehaviorTests {

        @Test
        @DisplayName("Should be throwable")
        void shouldBeThrowable() {
            // Given
            String errorMessage = "Entity not found error";

            // When & Then
            assertThrows(EntityNotFoundException.class, () -> {
                throw new EntityNotFoundException(errorMessage);
            });
        }

        @Test
        @DisplayName("Should preserve message when thrown and caught")
        void shouldPreserveMessage_WhenThrownAndCaught() {
            // Given
            String errorMessage = "Entity not found message";

            // When & Then
            EntityNotFoundException caughtException = assertThrows(EntityNotFoundException.class, () -> {
                throw new EntityNotFoundException(errorMessage);
            });

            assertEquals(errorMessage, caughtException.getMessage());
        }

        @Test
        @DisplayName("Should preserve cause when thrown and caught")
        void shouldPreserveCause_WhenThrownAndCaught() {
            // Given
            String errorMessage = "Entity not found";
            Throwable originalCause = new RuntimeException("Original cause");

            // When & Then
            EntityNotFoundException caughtException = assertThrows(EntityNotFoundException.class, () -> {
                throw new EntityNotFoundException(errorMessage, originalCause);
            });

            assertEquals(errorMessage, caughtException.getMessage());
            assertEquals(originalCause, caughtException.getCause());
        }
    }
}