# 📚 Matchmaking Service API Documentation

## Overview

The Matchmaking Service provides a comprehensive REST API for real-time player matchmaking with Elo-based skill ratings. This document covers all available endpoints, request/response formats, and integration examples.

## Base Information

- **Base URL**: `http://localhost:8080/api`
- **Authentication**: JWT Bearer Token
- **Content-Type**: `application/json`
- **API Version**: v1.0.0

## OpenAPI/Swagger Documentation

When the service is running, you can access interactive API documentation at:

- **Swagger UI**: `http://localhost:8080/swagger-ui.html`
- **OpenAPI JSON**: `http://localhost:8080/api/v3/api-docs`
- **OpenAPI YAML**: `http://localhost:8080/api/v3/api-docs.yaml`

## Authentication Endpoints

### POST /api/auth/register

Register a new user account.

**Request Body:**
```json
{
  "username": "string (3-50 chars, required)",
  "email": "string (valid email, required)",
  "password": "string (min 6 chars, required)"
}
```

**Responses:**
- `200 OK`: Registration successful
- `400 Bad Request`: Invalid input or user already exists
- `500 Internal Server Error`: Server error

**Success Response:**
```json
{
  "token": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
  "username": "player1",
  "role": "USER",
  "expiresAt": "2024-01-02T12:00:00"
}
```

### POST /api/auth/login

Authenticate user and receive JWT token.

**Request Body:**
```json
{
  "username": "string (required)",
  "password": "string (required)"
}
```

**Responses:**
- `200 OK`: Login successful
- `401 Unauthorized`: Invalid credentials
- `500 Internal Server Error`: Server error

## Matchmaking Endpoints

> **Authentication Required**: All matchmaking endpoints require a valid JWT token in the Authorization header.

### POST /api/match/join

Join the matchmaking queue.

**Headers:**
```
Authorization: Bearer <jwt_token>
```

**Request Body:**
```json
{
  "playerId": "string (required)",
  "elo": "integer (min 0, required)"
}
```

**Responses:**
- `200 OK`: Successfully joined queue
- `400 Bad Request`: Invalid request or player already in queue
- `401 Unauthorized`: Invalid or missing JWT token
- `500 Internal Server Error`: Server error

### DELETE /api/match/leave/{playerId}

Leave the matchmaking queue.

**Path Parameters:**
- `playerId` (string, required): Player ID to remove from queue

**Responses:**
- `200 OK`: Successfully left queue
- `404 Not Found`: Player not found in queue
- `401 Unauthorized`: Invalid or missing JWT token

### GET /api/match/status/{playerId}

Get player's queue status.

**Path Parameters:**
- `playerId` (string, required): Player ID to check

**Success Response:**
```json
{
  "success": true,
  "inQueue": true,
  "playerId": "player1",
  "elo": 1200,
  "queueTime": 15.5,
  "estimatedWait": 30.0,
  "position": 3
}
```

### POST /api/match/result

Submit match result for Elo calculation.

**Request Body:**
```json
{
  "matchId": "string (required)",
  "winnerId": "string (required)",
  "loserId": "string (required)",
  "winnerElo": "integer (required)",
  "loserElo": "integer (required)"
}
```

**Success Response:**
```json
{
  "success": true,
  "message": "Match result processed successfully",
  "matchId": "match-123",
  "eloChanges": {
    "player1": {
      "oldElo": 1200,
      "newElo": 1220,
      "change": 20
    },
    "player2": {
      "oldElo": 1200,
      "newElo": 1180,
      "change": -20
    }
  }
}
```

### GET /api/match/leaderboard

Get player leaderboard ordered by Elo rating.

**Query Parameters:**
- `limit` (integer, optional, default: 50): Maximum number of players to return

**Success Response:**
```json
{
  "success": true,
  "leaders": [
    {
      "playerId": "player1",
      "elo": 1500,
      "wins": 15,
      "losses": 5,
      "winRate": 75.0,
      "rank": 1
    }
  ],
  "count": 10
}
```

### GET /api/match/stats/{playerId}

Get detailed statistics for a player.

**Path Parameters:**
- `playerId` (string, required): Player ID to get stats for

**Success Response:**
```json
{
  "success": true,
  "playerId": "player1",
  "currentElo": 1500,
  "peakElo": 1550,
  "wins": 15,
  "losses": 5,
  "totalGames": 20,
  "winRate": 75.0,
  "averageOpponentElo": 1480,
  "rank": 1,
  "tier": "Gold"
}
```

### GET /api/match/health

Health check endpoint for matchmaking service.

**Success Response:**
```json
{
  "status": "UP",
  "service": "matchmaking",
  "timestamp": 1704110400000
}
```

## Administrative Endpoints

### POST /api/match/pause

Pause automatic matchmaking processing.

**Success Response:**
```json
{
  "success": true,
  "message": "Matchmaking paused",
  "enabled": false
}
```

### POST /api/match/resume

Resume automatic matchmaking processing.

**Success Response:**
```json
{
  "success": true,
  "message": "Matchmaking resumed",
  "enabled": true
}
```

### GET /api/match/enabled

Get current matchmaking enabled/paused state.

**Success Response:**
```json
{
  "success": true,
  "enabled": true,
  "wsConnections": 5,
  "wsActive": true,
  "timestamp": 1704110400000
}
```

### GET /api/match/websocket/status

Get WebSocket connection status.

**Success Response:**
```json
{
  "success": true,
  "activeConnections": 5,
  "hasConnections": true,
  "connections": ["session1", "session2"],
  "timestamp": 1704110400000
}
```

## Monitoring Endpoints

### GET /api/monitoring/metrics

Get comprehensive system metrics.

**Success Response:**
```json
{
  "success": true,
  "metrics": {
    "queueSize": 12,
    "activeMatches": 3,
    "totalMatches": 1500,
    "averageWaitTime": 25.5,
    "systemUptime": 86400000,
    "memoryUsage": {
      "used": 512,
      "max": 1024,
      "percentage": 50.0
    },
    "redisHealth": "UP",
    "dbHealth": "UP"
  },
  "timestamp": 1704110400000
}
```

### GET /api/monitoring/queue

Get detailed queue metrics.

**Success Response:**
```json
{
  "success": true,
  "queue": {
    "size": 12,
    "averageWaitTime": 25.5,
    "longestWaitTime": 120.0,
    "eloDistribution": {
      "0-1000": 2,
      "1000-1500": 8,
      "1500+": 2
    }
  },
  "processing": {
    "enabled": true,
    "lastProcessed": 1704110400000,
    "matchesPerMinute": 2.5
  }
}
```

## WebSocket Integration

### Connection Endpoint

**URL**: `ws://localhost:8080/ws-match`
**Protocol**: STOMP over WebSocket
**Authentication**: JWT token in connection headers

### Message Types

#### Match Found Notification
**Topic**: `/topic/matches`
```json
{
  "type": "MATCH_FOUND",
  "matchId": "match-123",
  "players": [
    {
      "playerId": "player1",
      "elo": 1200
    },
    {
      "playerId": "player2",
      "elo": 1180
    }
  ],
  "timestamp": "2024-01-01T12:00:00Z"
}
```

#### Queue Position Update
**Topic**: `/user/queue/updates`
```json
{
  "type": "QUEUE_UPDATE",
  "playerId": "player1",
  "position": 3,
  "estimatedWait": 45.0,
  "queueTime": 30.0
}
```

#### Match Result Notification
**Topic**: `/user/queue/results`
```json
{
  "type": "MATCH_RESULT",
  "matchId": "match-123",
  "result": "WIN",
  "eloChange": 20,
  "newElo": 1220
}
```

## Error Handling

### Standard Error Response Format

```json
{
  "success": false,
  "message": "Error description",
  "error": "Detailed error information",
  "timestamp": 1704110400000
}
```

### HTTP Status Codes

- `200 OK`: Request successful
- `400 Bad Request`: Invalid request data
- `401 Unauthorized`: Authentication required or invalid
- `403 Forbidden`: Access denied
- `404 Not Found`: Resource not found
- `429 Too Many Requests`: Rate limit exceeded
- `500 Internal Server Error`: Server error

### Common Error Scenarios

#### Authentication Errors
```json
{
  "error": "Unauthorized",
  "message": "Invalid or expired JWT token",
  "timestamp": 1704110400000
}
```

#### Validation Errors
```json
{
  "success": false,
  "message": "Validation failed",
  "errors": {
    "playerId": "Player ID is required",
    "elo": "Elo must be >= 0"
  }
}
```

#### Rate Limiting
```json
{
  "error": "Too Many Requests",
  "message": "Rate limit exceeded. Try again later.",
  "retryAfter": 60
}
```

## Rate Limits

- **Authentication endpoints**: 5 requests/second
- **General API endpoints**: 10 requests/second
- **Health checks**: No rate limiting

## Data Models

### User
```json
{
  "id": "integer",
  "username": "string",
  "email": "string",
  "role": "USER | ADMIN",
  "enabled": "boolean",
  "createdAt": "datetime",
  "lastLoginAt": "datetime"
}
```

### Match Request
```json
{
  "playerId": "string",
  "elo": "integer",
  "timestamp": "datetime"
}
```

### Match Result
```json
{
  "matchId": "string",
  "winnerId": "string",
  "loserId": "string",
  "winnerElo": "integer",
  "loserElo": "integer",
  "timestamp": "datetime"
}
```

### Player Stats
```json
{
  "playerId": "string",
  "currentElo": "integer",
  "peakElo": "integer",
  "wins": "integer",
  "losses": "integer",
  "totalGames": "integer",
  "winRate": "double",
  "rank": "integer",
  "tier": "string"
}
```

## Configuration

### Environment Variables

- `SPRING_DATA_REDIS_HOST`: Redis server host
- `SPRING_DATA_REDIS_PORT`: Redis server port
- `SECURITY_JWT_SECRET`: JWT signing secret
- `SECURITY_JWT_TTL_SECONDS`: JWT token expiration time

### Application Properties

Key configuration properties:
- `match.max-wait-seconds`: Maximum queue wait time
- `match.elo-tolerance`: Initial Elo matching tolerance
- `match.elo-tolerance-growth-per-second`: Tolerance growth rate
- `elo.kfactor.default`: Default Elo K-factor

## SDKs and Client Libraries

### JavaScript/TypeScript
```javascript
// Example using fetch API
class MatchmakingClient {
  constructor(baseUrl, token) {
    this.baseUrl = baseUrl;
    this.token = token;
  }
  
  async joinQueue(playerId, elo) {
    const response = await fetch(`${this.baseUrl}/api/match/join`, {
      method: 'POST',
      headers: {
        'Authorization': `Bearer ${this.token}`,
        'Content-Type': 'application/json'
      },
      body: JSON.stringify({ playerId, elo })
    });
    return response.json();
  }
}
```

### Python
```python
import requests

class MatchmakingClient:
    def __init__(self, base_url, token):
        self.base_url = base_url
        self.headers = {
            'Authorization': f'Bearer {token}',
            'Content-Type': 'application/json'
        }
    
    def join_queue(self, player_id, elo):
        response = requests.post(
            f'{self.base_url}/api/match/join',
            headers=self.headers,
            json={'playerId': player_id, 'elo': elo}
        )
        return response.json()
```

### Java
```java
// Example using Spring's RestTemplate
@Service
public class MatchmakingClient {
    private final RestTemplate restTemplate;
    private final String baseUrl;
    private final String token;
    
    public MatchmakingClient(String baseUrl, String token) {
        this.restTemplate = new RestTemplate();
        this.baseUrl = baseUrl;
        this.token = token;
    }
    
    public ResponseEntity<Map> joinQueue(String playerId, Integer elo) {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(token);
        headers.setContentType(MediaType.APPLICATION_JSON);
        
        Map<String, Object> request = Map.of(
            "playerId", playerId,
            "elo", elo
        );
        
        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(request, headers);
        
        return restTemplate.postForEntity(
            baseUrl + "/api/match/join",
            entity,
            Map.class
        );
    }
}
```

## Testing

### Health Check
```bash
curl -X GET "http://localhost:8080/actuator/health"
```

### Authentication Test
```bash
# Register
curl -X POST "http://localhost:8080/api/auth/register" \
  -H "Content-Type: application/json" \
  -d '{"username":"test","email":"test@example.com","password":"password"}'

# Login
curl -X POST "http://localhost:8080/api/auth/login" \
  -H "Content-Type: application/json" \
  -d '{"username":"test","password":"password"}'
```

### Matchmaking Test
```bash
# Join queue (replace TOKEN with actual JWT)
curl -X POST "http://localhost:8080/api/match/join" \
  -H "Authorization: Bearer TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"playerId":"test","elo":1200}'
```

## Support and Resources

- **GitHub Repository**: [Link to repository]
- **Issue Tracker**: [Link to issues]
- **API Status Page**: `http://localhost:8080/actuator/health`
- **Metrics**: `http://localhost:8080/actuator/prometheus`

---

**Last Updated**: 2024-01-01  
**API Version**: 1.0.0  
**Documentation Version**: 1.0.0
