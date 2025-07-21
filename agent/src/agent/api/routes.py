from fastapi import APIRouter
from fastapi import Request
from fastapi.responses import StreamingResponse
from src.agent.agent_runner import run_agent

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
    data = await request.json()
    message = data.get("message")
    member_id = data.get("memberId")
    channel_id = data.get("channelId")

    async def event_stream():
        async for chunk in run_agent(message, member_id, channel_id):
            yield chunk + "\n"

    return StreamingResponse(event_stream(), media_type="text/plain")
