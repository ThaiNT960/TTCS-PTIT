if (typeof API_URL === 'undefined') var API_URL = window.location.origin + '/api';

// Login Form Handler
const loginForm = document.getElementById('loginForm');
if (loginForm) {
    loginForm.addEventListener('submit', async (e) => {
        e.preventDefault();
        const email = document.getElementById('email').value;
        const password = document.getElementById('password').value;

        try {
            const response = await fetch(`${API_URL}/auth/login`, {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify({ email, password })
            });

            if (response.ok) {
                const user = await response.json();
                localStorage.setItem('user', JSON.stringify(user));
                // Admin → vào thẳng trang quản trị, User thường → trang chủ
                if (user.role === 'ROLE_ADMIN') {
                    window.location.href = 'admin.html';
                } else {
                    window.location.href = 'home.html';
                }
            } else {
                const errorMsg = await getErrorMessage(response);
                alert(errorMsg);
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
        const fullName = document.getElementById('fullName').value;
        const email = document.getElementById('email').value;
        const studentId = document.getElementById('studentId').value;
        const major = document.getElementById('major').value;
        const password = document.getElementById('password').value;
        const confirmPassword = document.getElementById('confirmPassword').value;

        if (password !== confirmPassword) {
            alert('Mật khẩu và xác nhận mật khẩu không trùng khớp.');
            return;
        }

        try {
            const response = await fetch(`${API_URL}/auth/register`, {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify({ email, fullName, studentId, major, password })
            });

            if (response.ok) {
                alert('Đăng ký thành công! Vui lòng đăng nhập.');
                window.location.href = 'login.html';
            } else {
                const errorMsg = await getErrorMessage(response);
                alert(errorMsg);
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

async function logout() {
    try {
        await fetch(`${API_URL}/auth/logout`, { method: 'POST' });
    } catch (e) {
        console.error('Logout error', e);
    }
    localStorage.clear();
    sessionStorage.clear();
    window.location.href = 'login.html';
}

async function fetchNotifications() {
    try {
        const notis = await apiGet(`${API_URL}/notifications`);
        
        const badge = document.getElementById('notiBadge');
        const list = document.getElementById('notiList');
        if(!badge || !list) return;

        const unreadCount = notis.filter(n => !n.isRead).length;
        if(unreadCount > 0) {
            badge.textContent = unreadCount;
            badge.classList.remove('hidden');
        } else {
            badge.classList.add('hidden');
        }

        if(notis.length === 0) {
            list.innerHTML = '<p class="text-center text-gray-400 text-sm py-8">Chưa có thông báo nào</p>';
            return;
        }

        list.innerHTML = notis.map(n => {
            const bgClass = n.isRead ? 'bg-white' : 'bg-blue-50/50';
            const iconMap = {
                'LIKE': '<i class="fas fa-thumbs-up text-blue-500"></i>',
                'COMMENT': '<i class="fas fa-comment text-green-500"></i>',
                'FRIEND_REQUEST': '<i class="fas fa-user-plus text-primary"></i>',
                'FRIEND_ACCEPT': '<i class="fas fa-user-check text-green-500"></i>',
                'MENTION': '<i class="fas fa-at text-purple-500"></i>'
            };
            const icon = iconMap[n.type] || '<i class="fas fa-bell text-gray-400"></i>';
            const initial = (n.senderFullName || n.senderUsername || '?').charAt(0).toUpperCase();
            const avatarHtml = n.senderAvatar ? `<img src="${n.senderAvatar}" class="w-full h-full object-cover">` : `<div class="w-full h-full bg-primary flex items-center justify-center text-white font-bold">${initial}</div>`;
            
            let message = '';
            if(n.type === 'LIKE') message = `<b>${n.senderFullName}</b> đã thích bài viết của bạn.`;
            else if(n.type === 'COMMENT') message = `<b>${n.senderFullName}</b> đã bình luận về bài viết của bạn.`;
            else if(n.type === 'FRIEND_REQUEST') message = `<b>${n.senderFullName}</b> đã gửi cho bạn một lời mời kết bạn.`;
            else if(n.type === 'FRIEND_ACCEPT') message = `<b>${n.senderFullName}</b> đã chấp nhận lời mời kết bạn của bạn.`;
            else message = `<b>${n.senderFullName}</b> có tương tác với bạn.`;

            return `
                <a href="${n.link || '#'}" onclick="markNotiAsRead(${n.id})" class="flex gap-3 p-3 border-b border-gray-100 hover:bg-gray-50 transition ${bgClass}">
                    <div class="relative w-10 h-10 rounded-full flex-shrink-0">
                        <div class="w-10 h-10 rounded-full overflow-hidden">${avatarHtml}</div>
                        <div class="absolute -bottom-1 -right-1 bg-white rounded-full p-0.5 border border-gray-200 shadow-sm text-[10px] w-5 h-5 flex items-center justify-center">${icon}</div>
                    </div>
                    <div class="flex-1 min-w-0">
                        <p class="text-sm text-gray-800 line-clamp-2">${message}</p>
                        <p class="text-[11px] text-primary font-medium mt-1">${formatTimeNoti(n.createdAt)}</p>
                    </div>
                </a>
            `;
        }).join('');
    } catch(e) { console.error("Lỗi tải thông báo:", e); }
}

function formatTimeNoti(dateStr) {
    if (!dateStr) return '';
    const now = new Date();
    const d = new Date(dateStr);
    const diffMin = Math.floor((now - d) / 60000);
    const diffHour = Math.floor(diffMin / 60);
    const diffDay = Math.floor(diffHour / 24);
    if (diffMin < 1) return 'Vừa xong';
    if (diffMin < 60) return `${diffMin} phút trước`;
    if (diffHour < 24) return `${diffHour} giờ trước`;
    if (diffDay < 30) return `${diffDay} ngày trước`;
    return d.toLocaleDateString('vi-VN');
}

function toggleNotiMenu() {
    document.getElementById('notiMenu').classList.toggle('hidden');
    const userMenu = document.getElementById('userMenu');
    if (userMenu && !userMenu.classList.contains('hidden')) {
        userMenu.classList.add('hidden');
    }
}

async function markNotiAsRead(id) {
    try {
        await apiPut(`${API_URL}/notifications/${id}/read`);
    } catch(e) { console.error(e); }
}

async function markAllNotiAsRead() {
    try {
        await apiPut(`${API_URL}/notifications/read-all`);
        fetchNotifications();
    } catch(e) { console.error(e); }
}

document.addEventListener('click', function(e) {
    const notiWrap = document.getElementById('notiWrap');
    if (notiWrap && !notiWrap.contains(e.target)) {
        document.getElementById('notiMenu')?.classList.add('hidden');
    }
});

document.addEventListener('DOMContentLoaded', () => {
    const userStr = localStorage.getItem('user');
    if (userStr) {
        try {
            const user = JSON.parse(userStr);
            setNavAvatar(user);
        } catch (e) {
            console.error('Error parsing user for nav avatar:', e);
        }
        fetchNotifications();
        setInterval(fetchNotifications, 30000);
    }
});

function setNavAvatar(user) {
    const el = document.getElementById('navAvatar');
    if (!el || !user) return;
    const initial = (user.fullName || user.username || 'U').charAt(0).toUpperCase();
    if (user.avatar) {
        el.innerHTML = `<img src="${user.avatar}" class="w-full h-full object-cover rounded-full" onerror="this.parentElement.textContent='${initial}'">`;
    } else {
        el.textContent = initial;
    }
}


// Helper for API responses and central redirection
async function handleApiResponse(res) {
    if (res.status === 401) {
        localStorage.clear();
        sessionStorage.clear();
        window.location.href = 'login.html';
        throw new Error('Phiên đăng nhập đã hết hạn. Vui lòng đăng nhập lại.');
    }

    if (!res.ok) {
        const errorMsg = await getErrorMessage(res);
        throw new Error(errorMsg);
    }

    const contentType = res.headers.get('content-type');
    if (contentType && contentType.includes('application/json')) {
        try {
            const text = await res.text();
            return text ? JSON.parse(text) : {};
        } catch (e) {
            return {};
        }
    }
    return await res.text();
}

async function apiGet(endpoint) {
    const res = await fetch(endpoint, {
        method: 'GET',
        headers: {
            'Content-Type': 'application/json'
        }
    });
    return handleApiResponse(res);
}

async function apiPost(endpoint, bodyData) {
    const isFormData = bodyData instanceof FormData;
    const headers = {};
    if (!isFormData) {
        headers['Content-Type'] = 'application/json';
    }
    const res = await fetch(endpoint, {
        method: 'POST',
        headers: headers,
        body: isFormData ? bodyData : JSON.stringify(bodyData)
    });
    return handleApiResponse(res);
}

async function apiPut(endpoint, bodyData) {
    const isFormData = bodyData instanceof FormData;
    const headers = {};
    if (!isFormData) {
        headers['Content-Type'] = 'application/json';
    }
    const res = await fetch(endpoint, {
        method: 'PUT',
        headers: headers,
        body: isFormData ? bodyData : JSON.stringify(bodyData)
    });
    return handleApiResponse(res);
}

async function apiDelete(endpoint) {
    const res = await fetch(endpoint, {
        method: 'DELETE',
        headers: {
            'Content-Type': 'application/json'
        }
    });
    return handleApiResponse(res);
}

async function getErrorMessage(res) {
    const defaultMsg = 'Đã có lỗi xảy ra.';
    try {
        const rawText = await res.text();
        try {
            const errJson = JSON.parse(rawText);
            return errJson.error || errJson.message || defaultMsg;
        } catch (e) {
            return rawText || defaultMsg;
        }
    } catch (e) {
        return defaultMsg;
    }
}
