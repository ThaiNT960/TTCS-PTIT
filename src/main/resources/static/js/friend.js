var API_URL = 'http://localhost:8080/api';

document.addEventListener('DOMContentLoaded', () => {
    const user = checkAuth();
    setNavAvatar(user);
    loadFriends(user);
    loadFriendRequests(user);
});

function setNavAvatar(user) {
    const el = document.getElementById('navAvatar');
    if (!el) return;
    const initial = (user.fullName || user.username || 'U').charAt(0).toUpperCase();
    if (user.avatar) {
        el.innerHTML = `<img src="${user.avatar}" class="w-full h-full object-cover rounded-full" onerror="this.parentElement.textContent='${initial}'">`;
    } else {
        el.textContent = initial;
    }
}

async function loadFriends(user) {
    const container = document.getElementById('friendsList');
    try {
        const res = await fetch(`${API_URL}/friends?username=${user.username}`);
        const friends = await res.json();
        if (!friends.length) {
            container.innerHTML = `<p class="text-center text-gray-400 text-sm py-6">Bạn chưa có bạn bè nào</p>`;
            return;
        }
        container.innerHTML = friends.map(f => {
            const initial = (f.fullName || f.username || '?').charAt(0).toUpperCase();
            return `
            <div class="flex items-center gap-3 py-3 border-b border-gray-100 last:border-0">
                <div class="w-11 h-11 rounded-full bg-primary flex items-center justify-center text-white font-bold flex-shrink-0">${initial}</div>
                <div class="flex-1 min-w-0">
                    <p class="font-semibold text-sm text-gray-900">${f.fullName}</p>
                    <p class="text-xs text-gray-400">@${f.username}</p>
                </div>
                <a href="chat.html" onclick="sessionStorage.setItem('currentChatUser','${f.username}');sessionStorage.setItem('currentChatFullName','${f.fullName}')"
                    class="text-xs bg-gray-100 hover:bg-gray-200 text-gray-600 font-medium px-3 py-1.5 rounded-full transition">
                    <i class="fas fa-comment-dots"></i> Nhắn tin
                </a>
            </div>`;
        }).join('');
    } catch (e) { console.error(e); }
}

async function loadFriendRequests(user) {
    const container = document.getElementById('friendRequests');
    try {
        const res = await fetch(`${API_URL}/friends/requests?username=${user.username}`);
        const requests = await res.json();
        if (!requests.length) {
            container.innerHTML = `<p class="text-center text-gray-400 text-sm py-6">Không có lời mời kết bạn nào</p>`;
            return;
        }
        container.innerHTML = requests.map(r => {
            const initial = (r.senderFullName || r.senderUsername || '?').charAt(0).toUpperCase();
            return `
            <div class="flex items-center gap-3 py-3 border-b border-gray-100 last:border-0">
                <div class="w-11 h-11 rounded-full bg-primary flex items-center justify-center text-white font-bold flex-shrink-0">${initial}</div>
                <div class="flex-1 min-w-0">
                    <p class="font-semibold text-sm text-gray-900">${r.senderFullName || r.senderUsername}</p>
                    <p class="text-xs text-gray-400">@${r.senderUsername}</p>
                </div>
                <div class="flex gap-2">
                    <button onclick="acceptRequest(${r.id}, this)" class="text-xs bg-primary hover:bg-primary-dark text-white font-semibold px-3 py-1.5 rounded-full transition">Chấp nhận</button>
                    <button onclick="rejectRequest(${r.id}, this)" class="text-xs bg-gray-100 hover:bg-gray-200 text-gray-600 font-medium px-3 py-1.5 rounded-full transition">Từ chối</button>
                </div>
            </div>`;
        }).join('');
    } catch (e) { console.error(e); }
}

async function acceptRequest(requestId, btn) {
    try {
        await fetch(`${API_URL}/friends/accept/${requestId}`, { method: 'POST' });
        btn.closest('.flex').innerHTML = `<span class="text-xs text-green-600 font-medium"><i class="fas fa-check"></i> Đã chấp nhận</span>`;
    } catch (e) { console.error(e); }
}

async function rejectRequest(requestId, btn) {
    try {
        await fetch(`${API_URL}/friends/reject/${requestId}`, { method: 'POST' });
        btn.closest('[class*="border-b"]').remove();
    } catch (e) { console.error(e); }
}

async function searchFriend() {
    const user = checkAuth();
    const query = document.getElementById('searchInput').value.trim();
    if (!query) return;
    const container = document.getElementById('searchResults');
    container.innerHTML = `<p class="text-center text-gray-400 text-sm py-4">Đang tìm kiếm...</p>`;
    try {
        const res = await fetch(`${API_URL}/auth/users/search?keyword=${encodeURIComponent(query)}`);
        const users = await res.json();
        const filtered = users.filter(u => u.username !== user.username);
        if (!filtered.length) {
            container.innerHTML = `<p class="text-center text-gray-400 text-sm py-4">Không tìm thấy người dùng nào</p>`;
            return;
        }
        container.innerHTML = filtered.map(u => {
            const initial = (u.fullName || u.username || '?').charAt(0).toUpperCase();
            return `
            <div class="flex items-center gap-3 py-3 border-b border-gray-100 last:border-0">
                <div class="w-11 h-11 rounded-full bg-primary flex items-center justify-center text-white font-bold flex-shrink-0">${initial}</div>
                <div class="flex-1 min-w-0">
                    <p class="font-semibold text-sm text-gray-900">${u.fullName || u.username}</p>
                    <p class="text-xs text-gray-400">@${u.username}</p>
                </div>
                <button onclick="sendRequest('${u.username}', this)" class="text-xs bg-primary hover:bg-primary-dark text-white font-semibold px-3 py-1.5 rounded-full transition">
                    <i class="fas fa-user-plus"></i> Kết bạn
                </button>
            </div>`;
        }).join('');
    } catch (e) {
        container.innerHTML = `<p class="text-center text-red-400 text-sm py-4">Lỗi tìm kiếm</p>`;
    }
}

async function sendRequest(receiverUsername, btn) {
    const user = checkAuth();
    try {
        await fetch(`${API_URL}/friends/request`, {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ senderUsername: user.username, receiverUsername })
        });
        btn.textContent = 'Đã gửi';
        btn.disabled = true;
        btn.className = 'text-xs bg-gray-200 text-gray-500 font-medium px-3 py-1.5 rounded-full';
    } catch (e) { console.error(e); }
}
