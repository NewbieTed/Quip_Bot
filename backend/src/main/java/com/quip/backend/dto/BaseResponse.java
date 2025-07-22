package com.quip.backend.dto;

import lombok.Data;
import org.springframework.http.HttpStatus;

import java.time.Instant;

/**
 * Generic response wrapper for all API endpoints.
 * <p>
 * This class provides a standardized structure for all API responses,
 * including status information, error messages, and the actual response data.
 * It supports generic typing to accommodate different types of response data.
 * </p>
 */
@Data
public class BaseResponse<T> {
    /**
     * Indicates whether the request was successful (true) or failed (false).
     */
    private boolean status;
    
    /**
     * HTTP status code of the response (e.g., 200, 404, 500).
     */
    private int statusCode;
    
    /**
     * A user-friendly message describing the result of the operation.
     */
    private String message;
    
    /**
     * The actual data being returned by the API.
     * This is a generic type that can hold any kind of response data.
     */
    private T data;
    
    /**
     * Timestamp when the response was created.
     */
    private Instant timestamp;

    /**
     * Default constructor that initializes the timestamp to the current time.
     */
    public BaseResponse() {
        this.timestamp = Instant.now();
    }

    /**
     * Constructor with parameters for creating a fully initialized response.
     *
     * @param status Whether the request was successful
     * @param statusCode The HTTP status code
     * @param message A user-friendly message
     * @param data The response data
     */
    public BaseResponse(boolean status, int statusCode, String message, T data) {
        this.status = status;
        this.statusCode = statusCode;
        this.message = message;
        this.data = data;
        this.timestamp = Instant.now();
    }

    /**
     * Creates a success response with no data.
     * <p>
     * This is typically used for operations that don't return any data,
     * such as successful deletions or updates.
     * </p>
     *
     * @return A success response with HTTP 200 status and no data
     */
    public static BaseResponse<Boolean> success() {
        return new BaseResponse<>(true, HttpStatus.OK.value(), "", null);
    }

    /**
     * Creates a success response with data.
     * <p>
     * This is typically used for operations that return data,
     * such as retrieving a resource or a list of resources.
     * </p>
     *
     * @param data The data to include in the response
     * @param <T> The type of the data
     * @return A success response with HTTP 200 status and the provided data
     */
    public static <T> BaseResponse<T> success(T data) {
        return new BaseResponse<>(true, HttpStatus.OK.value(), "", data);
    }

    /**
     * Creates a success response with a message and data.
     * <p>
     * This is typically used for operations that return data and need
     * to include a specific success message.
     * </p>
     *
     * @param message A user-friendly success message
     * @param data The data to include in the response
     * @param <T> The type of the data
     * @return A success response with HTTP 200 status, the provided message, and data
     */
    public static <T> BaseResponse<T> success(String message, T data) {
        return new BaseResponse<>(true, HttpStatus.OK.value(), message, data);
    }

    /**
     * Creates a failure response with a status code and message.
     * <p>
     * This is typically used for operations that fail due to client errors
     * or server errors.
     * </p>
     *
     * @param statusCode The HTTP status code indicating the type of failure
     * @param message A user-friendly error message
     * @param <T> The type that would have been returned if the operation succeeded
     * @return A failure response with the provided status code and message
     */
    public static <T> BaseResponse<T> failure(int statusCode, String message) {
        return new BaseResponse<>(false, statusCode, message, null);
    }
}