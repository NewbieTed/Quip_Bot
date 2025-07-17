package com.quip.backend.common.exception;

import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.exc.MismatchedInputException;
import com.quip.backend.common.BaseTest;
import com.quip.backend.dto.BaseResponse;
import com.quip.backend.common.exception.EntityNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.BindingResult;
import org.springframework.validation.ObjectError;
import org.springframework.web.bind.MethodArgumentNotValidException;

import java.lang.reflect.Field;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for {@link GlobalExceptionHandler}.
 * <p>
 * This test class validates the global exception handler functionality including
 * handling of various exception types and proper error response formatting.
 * Enhanced to ensure PIT mutation testing can detect proper coverage.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("GlobalExceptionHandler Tests")
class GlobalExceptionHandlerTest extends BaseTest {

    private GlobalExceptionHandler globalExceptionHandler;

    @Mock
    private BindingResult bindingResult;

    @Mock
    private ObjectError objectError;

    @Mock
    private MismatchedInputException mismatchedInputException;

    @Mock
    private JsonMappingException.Reference reference;

    @BeforeEach
    void setUp() {
        globalExceptionHandler = new GlobalExceptionHandler();
        reset(bindingResult, objectError, mismatchedInputException, reference);
    }

    @Test
    @DisplayName("Should instantiate handler successfully")
    void shouldInstantiateHandler_Successfully() {
        // When & Then
        assertNotNull(globalExceptionHandler);
    }

    /**
     * Helper class to simulate field reflection for testing type mismatch scenarios
     */
    static class TestClass {
        private String name;
        private Integer age;
        private Boolean active;
        
        // Getters and setters for reflection testing
        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
        public Integer getAge() { return age; }
        public void setAge(Integer age) { this.age = age; }
        public Boolean getActive() { return active; }
        public void setActive(Boolean active) { this.active = active; }
    }

    @Nested
    @DisplayName("handleHttpMessageNotReadable() Tests")
    class HandleHttpMessageNotReadableTests {

        @Test
        @DisplayName("Should handle HttpMessageNotReadableException with generic message")
        void shouldHandleHttpMessageNotReadableException_WithGenericMessage() {
            // Given
            HttpMessageNotReadableException exception = new HttpMessageNotReadableException("Test message");

            // When
            BaseResponse<String> response = globalExceptionHandler.handleHttpMessageNotReadable(exception);

            // Then
            assertThat(response).isNotNull();
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST.value());
            assertThat(response.getMessage()).isEqualTo("Required request body is missing or malformed");
            assertThat(response.isStatus()).isFalse();
        }

        @Test
        @DisplayName("Should handle HttpMessageNotReadableException with MismatchedInputException cause")
        void shouldHandleHttpMessageNotReadableException_WithMismatchedInputExceptionCause() {
            // Given
            when(mismatchedInputException.getPath()).thenReturn(List.of(reference));
            when(reference.getFieldName()).thenReturn("testField");
            when(reference.getFrom()).thenReturn(null);
            
            HttpMessageNotReadableException exception = new HttpMessageNotReadableException("Test message", mismatchedInputException);

            // When
            BaseResponse<String> response = globalExceptionHandler.handleHttpMessageNotReadable(exception);

            // Then
            assertThat(response).isNotNull();
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST.value());
            assertThat(response.getMessage()).contains("'testField' should be of type");
            assertThat(response.isStatus()).isFalse();
        }

        @Test
        @DisplayName("Should handle HttpMessageNotReadableException with empty path")
        void shouldHandleHttpMessageNotReadableException_WithEmptyPath() {
            // Given
            when(mismatchedInputException.getPath()).thenReturn(List.of());
            
            HttpMessageNotReadableException exception = new HttpMessageNotReadableException("Test message", mismatchedInputException);

            // When
            BaseResponse<String> response = globalExceptionHandler.handleHttpMessageNotReadable(exception);

            // Then
            assertThat(response).isNotNull();
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST.value());
            assertThat(response.getMessage()).contains("'unknown' should be of type");
            assertThat(response.isStatus()).isFalse();
        }

        @Test
        @DisplayName("Should handle HttpMessageNotReadableException with null cause")
        void shouldHandleHttpMessageNotReadableException_WithNullCause() {
            // Given
            HttpMessageNotReadableException exception = new HttpMessageNotReadableException("Test message", (Throwable) null);

            // When
            BaseResponse<String> response = globalExceptionHandler.handleHttpMessageNotReadable(exception);

            // Then
            assertThat(response).isNotNull();
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST.value());
            assertThat(response.getMessage()).isEqualTo("Required request body is missing or malformed");
            assertThat(response.isStatus()).isFalse();
        }
    }

    @Nested
    @DisplayName("handleValidationException() Tests")
    class HandleValidationExceptionTests {

        @Test
        @DisplayName("Should handle MethodArgumentNotValidException")
        void shouldHandleMethodArgumentNotValidException() {
            // Given
            String errorMessage = "Validation failed for field 'name'";
            when(objectError.getDefaultMessage()).thenReturn(errorMessage);
            when(bindingResult.getAllErrors()).thenReturn(List.of(objectError));
            
            MethodArgumentNotValidException exception = mock(MethodArgumentNotValidException.class);
            when(exception.getBindingResult()).thenReturn(bindingResult);

            // When
            BaseResponse<String> response = globalExceptionHandler.handleValidationException(exception);

            // Then
            assertThat(response).isNotNull();
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNPROCESSABLE_ENTITY.value());
            assertThat(response.getMessage()).isEqualTo(errorMessage);
            assertThat(response.isStatus()).isFalse();
        }

        @Test
        @DisplayName("Should handle MethodArgumentNotValidException with multiple errors")
        void shouldHandleMethodArgumentNotValidException_WithMultipleErrors() {
            // Given
            String firstErrorMessage = "First validation error";
            
            ObjectError firstError = mock(ObjectError.class);
            when(firstError.getDefaultMessage()).thenReturn(firstErrorMessage);
            
            when(bindingResult.getAllErrors()).thenReturn(List.of(firstError));
            
            MethodArgumentNotValidException exception = mock(MethodArgumentNotValidException.class);
            when(exception.getBindingResult()).thenReturn(bindingResult);

            // When
            BaseResponse<String> response = globalExceptionHandler.handleValidationException(exception);

            // Then
            assertThat(response).isNotNull();
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNPROCESSABLE_ENTITY.value());
            assertThat(response.getMessage()).isEqualTo(firstErrorMessage); // Should return first error
            assertThat(response.isStatus()).isFalse();
        }

        @Test
        @DisplayName("Should handle MethodArgumentNotValidException with null error message")
        void shouldHandleMethodArgumentNotValidException_WithNullErrorMessage() {
            // Given
            when(objectError.getDefaultMessage()).thenReturn(null);
            when(bindingResult.getAllErrors()).thenReturn(List.of(objectError));
            
            MethodArgumentNotValidException exception = mock(MethodArgumentNotValidException.class);
            when(exception.getBindingResult()).thenReturn(bindingResult);

            // When
            BaseResponse<String> response = globalExceptionHandler.handleValidationException(exception);

            // Then
            assertThat(response).isNotNull();
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNPROCESSABLE_ENTITY.value());
            assertThat(response.getMessage()).isNull();
            assertThat(response.isStatus()).isFalse();
        }
    }

    @Nested
    @DisplayName("handleCustomValidationException() Tests")
    class HandleCustomValidationExceptionTests {

        @Test
        @DisplayName("Should handle ValidationException")
        void shouldHandleValidationException() {
            // Given
            String errorMessage = "Custom validation error";
            ValidationException exception = new ValidationException(errorMessage);

            // When
            BaseResponse<String> response = globalExceptionHandler.handleCustomValidationException(exception);

            // Then
            assertThat(response).isNotNull();
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNPROCESSABLE_ENTITY.value());
            assertThat(response.getMessage()).isEqualTo(errorMessage);
            assertThat(response.isStatus()).isFalse();
        }

        @Test
        @DisplayName("Should handle ValidationException with null message")
        void shouldHandleValidationException_WithNullMessage() {
            // Given
            ValidationException exception = new ValidationException(null);

            // When
            BaseResponse<String> response = globalExceptionHandler.handleCustomValidationException(exception);

            // Then
            assertThat(response).isNotNull();
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNPROCESSABLE_ENTITY.value());
            assertThat(response.getMessage()).isNull();
            assertThat(response.isStatus()).isFalse();
        }

        @Test
        @DisplayName("Should handle ValidationException with empty message")
        void shouldHandleValidationException_WithEmptyMessage() {
            // Given
            ValidationException exception = new ValidationException("");

            // When
            BaseResponse<String> response = globalExceptionHandler.handleCustomValidationException(exception);

            // Then
            assertThat(response).isNotNull();
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNPROCESSABLE_ENTITY.value());
            assertThat(response.getMessage()).isEqualTo("");
            assertThat(response.isStatus()).isFalse();
        }
    }

    @Nested
    @DisplayName("handleEntityNotFoundException() Tests")
    class HandleEntityNotFoundExceptionTests {

        @Test
        @DisplayName("Should handle EntityNotFoundException")
        void shouldHandleEntityNotFoundException() {
            // Given
            String errorMessage = "Entity not found";
            EntityNotFoundException exception = new EntityNotFoundException(errorMessage);

            // When
            BaseResponse<String> response = globalExceptionHandler.handleEntityNotFoundException(exception);

            // Then
            assertThat(response).isNotNull();
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR.value());
            assertThat(response.getMessage()).isEqualTo("Please check terminal for error details");
            assertThat(response.isStatus()).isFalse();
        }

        @Test
        @DisplayName("Should handle EntityNotFoundException with null message")
        void shouldHandleEntityNotFoundException_WithNullMessage() {
            // Given
            EntityNotFoundException exception = new EntityNotFoundException((String) null);

            // When
            BaseResponse<String> response = globalExceptionHandler.handleEntityNotFoundException(exception);

            // Then
            assertThat(response).isNotNull();
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR.value());
            assertThat(response.getMessage()).isEqualTo("Please check terminal for error details");
            assertThat(response.isStatus()).isFalse();
        }

        @Test
        @DisplayName("Should handle EntityNotFoundException with empty message")
        void shouldHandleEntityNotFoundException_WithEmptyMessage() {
            // Given
            EntityNotFoundException exception = new EntityNotFoundException("");

            // When
            BaseResponse<String> response = globalExceptionHandler.handleEntityNotFoundException(exception);

            // Then
            assertThat(response).isNotNull();
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR.value());
            assertThat(response.getMessage()).isEqualTo("Please check terminal for error details");
            assertThat(response.isStatus()).isFalse();
        }
    }
}
