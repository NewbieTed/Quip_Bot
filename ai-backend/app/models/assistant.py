from pydantic import BaseModel


class AssistantRequest(BaseModel):
    message: str
    memberId: int


class AssistantResponse(BaseModel):
    response: str
