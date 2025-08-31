# 🚀 API Usage Examples

This document provides practical examples for using the Matchmaking Service API.

## 📋 Table of Contents

- [Authentication](#authentication)
- [Matchmaking Operations](#matchmaking-operations)
- [Player Statistics](#player-statistics)
- [Administrative Operations](#administrative-operations)
- [WebSocket Integration](#websocket-integration)
- [Error Handling](#error-handling)

## 🔐 Authentication

### Register a New User

```bash
curl -X POST "http://localhost:8080/api/auth/register" \
  -H "Content-Type: application/json" \
  -d '{
    "username": "player1",
    "email": "player1@example.com",
    "password": "securepassword123"
  }'
```

**Response:**
```json
{
  "token": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
  "username": "player1",
  "role": "USER",
  "expiresAt": "2024-01-02T12:00:00"
}
```

### Login

```bash
curl -X POST "http://localhost:8080/api/auth/login" \
  -H "Content-Type: application/json" \
  -d '{
    "username": "player1",
    "password": "securepassword123"
  }'
```

**Response:**
```json
{
  "token": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
  "username": "player1",
  "role": "USER",
  "expiresAt": "2024-01-02T12:00:00"
}
```

## 🎯 Matchmaking Operations

> **Note:** All matchmaking endpoints require authentication. Include the JWT token in the Authorization header.

### Join Matchmaking Queue

```bash
curl -X POST "http://localhost:8080/api/match/join" \
  -H "Authorization: Bearer YOUR_JWT_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "playerId": "player1",
    "elo": 1200
  }'
```

**Response:**
```json
{
  "success": true,
  "message": "Successfully joined matchmaking queue",
  "playerId": "player1",
  "elo": 1200
}
```

### Check Queue Status

```bash
curl -X GET "http://localhost:8080/api/match/status/player1" \
  -H "Authorization: Bearer YOUR_JWT_TOKEN"
```

**Response:**
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

### Leave Matchmaking Queue

```bash
curl -X DELETE "http://localhost:8080/api/match/leave/player1" \
  -H "Authorization: Bearer YOUR_JWT_TOKEN"
```

**Response:**
```json
{
  "success": true,
  "message": "Successfully left matchmaking queue",
  "playerId": "player1"
}
```

### Submit Match Result

```bash
curl -X POST "http://localhost:8080/api/match/result" \
  -H "Authorization: Bearer YOUR_JWT_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "matchId": "match-123",
    "winnerId": "player1",
    "loserId": "player2",
    "winnerElo": 1220,
    "loserElo": 1180
  }'
```

**Response:**
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

## 📊 Player Statistics

### Get Leaderboard

```bash
curl -X GET "http://localhost:8080/api/match/leaderboard?limit=10" \
  -H "Authorization: Bearer YOUR_JWT_TOKEN"
```

**Response:**
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
    },
    {
      "playerId": "player2",
      "elo": 1450,
      "wins": 12,
      "losses": 8,
      "winRate": 60.0,
      "rank": 2
    }
  ],
  "count": 10
}
```

### Get Player Statistics

```bash
curl -X GET "http://localhost:8080/api/match/stats/player1" \
  -H "Authorization: Bearer YOUR_JWT_TOKEN"
```

**Response:**
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

## ⚙️ Administrative Operations

### Pause Matchmaking

```bash
curl -X POST "http://localhost:8080/api/match/pause" \
  -H "Authorization: Bearer YOUR_JWT_TOKEN"
```

**Response:**
```json
{
  "success": true,
  "message": "Matchmaking paused",
  "enabled": false
}
```

### Resume Matchmaking

```bash
curl -X POST "http://localhost:8080/api/match/resume" \
  -H "Authorization: Bearer YOUR_JWT_TOKEN"
```

**Response:**
```json
{
  "success": true,
  "message": "Matchmaking resumed",
  "enabled": true
}
```

### Get System Status

```bash
curl -X GET "http://localhost:8080/api/match/enabled" \
  -H "Authorization: Bearer YOUR_JWT_TOKEN"
```

**Response:**
```json
{
  "success": true,
  "enabled": true,
  "wsConnections": 5,
  "wsActive": true,
  "timestamp": 1704110400000
}
```

### Get System Metrics

```bash
curl -X GET "http://localhost:8080/api/monitoring/metrics" \
  -H "Authorization: Bearer YOUR_JWT_TOKEN"
```

**Response:**
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

## 🔌 WebSocket Integration

### JavaScript Client Example

```javascript
// Connect to WebSocket
const socket = new SockJS('http://localhost:8080/ws-match');
const stompClient = Stomp.over(socket);

// Authentication headers
const headers = {
    'Authorization': 'Bearer ' + localStorage.getItem('jwt_token')
};

// Connect and subscribe
stompClient.connect(headers, function(frame) {
    console.log('Connected: ' + frame);
    
    // Subscribe to match notifications
    stompClient.subscribe('/topic/matches', function(message) {
        const matchData = JSON.parse(message.body);
        handleMatchFound(matchData);
    });
    
    // Subscribe to personal queue updates
    stompClient.subscribe('/user/queue/updates', function(message) {
        const update = JSON.parse(message.body);
        handleQueueUpdate(update);
    });
    
    // Subscribe to match results
    stompClient.subscribe('/user/queue/results', function(message) {
        const result = JSON.parse(message.body);
        handleMatchResult(result);
    });
});

// Handle match found
function handleMatchFound(matchData) {
    console.log('Match found!', matchData);
    // Example: { type: "MATCH_FOUND", matchId: "match-123", players: [...] }
    
    // Update UI to show match details
    showMatchFound(matchData.matchId, matchData.players);
}

// Handle queue updates
function handleQueueUpdate(update) {
    console.log('Queue update:', update);
    // Example: { type: "QUEUE_UPDATE", position: 3, estimatedWait: 45.0 }
    
    // Update queue position display
    updateQueuePosition(update.position, update.estimatedWait);
}

// Handle match results
function handleMatchResult(result) {
    console.log('Match result:', result);
    // Example: { type: "MATCH_RESULT", result: "WIN", eloChange: +20, newElo: 1220 }
    
    // Show match result to player
    showMatchResult(result.result, result.eloChange, result.newElo);
}

// Send match result (if authorized)
function submitMatchResult(matchId, winnerId, loserId) {
    stompClient.send('/app/match/result', {}, JSON.stringify({
        matchId: matchId,
        winnerId: winnerId,
        loserId: loserId
    }));
}
```

### Python Client Example

```python
import websocket
import json
import threading
import time

class MatchmakingWebSocketClient:
    def __init__(self, token):
        self.token = token
        self.ws = None
        
    def connect(self):
        # Note: This is a simplified example
        # In practice, you'd need a proper STOMP client library
        websocket_url = "ws://localhost:8080/ws-match"
        
        self.ws = websocket.WebSocketApp(
            websocket_url,
            header={"Authorization": f"Bearer {self.token}"},
            on_message=self.on_message,
            on_error=self.on_error,
            on_close=self.on_close
        )
        
        # Run in separate thread
        wst = threading.Thread(target=self.ws.run_forever)
        wst.daemon = True
        wst.start()
        
    def on_message(self, ws, message):
        try:
            data = json.loads(message)
            if data.get('type') == 'MATCH_FOUND':
                self.handle_match_found(data)
            elif data.get('type') == 'QUEUE_UPDATE':
                self.handle_queue_update(data)
        except json.JSONDecodeError:
            print(f"Invalid JSON received: {message}")
            
    def handle_match_found(self, data):
        print(f"Match found! Match ID: {data['matchId']}")
        print(f"Players: {data['players']}")
        
    def handle_queue_update(self, data):
        print(f"Queue position: {data['position']}")
        print(f"Estimated wait: {data['estimatedWait']}s")
        
    def on_error(self, ws, error):
        print(f"WebSocket error: {error}")
        
    def on_close(self, ws, close_status_code, close_msg):
        print("WebSocket connection closed")

# Usage
client = MatchmakingWebSocketClient("your_jwt_token_here")
client.connect()
```

## ❌ Error Handling

### Common Error Responses

#### 401 Unauthorized
```json
{
  "error": "Unauthorized",
  "message": "Invalid or expired JWT token",
  "timestamp": 1704110400000
}
```

#### 400 Bad Request
```json
{
  "success": false,
  "message": "Player already in queue",
  "playerId": "player1",
  "details": {
    "currentElo": 1200,
    "queueTime": 30.5
  }
}
```

#### 404 Not Found
```json
{
  "success": false,
  "message": "Player not found in queue",
  "playerId": "player1"
}
```

#### 500 Internal Server Error
```json
{
  "success": false,
  "message": "Internal server error",
  "error": "Database connection failed"
}
```

### Error Handling Best Practices

1. **Always check the `success` field** in API responses
2. **Handle JWT token expiration** by refreshing or re-authenticating
3. **Implement retry logic** for temporary failures
4. **Validate input data** before sending requests
5. **Monitor WebSocket connection status** and reconnect if needed

## 🧪 Testing with Postman

### Import Collection

Create a Postman collection with the following structure:

```json
{
  "info": {
    "name": "Matchmaking Service API",
    "schema": "https://schema.getpostman.com/json/collection/v2.1.0/collection.json"
  },
  "auth": {
    "type": "bearer",
    "bearer": [
      {
        "key": "token",
        "value": "{{jwt_token}}",
        "type": "string"
      }
    ]
  },
  "variable": [
    {
      "key": "base_url",
      "value": "http://localhost:8080"
    },
    {
      "key": "jwt_token",
      "value": "your_token_here"
    }
  ]
}
```

### Environment Variables

Set up these environment variables in Postman:
- `base_url`: `http://localhost:8080`
- `jwt_token`: Your JWT token (updated after login)
- `player_id`: Your player ID for testing

### Pre-request Script for Auto-Authentication

```javascript
// Auto-login if token is expired
if (!pm.environment.get("jwt_token")) {
    pm.sendRequest({
        url: pm.environment.get("base_url") + "/api/auth/login",
        method: 'POST',
        header: {
            'Content-Type': 'application/json'
        },
        body: {
            mode: 'raw',
            raw: JSON.stringify({
                username: "your_username",
                password: "your_password"
            })
        }
    }, function (err, response) {
        if (response.code === 200) {
            const token = response.json().token;
            pm.environment.set("jwt_token", token);
        }
    });
}
```

## 🔄 Rate Limiting

The API implements rate limiting:

- **Authentication endpoints**: 5 requests/second
- **General API endpoints**: 10 requests/second
- **Health checks**: No rate limiting

When rate limited, you'll receive:
```json
{
  "error": "Too Many Requests",
  "message": "Rate limit exceeded. Try again later.",
  "retryAfter": 60
}
```

## 📈 Monitoring and Metrics

### Prometheus Metrics Endpoint

```bash
curl -X GET "http://localhost:8080/actuator/prometheus"
```

### Key Metrics to Monitor

- `matchmaking_queue_size`: Current number of players in queue
- `matchmaking_matches_created_total`: Total matches created
- `matchmaking_processing_time_seconds`: Time to process matches
- `websocket_connections_active`: Active WebSocket connections
- `http_requests_total`: HTTP request counts by endpoint
- `jvm_memory_used_bytes`: JVM memory usage

---

**Happy Matching! 🎮**
