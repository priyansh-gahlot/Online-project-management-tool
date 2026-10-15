function showError(message) {
    const errorDiv = document.getElementById('error-message');
    if (errorDiv) {
        errorDiv.textContent = message;
        errorDiv.style.display = 'block';
    }
}

function hideError() {
    const errorDiv = document.getElementById('error-message');
    if (errorDiv) {
        errorDiv.style.display = 'none';
    }
}

// Send token to Java backend
async function authenticateBackend(user, fullName = null) {
    try {
        const idToken = await user.getIdToken(true);
        const formData = new URLSearchParams();
        formData.append('idToken', idToken);
        if (fullName) {
            formData.append('fullName', fullName);
        }

        const response = await fetch('/smart-project-manager/auth/verify', {
            method: 'POST',
            headers: {
                'Content-Type': 'application/x-www-form-urlencoded'
            },
            body: formData
        });

        const data = await response.json();
        if (response.ok && data.success) {
            window.location.href = '/smart-project-manager' + data.redirect;
        } else {
            showError(data.error || 'Backend authentication failed.');
            await auth.signOut();
        }
    } catch (error) {
        console.error('Backend auth error:', error);
        showError('An error occurred communicating with the server.');
        await auth.signOut();
    }
}

// Handle Login
const loginForm = document.getElementById('login-form');
if (loginForm) {
    loginForm.addEventListener('submit', async (e) => {
        e.preventDefault();
        hideError();
        
        const email = document.getElementById('email').value;
        const password = document.getElementById('password').value;
        const btn = document.getElementById('login-btn');
        btn.disabled = true;
        btn.textContent = 'Signing in...';

        try {
            const userCredential = await auth.signInWithEmailAndPassword(email, password);
            await authenticateBackend(userCredential.user);
        } catch (error) {
            console.error('Login error:', error);
            showError(error.message);
            btn.disabled = false;
            btn.textContent = 'Sign In';
        }
    });
}

// Handle Registration
const registerForm = document.getElementById('register-form');
if (registerForm) {
    registerForm.addEventListener('submit', async (e) => {
        e.preventDefault();
        hideError();
        
        const fullName = document.getElementById('fullName').value;
        const email = document.getElementById('email').value;
        const password = document.getElementById('password').value;
        const btn = document.getElementById('register-btn');
        btn.disabled = true;
        btn.textContent = 'Creating account...';

        try {
            const userCredential = await auth.createUserWithEmailAndPassword(email, password);
            await authenticateBackend(userCredential.user, fullName);
        } catch (error) {
            console.error('Registration error:', error);
            showError(error.message);
            btn.disabled = false;
            btn.textContent = 'Sign Up';
        }
    });
}
