# Agent Service

This is the Agent service for the Quip Bot, providing AI capabilities through LangGraph and tool integration.

## Features

- LangGraph-based agent with tool support
- Automatic tool discovery from the tools directory
- Integration with MCP (Model Context Protocol) servers
- Streaming responses for real-time feedback

## Setup

1. Install dependencies:
```bash
pip install -r requirements.txt
```

2. Set up environment variables:
```bash
cp .env.example .env
# Edit .env with your configuration
```

3. Run the service:
```bash
uvicorn src.agent.main:app --reload
```

## Configuration

Configuration is managed through:
- Environment variables (see `.env.example`)
- YAML configuration in `src/config/config.yaml`

## Tools

Tools are automatically discovered from the `src/agent/tools` directory. To add a new tool:

1. Create a new Python file in the tools directory or a subdirectory
2. Define functions decorated with `@tool` from langchain_core.tools
3. The tool will be automatically discovered and made available to the agent

See `src/agent/tools/README.md` for more details on creating tools.

## Development

For development:
- Use `uvicorn src.agent.main:app --reload` to automatically reload on changes
- Run tests with `pytest`
- Format code with `black` and `isort`