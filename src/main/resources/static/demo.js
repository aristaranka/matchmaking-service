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
    userInfo.innerHTML = `
      <strong>Welcome, ${username}!</strong><br>
      <button class="logout-btn" onclick="logout()">Logout</button>
    `;
  }

  function clearAuth() {
    localStorage.removeItem('authToken');
    localStorage.removeItem('username');
    authToken = null;
    currentUsername = null;
    const userInfo = $('user-info');
    userInfo.innerHTML = '<a href="/login" class="login-link">Login</a>';
    
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
    $("connStatus").textContent = connected ? "Connected" : "Disconnected";
    $("connStatus").className = `status ${connected ? "ok" : "bad"}`;
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
    stompClient.connect(headers, async function (frame) {
      setConnected(true);
      log(`Connected: ${frame}`);
      // resume matchmaking when client connects
      try { await fetch(window.__MM_API__?.resume || '/api/match/resume', { method: 'POST', headers: { Authorization: `Bearer ${authToken}` } }); } catch (_) {}
      subscribe();
    }, function (error) {
      setConnected(false);
      log(`Connection error: ${error}`);
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
    if (!stompClient || !stompClient.connected) return;
    subscription = stompClient.subscribe('/topic/matches', function (message) {
      try {
        const data = JSON.parse(message.body);
        renderMatch(data);
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
      body.innerHTML = '';
      players.forEach((p, idx) => {
        const tr = document.createElement('tr');
        tr.innerHTML = `
          <td>${idx + 1}</td>
          <td>${p.playerId ?? ''}</td>
          <td>${p.username ?? ''}</td>
          <td>${p.elo ?? ''}</td>
          <td>${p.online ? 'Yes' : 'No'}</td>
          <td>${formatDate(p.lastActive)}</td>
        `;
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
    tr.innerHTML = `
      <td>${time}</td>
      <td>${m.matchId ?? ''}</td>
      <td>${m.playerA ?? ''} (${m.oldEloA ?? ''})</td>
      <td>${m.playerB ?? ''} (${m.oldEloB ?? ''})</td>
      <td>${m.winner ?? ''}</td>
      <td>${m.oldEloA ?? ''}</td>
      <td>${m.oldEloB ?? ''}</td>
      <td>${m.newEloA ?? ''}</td>
      <td>${m.newEloB ?? ''}</td>
    `;
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


