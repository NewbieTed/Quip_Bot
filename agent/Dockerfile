FROM python:3.12-slim

# Set working directory inside the container
WORKDIR /app

# Copy requirements and install dependencies
COPY requirements.in .
RUN pip install --no-cache-dir -r requirements.in

# Copy the rest of your agent code
COPY . .

# Expose Flask's port
EXPOSE 5000

# Run FastAPI with host and port configured for Docker
CMD ["uvicorn", "src.agent.main:app", "--host", "0.0.0.0", "--port", "5000"]