from typing import List
from langchain_core.tools import tool
import httpx
from app.models import Choice
from langgraph.config import get_stream_writer
from langchain_core.tools import BaseTool
import sys
import inspect
from .config import Config
from app.utils.http_client import http_get, http_post

__all_tools__ = []


@tool(name_or_callable="weather_tool")
async def weather_tool(city: str) -> str:
    """Fetches current weather for a city."""
    status_update_message = "Querying problem categories...\n"
    writer = get_stream_writer()
    print(f"Message {status_update_message}", flush=True)
    writer({"progress": status_update_message})
    try:
        async with httpx.AsyncClient(timeout=5) as client:
            geo_url = f"https://nominatim.openstreetmap.org/search?q={city}&format=json&limit=1"
            geo_response = await client.get(geo_url, headers={'User-Agent': 'weather-app'})
            geo_data = geo_response.json()
            if not geo_data:
                return f"Could not find coordinates for {city}."

            lat = geo_data[0]["lat"]
            lon = geo_data[0]["lon"]

            weather_url = f"https://api.open-meteo.com/v1/forecast?latitude={lat}&longitude={lon}&current_weather=true"
            weather_response = await client.get(weather_url)
            weather_data = weather_response.json()

            if "current_weather" in weather_data:
                current = weather_data["current_weather"]
                return (
                    f"Current weather in {city}: "
                    f"{current['temperature']}°C, Windspeed {current['windspeed']} km/h, "
                    f"Weather code {current['weathercode']}."
                )
            else:
                return f"Could not fetch weather for {city}."
    except Exception as e:
        return f"Error fetching weather data: {str(e)}"


@tool("list_problem_categories_tool")
async def list_problem_categories_tool(channel_id: int, member_id: int) -> str:
    """
    Retrieves the available problem categories of a given server (which is queried by channelId).
    May be used to check if there are available categories to use (or check for potential duplicates)

    Parameters:
    - channelId: the channel ID the user is interacting from.
    - memberId: the member's ID.

    Returns:
    - The response from the backend as a string.
    """
    status_update_message = f"Querying problem categories...\n"
    writer = get_stream_writer()
    print(f"Message {status_update_message}", flush=True)
    writer({"progress": status_update_message})

    try:
        url = Config.BACKEND_APP_URL + "/problem-categories/list"
        params = {
            "channelId": channel_id,
            "memberId": member_id,
        }
        response_text = await http_get(url, params)
        return f"Queried problem categories: {response_text}"
    except Exception as e:
        return f"Error fetching problem categories: {str(e)}"


@tool(name_or_callable="list_problems_by_category_tool")
async def list_problems_by_category_tool(channel_id: int, member_id: int, problem_category_id: int) -> str:
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
    status_update_message = "Querying problems based on problem category...\n"
    writer = get_stream_writer()
    print(f"Message: {status_update_message}", flush=True)
    writer({"progress": status_update_message})

    try:
        url = Config.BACKEND_APP_URL + "/problems/list"
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
        channel_id: int,
        problem_category_id: int,
        member_id: int) -> str:
    """
    Creates a problem in the Quip backend database.
    Do check if similar problems exists before calling this tool.

    Parameters:
    - question: The problem question text.
    - choices: A list of Choice objects, each with 'choiceText' of type string and 'isCorrect' of type boolean.
    - channel_id: The channel ID of where the request is made.
    - problem_category_id: the category ID of the problem.
    - member_id: The member's ID.

    Returns:
    - The response from the backend as a string.
    """
    status_update_message = "Inserting problem to database...\n"
    writer = get_stream_writer()
    print(f"Message {status_update_message}", flush=True)
    writer({"progress": status_update_message})

    try:
        url = Config.BACKEND_APP_URL + "/problems/create"
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
        channel_id: int,
        member_id: int,
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
    status_update_message = "Inserting problem category to database...\n"
    writer = get_stream_writer()
    print(f"Message {status_update_message}", flush=True)
    writer({"progress": status_update_message})

    try:
        url = Config.BACKEND_APP_URL + "/problem-categories/create"
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
