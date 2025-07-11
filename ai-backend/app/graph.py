import os

from langchain_openai import ChatOpenAI
from langgraph.checkpoint.memory import MemorySaver
from app.tools import weather_tool, create_problem_tool
from app.config import Config
from langgraph.prebuilt import create_react_agent


memory = MemorySaver()
os.environ["OPENAI_API_KEY"] = Config.OPENAI_API_KEY
llm = ChatOpenAI(temperature=0, model="gpt-4o-mini", api_key=Config.OPENAI_API_KEY)
tools = [weather_tool, create_problem_tool]
graph = create_react_agent(
    llm,
    tools=tools,
    checkpointer=memory
)
