var API_URL = '/api';

function escapeHtml(str) {
    if (!str) return "";
    return String(str)
        .replaceAll("&", "&amp;")
        .replaceAll("<", "&lt;")
        .replaceAll(">", "&gt;")
        .replaceAll('"', "&quot;")
        .replaceAll("'", "&#039;");
}

document.addEventListener('DOMContentLoaded', () => {
    const user = checkAuth();
    setNavAvatar(user);
    loadFriends(user);
    loadFriendRequests(user);
    loadFriendSuggestions(user);
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
        const res = await fetch(`${API_URL}/friends`);
        const friends = await res.json();
        if (!friends.length) {
            container.innerHTML = `<p class="text-center text-gray-400 text-sm py-6">Bạn chưa có bạn bè nào</p>`;
            return;
        }
        container.innerHTML = friends.map(f => {
            const initial = (f.fullName || f.username || '?').charAt(0).toUpperCase();
            const avatarHtml = f.avatar 
                ? `<img src="${escapeHtml(f.avatar)}" class="w-full h-full object-cover">`
                : escapeHtml(initial);
                
            return `
            <div class="flex items-center gap-3 py-3 border-b border-gray-100 last:border-0">
                <a href="profile.html?username=${escapeHtml(f.username)}" class="w-11 h-11 rounded-full bg-primary flex items-center justify-center text-white font-bold flex-shrink-0 overflow-hidden no-underline hover:opacity-90 transition">
                    ${avatarHtml}
                </a>
                <div class="flex-1 min-w-0">
                    <a href="profile.html?username=${escapeHtml(f.username)}" class="font-semibold text-sm text-gray-900 hover:underline block truncate no-underline">${escapeHtml(f.fullName)}</a>
                    <p class="text-xs text-gray-400 truncate">@${escapeHtml(f.username)}</p>
                </div>
                <a href="chat.html" data-username="${escapeHtml(f.username)}" data-fullname="${escapeHtml(f.fullName)}"
                    class="friend-chat-btn text-xs bg-gray-100 hover:bg-gray-200 text-gray-600 font-medium px-3 py-1.5 rounded-full transition no-underline">
                    <i class="fas fa-comment-dots"></i> Nhắn tin
                </a>
                <button data-username="${escapeHtml(f.username)}" class="friend-unfriend-btn text-xs bg-red-50 hover:bg-red-100 text-red-500 font-medium px-3 py-1.5 rounded-full transition ml-2">
                    <i class="fas fa-user-times"></i> Hủy kết bạn
                </button>
            </div>`;
        }).join('');
        
        container.querySelectorAll('.friend-chat-btn').forEach(btn => {
            btn.addEventListener('click', () => {
                sessionStorage.setItem('currentChatUser', btn.dataset.username);
                sessionStorage.setItem('currentChatFullName', btn.dataset.fullname);
            });
        });
        
        container.querySelectorAll('.friend-unfriend-btn').forEach(btn => {
            btn.addEventListener('click', () => unfriend(btn.dataset.username, btn));
        });
    } catch (e) { console.error(e); }
}

async function loadFriendRequests(user) {
    const container = document.getElementById('friendRequests');
    try {
        const res = await fetch(`${API_URL}/friends/requests`);
        const requests = await res.json();
        if (!requests.length) {
            container.innerHTML = `<p class="text-center text-gray-400 text-sm py-6">Không có lời mời kết bạn nào</p>`;
            return;
        }
        container.innerHTML = requests.map(r => {
            const initial = (r.senderFullName || r.senderUsername || '?').charAt(0).toUpperCase();
            const avatarHtml = r.senderAvatar 
                ? `<img src="${escapeHtml(r.senderAvatar)}" class="w-full h-full object-cover">`
                : escapeHtml(initial);
                
            return `
            <div class="flex items-center gap-3 py-3 border-b border-gray-100 last:border-0">
                <a href="profile.html?username=${escapeHtml(r.senderUsername)}" class="w-11 h-11 rounded-full bg-primary flex items-center justify-center text-white font-bold flex-shrink-0 overflow-hidden no-underline hover:opacity-90 transition">
                    ${avatarHtml}
                </a>
                <div class="flex-1 min-w-0">
                    <a href="profile.html?username=${escapeHtml(r.senderUsername)}" class="font-semibold text-sm text-gray-900 hover:underline block truncate no-underline">${escapeHtml(r.senderFullName || r.senderUsername)}</a>
                    <p class="text-xs text-gray-400 truncate">@${escapeHtml(r.senderUsername)}</p>
                </div>
                <div class="flex gap-2">
                    <button data-id="${r.id}" class="request-accept-btn text-xs bg-primary hover:bg-primary-dark text-white font-semibold px-3 py-1.5 rounded-full transition">Chấp nhận</button>
                    <button data-id="${r.id}" class="request-reject-btn text-xs bg-gray-100 hover:bg-gray-200 text-gray-600 font-medium px-3 py-1.5 rounded-full transition">Từ chối</button>
                </div>
            </div>`;
        }).join('');
        
        container.querySelectorAll('.request-accept-btn').forEach(btn => {
            btn.addEventListener('click', () => acceptRequest(btn.dataset.id, btn));
        });
        
        container.querySelectorAll('.request-reject-btn').forEach(btn => {
            btn.addEventListener('click', () => rejectRequest(btn.dataset.id, btn));
        });
    } catch (e) { console.error(e); }
}

async function acceptRequest(requestId, btn) {
    try {
        const res = await fetch(`${API_URL}/friends/accept/${requestId}`, { method: 'POST' });
        if (!res.ok) {
            const error = await res.json().catch(() => ({}));
            throw new Error(error.error || 'Thao tác thất bại');
        }
        btn.closest('.flex').innerHTML = `<span class="text-xs text-green-600 font-medium"><i class="fas fa-check"></i> Đã chấp nhận</span>`;
    } catch (e) { 
        console.error(e); 
        alert(e.message);
    }
}

async function rejectRequest(requestId, btn) {
    try {
        const res = await fetch(`${API_URL}/friends/reject/${requestId}`, { method: 'POST' });
        if (!res.ok) {
            const error = await res.json().catch(() => ({}));
            throw new Error(error.error || 'Thao tác thất bại');
        }
        btn.closest('[class*="border-b"]').remove();
    } catch (e) { 
        console.error(e);
        alert(e.message);
    }
}

async function unfriend(targetUsername, btn) {
    if(!confirm('Bạn có chắc chắn muốn hủy kết bạn?')) return;
    const user = checkAuth();
    try {
        const res = await fetch(`${API_URL}/friends/unfriend/${targetUsername}`, { method: 'DELETE' });
        if (!res.ok) {
            const error = await res.json().catch(() => ({}));
            throw new Error(error.error || 'Thao tác thất bại');
        }
        btn.closest('.flex').remove();
        if(document.getElementById('friendsList').children.length === 0) {
            document.getElementById('friendsList').innerHTML = `<p class="text-center text-gray-400 text-sm py-6">Bạn chưa có bạn bè nào</p>`;
        }
    } catch (e) { 
        console.error(e);
        alert(e.message);
    }
}

async function searchFriend() {
    const user = checkAuth();
    const query = document.getElementById('searchInput').value.trim();
    if (!query) return;
    const container = document.getElementById('searchResults');
    container.innerHTML = `<p class="text-center text-gray-400 text-sm py-4">Đang tìm kiếm...</p>`;
    try {
        const [res, friendsRes] = await Promise.all([
            fetch(`${API_URL}/auth/users/search?keyword=${encodeURIComponent(query)}`),
            fetch(`${API_URL}/friends`)
        ]);
        const users = await res.json();
        const friends = friendsRes.ok ? await friendsRes.json() : [];
        const friendUsernames = new Set(friends.map(f => f.username));

        const filtered = users.filter(u => u.username !== user.username);
        if (!filtered.length) {
            container.innerHTML = `<p class="text-center text-gray-400 text-sm py-4">Không tìm thấy người dùng nào</p>`;
            return;
        }
        container.innerHTML = filtered.map(u => {
            const initial = (u.fullName || u.username || '?').charAt(0).toUpperCase();
            const avatarHtml = u.avatar 
                ? `<img src="${escapeHtml(u.avatar)}" class="w-full h-full object-cover">`
                : escapeHtml(initial);
                
            let btnHtml = '';
            if (friendUsernames.has(u.username)) {
                btnHtml = `<button disabled class="text-xs bg-gray-200 text-gray-500 font-semibold px-3 py-1.5 rounded-full"><i class="fas fa-check"></i> Đã là bạn bè</button>`;
            } else {
                btnHtml = `<button class="search-add-btn text-xs bg-primary hover:bg-primary-dark text-white font-semibold px-3 py-1.5 rounded-full transition" data-username="${escapeHtml(u.username)}">
                    <i class="fas fa-user-plus"></i> Kết bạn
                </button>`;
            }
            
            return `
            <div class="flex items-center gap-3 py-3 border-b border-gray-100 last:border-0">
                <a href="profile.html?username=${escapeHtml(u.username)}" class="w-11 h-11 rounded-full bg-primary flex items-center justify-center text-white font-bold flex-shrink-0 overflow-hidden no-underline hover:opacity-90 transition">
                    ${avatarHtml}
                </a>
                <div class="flex-1 min-w-0">
                    <a href="profile.html?username=${escapeHtml(u.username)}" class="font-semibold text-sm text-gray-900 hover:underline block truncate no-underline">${escapeHtml(u.fullName || u.username)}</a>
                    <p class="text-xs text-gray-400 truncate">@${escapeHtml(u.username)}</p>
                </div>
                ${btnHtml}
            </div>`;
        }).join('');
        
        container.querySelectorAll('.search-add-btn').forEach(btn => {
            btn.addEventListener('click', () => sendRequest(btn.dataset.username, btn));
        });
        
    } catch (e) {
        container.innerHTML = `<p class="text-center text-red-400 text-sm py-4">Lỗi tìm kiếm</p>`;
    }
}

async function sendRequest(receiverUsername, btn) {
    try {
        const res = await fetch(`${API_URL}/friends/request`, {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ receiverUsername })
        });
        if (!res.ok) {
            const error = await res.json().catch(() => ({}));
            throw new Error(error.error || 'Thao tác thất bại');
        }
        btn.textContent = 'Đã gửi';
        btn.disabled = true;
        btn.className = 'text-xs bg-gray-200 text-gray-500 font-medium px-3 py-1.5 rounded-full';
    } catch (e) { 
        console.error(e);
        alert(e.message);
    }
}

async function loadFriendSuggestions(user) {
    const container = document.getElementById('friendSuggestions');
    container.innerHTML = `<p class="text-center text-gray-400 text-sm py-4"><i class="fas fa-spinner fa-spin mr-2"></i>Đang tải gợi ý...</p>`;
    
    try {
        const res = await fetch(`${API_URL}/friends/suggestions?limit=10`);
        if (!res.ok) throw new Error('API error');
        const data = await res.json();
        
        if (!data || data.length === 0) {
            container.innerHTML = `
                <div class="py-10 text-center">
                    <div class="w-16 h-16 bg-gray-100 rounded-full flex items-center justify-center mx-auto mb-4">
                        <i class="fas fa-user-friends text-gray-400 text-2xl"></i>
                    </div>
                    <h3 class="text-gray-900 font-semibold mb-1">Chưa có gợi ý nào</h3>
                    <p class="text-gray-400 text-sm max-w-xs mx-auto">Hãy kết bạn thêm để chúng tôi có thể gợi ý những người phù hợp với bạn.</p>
                </div>
            `;
            return;
        }

        container.innerHTML = data.map(s => {
            const initials = (s.fullName || s.username || '?').charAt(0).toUpperCase();
            const avatarHtml = s.avatar 
                ? `<img src="${escapeHtml(s.avatar)}" class="w-full h-full object-cover">`
                : escapeHtml(initials);
                
            let reasonsHtml = '';
            if (s.reasons && s.reasons.length > 0) {
                // Show max 3 reasons
                reasonsHtml = s.reasons.slice(0, 3).map(r => `<p class="text-[11px] text-gray-400 mt-0.5"><i class="fas fa-info-circle mr-1"></i>${escapeHtml(r)}</p>`).join('');
            }
                
            return `
            <div class="flex items-center gap-3 p-3 hover:bg-gray-50 rounded-xl transition group">
                <a href="profile.html?username=${escapeHtml(s.username)}" class="w-12 h-12 rounded-full bg-primary flex items-center justify-center text-white font-bold text-lg flex-shrink-0 overflow-hidden shadow-sm hover:opacity-90 transition no-underline">
                    ${avatarHtml}
                </a>
                <div class="flex-1 min-w-0">
                    <a href="profile.html?username=${escapeHtml(s.username)}" class="font-semibold text-sm text-gray-900 hover:underline block truncate no-underline">${escapeHtml(s.fullName || s.username)}</a>
                    <p class="text-xs text-gray-400 mb-1 truncate">@${escapeHtml(s.username)}</p>
                    ${reasonsHtml}
                </div>
                <button class="send-suggestion-btn text-xs bg-primary text-white font-medium px-3 py-1.5 rounded-full hover:bg-primary-dark transition shadow-sm hover:shadow active:scale-95" data-username="${escapeHtml(s.username)}">
                    Kết bạn
                </button>
            </div>
            `;
        }).join('');
        
        container.querySelectorAll('.send-suggestion-btn').forEach(btn => {
            btn.addEventListener('click', () => {
                sendRequest(btn.dataset.username, btn);
            });
        });

    } catch (e) {
        console.error(e);
        container.innerHTML = `<p class="text-center text-red-400 text-sm py-4">Lỗi khi tải gợi ý kết bạn</p>`;
    }
}
