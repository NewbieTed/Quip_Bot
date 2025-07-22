# Quip Bot 🤖

[![Build Status](https://img.shields.io/badge/build-passing-brightgreen)](https://github.com/NewbieTed/Quip_Bot)
[![License](https://img.shields.io/badge/license-MIT-blue.svg)](LICENSE)
[![Docker](https://img.shields.io/badge/docker-ready-blue)](docker-compose.yml)
[![Java](https://img.shields.io/badge/java-17-orange)](backend/)
[![Python](https://img.shields.io/badge/python-3.12-blue)](agent/)
[![TypeScript](https://img.shields.io/badge/typescript-5.8-blue)](frontend/)

A modern, microservices-based Discord bot platform with AI-powered assistance capabilities. Built with a robust architecture featuring Spring Boot backend, FastAPI agent service, and Model Context Protocol (MCP) integration.

## 🚀 Features

- **Discord Integration**: Full-featured Discord bot with real-time messaging
- **AI-Powered Assistant**: Intelligent responses using OpenAI integration
- **Microservices Architecture**: Scalable, containerized services
- **Model Context Protocol**: Extensible tool integration via MCP
- **Real-time Communication**: WebSocket support for live interactions
- **Database Integration**: PostgreSQL with MyBatis Plus ORM
- **Health Monitoring**: Built-in health checks and logging
- **Development Ready**: Hot reload, comprehensive testing, and debugging tools


## 📋 Prerequisites

- **Docker & Docker Compose**: For containerized deployment
- **Java 17+**: For backend development
- **Python 3.12+**: For agent and MCP server development
- **Node.js 18+**: For frontend development
- **PostgreSQL**: Database (handled by Docker)

## 🚀 Quick Start

### Using Docker (Recommended)

1. **Clone the repository**
   ```bash
   git clone https://github.com/your-org/quip-bot.git
   cd quip-bot
   ```

2. **Set up environment variables**
   ```bash
   cp .env.example .env
   # Edit .env with your configuration
   ```

3. **Start all services**
   ```bash
   docker-compose up -d
   ```

4. **Verify deployment**
   ```bash
   # Check service health
   curl http://localhost:8080/health
   curl http://localhost:5001/health
   curl http://localhost:8000/health
   ```

### Manual Development Setup

<details>
<summary>Click to expand manual setup instructions</summary>

#### Backend (Spring Boot)
```bash
cd backend
./gradlew bootRun
```

#### Agent (FastAPI)
```bash
cd agent
python -m venv venv
source venv/bin/activate  # On Windows: venv\Scripts\activate
pip install -e .
python -m agent.main
```

#### Frontend (Discord Bot)
```bash
cd frontend
npm install
npm run dev
```

#### MCP Server
```bash
cd mcp-server
python -m venv venv
source venv/bin/activate
pip install -e .
python -m mcp_server.main
```

</details>

## 🔧 Configuration

### Environment Variables

| Variable | Description | Default |
|----------|-------------|---------|
| `DB_NAME` | PostgreSQL database name | `quip_db` |
| `DB_USER` | Database username | `postgres` |
| `DB_PASSWORD` | Database password | Required |
| `BACKEND_URL` | Backend service URL | `http://localhost:8080` |
| `AGENT_URL` | Agent service URL | `http://localhost:5000` |
| `MCP_SERVER_URL` | MCP server URL | `http://localhost:8000` |

### Service Ports

- **Frontend (Discord Bot)**: 3000
- **Backend (Spring Boot)**: 8080
- **Agent (FastAPI)**: 5001
- **MCP Server**: 8000
- **PostgreSQL**: 5432

## 🧪 Testing

### Backend Tests
```bash
cd backend
./gradlew test
./gradlew pitest  # Mutation testing
```

### Agent Tests
```bash
cd agent
pytest
```

### Frontend Tests
```bash
cd frontend
npm test
```

## 📊 Monitoring & Logging

All services implement structured logging with centralized log management:

- **Backend**: `backend/logs/backend.log`
- **Agent**: `agent/logs/agent.log`
- **Frontend**: `frontend/logs/frontend.log`
- **MCP Server**: `mcp-server/logs/mcp-server.log`

Health check endpoints:
- Backend: `GET /health`
- Agent: `GET /health`
- MCP Server: `GET /health`

## 🛠️ Development

### Adding New MCP Tools

1. Create tool in `mcp-server/src/mcp_server/tools/`
2. Register in `mcp-server/src/mcp_server/main.py`
3. Update configuration in `mcp-server/config.yaml`

### Database Migrations

```bash
cd backend
./gradlew flywayMigrate
```

### Code Quality

- **Java**: Checkstyle, SpotBugs, PMD
- **Python**: Black, isort, mypy
- **TypeScript**: ESLint, Prettier

## 🚢 Deployment

### Production Docker Build
```bash
docker-compose -f docker-compose.prod.yml up -d
```

### Kubernetes
```bash
kubectl apply -f k8s/
```


## 📝 API Documentation

- **Backend API**: http://localhost:8080/swagger-ui.html
- **Agent API**: http://localhost:5001/docs
- **MCP Server**: http://localhost:8000/docs


## 🐛 Troubleshooting

### Common Issues

**Services not starting?**
```bash
docker-compose logs [service-name]
```

**Database connection issues?**
```bash
docker-compose exec quip-db psql -U postgres -d quip_db
```

**Port conflicts?**
```bash
# Check what's using the port
lsof -i :8080
```

## 📄 License

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.

## 🙏 Acknowledgments

- [Spring Boot](https://spring.io/projects/spring-boot) - Backend framework
- [FastAPI](https://fastapi.tiangolo.com/) - Agent service framework
- [Discord.js](https://discord.js.org/) - Discord API wrapper
- [Model Context Protocol](https://modelcontextprotocol.io/) - Tool integration standard

---

**Maintained by**: Quip Team  
**Last Updated**: July 2025  
**Status**: ✅ Active Development