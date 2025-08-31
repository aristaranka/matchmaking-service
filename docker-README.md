# Docker Containerization Guide

This guide explains how to run the Matchmaking Service using Docker and Docker Compose.

## Prerequisites

- Docker Engine 20.10+ 
- Docker Compose 2.0+
- At least 2GB RAM available for containers

## Quick Start

### 1. Basic Setup (Application + Redis)

```bash
# Build and start all services
docker-compose up --build

# Or run in detached mode
docker-compose up -d --build
```

The application will be available at:
- **Main Application**: http://localhost:8080
- **Health Check**: http://localhost:8080/actuator/health
- **H2 Console**: http://localhost:8080/h2-console
- **Metrics**: http://localhost:8080/actuator/prometheus

### 2. Production Setup (with Nginx)

```bash
# Start with production profile (includes Nginx)
docker-compose --profile production up -d --build
```

With Nginx reverse proxy:
- **Application**: http://localhost (port 80)
- **Direct App Access**: http://localhost:8080

## Container Architecture

```
┌─────────────────┐    ┌──────────────────┐    ┌─────────────────┐
│   Nginx Proxy   │────│  Spring Boot App │────│   Redis Cache   │
│   (Optional)    │    │   (Port 8080)    │    │   (Port 6379)   │
│   (Port 80)     │    │                  │    │                 │
└─────────────────┘    └──────────────────┘    └─────────────────┘
```

## Available Services

### Core Services
- **app**: Spring Boot application container
- **redis**: Redis cache for session storage and matchmaking queues

### Optional Services  
- **nginx**: Reverse proxy with rate limiting (production profile)

## Environment Variables

Key environment variables that can be customized:

```yaml
# Redis Configuration
SPRING_DATA_REDIS_HOST=redis
SPRING_DATA_REDIS_PORT=6379

# JWT Security
SECURITY_JWT_SECRET=your-secret-key
SECURITY_JWT_TTL_SECONDS=86400

# Application Settings
SPRING_PROFILES_ACTIVE=docker
SERVER_PORT=8080

# JVM Settings
JAVA_OPTS=-Xmx512m -Xms256m
```

## Docker Commands

### Build and Run
```bash
# Build only the application image
docker build -t matchmaking-service .

# Run with compose
docker-compose up -d

# View logs
docker-compose logs -f app
docker-compose logs -f redis

# Stop services
docker-compose down

# Stop and remove volumes
docker-compose down -v
```

### Development Commands
```bash
# Rebuild app container only
docker-compose up -d --build app

# Scale the application (multiple instances)
docker-compose up -d --scale app=3

# Execute commands in running container
docker-compose exec app bash
docker-compose exec redis redis-cli
```

### Debugging
```bash
# Check container health
docker-compose ps

# View detailed logs
docker-compose logs --tail=100 -f app

# Check resource usage
docker stats

# Inspect container
docker-compose exec app env
```

## Volumes and Data Persistence

- **redis_data**: Persists Redis data between container restarts
- **app_logs**: Application logs volume

```bash
# Backup Redis data
docker-compose exec redis redis-cli BGSAVE

# View volume information
docker volume ls
docker volume inspect matchmaking-service_redis_data
```

## Health Checks

All services include health checks:

- **App**: HTTP check on `/actuator/health`
- **Redis**: Redis ping command
- **Nginx**: HTTP check (when enabled)

Check health status:
```bash
docker-compose ps
curl http://localhost:8080/actuator/health
```

## Networking

Services communicate through a custom bridge network `matchmaking-network`:

- Internal DNS resolution (app → redis)
- Isolated from other Docker networks
- External access through published ports

## Security Considerations

### Development
- H2 console enabled for debugging
- CORS enabled for all origins
- Debug logging enabled

### Production Recommendations
1. **Change JWT Secret**: Set strong `SECURITY_JWT_SECRET`
2. **Use External Database**: Replace H2 with PostgreSQL/MySQL
3. **Enable HTTPS**: Configure SSL certificates in Nginx
4. **Restrict CORS**: Limit allowed origins
5. **Monitor Resources**: Set up proper monitoring

## Troubleshooting

### Common Issues

1. **Port Conflicts**
   ```bash
   # Check what's using port 8080
   lsof -i :8080
   
   # Use different port
   docker-compose up -p 8081:8080
   ```

2. **Memory Issues**
   ```bash
   # Check container memory usage
   docker stats
   
   # Increase memory limits in docker-compose.yml
   ```

3. **Redis Connection Issues**
   ```bash
   # Test Redis connectivity
   docker-compose exec app ping redis
   docker-compose exec redis redis-cli ping
   ```

4. **Build Issues**
   ```bash
   # Clean build
   docker-compose down -v
   docker system prune -f
   docker-compose up --build --force-recreate
   ```

### Logs and Monitoring

```bash
# Application logs
docker-compose logs -f app

# All services logs
docker-compose logs -f

# System resource monitoring
docker stats $(docker-compose ps -q)
```

## Production Deployment

For production deployment:

1. Use external managed Redis (AWS ElastiCache, etc.)
2. Replace H2 with production database
3. Set up proper SSL/TLS certificates
4. Configure monitoring and alerting
5. Set up log aggregation
6. Use container orchestration (Kubernetes, Docker Swarm)

Example production environment variables:
```bash
export SPRING_DATA_REDIS_HOST=your-redis-cluster.cache.amazonaws.com
export SPRING_DATASOURCE_URL=jdbc:postgresql://your-db:5432/matchmaking
export SECURITY_JWT_SECRET=your-super-secure-256-bit-key
```
