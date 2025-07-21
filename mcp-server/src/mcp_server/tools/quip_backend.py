import httpx
from typing import List, Dict, Any
from fastmcp import Context
from mcp_server.app import mcp
from mcp_server.config import Config


async def http_get(url: str, params: Dict[str, Any] = None) -> str:
    """Helper function for HTTP GET requests"""
    async with httpx.AsyncClient(timeout=Config.get_http_timeout()) as client:
        response = await client.get(url, params=params)
        response.raise_for_status()
        return response.text


async def http_post(url: str, json_data: Dict[str, Any]) -> str:
    """Helper function for HTTP POST requests"""
    async with httpx.AsyncClient(timeout=Config.get_http_timeout()) as client:
        response = await client.post(url, json=json_data)
        response.raise_for_status()
        return response.text


@mcp.tool(
    name="list_problem_categories",
    exclude_args=["member_id", "channel_id", "context"]
)
async def list_problem_categories_tool(
        member_id: int = -1,
        channel_id: int = -1,
        context: Context = None) -> str:
    """
    Retrieves the available problem categories of a given server.
    
    Args:
        member_id: User identifier (automatically injected)
        channel_id: Channel identifier (automatically injected)
        context: the request context (automatically injected)
    """
    if context is not None:
        await context.report_progress(0, None, "Querying problem categories...")

    print(f"Got memberId: {member_id}, channel_id: {channel_id}", flush=True)
    return "{\"categories\": [\"test\", \"capitals\"]}"
    # try:
    #     url = f"{Config.get_backend_url()}/problem-categories/list"
    #     params = {
    #         "channelId": channel_id,
    #         "memberId": member_id,
    #     }
    #     response_text = await http_get(url, params)
    #     return f"Problem categories: {response_text}"
    # except Exception as e:
    #     return f"Error fetching problem categories: {str(e)}"


@mcp.tool(name="list_problems_by_category")
async def list_problems_by_category_tool(problem_category_id: int, member_id: int, channel_id: int, context: Context) -> str:
    """
    Retrieves all problems of a certain problem category.
    
    Args:
        problem_category_id: The category ID to query
        member_id: User identifier (automatically injected)
        channel_id: Channel identifier (automatically injected)
    """
    await context.report_progress(0, None, "Querying problems by category...")
    
    try:
        url = f"{Config.get_backend_url()}/problems/list"
        params = {
            "channelId": channel_id,
            "memberId": member_id,
            "problemCategoryId": problem_category_id
        }
        response_text = await http_get(url, params)
        return f"Problems in category: {response_text}"
    except Exception as e:
        return f"Error fetching problems: {str(e)}"


@mcp.tool(name="create_problem")
async def create_problem_tool(
    question: str,
    choices: List[Dict[str, Any]],
    problem_category_id: int,
    member_id: int,
    channel_id: int,
    context: Context
) -> str:
    """
    Creates a problem in the Quip backend database.
    
    Args:
        question: The problem question text
        choices: List of choice objects with 'choiceText' and 'isCorrect' fields
        problem_category_id: The category ID for the problem
        member_id: User identifier (automatically injected)
        channel_id: Channel identifier (automatically injected)
    """
    await context.report_progress(0, None, "Creating problem...")
    
    try:
        url = f"{Config.get_backend_url()}/problems/create"
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


@mcp.tool(name="create_problem_category")
async def create_problem_category_tool(
    problem_category_name: str,
    problem_category_description: str,
    member_id: int,
    channel_id: int,
    context: Context
) -> str:
    """
    Creates a problem category in the Quip backend database.
    
    Args:
        problem_category_name: Name of the problem category
        problem_category_description: Description of the problem category
        member_id: User identifier (automatically injected)
        channel_id: Channel identifier (automatically injected)
    """
    await context.report_progress(0, None, "Creating problem category...")
    
    try:
        url = f"{Config.get_backend_url()}/problem-categories/create"
        json_body = {
            "channelId": channel_id,
            "memberId": member_id,
            "problemCategoryName": problem_category_name,
            "problemCategoryDescription": problem_category_description
        }
        response_text = await http_post(url, json_body)
        return f"Created problem category: {response_text}"
    except Exception as e:
        return f"Error creating problem category: {str(e)}"