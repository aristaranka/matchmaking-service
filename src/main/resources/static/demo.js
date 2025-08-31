(function () {
  let stompClient = null;
  let subscription = null;
  let queueTimer = null;
  let authToken = null;
  let currentUsername = null;

  const $ = (id) => document.getElementById(id);
  const log = (msg) => {
    const time = new Date().toLocaleTimeString();
    $("log").textContent = `[${time}] ${msg}\n` + $("log").textContent;
  };

  // Authentication functions
  function checkAuthStatus() {
    const token = localStorage.getItem('authToken');
    const username = localStorage.getItem('username');
    
    console.log('Checking auth status:', { hasToken: !!token, username });
    
    if (token && username) {
      // Validate token
      fetch('/api/auth/validate', {
        headers: { 'Authorization': `Bearer ${token}` }
      })
      .then(response => response.json())
      .then(data => {
        console.log('Token validation result:', data);
        if (data.valid) {
          showAuthenticatedUser(username);
          authToken = token;
          currentUsername = username;
          // Set default player ID to username
          $("playerId").value = username;
          $("leavePlayerId").value = username;
          hideAuthNotice(); // Hide notice when authenticated
          
          // Initialize the page for authenticated users
          ensurePlayerId();
          refreshQueue();
        } else {
          console.log('Token invalid, clearing auth');
          clearAuth();
          redirectToLogin();
        }
      })
      .catch((error) => {
        console.error('Token validation error:', error);
        clearAuth();
        redirectToLogin();
      });
    } else {
      console.log('No token or username, clearing auth');
      clearAuth();
      redirectToLogin();
    }
  }

  function showAuthenticatedUser(username) {
    const userInfo = $('user-info');
    
    // Clear existing content safely
    userInfo.textContent = '';
    
    // Create welcome message element
    const welcomeText = document.createElement('strong');
    welcomeText.textContent = `Welcome, ${username}!`;
    
    // Create line break
    const lineBreak = document.createElement('br');
    
    // Create logout button
    const logoutBtn = document.createElement('button');
    logoutBtn.className = 'logout-btn';
    logoutBtn.textContent = 'Logout';
    logoutBtn.addEventListener('click', logout);
    
    // Append elements safely
    userInfo.appendChild(welcomeText);
    userInfo.appendChild(lineBreak);
    userInfo.appendChild(logoutBtn);
  }

  function clearAuth() {
    localStorage.removeItem('authToken');
    localStorage.removeItem('username');
    authToken = null;
    currentUsername = null;
    const userInfo = $('user-info');
    // Clear existing content safely
    userInfo.textContent = '';
    
    // Create login link element
    const loginLink = document.createElement('a');
    loginLink.href = '/login';
    loginLink.className = 'login-link';
    loginLink.textContent = 'Login';
    
    userInfo.appendChild(loginLink);
    
    // Show authentication notice and hide main content
    showAuthNotice();
  }

  function showAuthNotice() {
    const authNotice = document.getElementById('auth-notice');
    const mainContent = document.querySelectorAll('fieldset');
    
    if (authNotice) {
      authNotice.style.display = 'block';
    }
    
    // Hide all fieldsets when not authenticated
    mainContent.forEach(fieldset => {
      fieldset.style.display = 'none';
    });
  }

  function hideAuthNotice() {
    const authNotice = document.getElementById('auth-notice');
    const mainContent = document.querySelectorAll('fieldset');
    
    if (authNotice) {
      authNotice.style.display = 'none';
    }
    
    // Show all fieldsets when authenticated
    mainContent.forEach(fieldset => {
      fieldset.style.display = 'block';
    });
  }

  function redirectToLogin() {
    // Check if we're already on the login page to avoid infinite redirects
    if (window.location.pathname !== '/login' && window.location.pathname !== '/auth-success') {
      window.location.href = '/login';
    }
  }

  function logout() {
    clearAuth();
    if (stompClient && stompClient.connected) {
      disconnect();
    }
    log('Logged out');
    redirectToLogin();
  }

  // Make logout function global
  window.logout = logout;

  function setConnected(connected) {
    $("connectBtn").disabled = connected;
    $("disconnectBtn").disabled = !connected;
    $("connStatus").textContent = connected ? "Connected (Matchmaking Active)" : "Disconnected (Matchmaking Paused)";
    $("connStatus").className = `status ${connected ? "ok" : "bad"}`;
    
    if (connected) {
      log('🔗 WebSocket connected - matchmaking will now process queued players');
    } else {
      log('❌ WebSocket disconnected - players can queue but will not be matched until reconnection');
    }
  }

  function randomId() {
    return `player-${Math.floor(1000 + Math.random() * 9000)}`;
  }

  function ensurePlayerId() {
    const input = $("playerId");
    if (!input.value) {
      // Use username as default player ID if available
      input.value = currentUsername || randomId();
    }
  }

  async function ensureToken() {
    if (authToken) return authToken;
    
    // If no auth token, redirect to login
    redirectToLogin();
    throw new Error('Authentication required');
  }

  function connect() {
    if (stompClient && stompClient.connected) return;
    
    // Check if user is authenticated before allowing connection
    if (!authToken) {
      log('Please login first');
      redirectToLogin();
      return;
    }
    
    const endpoint = $("ws-endpoint").textContent.trim();
    const sock = new SockJS(endpoint);
    stompClient = Stomp.over(sock);
    stompClient.debug = null; // silence console spam
    
    const headers = { Authorization: `Bearer ${authToken}` };
    log(`Attempting WebSocket connection with auth token: ${authToken ? 'present' : 'missing'}`);
    stompClient.connect(headers, async function (frame) {
      setConnected(true);
      log(`WebSocket Connected successfully: ${frame}`);
      // resume matchmaking when client connects
      try { 
        const response = await fetch(window.__MM_API__?.resume || '/api/match/resume', { method: 'POST', headers: { Authorization: `Bearer ${authToken}` } }); 
        log(`Matchmaking resumed: ${response.status}`);
      } catch (e) { 
        log(`Failed to resume matchmaking: ${e}`);
      }
      subscribe();
    }, function (error) {
      setConnected(false);
      log(`WebSocket Connection error: ${error}`);
    });
  }

  function disconnect() {
    if (subscription) {
      try { subscription.unsubscribe(); } catch (_) {}
      subscription = null;
    }
    if (stompClient) {
      try { stompClient.disconnect(() => log("Disconnected")); } catch (_) {}
    }
    // pause matchmaking when client disconnects
    if (authToken) {
      (async () => { 
        try { await fetch(window.__MM_API__?.pause || '/api/match/pause', { method: 'POST', headers: { Authorization: `Bearer ${authToken}` } }); } catch (_) {} 
      })();
    }
    setConnected(false);
  }

  function subscribe() {
    if (!stompClient || !stompClient.connected) {
      log('Cannot subscribe: WebSocket not connected');
      return;
    }
    subscription = stompClient.subscribe('/topic/matches', function (message) {
      try {
        log(`Received WebSocket message: ${message.body}`);
        const data = JSON.parse(message.body);
        log(`Parsed match data: ${JSON.stringify(data)}`);
        renderMatch(data);
        log(`Match rendered successfully`);
      } catch (e) {
        log(`Failed to parse message: ${e}`);
      }
    });
    log('Subscribed to /topic/matches');
  }

  function formatDate(value) {
    if (!value) return '';
    try {
      const d = new Date(value);
      if (isNaN(d.getTime())) return '';
      return d.toLocaleString();
    } catch (_) { return ''; }
  }

  async function refreshQueue() {
    try {
      const token = await ensureToken();
      const res = await fetch(`/api/match/status?_t=${Date.now()}`, { headers: { Authorization: `Bearer ${token}` } });
      if (!res.ok) throw new Error(res.statusText);
      const json = await res.json();
      const players = Array.isArray(json.queuePlayers) ? json.queuePlayers : [];
      $("queueCount").textContent = json.queueSize ?? players.length;
      const body = $("queueBody");
      // Clear existing content safely
      body.textContent = '';
      players.forEach((p, idx) => {
        const tr = document.createElement('tr');
        
        // Create table cells safely
        const cells = [
          String(idx + 1),
          String(p.playerId ?? ''),
          String(p.username ?? ''),
          String(p.elo ?? ''),
          p.online ? 'Yes' : 'No',
          formatDate(p.lastActive)
        ];
        
        cells.forEach(cellText => {
          const td = document.createElement('td');
          td.textContent = cellText;
          tr.appendChild(td);
        });
        
        body.appendChild(tr);
      });
      log(`Queue refreshed: ${players.length} players`);
    } catch (e) {
      log(`Queue refresh failed: ${e}`);
    }
  }

  function startAutoRefresh() {
    stopAutoRefresh();
    const ms = parseInt($("autoRefreshMs").value, 10) || 3000;
    queueTimer = setInterval(refreshQueue, ms);
  }

  function stopAutoRefresh() {
    if (queueTimer) { clearInterval(queueTimer); queueTimer = null; }
  }

  function renderMatch(m) {
    const tr = document.createElement('tr');
    const time = m.playedAt ? new Date(m.playedAt).toLocaleTimeString() : new Date().toLocaleTimeString();
    
    // Create table cells safely
    const cells = [
      time,
      String(m.matchId ?? ''),
      `${String(m.playerA ?? '')} (${String(m.oldEloA ?? '')})`,
      `${String(m.playerB ?? '')} (${String(m.oldEloB ?? '')})`,
      String(m.winner ?? ''),
      String(m.oldEloA ?? ''),
      String(m.oldEloB ?? ''),
      String(m.newEloA ?? ''),
      String(m.newEloB ?? '')
    ];
    
    cells.forEach(cellText => {
      const td = document.createElement('td');
      td.textContent = cellText;
      tr.appendChild(td);
    });
    
    const body = $("matchesBody");
    body.insertBefore(tr, body.firstChild);
  }

  async function joinQueue() {
    ensurePlayerId();
    const playerId = $("playerId").value.trim();
    const elo = parseInt($("elo").value, 10) || 1200;
    
    // Check if user is authenticated
    if (!authToken) {
      $("enqueueResult").textContent = 'Please login first';
      redirectToLogin();
      return;
    }
    
    try {
      const res = await fetch('/api/match/join', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json', Authorization: `Bearer ${authToken}` },
        body: JSON.stringify({ playerId, elo })
      });
      const json = await res.json();
      $("enqueueResult").textContent = json.message || (json.success ? 'Joined queue' : 'Failed');
      log(`Join ${json.success ? 'ok' : 'fail'}: ${JSON.stringify(json)}`);
    } catch (e) {
      $("enqueueResult").textContent = 'Request failed';
      log(`Join error: ${e}`);
    }
  }

  async function leaveQueue() {
    const playerId = ( $("leavePlayerId").value.trim() || $("playerId").value.trim() );
    if (!playerId) { $("leaveResult").textContent = 'Enter a player ID'; return; }
    
    // Check if user is authenticated
    if (!authToken) {
      $("leaveResult").textContent = 'Please login first';
      redirectToLogin();
      return;
    }
    
    try {
      const res = await fetch(`/api/match/leave/${encodeURIComponent(playerId)}`, { method: 'DELETE', headers: { Authorization: `Bearer ${authToken}` } });
      const json = await res.json();
      $("leaveResult").textContent = json.message || (json.success ? 'Left queue' : 'Not found');
      log(`Leave ${json.success ? 'ok' : 'fail'}: ${JSON.stringify(json)}`);
    } catch (e) {
      $("leaveResult").textContent = 'Request failed';
      log(`Leave error: ${e}`);
    }
  }

  // Wire UI
  $("connectBtn").addEventListener('click', connect);
  $("disconnectBtn").addEventListener('click', disconnect);
  $("joinBtn").addEventListener('click', joinQueue);
  $("leaveBtn").addEventListener('click', leaveQueue);
  $("refreshQueueBtn").addEventListener('click', refreshQueue);
  $("autoRefreshChk").addEventListener('change', (e) => e.target.checked ? startAutoRefresh() : stopAutoRefresh());
  $("autoRefreshMs").addEventListener('change', () => { if ($("autoRefreshChk").checked) startAutoRefresh(); });

  // Initialize authentication status first
  checkAuthStatus();
  
  // Show auth notice by default (will be hidden if user is authenticated)
  showAuthNotice();
  
  // Only proceed with initialization if authenticated
  if (authToken) {
    // Defaults
    ensurePlayerId();
    refreshQueue();
  }
})();


