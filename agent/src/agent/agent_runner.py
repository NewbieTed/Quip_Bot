import logging
import json
from typing import Dict, List, Any, Literal, Tuple, Optional

# Import the graph cache from graph module
from src.agent.graph import get_cached_graph
from langgraph.graph.state import CompiledStateGraph

from langgraph.types import Command
from langchain_core.messages import HumanMessage
from langchain_core.tools import BaseTool

# Import prompt loader
from src.agent.utils.prompt_loader import load_prompt

# Tool discovery and Redis publishing is now handled in graph.py

# Configure logger for this module
logger = logging.getLogger(__name__)


class AgentRunnerError(Exception):
    """Custom exception for agent runner errors."""
    pass


def load_system_prompt() -> str:
    """Load the main system prompt."""
    return load_prompt("main_system")


def _format_json_response(data: Dict[str, Any]) -> str:
    """Format a response as JSON string, ensuring content ends with newline."""
    if "content" in data and data["content"] and not data["content"].endswith('\n'):
        data = data.copy()  # Don't modify the original
        data["content"] += "\n"
    return json.dumps(data)


def _validate_message(member_message: str) -> None:
    """Validate the input message."""
    if not isinstance(member_message, str) or not member_message.strip():
        logger.error("Invalid message provided: %s", type(member_message))
        raise AgentRunnerError("Provided message must be a non-empty string.")


def _log_agent_start(server_id: int, channel_id: int, member_id: int, action: str = "Starting") -> None:
    """Log agent start with consistent format."""
    logger.info("%s agent run for server_id=%s, channel_id=%s, member_id=%s",
                action, server_id, channel_id, member_id)


def _log_agent_complete(member_id: int) -> None:
    """Log agent completion with consistent format."""
    logger.info("Agent run completed for member_id=%s", member_id)


def _member_message_validation(member_message: str):
    """Handle validation errors and return formatted error response."""
    try:
        _validate_message(member_message)
        return None
    except AgentRunnerError as e:
        logger.error("Validation failed: %s", str(e))
        return _format_json_response({"content": f"Error: {str(e)}"})


async def _setup_agent(
        member_message: str,
        server_id: int,
        channel_id: int,
        member_id: int,
        conversation_id: int,
        tool_whitelist: List[str]
) -> Tuple[CompiledStateGraph, Dict[str, Any], Dict[str, Any]]:
    """Common setup for agent runs."""
    # Get the cached graph (includes unified tool discovery and Redis publishing)
    logger.debug("Retrieving cached graph with integrated tool discovery")
    graph: CompiledStateGraph = await get_cached_graph()

    # Log available tools for debugging
    from src.agent.tools import get_all_tools
    tools: List[BaseTool] = get_all_tools()
    logger.info("Agent has access to %d local tools: %s", len(tools), [tool.name for tool in tools])

    # Create config with thread ID and other context
    config: Dict[str, Any] = _build_graph_config(member_id, server_id, conversation_id)

    # Prepare messages with system prompt and user message
    system_message: str = load_system_prompt()
    messages: List[Dict[str, str]] = [
        {
            "role": "system",
            "content": system_message
        },
        {
            "role": "user",
            "content": member_message.strip()
        }
    ]

    # Create initial state with runtime context
    initial_state = {
        "messages": messages,
        "server_id": server_id,
        "channel_id": channel_id,
        "member_id": member_id,
        "conversation_id": conversation_id,
        "tool_whitelist": set(tool_whitelist)
    }

    return graph, config, initial_state


async def _process_stream(
        graph: CompiledStateGraph,
        state: Dict[str, Any] | Command,
        config: Dict[str, Any],
        stream_mode: List[Literal["values", "updates", "checkpoints", "tasks", "debug", "messages", "custom"]] = None):
    """Process the agent's stream and yield responses."""
    if stream_mode is None:
        stream_mode = ["updates", "custom"]

    last_content: Optional[str] = None

    try:
        async for mode, chunk in graph.astream(state, config, stream_mode=stream_mode):
            if mode == "custom":
                if isinstance(chunk, dict):
                    # If chunk has 'progress', use it as content, otherwise use the whole chunk
                    if 'progress' in chunk:
                        message = {"content": chunk['progress'], "type": "progress"}
                        last_content = chunk['progress']
                    else:
                        message = {"content": str(chunk), "type": "unknown"}
                        last_content = str(chunk)
                else:
                    message = {"content": str(chunk), "type": "unknown"}
                    last_content = str(chunk)
                logger.debug("Custom stream message: %s", message)
                yield _format_json_response(message)
                continue

            if mode == "updates":
                logger.debug("Received update stream chunk %s", chunk)
                # Check for interrupt message
                if isinstance(chunk, dict) and '__interrupt__' in chunk:
                    interrupt_obj = chunk['__interrupt__'][0]
                    request_value = interrupt_obj.value.get('request')
                    if request_value:
                        logger.info("Interrupt request: %s", request_value)
                        # Yield the complete request object with content and tool_name
                        yield _format_json_response({
                            "content": request_value.get('content', str(request_value)),
                            "tool_name": request_value.get('tool_name'),
                            "type": "interrupt"
                        })
                        last_content = request_value.get('content', str(request_value))
                        continue

                # Check for agent response
                if isinstance(chunk, dict) and 'agent' in chunk and 'messages' in chunk["agent"]:
                    last_content = chunk["agent"]["messages"][-1].content.strip()
                    if last_content is not None and last_content != "":
                        yield _format_json_response({"content": last_content, "type": "update"})
                    continue

    except Exception as e:
        logger.exception("Stream error occurred: %s", str(e))
        yield _format_json_response({"content": f"Error: {str(e)}"})
        return

    if not last_content:
        logger.warning("No response generated by assistant")
        yield _format_json_response({"content": "No response generated by the assistant."})

    return


async def run_agent(
        member_message: str,
        server_id: int,
        channel_id: int,
        member_id: int,
        conversation_id: int,
        approved: bool,
        tool_whitelist_update: List[str]
):
    """Run an agent with approval flow."""
    _log_agent_start(server_id, channel_id, member_id, "Resuming")

    # Get common setup
    graph, config, _ = await _setup_agent(
        member_message=member_message,
        server_id=server_id,
        channel_id=channel_id,
        member_id=member_id,
        conversation_id=conversation_id,
        tool_whitelist=[]
    )

    # Handle approval flow if provided
    # None approval means that there is no approval to be made, it is just a regular message
    if approved is not None:
        command: Command = Command(resume={"approved": approved, "tool_whitelist_update": tool_whitelist_update})
        async for message in _process_stream(graph, command, config):
            yield message

    # Skip if message is empty
    if not member_message.strip():
        return

    new_graph_state: Dict[str, Any] = graph.get_state(config).values
    new_graph_state["messages"] += [HumanMessage(member_message)]
    # Continue with the regular message flow
    async for message in _process_stream(graph, new_graph_state, config):
        yield message
    _log_agent_complete(member_id)


async def run_new_agent(
        member_message: str,
        server_id: int,
        channel_id: int,
        member_id: int,
        conversation_id: int,
        tool_whitelist: List[str]
):
    """Run a new agent instance."""
    _log_agent_start(server_id, channel_id, member_id)

    # Validate input
    validation_error = _member_message_validation(member_message)
    if validation_error:
        yield validation_error
        return

    # Get common setup
    graph, config, initial_state = await _setup_agent(
        member_message=member_message,
        server_id=server_id,
        channel_id=channel_id,
        member_id=member_id,
        conversation_id=conversation_id,
        tool_whitelist=tool_whitelist
    )

    # Process the stream
    async for message in _process_stream(graph, initial_state, config):
        yield message

    _log_agent_complete(member_id)





def _build_graph_config(member_id: int, server_id: int, conversation_id: int) -> Dict[str, Any]:
    """Build graph configuration for a specific conversation."""
    return {
        "configurable": {
            "thread_id": str(member_id) + str(server_id) + str(conversation_id),
            "member_id": member_id,
            "server_id": server_id,
            "conversation_id": conversation_id
        }
    }


async def update_tool_whitelist(
        member_id: int,
        conversations: List[Dict[str, Any]],
        added_tools: List[str],
        removed_tools: List[str]
) -> Dict[str, Any]:
    """
    Update tool whitelist for multiple conversations.
    
    Args:
        member_id: The member whose tool whitelist is being updated
        conversations: List of conversation objects with conversationId, serverId, memberId
        added_tools: List of tool names that were added to the whitelist
        removed_tools: List of tool names that were removed from the whitelist
    
    Returns:
        Dictionary with update results and statistics
    """
    logger.info("Updating tool whitelist for member %s across %d conversations",
                member_id, len(conversations))

    # Get the cached graph
    graph: CompiledStateGraph = await get_cached_graph()

    updated_conversations = []
    failed_conversations = []

    for conversation in conversations:
        conversation_id: int = conversation["conversationId"]
        server_id: int = conversation["serverId"]
        conv_member_id: int = conversation["memberId"]

        # Build config for this specific conversation
        config: dict[str, Any] = _build_graph_config(conv_member_id, server_id, conversation_id)

        # Get current state
        current_state: dict[str, Any] = graph.get_state(config).values

        if current_state is None:
            logger.warning("No state found for conversation %s, skipping", conversation_id)
            failed_conversations.append({
                "conversationId": conversation_id,
                "serverId": server_id,
                "error": "No state found"
            })
            continue

        # Get current tool whitelist from state, or initialize empty set
        current_whitelist: set = set(current_state.get("tool_whitelist", []))

        # Apply changes (no need to copy as current_state is a SnapShot, not the original thing
        updated_whitelist = current_whitelist
        for tool in added_tools:
            updated_whitelist.add(tool)
            logger.debug("Added tool '%s' to conversation %s", tool, conversation_id)
        for tool in removed_tools:
            updated_whitelist.discard(tool)
            logger.debug("Removed tool '%s' from conversation %s", tool, conversation_id)

        # Update graph
        try:
            graph.update_state(config, {"tool_whitelist": updated_whitelist})
            updated_conversations.append({
                "conversationId": conversation_id,
                "serverId": server_id,
                "previousToolCount": len(current_whitelist),
                "newToolCount": len(updated_whitelist),
                "addedTools": added_tools,
                "removedTools": removed_tools
            })
            logger.info("Successfully updated tool whitelist for conversation %s (server %s): %d -> %d tools",
                        conversation_id, server_id, len(current_whitelist), len(updated_whitelist))
        except Exception as e:
            logger.error("Failed to update tool whitelist for conversation %s (server %s): %s",
                         conversation_id, server_id, str(e))
            failed_conversations.append({
                "conversationId": conversation_id,
                "serverId": server_id,
                "error": str(e)
            })

    # Log summary
    success_count = len(updated_conversations)
    failure_count = len(failed_conversations)

    logger.info("Tool whitelist update completed for member %s: %d successful, %d failed",
                member_id, success_count, failure_count)

    return {
        "memberId": member_id,
        "totalConversations": len(conversations),
        "successfulUpdates": success_count,
        "failedUpdates": failure_count,
        "updatedConversations": updated_conversations,
        "failedConversations": failed_conversations,
        "addedTools": added_tools,
        "removedTools": removed_tools
    }
