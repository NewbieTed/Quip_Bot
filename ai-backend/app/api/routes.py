from fastapi import APIRouter
import json
from app.agent_runner import run_agent
from fastapi import WebSocket, WebSocketDisconnect, Request
from fastapi.responses import StreamingResponse

router = APIRouter()


@router.get("/health")
def health_check():
    return {
        "status": "OK",
        "message": "Yeah yeah yeah I'm fine stop checking if I'm fine"
    }


# @router.websocket("/assistant")
# async def invoke_agent(websocket: WebSocket):
#     await websocket.accept()
#     try:
#         data = await websocket.receive_json()
#         message = data.get("message")
#         member_id = data.get("memberId")
#         channel_id = data.get("channelId")
#
#         async for chunk in run_agent(message, member_id, channel_id):
#             await websocket.send_text(json.dumps({"response": chunk}))
#         await websocket.close()
#     except WebSocketDisconnect:
#         print("WebSocket disconnected")
#     except Exception as e:
#         await websocket.send_text(json.dumps({"error": str(e)}))
#         await websocket.close()


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
