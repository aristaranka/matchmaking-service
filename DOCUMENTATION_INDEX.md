# 📚 Documentation Index

Welcome to the Matchmaking Service documentation! This index provides links to all available documentation.

## 📋 Core Documentation

### 🚀 [README.md](README.md)
- **Overview**: Complete project overview and quick start guide
- **Features**: Core functionality and technical specifications
- **Installation**: Local development and Docker setup instructions
- **Architecture**: System design and component overview

### 🔧 [Docker Setup](DOCKER_SETUP.md)
- **Quick Start**: Get running with Docker in minutes
- **Architecture**: Container setup and networking
- **Production**: Deployment guidelines and best practices
- **Troubleshooting**: Common issues and solutions

## 📖 API Documentation

### 📚 [Complete API Documentation](docs/API_DOCUMENTATION.md)
- **All Endpoints**: Comprehensive API reference
- **Authentication**: JWT token management
- **Request/Response**: Detailed schemas and examples
- **Error Handling**: Status codes and error formats
- **Data Models**: Complete schema definitions

### 🚀 [API Usage Examples](API_USAGE_EXAMPLES.md)
- **Getting Started**: Step-by-step API usage
- **Code Examples**: JavaScript, Python, Java, curl
- **WebSocket Integration**: Real-time communication
- **Testing**: Postman collections and test scripts
- **Best Practices**: Error handling and rate limiting

## 🛠️ Interactive Documentation

When the service is running locally, access these interactive docs:

### 🌐 Swagger UI
- **URL**: http://localhost:8080/swagger-ui.html
- **Features**: Interactive API explorer
- **Testing**: Try endpoints directly in browser
- **Authentication**: Built-in JWT token management

### 📄 OpenAPI Specification
- **JSON**: http://localhost:8080/api/v3/api-docs
- **YAML**: http://localhost:8080/api/v3/api-docs.yaml
- **Import**: Use with Postman, Insomnia, or other tools

## 🏗️ Development Documentation

### 🐳 Docker Documentation
- **[Docker README](docker-README.md)**: Comprehensive Docker guide
- **Development**: Local development with containers
- **Production**: Scalable deployment strategies
- **Monitoring**: Container health and metrics

### ⚙️ Configuration
- **Application Properties**: See [application.properties](src/main/resources/application.properties)
- **Docker Environment**: See [docker-compose.yml](docker-compose.yml)
- **Security Settings**: JWT and authentication configuration

## 🔍 Quick Reference

### 🚀 Quick Start Commands
```bash
# Local Development
./mvnw spring-boot:run

# Docker Development
docker-compose up -d

# Docker Production
docker-compose --profile production up -d
```

### 🔗 Important Endpoints
- **Health Check**: http://localhost:8080/actuator/health
- **API Base**: http://localhost:8080/api
- **Web Interface**: http://localhost:8080
- **Swagger UI**: http://localhost:8080/swagger-ui.html

### 🔐 Authentication Flow
1. **Register**: `POST /api/auth/register`
2. **Login**: `POST /api/auth/login`
3. **Use Token**: Include `Authorization: Bearer <token>` header
4. **Join Queue**: `POST /api/match/join`

## 📊 System Monitoring

### 📈 Metrics and Health
- **Health**: http://localhost:8080/actuator/health
- **Metrics**: http://localhost:8080/actuator/metrics
- **Prometheus**: http://localhost:8080/actuator/prometheus
- **Info**: http://localhost:8080/actuator/info

### 🔍 Debugging
- **Logs**: Check container logs with `docker-compose logs -f app`
- **H2 Console**: http://localhost:8080/h2-console (development only)
- **Redis**: Connect with `docker-compose exec redis redis-cli`

## 🤝 Contributing

### 📝 Documentation Guidelines
- Keep documentation up-to-date with code changes
- Include examples for all new endpoints
- Test all code examples before committing
- Follow the existing documentation structure

### 🧪 Testing Documentation
- Verify all URLs and endpoints work
- Test code examples in multiple environments
- Validate OpenAPI specification generates correctly
- Check Docker documentation matches actual behavior

## 📞 Support

### 🐛 Issues and Bugs
- Check existing documentation first
- Search through API examples
- Test with Swagger UI
- Create detailed issue reports

### 💡 Feature Requests
- Review current API capabilities
- Check roadmap and planned features
- Provide detailed use cases
- Consider contributing implementations

## 📋 Documentation Checklist

When updating the service, ensure these docs are current:

- [ ] **README.md** - Project overview and quick start
- [ ] **API_DOCUMENTATION.md** - Complete API reference
- [ ] **API_USAGE_EXAMPLES.md** - Code examples and integration
- [ ] **DOCKER_SETUP.md** - Docker deployment guide
- [ ] **OpenAPI annotations** - In-code documentation
- [ ] **Configuration examples** - Environment variables and properties

---

**Happy Coding! 🎮**

*Last Updated: 2024-01-01*  
*Documentation Version: 1.0.0*
