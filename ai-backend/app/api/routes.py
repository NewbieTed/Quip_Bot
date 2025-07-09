from fastapi import APIRouter, HTTPException
from fastapi.responses import StreamingResponse
from pydantic import BaseModel
import json
from app.agent_runner import run_agent

router = APIRouter()

class AssistantRequest(BaseModel):
    message: str
    memberId: int

def generate_response(message: str, member_id: int):
    for chunk in run_agent(message, member_id):
        yield json.dumps({"response": chunk}) + "\n"

@router.get("/health")
def health_check():
    return {"status": "ok"}


@router.post("/assistant")
async def invoke_agent(request: AssistantRequest):
    try:
        return StreamingResponse(generate_response(request.message, request.memberId), media_type="application/json")
    except Exception as e:
        raise HTTPException(status_code=500, detail=str(e))