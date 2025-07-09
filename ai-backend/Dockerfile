FROM python:3.12-slim

# Set working directory inside the container
WORKDIR /app

# Copy requirements and install dependencies
COPY requirements.txt .
RUN pip install --no-cache-dir -r requirements.txt

# Copy the rest of your Flask app code
COPY . .

# Expose Flask's port
EXPOSE 5000

# Run Flask with host and port configured for Docker
CMD ["python", "app.py"]