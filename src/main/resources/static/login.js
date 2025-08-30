let isLogin = true;

function toggleForm() {
    isLogin = !isLogin;
    console.log('Toggle form called, isLogin:', isLogin); // Debug log
    
    const title = document.getElementById('form-title');
    const submitBtn = document.getElementById('submit-btn');
    const toggleLink = document.getElementById('toggle-link');
    const emailGroup = document.getElementById('email-group');
    const emailInput = document.getElementById('email');
    
    if (isLogin) {
        title.textContent = 'Login';
        submitBtn.textContent = 'Login';
        toggleLink.textContent = "Don't have an account? Register";
        emailGroup.style.display = 'none';
        emailInput.removeAttribute('required');
    } else {
        title.textContent = 'Register';
        submitBtn.textContent = 'Register';
        toggleLink.textContent = 'Already have an account? Login';
        emailGroup.style.display = 'block';
        emailInput.setAttribute('required', 'required');
    }
    
    clearMessages();
}

function clearMessages() {
    document.getElementById('error-message').style.display = 'none';
    document.getElementById('success-message').style.display = 'none';
}

function showMessage(message, isError = false) {
    const errorDiv = document.getElementById('error-message');
    const successDiv = document.getElementById('success-message');
    
    if (isError) {
        errorDiv.textContent = message;
        errorDiv.style.display = 'block';
        successDiv.style.display = 'none';
    } else {
        successDiv.textContent = message;
        successDiv.style.display = 'block';
        errorDiv.style.display = 'none';
    }
}

// Event Listeners
document.addEventListener('DOMContentLoaded', function() {
    console.log('DOM loaded, setting up event listeners'); // Debug log
    
    const toggleLink = document.getElementById('toggle-link');
    console.log('Toggle link element:', toggleLink); // Debug log
    
    toggleLink.addEventListener('click', function(e) {
        e.preventDefault();
        console.log('Toggle link clicked'); // Debug log
        toggleForm();
    });
    
    document.getElementById('auth-form').addEventListener('submit', async function(e) {
        e.preventDefault();
        
        const username = document.getElementById('username').value;
        const password = document.getElementById('password').value;
        const email = document.getElementById('email').value;
        
        console.log('Form submission:', { isLogin, username, password: '***', email });
        
        // Basic validation
        if (!username || !password) {
            showMessage('Username and password are required', true);
            return;
        }
        
        if (!isLogin && !email) {
            showMessage('Email is required for registration', true);
            return;
        }
        
        try {
            if (isLogin) {
                // Login
                const response = await fetch('/api/auth/login', {
                    method: 'POST',
                    headers: {
                        'Content-Type': 'application/json',
                        'Accept': 'application/json'
                    },
                    body: JSON.stringify({ username, password })
                });
                
                console.log('Login response status:', response.status);
                console.log('Login response headers:', [...response.headers.entries()]);
                
                const data = await response.json();
                console.log('Login response data:', data);
                
                if (response.ok) {
                    showMessage('Login successful! Redirecting...');
                    localStorage.setItem('authToken', data.token);
                    localStorage.setItem('username', data.username);
                    setTimeout(() => {
                        // Redirect to a page that can handle the auth token properly
                        window.location.href = '/auth-success';
                    }, 1500);
                } else {
                    showMessage(data.message || 'Login failed', true);
                }
            } else {
                // Register
                const response = await fetch('/api/auth/register', {
                    method: 'POST',
                    headers: {
                        'Content-Type': 'application/json',
                        'Accept': 'application/json'
                    },
                    body: JSON.stringify({ username, password, email })
                });
                
                console.log('Register response status:', response.status);
                console.log('Register response headers:', [...response.headers.entries()]);
                
                const data = await response.json();
                console.log('Register response data:', data);
                
                if (response.ok) {
                    showMessage('Registration successful! You can now login.');
                    toggleForm(); // Switch to login form
                } else {
                    // Show specific error messages
                    let errorMessage = 'Registration failed';
                    if (data.message) {
                        if (data.message.includes('Username already exists')) {
                            errorMessage = 'Username already exists. Please choose a different username.';
                        } else if (data.message.includes('Email already exists')) {
                            errorMessage = 'Email already exists. Please use a different email.';
                        } else {
                            errorMessage = data.message;
                        }
                    }
                    showMessage(errorMessage, true);
                }
            }
        } catch (error) {
            console.error('Request failed:', error);
            showMessage('An error occurred. Please try again. Check console for details.', true);
        }
    });
});
