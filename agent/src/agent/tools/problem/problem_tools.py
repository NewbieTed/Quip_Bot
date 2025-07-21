from typing import List
from langchain_core.tools import tool
from src.agent.models import Choice
from langchain_core.tools import BaseTool
import sys
import inspect
from src.config import Config
from src.agent.utils.http_client import http_get, http_post
from typing_extensions import Annotated
from langgraph.prebuilt import InjectedState

__all_tools__ = []


@tool("list_problem_categories_tool")
async def list_problem_categories_tool(
        channel_id: Annotated[int, InjectedState("channel_id")],
        member_id: Annotated[int, InjectedState("member_id")]) -> str:
    """
    Retrieves the available problem categories of a given server (which is queried by channelId).
    May be used to check if there are available categories to use (or check for potential duplicates)

    Parameters:
    - channelId: the channel ID the user is interacting from.
    - memberId: the member's ID.

    Returns:
    - The response from the backend as a string.
    """
    try:
        backend_config = Config.get_backend_config()
        url = backend_config["url"] + "/problem-categories/list"
        params = {
            "channelId": channel_id,
            "memberId": member_id,
        }
        response_text = await http_get(url, params)
        return f"Queried problem categories: {response_text}"
    except Exception as e:
        return f"Error fetching problem categories: {str(e)}"


@tool(name_or_callable="list_problems_by_category_tool")
async def list_problems_by_category_tool(
        channel_id: Annotated[int, InjectedState("channel_id")],
        member_id: Annotated[int, InjectedState("member_id")],
        problem_category_id: int) -> str:
    """
    Retrieves all problems of a certain problem category of a given server (which is queried by channelId).
    May be used to determine if a question should be added or not (avoid duplicates).

    Parameters:
    - channelId: the channel ID the user is interacting from.
    - memberId: the member's ID.
    - problem_category_id: the problem category ID

    Returns:
    - The response from the backend as a string.
    """
    try:
        backend_config = Config.get_backend_config()
        url = backend_config["url"] + "/problems/list"
        params = {
            "channelId": channel_id,
            "memberId": member_id,
            "problemCategoryId": problem_category_id
        }
        response_text = await http_get(url, params)
        return f"Queried problem categories: {response_text}"
    except Exception as e:
        return f"Error fetching problem categories: {str(e)}"


@tool(name_or_callable="create_problem_tool")
async def create_problem_tool(
        question: str,
        choices: List[Choice],
        channel_id: Annotated[int, InjectedState("channel_id")],
        member_id: Annotated[int, InjectedState("member_id")],
        problem_category_id: int
        ) -> str:
    """
    Creates a problem in the Quip backend database.
    Do check if similar problems exists before calling this tool.

    Parameters:
    - question: The problem question text.
    - choices: A list of Choice objects, each with 'choiceText' of type string and 'isCorrect' of type boolean.
    - channel_id: The channel ID of where the request is made.
    - member_id: The member's ID.
    - problem_category_id: the category ID of the problem.

    Returns:
    - The response from the backend as a string.
    """

    try:
        backend_config = Config.get_backend_config()
        url = backend_config["url"] + "/problems/create"
        json_body = {
            "question": question,
            "choices": choices,
            "channelId": channel_id,
            "problemCategoryId": problem_category_id,
            "memberId": member_id,
        }
        response_text = await http_post(url, json_body)
        return f"Created problem: {response_text}"
    except Exception as e:
        return f"Error creating problem: {str(e)}"


@tool(name_or_callable="create_problem_category_tool")
async def create_problem_category_tool(
        channel_id: Annotated[int, InjectedState("channel_id")],
        member_id: Annotated[int, InjectedState("member_id")],
        problem_category_name: str,
        problem_category_description: str) -> str:
    """
    Creates a problem category in the Quip backend database.
    Do check if similar categories exist before calling this tool.

    Parameters:
    - channel_id: The channel ID of where the request is made.
    - member_id: The member's ID.
    - problem_category_name: The inserted problem category's name
    - problem_category_description: The inserted problem category's description (cannot be null)

    Returns:
    - The response from the backend as a string.
    """

    try:
        backend_config = Config.get_backend_config()
        url = backend_config["url"] + "/problem-categories/create"
        json_body = {
            "channelId": channel_id,
            "memberId": member_id,
            "problemCategoryName": problem_category_name,
            "problemCategoryDescription": problem_category_description
        }
        response_text = await http_post(url, json_body)
        return f"Created problem: {response_text}"
    except Exception as e:
        return f"Error creating problem: {str(e)}"


current_module = sys.modules[__name__]
for name, obj in inspect.getmembers(current_module):
    if isinstance(obj, BaseTool):
        print(f"Added {name} to tools", flush=True)
        __all_tools__.append(obj)