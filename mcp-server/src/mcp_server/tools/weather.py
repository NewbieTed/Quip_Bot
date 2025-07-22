import httpx
import logging

from fastmcp import Context
from src.mcp_server.app import mcp

logger = logging.getLogger(__name__)


@mcp.tool(name="weather_tool")
async def weather_tool(city: str, context: Context) -> str:
    """Fetches current weather for a city."""
    logger.info("Weather tool called for city: %s", city)
    status_update_message = "Looking up weather...\n"
    await context.report_progress(0, None, status_update_message)
    try:
        async with httpx.AsyncClient(timeout=5) as client:
            geo_url = f"https://nominatim.openstreetmap.org/search?q={city}&format=json&limit=1"
            geo_response = await client.get(geo_url, headers={'User-Agent': 'weather-app'})
            geo_data = geo_response.json()
            if not geo_data:
                logger.warning("Could not find coordinates for city: %s", city)
                return f"Could not find coordinates for {city}."

            lat = geo_data[0]["lat"]
            lon = geo_data[0]["lon"]

            weather_url = f"https://api.open-meteo.com/v1/forecast?latitude={lat}&longitude={lon}&current_weather=true"
            weather_response = await client.get(weather_url)
            weather_data = weather_response.json()

            if "current_weather" in weather_data:
                current = weather_data["current_weather"]
                result = (
                    f"Current weather in {city}: "
                    f"{current['temperature']}°C, Windspeed {current['windspeed']} km/h, "
                    f"Weather code {current['weathercode']}."
                )
                logger.info("Weather data retrieved successfully for %s", city)
                return result
            else:
                logger.error("No current weather data in response for %s", city)
                return f"Could not fetch weather for {city}."
    except Exception as e:
        logger.exception("Error fetching weather data for %s: %s", city, str(e))
        return f"Error fetching weather data: {str(e)}"

