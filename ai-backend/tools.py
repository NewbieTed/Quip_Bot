import time
from typing import List, Dict, Any

from langchain_openai import ChatOpenAI
from langchain_core.tools import tool
from langgraph.prebuilt import create_react_agent
import requests
from models import Choice
from langgraph.config import get_stream_writer

@tool
def weather_tool(city: str) -> str:
    """Fetches current weather for a city."""
    try:
        geo_url = f"https://nominatim.openstreetmap.org/search?q={city}&format=json&limit=1"
        geo_response = requests.get(geo_url, headers={'User-Agent': 'weather-app'}, timeout=5)
        geo_data = geo_response.json()
        if not geo_data:
            return f"Could not find coordinates for {city}."

        lat = geo_data[0]["lat"]
        lon = geo_data[0]["lon"]

        weather_url = f"https://api.open-meteo.com/v1/forecast?latitude={lat}&longitude={lon}&current_weather=true"
        weather_response = requests.get(weather_url, timeout=5)
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

@tool
def create_problem_tool(question: str, choices: List[Choice], contributorId: int = 0) -> str:
    """
    Creates a problem in the Quip backend database. contributorId should not be provided unless
    specifically requested.

    Parameters:
    - question: The problem question text.
    - choices: A list of Choice objects, each with 'choiceText' and 'isCorrect'.
    - contributorId: (Optional) The contributor's ID.

    Returns:
    - The response from the backend as a string.
    """
    status_update_message = "Inserting problem to database...\n"
    writer = get_stream_writer()
    print(f"Message {status_update_message}", flush=True)
    writer({"progress": status_update_message})

    try:
        url = "http://host.docker.internal:8080/problem/create"
        json_body = {
            "question": question,
            "choices": choices,
            "contributorId": contributorId
        }
        response = requests.post(url, json=json_body, timeout=5)
        response.raise_for_status()
        return f"Created problem: {response.text}"
    except Exception as e:
        return f"Error creating problem: {str(e)}"


