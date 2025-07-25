import os
import hashlib
import logging
from langchain_openai import ChatOpenAI
from langgraph.config import get_stream_writer
from langgraph.constants import END
from langgraph.graph.state import CompiledStateGraph
from src.config import Config
from src.agent.tools import get_all_tools
from src.agent.utils.prompt_loader import load_prompt
from langchain_mcp_adapters.client import MultiServerMCPClient
from langgraph.types import interrupt, Command
from langchain_core.messages import ToolMessage
from langchain_core.tools.base import BaseTool
from typing_extensions import TypedDict, NotRequired
from langgraph.checkpoint.memory import InMemorySaver
from langgraph.graph import StateGraph
from langgraph.graph.message import add_messages
from langgraph.prebuilt import ToolNode
from typing import Annotated, Literal, Set, List

# Configure logger for this module
logger = logging.getLogger(__name__)


class AgentState(TypedDict):
    messages: Annotated[list, add_messages]
    server_id: int
    channel_id: int
    member_id: int
    conversation_id: int
    decision: NotRequired[str]
    tool_call_ids: List[str]
    tool_whitelist: Set[str]


def route_to_human_confirmation_before_tools(
        state: AgentState,
) -> str:
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
        return "human_confirmation"
    return END


class Decision(TypedDict):
    approved: bool
    tool_whitelist_update: List[str]


class HumanConfirmationNode:
    """A node that asks the user to confirm the tool call."""
    def __init__(self):
        openai_config = Config.get_openai_config()
        self.llm = ChatOpenAI(
            temperature=openai_config.get("temperature", 0),
            model=openai_config.get("model", "gpt-4o-mini"),
            api_key=Config.OPENAI_API_KEY
        )
        self.system_prompt = {
            "role": "system",
            "content": load_prompt("human_confirmation")
        }

    def __call__(self, state: AgentState) -> Command[Literal["progress_report", "reject_action"]]:
        logger.debug("HumanConfirmationNode called with state type: %s", type(state))
        messages = state.get("messages", [])
        if not messages:
            logger.debug("No messages in state, returning unchanged")
            return Command(goto="reject_action", update={"decision": "rejected"})

        last_message = messages[-1]
        if not hasattr(last_message, "tool_calls") or not last_message.tool_calls:
            logger.debug("No tool calls in last message, returning unchanged")
            return Command(goto="reject_action", update={"decision": "rejected"})

        tool_whitelist: Set[str] = state["tool_whitelist"]
        for tool_call in last_message.tool_calls:
            if tool_call['name'] in state["tool_whitelist"]:
                continue
            user_prompt = f"Tool name: {tool_call['name']}, Tool args: {tool_call['args']}"
            logger.debug("Generating confirmation request for: %s", user_prompt)
            messages_for_llm = [
                self.system_prompt,
                {
                    "role": "user",
                    "content": user_prompt
                }
            ]
            new_message = self.llm.invoke(messages_for_llm)
            decision: Decision = interrupt({
                "request": {
                    "content": f"Do you approve the agent to {new_message.content}?",
                    "tool_name": tool_call["name"]
                }
            })

            for tool in decision["tool_whitelist_update"]:
                tool_whitelist.add(tool)

            logger.info("Got decision: %s", decision)

            # Reject all tool calls if one is rejected
            if not decision["approved"]:
                logger.info("Rejecting all tool calls")
                tool_call_ids = [tool_call["id"] for tool_call in last_message.tool_calls]

                return Command(goto="reject_action",
                               update={
                                    "decision": "rejected",
                                    "tool_call_ids": tool_call_ids,
                                    "tool_whitelist": tool_whitelist
                                    }
                               )

        return Command(goto="progress_report", update={"decision": "approved", "tool_whitelist": tool_whitelist})


class ProgressReportNode:
    """A node that reports progress to the user."""

    def __init__(self):
        openai_config = Config.get_openai_config()
        self.llm = ChatOpenAI(
            temperature=openai_config.get("temperature", 0),
            model=openai_config.get("model", "gpt-4o-mini"),
            api_key=Config.OPENAI_API_KEY
        )
        self.system_prompt = {
            "role": "system",
            "content": load_prompt("progress_report")
        }

    def __call__(self, state: AgentState):
        logger.debug("ProgressReportNode called with state type: %s", type(state))

        messages = state.get("messages", [])
        if not messages:
            logger.debug("No messages in state, returning unchanged")
            return state

        last_message = messages[-1]
        if not hasattr(last_message, "tool_calls") or not last_message.tool_calls:
            logger.debug("No tool calls in last message, returning unchanged")
            return state

        logger.info("Processing %d tool calls for progress report", len(last_message.tool_calls))

        for tool_call in last_message.tool_calls:
            user_prompt = f"Tool name: {tool_call['name']} Tool args: {tool_call['args']}"
            logger.debug("Generating progress report for: %s", user_prompt)

            messages_for_llm = [
                self.system_prompt,
                {
                    "role": "user",
                    "content": user_prompt
                }
            ]
            new_message = self.llm.invoke(messages_for_llm)
            writer = get_stream_writer()
            writer({"progress": new_message.content})
            logger.info("Progress report generated: %s", new_message.content)

        return state


class ContextInjectionNode:
    """Node that injects runtime context into tool calls for legacy MCP tools"""

    RUNTIME_SUBSTITUTIONS = {
        "member_id": lambda state: state["member_id"],
        "channel_id": lambda state: state["channel_id"],
        "memberId": lambda state: state["member_id"],
        "channelId": lambda state: state["channel_id"],
    }

    def __call__(self, state: AgentState):
        messages = state["messages"]
        if not messages:
            return state

        last_message = messages[-1]
        if not hasattr(last_message, "tool_calls") or not last_message.tool_calls:
            return state

        logger.info("Context injection: Processing %d tool calls", len(last_message.tool_calls))

        # Inject context into tool calls (mainly for MCP tools that don't use InjectedState)
        enhanced_tool_calls = []
        for tool_call in last_message.tool_calls:
            enhanced_args = tool_call["args"].copy()

            # Substitute runtime values for tools that need manual injection
            for arg_name in enhanced_args:
                if arg_name in self.RUNTIME_SUBSTITUTIONS:
                    old_value = enhanced_args[arg_name]
                    new_value = self.RUNTIME_SUBSTITUTIONS[arg_name](state)
                    enhanced_args[arg_name] = new_value
                    logger.info("Context injection: %s %s -> %s", arg_name, old_value, new_value)

            enhanced_tool_calls.append({
                **tool_call,
                "args": enhanced_args
            })

        # Modify the existing message in place
        last_message.tool_calls = enhanced_tool_calls
        return state


class RejectActionNode:
    def __call__(self, state: AgentState):
        writer = get_stream_writer()
        writer({"progress": "Rejected tool call."})
        
        # Get the tool_call_id from state, or find it from the last message
        tool_call_ids = state.get("tool_call_ids")

        if not tool_call_ids:
            logger.error("No tool_call_id found to reject")
            return state

        for tool_call_id in tool_call_ids:
            state["messages"].append(
                ToolMessage(
                    "Tool call operation cancelled by user. \n"
                    "Note: the tool is still fine to use (accessible), it is OK to continue using it",
                    tool_call_id=tool_call_id
                )
            )
            
        return state


async def setup_graph(tools=None, memory_saver=None) -> CompiledStateGraph:
    if tools is None:
        tools = []
    if memory_saver is None:
        memory_saver = InMemorySaver()

    os.environ["OPENAI_API_KEY"] = Config.OPENAI_API_KEY
    openai_config = Config.get_openai_config()
    llm = ChatOpenAI(
        temperature=openai_config.get("temperature", 0),
        model=openai_config.get("model", "gpt-4o-mini"),
        api_key=Config.OPENAI_API_KEY
    )
    llm_with_tools = llm.bind_tools(tools)

    def agent(state: AgentState):
        return {"messages": [llm_with_tools.invoke(state["messages"])]}

    graph_builder = StateGraph(AgentState)
    graph_builder.add_node("agent", agent)

    tool_node = ToolNode(tools=tools)
    human_confirmation_node = HumanConfirmationNode()
    reject_action_node = RejectActionNode()
    progress_report_node = ProgressReportNode()
    context_injection_node = ContextInjectionNode()

    graph_builder.add_node("human_confirmation", human_confirmation_node)
    graph_builder.add_node("reject_action", reject_action_node)
    graph_builder.add_node("progress_report", progress_report_node)
    graph_builder.add_node("context_injection", context_injection_node)
    graph_builder.add_node("tools", tool_node)

    graph_builder.add_conditional_edges(
        "agent",
        route_to_human_confirmation_before_tools,
        {"human_confirmation": "human_confirmation", END: END},
    )

    graph_builder.add_conditional_edges(
        "human_confirmation",
        lambda state: state["decision"],
        {"approved": "progress_report", "rejected": "reject_action"}
    )

    graph_builder.add_edge("progress_report", "context_injection")
    graph_builder.add_edge("context_injection", "tools")
    graph_builder.add_edge("tools", "agent")

    graph_builder.add_edge("reject_action", END)
    graph_builder.add_edge("agent", END)
    graph_builder.set_entry_point("agent")

    # Use the provided memory saver (preserves memory across recompilations)
    graph = graph_builder.compile(checkpointer=memory_saver)
    return graph


# Global cache for graph and tools
class GraphCache:
    def __init__(self):
        self.graph = None
        self.client = None
        self.tools_hash = None
        self.memory_saver = InMemorySaver()  # Persistent memory across recompilations

    def _compute_tools_hash(self, tools) -> str:
        """Compute a hash of the tools to detect changes."""
        # Create a stable representation of tools for hashing
        tool_signatures = []
        for tool in tools:
            # Include tool name, description, and parameter schema
            signature = {
                'name': tool.name,
                'description': tool.description,
                'args_schema': str(tool.args_schema) if hasattr(tool, 'args_schema') else None
            }
            tool_signatures.append(str(signature))

        # Sort to ensure consistent hashing regardless of tool order
        tool_signatures.sort()
        tools_string = '|'.join(tool_signatures)
        return hashlib.md5(tools_string.encode()).hexdigest()

    async def get_graph(self):
        """Get graph, recompiling only if tools have changed."""
        # Get local tools with force_reload in development mode
        local_tools = get_all_tools(force_reload=True)
        logger.info("Loaded %d local tools: %s", len(local_tools), [tool.name for tool in local_tools])

        # Create MCP client and get MCP tools
        mcp_tools = []
        mcp_config = Config.get_mcp_config()
        mcp_enabled = mcp_config.get("enabled", True)

        if mcp_enabled and self.client is None:
            logger.info("Creating MCP client connection")
            servers_config = {}
            for server_name, server_config in mcp_config.get("servers", {}).items():
                server_url = server_config.get("url", Config.MCP_SERVER_URL)
                servers_config[server_name] = {
                    "url": server_url,
                    "transport": server_config.get("transport", "streamable_http")
                }
                logger.info("Configured MCP server '%s' with URL: %s", server_name, server_url)

            if servers_config:
                self.client = MultiServerMCPClient(servers_config)
                mcp_tools: List[BaseTool] = await self.client.get_tools()

                logger.info("Loaded %d MCP tools: %s", len(mcp_tools), [tool.name for tool in mcp_tools])
            else:
                logger.info("No MCP servers configured, using local tools only")

            # TODO: Tool name conflicts?

        elif mcp_enabled and self.client:
            mcp_tools = await self.client.get_tools()
            logger.debug("Retrieved %d MCP tools from existing client", len(mcp_tools))

        elif not mcp_enabled:
            logger.info("MCP disabled in configuration, using local tools only")

        # Combine local and MCP tools
        all_tools = local_tools + mcp_tools
        # Check for duplicate tool names
        name_counts = {}
        for tool in all_tools:
            name_counts[tool.name] = name_counts.get(tool.name, 0) + 1
        duplicate_names = [name for name, count in name_counts.items() if count > 1]
        if duplicate_names:
            logger.warning("Duplicate tool names detected: %s", duplicate_names)
            raise ValueError(f"Duplicate tool names detected: {duplicate_names}")

        current_tools_hash = self._compute_tools_hash(all_tools)
        tool_names = [tool.name for tool in all_tools]

        # Check if we need to recompile
        if self.graph is None:
            logger.info("Initial graph compilation with %d total tools (%d local + %d MCP): %s",
                        len(all_tools), len(local_tools), len(mcp_tools), tool_names)
            logger.debug("Tools hash: %s", current_tools_hash)
            self.graph = await setup_graph(all_tools, self.memory_saver)
            self.tools_hash = current_tools_hash

        elif current_tools_hash != self.tools_hash:
            logger.warning("Tools changed! Recompiling graph...")
            logger.info("Previous hash: %s, New hash: %s", self.tools_hash, current_tools_hash)
            logger.info("New tools (%d local + %d MCP): %s", len(local_tools), len(mcp_tools), tool_names)
            logger.info("Preserving conversation memory across recompilation")

            # Recompile with the SAME memory saver to preserve history
            self.graph = await setup_graph(all_tools, self.memory_saver)
            self.tools_hash = current_tools_hash

        else:
            logger.debug("Using cached graph (tools unchanged, hash: %s)", current_tools_hash)

        return self.graph


# Global cache instance
_graph_cache = GraphCache()


async def get_cached_graph():
    """Get the cached graph instance, recompiling only if tools have changed."""
    return await _graph_cache.get_graph()
