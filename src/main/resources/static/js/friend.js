var API_URL = 'http://localhost:8080/api';

document.addEventListener('DOMContentLoaded', () => {
    const user = checkAuth();
    setNavAvatar(user);
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
                <a href="profile.html?username=${r.senderUsername}" class="w-11 h-11 rounded-full bg-primary flex items-center justify-center text-white font-bold flex-shrink-0 hover:opacity-80 transition overflow-hidden">
                    ${r.senderAvatar ? `<img src="${r.senderAvatar}" class="w-full h-full object-cover">` : initial}
                </a>
                <div class="flex-1 min-w-0">
                    <a href="profile.html?username=${r.senderUsername}" class="font-semibold text-sm text-gray-900 hover:underline no-underline">${r.senderFullName || r.senderUsername}</a>
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
