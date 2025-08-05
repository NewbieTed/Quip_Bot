package com.quip.backend.tool.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

import java.time.Instant;
import java.util.Objects;

/**
 * Model representing a tool resync request sent from backend to agent via HTTP.
 * Used for sync recovery when Redis message processing fails.
 */
public class ToolResyncRequest {

    @NotBlank(message = "Request ID cannot be blank")
    @Pattern(regexp = "^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}$", 
             message = "Request ID must be a valid UUID")
    @JsonProperty("requestId")
    private String requestId;

    @NotNull(message = "Timestamp cannot be null")
    @JsonProperty("timestamp")
    private Instant timestamp;

    @NotBlank(message = "Reason cannot be blank")
    @JsonProperty("reason")
    private String reason;

    public ToolResyncRequest() {
        // Default constructor for JSON deserialization
    }

    public ToolResyncRequest(String requestId, Instant timestamp, String reason) {
        this.requestId = requestId;
        this.timestamp = timestamp;
        this.reason = reason;
    }

    public String getRequestId() {
        return requestId;
    }

    public void setRequestId(String requestId) {
        this.requestId = requestId;
    }

    public Instant getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(Instant timestamp) {
        this.timestamp = timestamp;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ToolResyncRequest that = (ToolResyncRequest) o;
        return Objects.equals(requestId, that.requestId) &&
               Objects.equals(timestamp, that.timestamp) &&
               Objects.equals(reason, that.reason);
    }

    @Override
    public int hashCode() {
        return Objects.hash(requestId, timestamp, reason);
    }

    @Override
    public String toString() {
        return "ToolResyncRequest{" +
               "requestId='" + requestId + '\'' +
               ", timestamp=" + timestamp +
               ", reason='" + reason + '\'' +
               '}';
    }
}