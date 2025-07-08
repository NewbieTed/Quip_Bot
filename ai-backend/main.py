from typing import List, Dict, Any

from langchain_openai import ChatOpenAI
from langchain_core.tools import tool
from langgraph.prebuilt import create_react_agent
import requests
import os

# Use your provided OpenAI API Key securely
api_key = os.getenv("OPENAI_API_KEY", "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")

# Initialize ChatOpenAI for LangGraph compatibility
llm = ChatOpenAI(temperature=0, model="gpt-4o-mini", api_key=api_key)

# Retain your weather tool using @tool
@tool
def weather_tool(city: str) -> str:
    """Fetches current weather for a city using Open-Meteo API (no API key required)."""
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
            temperature = current["temperature"]
            windspeed = current["windspeed"]
            weathercode = current["weathercode"]
            return (
                f"Current weather in {city}: Temperature {temperature}°C, "
                f"Windspeed {windspeed} km/h, Weather code {weathercode}."
            )
        else:
            return f"Could not fetch weather for {city}."
    except Exception as e:
        return f"Error fetching weather data: {str(e)}"

@tool
def create_problem_tool(question: str, choices: List[Dict[str, Any]]) -> str:
    """
    Creates a problem in the Quip backend database.

    Parameters:
    - question: The problem question text.
    - choices: A list of dictionaries, each with 'choiceText' and 'isCorrect'.
    - contributorId: The contributor's ID.

    Returns:
    - The response from the backend as a string.
    """
    try:
        url = "http://localhost:8080/problem/create"
        json_body = {
            "question": question,
            "choices": choices,
            "contributorId": 0
        }
        response = requests.post(url, json=json_body, timeout=5)
        response.raise_for_status()
        return f"Response from /problem/create: {response.text}"
    except Exception as e:
        return f"Error creating problem: {str(e)}"

# Create the LangGraph ReAct agent with your tools
graph = create_react_agent(
    llm,
    tools=[weather_tool, create_problem_tool]
)

# Compile the graph
app = graph



if __name__ == "__main__":
    # Correctly pass structured input for LangGraph
    for output in app.stream({"messages": ["Insert a question for me into the database that is related to capital cities"]}):
        print(output)
