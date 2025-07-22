# agent/utils/http_client.py

import httpx
import logging
import asyncio
from typing import Optional

logger = logging.getLogger(__name__)

async def http_get(url: str, params: dict, timeout: int = 10, retries: int = 3) -> str:
    logger.info(f"Making GET request to: {url} with params: {params}")
    
    for attempt in range(retries):
        try:
            async with httpx.AsyncClient(timeout=timeout) as client:
                response = await client.get(url, params=params)
                logger.info(f"Response status: {response.status_code}")
                response.raise_for_status()
                return response.text
        except httpx.ConnectError as e:
            logger.warning(f"Connection error to {url} (attempt {attempt + 1}/{retries}): {str(e)}")
            if attempt == retries - 1:
                raise Exception(f"Failed to connect to backend at {url} after {retries} attempts: {str(e)}")
            await asyncio.sleep(2 ** attempt)  # Exponential backoff
        except httpx.TimeoutException as e:
            logger.warning(f"Timeout error to {url} (attempt {attempt + 1}/{retries}): {str(e)}")
            if attempt == retries - 1:
                raise Exception(f"Request timeout to backend at {url} after {retries} attempts: {str(e)}")
            await asyncio.sleep(2 ** attempt)
        except httpx.HTTPStatusError as e:
            logger.error(f"HTTP error {e.response.status_code} from {url}: {e.response.text}")
            raise Exception(f"HTTP {e.response.status_code} error from backend: {e.response.text}")
        except Exception as e:
            logger.error(f"Unexpected error calling {url}: {str(e)}")
            raise Exception(f"Unexpected error calling backend: {str(e)}")


async def http_post(url: str, json_body: dict, timeout: int = 10, retries: int = 3) -> str:
    logger.info(f"Making POST request to: {url} with body: {json_body}")
    
    for attempt in range(retries):
        try:
            async with httpx.AsyncClient(timeout=timeout) as client:
                response = await client.post(url, json=json_body)
                logger.info(f"Response status: {response.status_code}")
                response.raise_for_status()
                return response.text
        except httpx.ConnectError as e:
            logger.warning(f"Connection error to {url} (attempt {attempt + 1}/{retries}): {str(e)}")
            if attempt == retries - 1:
                raise Exception(f"Failed to connect to backend at {url} after {retries} attempts: {str(e)}")
            await asyncio.sleep(2 ** attempt)  # Exponential backoff
        except httpx.TimeoutException as e:
            logger.warning(f"Timeout error to {url} (attempt {attempt + 1}/{retries}): {str(e)}")
            if attempt == retries - 1:
                raise Exception(f"Request timeout to backend at {url} after {retries} attempts: {str(e)}")
            await asyncio.sleep(2 ** attempt)
        except httpx.HTTPStatusError as e:
            logger.error(f"HTTP error {e.response.status_code} from {url}: {e.response.text}")
            raise Exception(f"HTTP {e.response.status_code} error from backend: {e.response.text}")
        except Exception as e:
            logger.error(f"Unexpected error calling {url}: {str(e)}")
            raise Exception(f"Unexpected error calling backend: {str(e)}")
