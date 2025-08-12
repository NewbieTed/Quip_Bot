from typing import List, Optional, Dict, Any
from datetime import datetime, timezone

from fastapi import APIRouter, HTTPException
from fastapi import Request
from fastapi.responses import StreamingResponse
from src.agent.agent_runner import run_new_agent, run_agent
from src.agent.models.tool_sync import ToolResyncRequest, ToolInventoryResponse
from src.agent.api.tool_sync_controller import get_tool_sync_controller

from http import HTTPStatus

router = APIRouter()


class RequestValidationError(Exception):
    """Custom exception for request validation errors."""

    def __init__(self, message: str, status_code: int = HTTPStatus.UNPROCESSABLE_ENTITY):
        self.message = message
        self.status_code = status_code
        super().__init__(message)


async def _parse_request_json(request: Request) -> Dict[str, Any]:
    """Parse JSON from request with error handling."""
    try:
        return await request.json()
    except Exception:
        raise HTTPException(status_code=HTTPStatus.BAD_REQUEST, detail="Invalid JSON payload")


def _extract_common_fields(data: Dict[str, Any]) -> Dict[str, Any]:
    """Extract common fields from request data."""
    return {
        "server_id": data.get("serverId"),
        "channel_id": data.get("channelId"),
        "member_id": data.get("memberId"),
        "conversation_id": data.get("conversationId")
    }


def _validate_required_fields(fields: Dict[str, Any], required_fields: List[str]) -> None:
    """Validate that required fields are present and not None."""
    missing_fields = [field for field in required_fields if fields.get(field) is None]

    if missing_fields:
        # Convert snake_case back to camelCase for error message
        missing_request_fields = []
        for field in missing_fields:
            if field == "server_id":
                missing_request_fields.append("serverId")
            elif field == "channel_id":
                missing_request_fields.append("channelId")
            elif field == "member_id":
                missing_request_fields.append("memberId")
            elif field == "conversation_id":
                missing_request_fields.append("conversationId")
            else:
                missing_request_fields.append(field)

        raise HTTPException(
            status_code=HTTPStatus.UNPROCESSABLE_ENTITY,
            detail=f"Missing or empty required fields: {', '.join(missing_request_fields)}"
        )


def _validate_approval_logic(approved: Optional[bool], message: Optional[str]) -> None:
    """Validate approval logic constraints."""
    if approved is True and message and message.strip():
        raise HTTPException(
            status_code=HTTPStatus.UNPROCESSABLE_ENTITY,
            detail="Cannot approve with a new message. Please send approval without additional text."
        )


async def _create_event_stream(agent_generator):
    """Create event stream from agent generator."""
    async for chunk in agent_generator:
        formatted_chunk = chunk if chunk else '\n'
        yield formatted_chunk


def _validate_conversation_dto(conversation: Dict[str, Any], index: int) -> None:
    """Validate a single conversation DTO."""
    required_fields = ["conversationId", "serverId", "memberId"]
    missing_fields = [field for field in required_fields if conversation.get(field) is None]

    if missing_fields:
        raise HTTPException(
            status_code=HTTPStatus.UNPROCESSABLE_ENTITY,
            detail=f"Conversation at index {index} is missing required fields: {', '.join(missing_fields)}"
        )

    # Validate field types
    for field in required_fields:
        value = conversation.get(field)
        if not isinstance(value, int) or value < 0:
            raise HTTPException(
                status_code=HTTPStatus.UNPROCESSABLE_ENTITY,
                detail=f"Conversation at index {index}: {field} must be a positive integer"
            )


def _validate_conversations_list(conversations: List[Dict[str, Any]]) -> None:
    """Validate the conversations list structure and content."""
    if not isinstance(conversations, list):
        raise HTTPException(
            status_code=HTTPStatus.UNPROCESSABLE_ENTITY,
            detail="conversations must be a list"
        )

    if len(conversations) == 0:
        raise HTTPException(
            status_code=HTTPStatus.UNPROCESSABLE_ENTITY,
            detail="conversations list cannot be empty"
        )

    # Validate each conversation DTO
    for index, conversation in enumerate(conversations):
        if not isinstance(conversation, dict):
            raise HTTPException(
                status_code=HTTPStatus.UNPROCESSABLE_ENTITY,
                detail=f"Conversation at index {index} must be an object"
            )
        _validate_conversation_dto(conversation, index)

    # Validate that all conversations belong to the same member
    member_ids = {conv.get("memberId") for conv in conversations}
    if len(member_ids) > 1:
        raise HTTPException(
            status_code=HTTPStatus.UNPROCESSABLE_ENTITY,
            detail="All conversations must belong to the same member"
        )


def _validate_tool_lists(added_tools: List[str], removed_tools: List[str]) -> None:
    """Validate the added and removed tools lists."""
    # Basic validation for tool changes
    if not isinstance(added_tools, list):
        raise HTTPException(
            status_code=HTTPStatus.UNPROCESSABLE_ENTITY,
            detail="addedTools must be a list of tool names"
        )

    if not isinstance(removed_tools, list):
        raise HTTPException(
            status_code=HTTPStatus.UNPROCESSABLE_ENTITY,
            detail="removedTools must be a list of tool names"
        )

    # Validate that tool names are strings
    for tool in added_tools:
        if not isinstance(tool, str) or not tool.strip():
            raise HTTPException(
                status_code=HTTPStatus.UNPROCESSABLE_ENTITY,
                detail="All added tools must be non-empty strings"
            )

    for tool in removed_tools:
        if not isinstance(tool, str) or not tool.strip():
            raise HTTPException(
                status_code=HTTPStatus.UNPROCESSABLE_ENTITY,
                detail="All removed tools must be non-empty strings"
            )

    # Validate that there are actual changes
    if not added_tools and not removed_tools:
        raise HTTPException(
            status_code=HTTPStatus.UNPROCESSABLE_ENTITY,
            detail="At least one tool must be added or removed"
        )


@router.get("/health")
def health_check():
    return {
        "status": "OK",
        "message": "Yeah yeah yeah I'm fine stop checking if I'm fine"
    }


@router.get("/health/detailed")
async def detailed_health_check():
    """
    Detailed health check endpoint including sync recovery monitoring.
    
    Returns comprehensive health information about all agent components
    including tool discovery, Redis connectivity, and sync recovery metrics.
    """
    from src.agent.monitoring.health_check import get_system_status
    
    try:
        health_status = await get_system_status()
        return health_status
    except Exception as e:
        return {
            "status": "ERROR",
            "error": f"Health check failed: {str(e)}",
            "timestamp": datetime.now(timezone.utc).isoformat()
        }


@router.get("/health/sync-recovery")
def sync_recovery_health():
    """
    Sync recovery specific health check endpoint.
    
    Returns metrics and status information specifically related to
    sync recovery operations and resync request handling.
    """
    from src.agent.monitoring.metrics_service import get_metrics_service
    
    try:
        metrics_service = get_metrics_service()
        metrics = metrics_service.get_metrics_summary()
        health_status = metrics_service.get_health_status()
        
        # Extract sync recovery specific metrics
        system_metrics = metrics.get("system_metrics", {})
        sync_recovery_metrics = {
            "resync_requests_received": system_metrics.get("resync_requests_received", 0),
            "resync_requests_successful": system_metrics.get("resync_requests_successful", 0),
            "resync_requests_failed": system_metrics.get("resync_requests_failed", 0),
            "resync_success_rate": system_metrics.get("resync_success_rate", 0.0),
            "average_resync_latency_ms": system_metrics.get("average_resync_latency_ms", 0.0),
            "last_resync_request": system_metrics.get("last_resync_request")
        }
        
        return {
            "status": health_status.get("status", "unknown"),
            "sync_recovery_metrics": sync_recovery_metrics,
            "health_issues": health_status.get("issues", []),
            "timestamp": datetime.now(timezone.utc).isoformat()
        }
        
    except Exception as e:
        return {
            "status": "ERROR",
            "error": f"Sync recovery health check failed: {str(e)}",
            "timestamp": datetime.now(timezone.utc).isoformat()
        }


# HTTP streaming endpoint for assistant
@router.post("/assistant")
async def invoke_agent(request: Request):
    data = await _parse_request_json(request)

    # Extract variables from request data
    common_fields = _extract_common_fields(data)
    message: Optional[str] = data.get("message")
    approved: Optional[bool] = data.get("approved")
    tool_whitelist_update: Optional[List[str]] = data.get("toolWhitelistUpdate", [])

    # Validate required fields (approved is None means it's a new message)
    required_fields = ["server_id", "channel_id", "member_id", "conversation_id"]
    _validate_required_fields(common_fields, required_fields)

    # Validate approval logic
    _validate_approval_logic(approved, message)

    # Create agent generator
    agent_generator = run_agent(
        member_message=message,
        server_id=common_fields["server_id"],
        channel_id=common_fields["channel_id"],
        member_id=common_fields["member_id"],
        conversation_id=common_fields["conversation_id"],
        approved=approved,
        tool_whitelist_update=tool_whitelist_update
    )

    return StreamingResponse(_create_event_stream(agent_generator), media_type="text/plain")


# HTTP streaming endpoint for new assistant
@router.post("/assistant/new")
async def invoke_new_agent(request: Request):
    data = await _parse_request_json(request)

    # Extract variables from request data
    common_fields = _extract_common_fields(data)
    message: Optional[str] = data.get("message")
    tool_whitelist: Optional[List[str]] = data.get("toolWhitelist", [])

    # Validate required fields (message is required for new agents)
    required_fields = ["server_id", "channel_id", "member_id", "conversation_id"]
    _validate_required_fields(common_fields, required_fields)

    # Message is required for new agents
    if message is None:
        raise HTTPException(
            status_code=HTTPStatus.UNPROCESSABLE_ENTITY,
            detail="Missing or empty required fields: message"
        )

    # Create agent generator
    agent_generator = run_new_agent(
        member_message=message,
        server_id=common_fields["server_id"],
        channel_id=common_fields["channel_id"],
        member_id=common_fields["member_id"],
        conversation_id=common_fields["conversation_id"],
        tool_whitelist=tool_whitelist
    )

    return StreamingResponse(_create_event_stream(agent_generator), media_type="text/plain")


@router.post("/tool-whitelist/update")
async def update_tool_whitelist(request: Request):
    """
    Endpoint to handle tool whitelist updates from the backend.
    This endpoint receives notifications when tool whitelist changes occur
    and can affect multiple conversations across different servers.
    """
    data = await _parse_request_json(request)

    # Extract variables from request data
    member_id: Optional[int] = data.get("memberId")
    conversations: Optional[List[Dict[str, Any]]] = data.get("conversations", [])
    added_tools: Optional[List[str]] = data.get("addedTools", [])
    removed_tools: Optional[List[str]] = data.get("removedTools", [])

    # Validate required fields
    if member_id is None:
        raise HTTPException(
            status_code=HTTPStatus.UNPROCESSABLE_ENTITY,
            detail="Missing or empty required fields: memberId"
        )

    if not isinstance(member_id, int) or member_id < 0:
        raise HTTPException(
            status_code=HTTPStatus.UNPROCESSABLE_ENTITY,
            detail="memberId must be a non negative integer"
        )

    # Validate conversations list
    _validate_conversations_list(conversations)

    # Validate that the member_id matches the conversations
    conversation_member_id = conversations[0].get("memberId")
    if member_id != conversation_member_id:
        raise HTTPException(
            status_code=HTTPStatus.UNPROCESSABLE_ENTITY,
            detail="memberId must match the memberId in all conversations"
        )

    # Validate tool lists
    _validate_tool_lists(added_tools, removed_tools)

    # Import the update function
    from src.agent.agent_runner import update_tool_whitelist

    # Update tool whitelist for all affected conversations
    try:
        update_result = await update_tool_whitelist(
            member_id=member_id,
            conversations=conversations,
            added_tools=added_tools,
            removed_tools=removed_tools
        )

        # Log the results
        print(f"Tool whitelist update completed for member {member_id}: ")
        print(f"  Successful updates: {update_result['successfulUpdates']}")
        print(f"  Failed updates: {update_result['failedUpdates']}")
        print(f"  Added tools: {added_tools}")
        print(f"  Removed tools: {removed_tools}")

        # Include update details in response
        response_data = {
            "statusCode": HTTPStatus.OK,
            "status": True,
            "message": f"Tool whitelist update processed for member "
            f"{member_id} across {len(conversations)} conversations",
            "affectedConversations": len(conversations),
            "addedTools": len(added_tools),
            "removedTools": len(removed_tools),
            "updateResults": {
                "successful": update_result['successfulUpdates'],
                "failed": update_result['failedUpdates']
            }
        }

        # Add failure details if any
        if update_result['failedUpdates'] > 0:
            response_data["failedConversations"] = update_result['failedConversations']

        return response_data

    except Exception as e:
        print(f"Error updating tool whitelist for member {member_id}: {str(e)}")
        raise HTTPException(
            status_code=HTTPStatus.INTERNAL_SERVER_ERROR,
            detail=f"Failed to update tool whitelist: {str(e)}"
        )


@router.post("/api/tools/resync", response_model=ToolInventoryResponse)
async def handle_tool_resync(request: ToolResyncRequest) -> ToolInventoryResponse:
    """
    Handle tool resync requests from the backend.
    
    This endpoint is called by the backend when Redis message processing fails
    and a complete tool inventory sync is needed for recovery.
    
    Args:
        request: ToolResyncRequest containing the resync request details
        
    Returns:
        ToolInventoryResponse with the complete current tool inventory
        
    Raises:
        HTTPException: If tool discovery fails or times out
    """
    controller = get_tool_sync_controller()
    return await controller.handle_resync_request(request)
