var API_URL = 'http://localhost:8080/api';

document.addEventListener('DOMContentLoaded', () => {
    const user = checkAuth();
    setNavAvatar(user);
    if (user.role !== 'ROLE_ADMIN') {
        alert('Bạn không có quyền truy cập trang này');
        window.location.href = 'home.html';
        return;
    }
    loadUsers();
});

function setNavAvatar(user) {
    const el = document.getElementById('navAvatar');
    if (!el) return;
    const initial = (user.fullName || user.username || 'A').charAt(0).toUpperCase();
    el.textContent = initial;
}

async function loadUsers() {
    try {
        const res = await fetch(`${API_URL}/admin/users`);
        const users = await res.json();
        document.getElementById('statUsers').textContent = users.length;
        const tbody = document.getElementById('usersTableBody');
        tbody.innerHTML = users.map(u => `
            <tr class="border-b border-gray-50 hover:bg-gray-50">
                <td class="py-3 pr-4 text-sm text-gray-500">${u.id}</td>
                <td class="py-3 pr-4">
                    <div class="flex items-center gap-2">
                        <div class="w-8 h-8 bg-primary rounded-full flex items-center justify-center text-white text-xs font-bold flex-shrink-0">
                            ${(u.fullName || u.username || '?').charAt(0).toUpperCase()}
                        </div>
                        <span class="text-sm font-medium text-gray-900">${u.fullName || '-'}</span>
                    </div>
                </td>
                <td class="py-3 pr-4 text-sm text-gray-600">@${u.username}</td>
                <td class="py-3 pr-4">
                    <span class="text-xs font-semibold px-2.5 py-1 rounded-full ${u.role === 'ROLE_ADMIN' ? 'bg-red-50 text-primary' : 'bg-gray-100 text-gray-500'}">
                        ${u.role === 'ROLE_ADMIN' ? 'Admin' : 'User'}
                    </span>
                </td>
                <td class="py-3">
                    <button onclick="deleteUser(${u.id}, this)" class="text-xs text-gray-400 hover:text-red-500 transition px-2 py-1 rounded-lg hover:bg-red-50">
                        <i class="fas fa-trash"></i> Xóa
                    </button>
                </td>
            </tr>
        `).join('');
    } catch (e) { console.error(e); }
}

async function createUser() {
    const username = document.getElementById('newUsername').value.trim();
    const fullName = document.getElementById('newFullName').value.trim();
    const password = document.getElementById('newPassword').value.trim();
    if (!username || !fullName || !password) { alert('Vui lòng điền đầy đủ'); return; }
    try {
        const res = await fetch(`${API_URL}/admin/users`, {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ username, fullName, password })
        });
        if (res.ok) {
            document.getElementById('addUserModal').classList.add('hidden');
            document.getElementById('newUsername').value = '';
            document.getElementById('newFullName').value = '';
            document.getElementById('newPassword').value = '';
            loadUsers();
        } else { alert('Lỗi khi tạo người dùng'); }
    } catch (e) { alert('Lỗi kết nối'); }
}

async function deleteUser(userId, btn) {
    if (!confirm('Xóa người dùng này?')) return;
    try {
        await fetch(`${API_URL}/admin/users/${userId}`, { method: 'DELETE' });
        btn.closest('tr').remove();
        const current = parseInt(document.getElementById('statUsers').textContent);
        document.getElementById('statUsers').textContent = current - 1;
    } catch (e) { console.error(e); }
}
