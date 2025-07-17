package com.quip.backend.common.exception;

import com.fasterxml.jackson.databind.JsonMappingException;
import com.quip.backend.common.exception.ValidationException;
import com.quip.backend.dto.BaseResponse;
import com.quip.backend.common.exception.EntityExistsException;
import com.quip.backend.common.exception.EntityNotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger logger = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    // Handle missing or malformed request bodies
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public BaseResponse<String> handleHttpMessageNotReadable(HttpMessageNotReadableException ex) {
        String detailedMessage = "Required request body is missing or malformed";

        // Check if the exception contains type mismatch information
        Throwable cause = ex.getCause();
        if (cause instanceof com.fasterxml.jackson.databind.exc.MismatchedInputException mismatchedInputException) {

            // Extract the problematic field name
            String fieldName = mismatchedInputException.getPath().stream()
                    .map(JsonMappingException.Reference::getFieldName)
                    .findFirst()
                    .orElse("unknown");

            // Use the first path element to determine the target class (if available)
            Class<?> targetClass = mismatchedInputException.getPath().stream()
                    .findFirst()
                    .map(ref -> ref.getFrom() != null ? ref.getFrom().getClass() : null)
                    .orElse(null);

            // Dynamically determine the expected type
            String expectedType = "unknown";
            if (targetClass != null) {
                try {
                    expectedType = targetClass.getDeclaredField(fieldName).getType().getSimpleName();
                } catch (NoSuchFieldException | SecurityException e) {
                    logger.error("Reflection error: {}", e.getMessage(), e);
                }
            }

            // Since we don't have the actual value, assume it's a String (the common error case)
            String actualType = "String";

            // Build the custom error message
            detailedMessage = String.format(
                    "'%s' should be of type %s, but received %s", fieldName, expectedType, actualType
            );
        }

        // Log the detailed error message
        logger.error("HttpMessageNotReadableException: {}", detailedMessage, ex);

        // Return the error response
        return BaseResponse.failure(HttpStatus.BAD_REQUEST.value(), detailedMessage);
    }


    // Handle validation errors (e.g., @Valid @RequestBody)
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public BaseResponse<String> handleValidationException(MethodArgumentNotValidException ex) {
        String errorMessage = ex.getBindingResult().getAllErrors().get(0).getDefaultMessage();

        // Log the validation error message
        logger.error("Validation error: {}", errorMessage);

        return BaseResponse.failure(HttpStatus.UNPROCESSABLE_ENTITY.value(), errorMessage);
    }

    // Handle custom ValidationException
    @ExceptionHandler(ValidationException.class)
    public BaseResponse<String> handleCustomValidationException(ValidationException ex) {
        String errorMessage = ex.getMessage();
        logger.error("ValidationException: {}", errorMessage, ex);
        return BaseResponse.failure(HttpStatus.UNPROCESSABLE_ENTITY.value(), errorMessage);
    }

    // Handle EntityExistsException
    @ExceptionHandler(EntityNotFoundException.class)
    public BaseResponse<String> handleEntityNotFoundException(EntityNotFoundException ex) {
        String errorMessage = ex.getMessage();
        logger.error("EntityNotFoundException: {}", errorMessage, ex);
        return BaseResponse.failure(HttpStatus.INTERNAL_SERVER_ERROR.value(),
                "Please check terminal for error details");
    }
}