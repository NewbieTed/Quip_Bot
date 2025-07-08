import os

class Config:
    DEBUG = os.getenv("DEBUG", "False").lower() == "true"
    HOST = os.getenv("HOST", "0.0.0.0")
    PORT = int(os.getenv("PORT", 5000))
    OPENAI_API_KEY = os.getenv(
        "OPENAI_API_KEY",
        "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")
