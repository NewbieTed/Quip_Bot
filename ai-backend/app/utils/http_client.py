# app/utils/http_client.py

import httpx

async def http_get(url: str, params: dict, timeout: int = 10) -> str:
    async with httpx.AsyncClient(timeout=timeout) as client:
        response = await client.get(url, params=params)
        response.raise_for_status()
        return response.text


async def http_post(url: str, json_body: dict, timeout: int = 10) -> str:
    async with httpx.AsyncClient(timeout=timeout) as client:
        response = await client.post(url, json=json_body)
        response.raise_for_status()
        return response.text
