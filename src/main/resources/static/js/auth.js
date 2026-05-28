if (typeof API_URL === 'undefined') var API_URL = '/api';

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
                // Admin → vào thẳng trang quản trị, User thường → trang chủ
                if (user.role === 'ROLE_ADMIN') {
                    window.location.href = 'admin.html';
                } else {
                    window.location.href = 'home.html';
                }
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

// --- NOTIFICATIONS SYSTEM ---
function escapeHtml(unsafe) {
    if (!unsafe) return "";
    return String(unsafe)
         .replace(/&/g, "&amp;")
         .replace(/</g, "&lt;")
         .replace(/>/g, "&gt;")
         .replace(/"/g, "&quot;")
         .replace(/'/g, "&#039;");
}

async function fetchNotifications() {
    const user = checkAuth();
    try {
        const res = await fetch(`${API_URL}/notifications`);
        if(!res.ok) return;
        const notis = await res.json();
        
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
            const safeSenderName = escapeHtml(n.senderFullName || n.senderUsername || '?');
            const initial = safeSenderName.charAt(0).toUpperCase();
            const avatarHtml = n.senderAvatar ? `<img src="${escapeHtml(n.senderAvatar)}" class="w-full h-full object-cover">` : `<div class="w-full h-full bg-primary flex items-center justify-center text-white font-bold">${initial}</div>`;
            
            let message = '';
            if(n.type === 'LIKE') message = `<b>${safeSenderName}</b> đã thích bài viết của bạn.`;
            else if(n.type === 'COMMENT') message = `<b>${safeSenderName}</b> đã bình luận về bài viết của bạn.`;
            else if(n.type === 'FRIEND_REQUEST') message = `<b>${safeSenderName}</b> đã gửi cho bạn một lời mời kết bạn.`;
            else if(n.type === 'FRIEND_ACCEPT') message = `<b>${safeSenderName}</b> đã chấp nhận lời mời kết bạn của bạn.`;
            else message = `<b>${safeSenderName}</b> có tương tác với bạn.`;

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
        await fetch(`${API_URL}/notifications/${id}/read`, { method: 'PUT' });
    } catch(e) { console.error(e); }
}

async function markAllNotiAsRead() {
    const user = checkAuth();
    try {
        await fetch(`${API_URL}/notifications/read-all`, { method: 'PUT' });
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
    if(localStorage.getItem('user')) {
        fetchNotifications();
        setInterval(fetchNotifications, 30000);
    }
});
