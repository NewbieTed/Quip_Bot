from typing import List, Optional, Dict, Any

from fastapi import APIRouter, HTTPException
from fastapi import Request
from fastapi.responses import StreamingResponse
from src.agent.agent_runner import run_new_agent, run_agent

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


@router.get("/health")
def health_check():
    return {
        "status": "OK",
        "message": "Yeah yeah yeah I'm fine stop checking if I'm fine"
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
