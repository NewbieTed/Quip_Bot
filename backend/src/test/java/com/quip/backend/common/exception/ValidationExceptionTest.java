package com.quip.backend.common.exception;

import com.quip.backend.common.BaseTest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for {@link ValidationException}.
 * <p>
 * This test class validates the custom validation exception functionality.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("ValidationException Tests")
public class ValidationExceptionTest extends BaseTest {

    @Nested
    @DisplayName("Constructor Tests")
    class ConstructorTests {

        @Test
        @DisplayName("Should create exception with message")
        void shouldCreateException_WithMessage() {
            // Given
            String errorMessage = "Validation failed";

            // When
            ValidationException exception = new ValidationException(errorMessage);

            // Then
            assertNotNull(exception);
            assertEquals(errorMessage, exception.getMessage());
            assertNull(exception.getCause());
        }

        @Test
        @DisplayName("Should create exception with null message")
        void shouldCreateException_WithNullMessage() {
            // When
            ValidationException exception = new ValidationException(null);

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
            ValidationException exception = new ValidationException(emptyMessage);

            // Then
            assertNotNull(exception);
            assertEquals(emptyMessage, exception.getMessage());
            assertNull(exception.getCause());
        }

        @Test
        @DisplayName("Should be instance of RuntimeException")
        void shouldBeInstanceOf_RuntimeException() {
            // Given
            ValidationException exception = new ValidationException("Test message");

            // When & Then
            assertInstanceOf(RuntimeException.class, exception);
        }
    }

    @Nested
    @DisplayName("Exception Behavior Tests")
    class ExceptionBehaviorTests {

        @Test
        @DisplayName("Should be throwable")
        void shouldBeThrowable() {
            // Given
            String errorMessage = "Test validation error";

            // When & Then
            assertThrows(ValidationException.class, () -> {
                throw new ValidationException(errorMessage);
            });
        }

        @Test
        @DisplayName("Should preserve message when thrown and caught")
        void shouldPreserveMessage_WhenThrownAndCaught() {
            // Given
            String errorMessage = "Validation error message";

            // When & Then
            ValidationException caughtException = assertThrows(ValidationException.class, () -> {
                throw new ValidationException(errorMessage);
            });

            assertEquals(errorMessage, caughtException.getMessage());
        }
    }
}
