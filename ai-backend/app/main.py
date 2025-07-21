import logging
from contextlib import asynccontextmanager

from fastapi import FastAPI
from fastapi.middleware.cors import CORSMiddleware
from app.config import Config
from app.api.routes import router
from app import graph as graph_module
from app.graph import setup_graph

# Configure logging
logging.basicConfig(
    level=logging.INFO,
    format='%(asctime)s - %(name)s - %(levelname)s - %(message)s',
    handlers=[
        logging.StreamHandler(),
        logging.FileHandler('app.log')
    ]
)

# Set specific log levels for different modules
logging.getLogger('app.graph').setLevel(logging.INFO)
logging.getLogger('app.agent_runner').setLevel(logging.INFO)
logging.getLogger('httpx').setLevel(logging.WARNING)  # Reduce HTTP client noise
logging.getLogger('openai').setLevel(logging.WARNING)  # Reduce OpenAI client noise

logger = logging.getLogger(__name__)


@asynccontextmanager
async def lifespan(app: FastAPI):
    logger.info("Starting application lifespan")
    # Graph will be initialized on first request due to caching system
    yield
    logger.info("Shutting down application")

app = FastAPI(lifespan=lifespan)

app.add_middleware(
    CORSMiddleware,
    allow_origins=["*"],
    allow_credentials=True,
    allow_methods=["*"],
    allow_headers=["*"],
)

app.include_router(router)



if __name__ == "__main__":
    import uvicorn
    uvicorn.run(app, host=Config.HOST, port=Config.PORT)
