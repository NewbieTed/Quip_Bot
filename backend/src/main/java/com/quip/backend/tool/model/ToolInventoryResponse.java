package com.quip.backend.tool.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

import java.time.Instant;
import java.util.List;
import java.util.Objects;

/**
 * Model representing a tool inventory response from agent to backend via HTTP.
 * Contains the complete current tool inventory from the agent.
 */
public class ToolInventoryResponse {

    @NotBlank(message = "Request ID cannot be blank")
    @Pattern(regexp = "^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}$", 
             message = "Request ID must be a valid UUID")
    @JsonProperty("requestId")
    private String requestId;

    @NotNull(message = "Timestamp cannot be null")
    @JsonProperty("timestamp")
    private Instant timestamp;

    @NotNull(message = "Current tools list cannot be null")
    @JsonProperty("currentTools")
    private List<ToolInfo> currentTools;

    @NotNull(message = "Discovery timestamp cannot be null")
    @JsonProperty("discoveryTimestamp")
    private Instant discoveryTimestamp;

    public ToolInventoryResponse() {
        // Default constructor for JSON deserialization
    }

    public ToolInventoryResponse(String requestId, Instant timestamp, List<ToolInfo> currentTools, Instant discoveryTimestamp) {
        this.requestId = requestId;
        this.timestamp = timestamp;
        this.currentTools = currentTools;
        this.discoveryTimestamp = discoveryTimestamp;
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

    public List<ToolInfo> getCurrentTools() {
        return currentTools;
    }

    public void setCurrentTools(List<ToolInfo> currentTools) {
        this.currentTools = currentTools;
    }

    public Instant getDiscoveryTimestamp() {
        return discoveryTimestamp;
    }

    public void setDiscoveryTimestamp(Instant discoveryTimestamp) {
        this.discoveryTimestamp = discoveryTimestamp;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ToolInventoryResponse that = (ToolInventoryResponse) o;
        return Objects.equals(requestId, that.requestId) &&
               Objects.equals(timestamp, that.timestamp) &&
               Objects.equals(currentTools, that.currentTools) &&
               Objects.equals(discoveryTimestamp, that.discoveryTimestamp);
    }

    @Override
    public int hashCode() {
        return Objects.hash(requestId, timestamp, currentTools, discoveryTimestamp);
    }

    @Override
    public String toString() {
        return "ToolInventoryResponse{" +
               "requestId='" + requestId + '\'' +
               ", timestamp=" + timestamp +
               ", currentTools=" + currentTools +
               ", discoveryTimestamp=" + discoveryTimestamp +
               '}';
    }
}