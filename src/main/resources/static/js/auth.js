if (typeof API_URL === 'undefined') var API_URL = 'http://localhost:8080/api';

// Login Form Handler
const loginForm = document.getElementById('loginForm');
if (loginForm) {
    loginForm.addEventListener('submit', async (e) => {
        e.preventDefault();
        const username = document.getElementById('username').value;
        const password = document.getElementById('password').value;

        try {
            const response = await fetch(`${API_URL}/auth/login`, {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify({ username, password })
            });

            if (response.ok) {
                const user = await response.json();
                localStorage.setItem('user', JSON.stringify(user));
                window.location.href = 'home.html';
            } else {
                alert('Đăng nhập thất bại. Vui lòng kiểm tra lại tên đăng nhập và mật khẩu.');
            }
        } catch (error) {
            console.error('Error:', error);
            alert('Lỗi kết nối. Vui lòng thử lại.');
        }
    });
}

// Register Form Handler
const registerForm = document.getElementById('registerForm');
if (registerForm) {
    registerForm.addEventListener('submit', async (e) => {
        e.preventDefault();
        const username = document.getElementById('username').value;
        const fullName = document.getElementById('fullName').value;
        const password = document.getElementById('password').value;

        try {
            const response = await fetch(`${API_URL}/auth/register`, {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify({ username, fullName, password })
            });

            if (response.ok) {
                alert('Đăng ký thành công! Vui lòng đăng nhập.');
                window.location.href = 'login.html';
            } else {
                alert('Đăng ký thất bại. Tên đăng nhập có thể đã tồn tại.');
            }
        } catch (error) {
            console.error('Error:', error);
            alert('Lỗi kết nối. Vui lòng thử lại.');
        }
    });
}

// Check if user is logged in
function checkAuth() {
    const user = localStorage.getItem('user');
    if (!user) {
        window.location.href = 'login.html';
    }
    return JSON.parse(user);
}

// Logout
function logout() {
    localStorage.removeItem('user');
    window.location.href = 'login.html';
}
