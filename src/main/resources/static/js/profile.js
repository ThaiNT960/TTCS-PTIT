var API_URL = 'http://localhost:8080/api';

document.addEventListener('DOMContentLoaded', () => {
    const user = checkAuth();
    setNavAvatar(user);
    loadPosts(user);
    loadProfileInfo(user);
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

function loadProfileInfo(user) {
    const nameEl = document.getElementById('profileName');
    const unameEl = document.getElementById('profileUsername');
    const avatarEl = document.getElementById('profileAvatar');
    const editName = document.getElementById('editFullName');
    const editAv = document.getElementById('editAvatar');

    if (nameEl) nameEl.textContent = user.fullName || user.username;
    if (unameEl) unameEl.textContent = '@' + user.username;
    const initial = (user.fullName || user.username || 'U').charAt(0).toUpperCase();
    if (avatarEl) {
        if (user.avatar) {
            avatarEl.innerHTML = `<img src="${user.avatar}" class="w-full h-full object-cover" onerror="this.textContent='${initial}'">`;
        } else {
            avatarEl.textContent = initial;
        }
    }
    if (editName) editName.value = user.fullName || '';
    if (editAv) editAv.value = user.avatar || '';
}

function formatTime(dateStr) {
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

async function loadPosts(user) {
    try {
        const res = await fetch(`${API_URL}/posts?username=${encodeURIComponent(user.username)}`);
        const posts = await res.json();
        const container = document.getElementById('profilePosts');
        if (!container) return;
        if (!posts.length) {
            container.innerHTML = `<p class="text-center text-gray-400 text-sm py-6">Chưa có bài viết nào</p>`;
            return;
        }
        container.innerHTML = '';
        const userPosts = posts.filter(p => p.username === user.username);
        if (!userPosts.length) {
            container.innerHTML = `<p class="text-center text-gray-400 text-sm py-6">Chưa có bài viết nào</p>`;
            return;
        }
        userPosts.forEach(post => {
            const div = document.createElement('div');
            div.className = 'border border-gray-100 rounded-xl p-4 mb-4 last:mb-0';
            div.innerHTML = `
                <p class="text-gray-800 text-sm leading-relaxed mb-2">${post.content}</p>
                ${post.imageUrl ? `<img src="${post.imageUrl}" class="w-full rounded-xl mb-2 max-h-60 object-cover" onerror="this.style.display='none'">` : ''}
                <p class="text-xs text-gray-400">${formatTime(post.createdAt)} · ${post.likeCount || 0} thích · ${(post.comments || []).length} bình luận</p>
            `;
            container.appendChild(div);
        });
    } catch (e) { console.error(e); }
}

async function saveProfile() {
    const user = checkAuth();
    const fullName = document.getElementById('editFullName').value.trim();
    const avatar = document.getElementById('editAvatar').value.trim();
    if (!fullName) { alert('Vui lòng nhập tên'); return; }
    try {
        const res = await fetch(`${API_URL}/auth/profile`, {
            method: 'PUT',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ username: user.username, fullName, avatar: avatar || null })
        });
        if (res.ok) {
            const updated = { ...user, fullName, avatar: avatar || user.avatar };
            localStorage.setItem('user', JSON.stringify(updated));
            document.getElementById('editModal').classList.add('hidden');
            loadProfileInfo(updated);
            setNavAvatar(updated);
        } else {
            alert('Lỗi khi lưu hồ sơ');
        }
    } catch (e) { alert('Lỗi kết nối'); }
}
