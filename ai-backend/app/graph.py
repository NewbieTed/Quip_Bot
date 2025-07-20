import os

from langchain_openai import ChatOpenAI
from langgraph.checkpoint.memory import MemorySaver
from langgraph.config import get_stream_writer
from langgraph.constants import END
from langgraph.graph.state import CompiledStateGraph

from app.config import Config
from langgraph.prebuilt import create_react_agent


# async def setup_graph(tools=[]):
#     if tools is None:
#         tools = []
#     memory = MemorySaver()
#     os.environ["OPENAI_API_KEY"] = Config.OPENAI_API_KEY
#     llm = ChatOpenAI(temperature=0, model="gpt-4o-mini", api_key=Config.OPENAI_API_KEY)
#
#     return create_react_agent(llm, tools=tools, checkpointer=memory)


from typing import Annotated

from langchain.chat_models import init_chat_model
from langchain_core.messages import BaseMessage
from typing_extensions import TypedDict

from langgraph.checkpoint.memory import MemorySaver
from langgraph.graph import StateGraph
from langgraph.graph.message import add_messages
from langgraph.prebuilt import ToolNode, tools_condition

class State(TypedDict):
    messages: Annotated[list, add_messages]

def route_to_progress_report_before_tools(
    state: State,
):
    """
    Use in the conditional_edge to route to the ToolNode if the last message
    has tool calls. Otherwise, route to the end.
    """
    if isinstance(state, list):
        ai_message = state[-1]
    elif messages := state.get("messages", []):
        ai_message = messages[-1]
    else:
        raise ValueError(f"No messages found in input state to tool_edge: {state}")
    if hasattr(ai_message, "tool_calls") and len(ai_message.tool_calls) > 0:
        return "progress_report"
    return END

class ProgressReportNode:
    """A node that reports progress to the user."""
    input: dict
    message: str
    llm = ChatOpenAI(temperature=0, model="gpt-4o-mini", api_key=Config.OPENAI_API_KEY)
    system_prompt: dict = {
                "role": "system",
                "content": "You are an expert in turning function calls into human understandable language.  You also "
                           "prefer to use as few words as possible. When given you a tool name and function call, "
                           "you shall output a short label in natural language and present continuous tense about what "
                           "the call is trying to do, also use '...' in the end to indicate in progress."
            }
    def __init__(self, ai_input: dict) -> None:
        self.input = ai_input
        print(self.input, flush=True)
        if messages := ai_input.get("messages", []):
            message = messages[-1]
            self.message = message
        else:
            return
        for tool_call in message.tool_calls:
            user_prompt = "Tool name: " + str(message.tool_calls[0]["name"]) + "Tool args: " + str(tool_call["args"])
            print(user_prompt, flush=True)
            messages = [
                self.system_prompt,
                {
                    "role": "user",
                    "content": user_prompt
                }
            ]
            new_message = self.llm.invoke(messages)
            writer = get_stream_writer()
            writer({"progress": new_message.content})
            print(new_message.content, flush=True)
        # for tool_call in message.tool_calls:
        #     tool_name = tool_call["name"]
        #     status_update_message = f"Calling tool: {tool_name}..."
            # print(f"Message {status_update_message}", flush=True)

    def __call__(self, inputs: dict):
        pass

async def setup_graph(tools=None) -> CompiledStateGraph:
    if tools is None:
        tools = []
    os.environ["OPENAI_API_KEY"] = Config.OPENAI_API_KEY
    llm = ChatOpenAI(temperature=0, model="gpt-4o-mini", api_key=Config.OPENAI_API_KEY)
    llm_with_tools = llm.bind_tools(tools)
    def agent(state: State):
        return {"messages": [llm_with_tools.invoke(state["messages"])]}

    graph_builder = StateGraph(State)
    graph_builder.add_node("agent", agent)

    tool_node = ToolNode(tools=tools)
    progress_report_node = ProgressReportNode
    graph_builder.add_node("tools", tool_node)
    graph_builder.add_node("progress_report", progress_report_node)

    graph_builder.add_conditional_edges(
        "agent",
        route_to_progress_report_before_tools,
        {"progress_report": "progress_report", END: END},
    )

    graph_builder.add_edge("progress_report", "tools")
    graph_builder.add_edge("tools", "agent")
    graph_builder.set_entry_point("agent")
    memory = MemorySaver()
    graph = graph_builder.compile(checkpointer=memory)
    return graph



# from IPython.display import Image, display

# try:
#     img_bytes = graph.get_graph().draw_mermaid_png()
#     with open("graph.png", "wb") as f:
#         f.write(img_bytes)
# except Exception as e:
#     print("Failed to generate graph:", e)
#     # This requires some extra dependencies and is optional
#     pass