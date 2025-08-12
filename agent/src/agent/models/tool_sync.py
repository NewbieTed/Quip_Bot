"""
Data models for tool synchronization between agent and backend.
Used for HTTP-based sync recovery when Redis message processing fails.
"""

from datetime import datetime, timezone
from typing import List, Optional, Dict
from pydantic import BaseModel, Field, validator, ConfigDict, field_serializer
import uuid
import re


class ToolInfo(BaseModel):
    """
    Model representing a tool with its MCP server information.
    """
    
    model_config = ConfigDict(populate_by_name=True)
    
    name: str = Field(..., description="Name of the tool")
    mcp_server_name: str = Field(..., description="Name of the MCP server providing this tool", alias="mcpServerName")
    
    @validator('name')
    def validate_name(cls, v):
        """Validate that tool name is valid."""
        if not v or not v.strip():
            raise ValueError('Tool name cannot be empty')
        
        # Tool names should contain only alphanumeric characters, hyphens, and underscores
        if not re.match(r'^[a-zA-Z0-9_-]+$', v.strip()):
            raise ValueError(f'Invalid tool name: {v}. Tool names must contain only alphanumeric characters, hyphens, and underscores')
        
        return v.strip()
    
    @validator('mcp_server_name')
    def validate_mcp_server_name(cls, v):
        """Validate that MCP server name is not empty."""
        if not v or not v.strip():
            raise ValueError('MCP server name cannot be empty')
        return v.strip()


class ToolResyncRequest(BaseModel):
    """
    Model representing a tool resync request from backend to agent via HTTP.
    Used for sync recovery when Redis message processing fails.
    """
    
    request_id: str = Field(..., description="Unique identifier for the resync request", alias="requestId")
    timestamp: datetime = Field(..., description="Timestamp when the request was created")
    reason: str = Field(..., description="Reason for the resync request")
    
    @field_serializer('timestamp')
    def serialize_timestamp(self, dt: datetime) -> str:
        """Serialize timestamp to ISO format with timezone for Java Instant compatibility."""
        if dt.tzinfo is None:
            # Assume UTC if no timezone info
            dt = dt.replace(tzinfo=timezone.utc)
        return dt.isoformat()
    
    @validator('request_id')
    def validate_request_id(cls, v):
        """Validate that request_id is a valid UUID format."""
        uuid_pattern = re.compile(
            r'^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}$'
        )
        if not uuid_pattern.match(v):
            raise ValueError('Request ID must be a valid UUID')
        return v
    
    @validator('reason')
    def validate_reason(cls, v):
        """Validate that reason is not empty."""
        if not v or not v.strip():
            raise ValueError('Reason cannot be empty')
        return v.strip()
    
    model_config = ConfigDict(
        populate_by_name=True,
        json_schema_extra={
            "example": {
                "requestId": "550e8400-e29b-41d4-a716-446655440000",
                "timestamp": "2025-01-28T10:30:00Z",
                "reason": "message_processing_failure"
            }
        }
    )


class ToolInventoryResponse(BaseModel):
    """
    Model representing a tool inventory response from agent to backend via HTTP.
    Contains the complete current tool inventory from the agent.
    """
    
    request_id: str = Field(..., description="Request ID from the original resync request", alias="requestId")
    timestamp: datetime = Field(..., description="Timestamp when the response was created")
    current_tools: List[ToolInfo] = Field(..., description="List of currently available tools with server info", alias="currentTools")
    discovery_timestamp: datetime = Field(..., description="Timestamp when tool discovery was performed", alias="discoveryTimestamp")
    
    @field_serializer('timestamp', 'discovery_timestamp')
    def serialize_timestamps(self, dt: datetime) -> str:
        """Serialize timestamps to ISO format with timezone for Java Instant compatibility."""
        if dt.tzinfo is None:
            # Assume UTC if no timezone info
            dt = dt.replace(tzinfo=timezone.utc)
        return dt.isoformat()
    
    @validator('request_id')
    def validate_request_id(cls, v):
        """Validate that request_id is a valid UUID format."""
        uuid_pattern = re.compile(
            r'^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}$'
        )
        if not uuid_pattern.match(v):
            raise ValueError('Request ID must be a valid UUID')
        return v
    
    @validator('current_tools')
    def validate_current_tools(cls, v):
        """Validate that current_tools is a list and contains valid ToolInfo objects."""
        if not isinstance(v, list):
            raise ValueError('Current tools must be a list')
        
        # Validation is handled by the ToolInfo model itself
        return v
    
    model_config = ConfigDict(
        populate_by_name=True,
        json_schema_extra={
            "example": {
                "requestId": "550e8400-e29b-41d4-a716-446655440000",
                "timestamp": "2025-01-28T10:30:00Z",
                "currentTools": [
                    {"name": "tool1", "mcpServerName": "built-in"},
                    {"name": "geo-data-weather", "mcpServerName": "geo-data"}
                ],
                "discoveryTimestamp": "2025-01-28T10:29:55Z"
            }
        }
    )


def create_resync_response(request: ToolResyncRequest, tools: List[ToolInfo]) -> ToolInventoryResponse:
    """
    Helper function to create a ToolInventoryResponse from a ToolResyncRequest.
    
    Args:
        request: The original resync request
        tools: List of currently available tools with server info
        
    Returns:
        ToolInventoryResponse with the current tool inventory
    """
    now = datetime.now(timezone.utc)
    return ToolInventoryResponse(
        request_id=request.request_id,
        timestamp=now,
        current_tools=tools,
        discovery_timestamp=now
    )


def create_resync_request(reason: str) -> ToolResyncRequest:
    """
    Helper function to create a ToolResyncRequest.
    
    Args:
        reason: Reason for the resync request
        
    Returns:
        ToolResyncRequest with generated UUID and current timestamp
    """
    return ToolResyncRequest(
        request_id=str(uuid.uuid4()),
        timestamp=datetime.now(timezone.utc),
        reason=reason
    )