from fastapi import APIRouter, HTTPException
from fastapi import Request
from fastapi.responses import StreamingResponse
from src.agent.agent_runner import run_new_agent, run_agent

from http import HTTPStatus

router = APIRouter()


@router.get("/health")
def health_check():
    return {
        "status": "OK",
        "message": "Yeah yeah yeah I'm fine stop checking if I'm fine"
    }


# HTTP streaming endpoint for assistant
@router.post("/assistant")
async def invoke_agent(request: Request):
    try:
        data = await request.json()
    except Exception:
        raise HTTPException(status_code=HTTPStatus.BAD_REQUEST, detail="Invalid JSON payload")

    # Extract variables from request data
    message: str = data.get("message")
    server_id: int = data.get("serverId")
    channel_id: int = data.get("channelId")
    member_id: int = data.get("memberId")
    conversation_id: int = data.get("conversationId")
    approved: bool = data.get("approved")

    # Validate that all required variables have values
    missing_fields = []
    if server_id is None:
        missing_fields.append("serverId")
    if channel_id is None:
        missing_fields.append("channelId")
    if member_id is None:
        missing_fields.append("memberId")
    if conversation_id is None:
        missing_fields.append("conversationId")

    if missing_fields:
        raise HTTPException(
            status_code=HTTPStatus.UNPROCESSABLE_ENTITY,
            detail=f"Missing or empty required fields: {', '.join(missing_fields)}"
        )

    if not approved:
        approved = False

    async def event_stream():
        async for chunk in run_agent(member_message=message, server_id=server_id, channel_id=channel_id,
                                     member_id=member_id, conversation_id=conversation_id, approved=approved):
            # Ensure chunk ends with newline for proper formatting
            if chunk and not chunk.endswith('\n'):
                formatted_chunk = chunk + '\n'
            else:
                formatted_chunk = chunk if chunk else '\n'

            yield formatted_chunk

    return StreamingResponse(event_stream(), media_type="text/plain")


# HTTP streaming endpoint for assistant
@router.post("/assistant/new")
async def invoke_new_agent(request: Request):
    try:
        data = await request.json()
    except Exception:
        raise HTTPException(status_code=HTTPStatus.BAD_REQUEST, detail="Invalid JSON payload")

    # Extract variables from request data
    message = data.get("message")
    server_id = data.get("serverId")
    channel_id = data.get("channelId")
    member_id = data.get("memberId")
    conversation_id = data.get("conversationId")

    # Validate that all required variables have values
    missing_fields = []
    if message is None:
        missing_fields.append("message")
    if server_id is None:
        missing_fields.append("serverId")
    if channel_id is None:
        missing_fields.append("channelId")
    if member_id is None:
        missing_fields.append("memberId")
    if conversation_id is None:
        missing_fields.append("conversationId")

    if missing_fields:
        raise HTTPException(
            status_code=HTTPStatus.UNPROCESSABLE_ENTITY,
            detail=f"Missing or empty required fields: {', '.join(missing_fields)}"
        )

    async def event_stream():
        async for chunk in run_new_agent(member_message=message, server_id=server_id, channel_id=channel_id,
                                         member_id=member_id, conversation_id=conversation_id):
            # Ensure chunk ends with newline for proper formatting
            if chunk and not chunk.endswith('\n'):
                formatted_chunk = chunk + '\n'
            else:
                formatted_chunk = chunk if chunk else '\n'

            yield formatted_chunk

    return StreamingResponse(event_stream(), media_type="text/plain")
