var API_URL = '/api';

document.addEventListener('DOMContentLoaded', () => {
    const user = checkAuth();
    if (user.role !== 'ROLE_ADMIN') {
        alert('Bạn không có quyền truy cập trang này');
        window.location.href = 'home.html';
        return;
    }
    loadDashboardStats();
    loadUsers();
    loadPosts();
    loadAnnouncements();
    checkAiService();
});

var currentTab = 'all';

function showToast(message, type) {
    var toast = document.getElementById('toast');
    if (!toast) return;
    var icon = document.getElementById('toastIcon');
    var msg = document.getElementById('toastMessage');
    msg.textContent = message;
    if (type === 'success') {
        icon.className = 'fas fa-check-circle text-green-500 text-lg';
        toast.firstElementChild.style.borderColor = '#dcfce7';
    } else if (type === 'error') {
        icon.className = 'fas fa-exclamation-circle text-red-500 text-lg';
        toast.firstElementChild.style.borderColor = '#fee2e2';
    } else {
        icon.className = 'fas fa-info-circle text-blue-500 text-lg';
        toast.firstElementChild.style.borderColor = '#dbeafe';
    }
    toast.classList.remove('hidden');
    toast.style.animation = 'fadeIn 0.3s ease';
    setTimeout(function() { toast.classList.add('hidden'); }, 3000);
}

function updateModeUI(mode) {
    document.querySelectorAll('.mode-card').forEach(function(card) {
        card.classList.remove('active');
        card.querySelector('.mode-icon').classList.remove('bg-primary', 'text-white');
        card.querySelector('.mode-icon').classList.add('bg-gray-200', 'text-gray-500');
    });
    var activeCard = document.getElementById('mode-' + mode);
    if(activeCard) {
        activeCard.classList.add('active');
        activeCard.querySelector('.mode-icon').classList.remove('bg-gray-200', 'text-gray-500');
        activeCard.querySelector('.mode-icon').classList.add('bg-primary', 'text-white');
    }
}

async function loadDashboardStats() {
    try {
        const res = await fetch(`${API_URL}/admin/dashboard`);
        if (res.ok) {
            const data = await res.json();
            // backend trả về usersCount, postsCount
            document.getElementById('statUsers').textContent = data.usersCount ?? data.userCount ?? '-';
            document.getElementById('statPosts').textContent = data.postsCount ?? data.postCount ?? '-';
            document.getElementById('statPending').textContent = data.pendingCount ?? '-';
            document.getElementById('statRejected').textContent = data.rejectedCount ?? '-';
            
            document.getElementById('tabAllCount').textContent = data.postsCount ?? data.postCount ?? 0;
            document.getElementById('tabPendingCount').textContent = data.pendingCount ?? 0;
            document.getElementById('tabRejectedCount').textContent = data.rejectedCount ?? 0;
            
            updateModeUI(data.moderationMode);
        }
    } catch(e) { console.error(e); }
}



async function loadUsers() {
    try {
        const res = await fetch(`${API_URL}/admin/users`);
        const users = await res.json();
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
    if (!confirm('Xóa người dùng này? Thao tác này sẽ xóa mọi dữ liệu liên quan!')) return;
    try {
        await fetch(`${API_URL}/admin/users/${userId}`, { method: 'DELETE' });
        btn.closest('tr').remove();
        showToast('Đã xóa người dùng', 'success');
        loadDashboardStats();
    } catch (e) { console.error(e); showToast('Lỗi xóa người dùng', 'error'); }
}

async function setModerationMode(mode) {
    try {
        // Backend nhận @RequestBody Map<String,String> → phải gửi JSON
        const res = await fetch(`${API_URL}/admin/moderation/mode`, {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ mode: mode })
        });
        if(res.ok) {
            updateModeUI(mode);
            var modeNames = { 'NONE': 'Không kiểm duyệt', 'MANUAL': 'Kiểm duyệt thủ công', 'AUTO_AI': 'Tự động (AI)' };
            showToast('Đã chuyển sang: ' + modeNames[mode], 'success');
        } else {
            const errText = await res.text();
            console.error('Lỗi đổi chế độ:', errText);
            showToast('Lỗi đổi chế độ kiểm duyệt', 'error');
        }
    } catch(e) { console.error(e); showToast('Lỗi kết nối', 'error'); }
}

async function checkAiService() {
    try {
        // Đúng endpoint backend: /api/admin/moderation/ai-status
        const res = await fetch(`${API_URL}/admin/moderation/ai-status`);
        const data = await res.json();
        var dot = document.getElementById('aiStatusDot');
        var text = document.getElementById('aiStatusText');
        if(dot && text) {
            if (data.available) {
                dot.className = 'ai-status-dot ai-online';
                text.className = 'text-green-600 font-medium';
                text.textContent = 'AI Service: Đang chạy';
            } else {
                dot.className = 'ai-status-dot ai-offline';
                text.className = 'text-red-500 font-medium';
                text.textContent = 'AI Service: Không khả dụng';
            }
        }
    } catch(e) {
        console.error(e);
        var dot = document.getElementById('aiStatusDot');
        var text = document.getElementById('aiStatusText');
        if(dot) dot.className = 'ai-status-dot ai-offline';
        if(text) { text.className = 'text-red-500 font-medium'; text.textContent = 'AI Service: Không kết nối được'; }
    }
}

function switchTab(tab, btn) {
    currentTab = tab;
    document.querySelectorAll('.tab-btn').forEach(function(b) { b.classList.remove('active'); });
    btn.classList.add('active');

    var bulkActions = document.getElementById('bulkActions');
    if(bulkActions) {
        bulkActions.style.display = tab === 'pending' ? 'flex' : 'none';
    }

    document.querySelectorAll('.post-row').forEach(function(row) {
        if (tab === 'all') {
            row.style.display = '';
        } else {
            row.style.display = row.dataset.status === tab.toUpperCase() ? '' : 'none';
        }
    });
}

function formatDateDisplay(dateStr) {
    if(!dateStr) return '';
    try {
        return new Date(dateStr).toLocaleString('vi-VN');
    } catch(e) { return dateStr; }
}

async function loadPosts() {
    try {
        // Dùng endpoint admin để lấy TẤT CẢ bài viết (kể cả PENDING/REJECTED)
        const res = await fetch(`${API_URL}/admin/posts`);
        const posts = await res.json();
        const tbody = document.getElementById('postsTableBody');
        
        if(!posts || posts.length === 0) {
            tbody.innerHTML = '<tr><td colspan="7" class="text-center text-gray-400 py-4">Không có bài viết nào.</td></tr>';
            return;
        }

        tbody.innerHTML = posts.map(p => {
            let statusBadge = '';
            const st = p.status || 'APPROVED';
            if (st === 'PENDING') statusBadge = '<span class="status-badge status-pending"><i class="fas fa-hourglass-half mr-1"></i>Chờ duyệt</span>';
            else if (st === 'REJECTED') statusBadge = '<span class="status-badge status-rejected"><i class="fas fa-ban mr-1"></i>Từ chối</span>';
            else statusBadge = '<span class="status-badge status-approved"><i class="fas fa-check-circle mr-1"></i>Đã duyệt</span>';

            let aiLabelHtml = '<span class="text-gray-300 text-xs">—</span>';
            if (p.moderationLabel) {
                if (p.moderationLabel === 'CLEAN') aiLabelHtml = '<span class="text-green-600 font-medium text-xs"><i class="fas fa-leaf mr-1"></i>Clean</span>';
                else if (p.moderationLabel === 'OFFENSIVE') aiLabelHtml = '<span class="text-orange-500 font-medium text-xs"><i class="fas fa-exclamation-triangle mr-1"></i>Offensive</span>';
                else if (p.moderationLabel === 'HATE') aiLabelHtml = '<span class="text-red-600 font-medium text-xs"><i class="fas fa-skull-crossbones mr-1"></i>Hate</span>';
                
                if (p.moderationConfidence) {
                    aiLabelHtml += `<span class="text-gray-400 text-xs ml-1">${(p.moderationConfidence * 100).toFixed(0)}%</span>`;
                }
            }

            let actionsHtml = '';
            if (st === 'PENDING') {
                actionsHtml += `<button onclick="approvePost(${p.id})" class="text-xs text-green-500 hover:text-green-700 transition px-2 py-1 rounded-lg hover:bg-green-50 font-medium"><i class="fas fa-check"></i> Duyệt</button>`;
                actionsHtml += `<button onclick="rejectPost(${p.id})" class="text-xs text-red-400 hover:text-red-600 transition px-2 py-1 rounded-lg hover:bg-red-50 font-medium"><i class="fas fa-times"></i> Từ chối</button>`;
            } else if (st === 'REJECTED') {
                actionsHtml += `<button onclick="approvePost(${p.id})" class="text-xs text-green-500 hover:text-green-700 transition px-2 py-1 rounded-lg hover:bg-green-50 font-medium"><i class="fas fa-check"></i> Duyệt lại</button>`;
            }
            actionsHtml += `<button onclick="deletePost(${p.id}, this)" class="text-xs text-gray-400 hover:text-red-500 transition px-2 py-1 rounded-lg hover:bg-red-50"><i class="fas fa-trash"></i> Xóa</button>`;

            return `
            <tr class="border-b border-gray-50 hover:bg-gray-50 post-row" data-status="${st}">
                <td class="py-3 pr-4 text-sm text-gray-500">${p.id}</td>
                <td class="py-3 pr-4 text-sm font-medium text-gray-900">${escapeHtml(p.userFullName || p.username)}</td>
                <td class="py-3 pr-4 text-sm text-gray-600 max-w-xs truncate">${escapeHtml(p.content)}</td>
                <td class="py-3 pr-4">${statusBadge}</td>
                <td class="py-3 pr-4 text-sm">${aiLabelHtml}</td>
                <td class="py-3 pr-4 text-sm text-gray-500">${formatDateDisplay(p.createdAt)}</td>
                <td class="py-3"><div class="flex items-center gap-1">${actionsHtml}</div></td>
            </tr>`;
        }).join('');
    } catch(e) { console.error(e); }
}

async function approvePost(postId) {
    try {
        const res = await fetch(`${API_URL}/admin/posts/${postId}/approve`, { method: 'POST' });
        if(res.ok) {
            showToast('Đã duyệt bài #' + postId, 'success');
            loadDashboardStats();
            loadPosts();
        } else showToast('Lỗi duyệt bài', 'error');
    } catch(e) { console.error(e); }
}

async function rejectPost(postId) {
    if(!confirm('Từ chối bài viết #' + postId + '?')) return;
    try {
        const res = await fetch(`${API_URL}/admin/posts/${postId}/reject`, { method: 'POST' });
        if(res.ok) {
            showToast('Đã từ chối bài #' + postId, 'success');
            loadDashboardStats();
            loadPosts();
        } else showToast('Lỗi từ chối', 'error');
    } catch(e) { console.error(e); }
}

async function approveAllPending() {
    if(!confirm('Duyệt tất cả bài đang chờ?')) return;
    try {
        const res = await fetch(`${API_URL}/admin/posts/approve-all`, { method: 'POST' });
        if(res.ok) {
            const data = await res.json();
            showToast(`Đã duyệt ${data.count} bài viết!`, 'success');
            loadDashboardStats();
            loadPosts();
        } else showToast('Lỗi duyệt tất cả', 'error');
    } catch(e) { console.error(e); }
}

async function deletePost(postId, btn) {
    if(!confirm('Xóa bài viết này?')) return;
    try {
        const currentUser = checkAuth();
        const res = await fetch(`${API_URL}/admin/posts/${postId}`, { method: 'DELETE' });
        if(res.ok) {
            showToast('Đã xóa bài viết', 'success');
            loadDashboardStats();
            loadPosts();
        } else showToast('Lỗi xóa bài', 'error');
    } catch(e) { console.error(e); }
}

async function loadAnnouncements() {
    try {
        const res = await fetch(`${API_URL}/announcements`);
        const anns = await res.json();
        const list = document.getElementById('announcementsList');
        if(!anns || anns.length === 0) {
            list.innerHTML = '<p class="text-sm text-gray-400 text-center py-4">Chưa có thông báo nào.</p>';
            return;
        }

        list.innerHTML = anns.map(ann => `
            <div class="flex items-start gap-3 p-3 bg-gray-50 rounded-xl">
                <div class="w-8 h-8 bg-yellow-100 rounded-lg flex items-center justify-center flex-shrink-0">
                    <i class="fas fa-bullhorn text-yellow-600 text-sm"></i>
                </div>
                <div class="flex-1 min-w-0">
                    <p class="font-semibold text-sm text-gray-900">${escapeHtml(ann.title)}</p>
                    <p class="text-xs text-gray-500 mt-0.5 truncate">${escapeHtml(ann.content)}</p>
                    <p class="text-xs text-gray-400 mt-1">${formatDateDisplay(ann.createdAt)} — ${escapeHtml(ann.adminName || 'Admin')}</p>
                </div>
                <button onclick="deleteAnnouncement(${ann.id})" class="text-xs text-gray-400 hover:text-red-500 transition px-2 py-1 rounded-lg hover:bg-red-50 flex-shrink-0">
                    <i class="fas fa-trash"></i>
                </button>
            </div>
        `).join('');
    } catch(e) { console.error(e); }
}

async function createAnnouncement() {
    const title = document.getElementById('annTitle').value.trim();
    const content = document.getElementById('annContent').value.trim();
    if(!title || !content) { alert('Vui lòng điền đủ Tiêu đề và Nội dung'); return; }
    
    try {
        const user = checkAuth();
        const res = await fetch(`${API_URL}/announcements`, {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ title, content })
        });
        if(res.ok) {
            document.getElementById('addAnnouncementModal').classList.add('hidden');
            document.getElementById('annTitle').value = '';
            document.getElementById('annContent').value = '';
            showToast('Đã tạo thông báo!', 'success');
            loadAnnouncements();
        } else showToast('Lỗi tạo thông báo', 'error');
    } catch(e) { console.error(e); }
}

async function deleteAnnouncement(annId) {
    if(!confirm('Xóa thông báo này?')) return;
    try {
        const user = checkAuth();
        const res = await fetch(`${API_URL}/announcements/${annId}`, { method: 'DELETE' });
        if(res.ok) {
            showToast('Đã xóa thông báo', 'success');
            loadAnnouncements();
        } else showToast('Lỗi xóa thông báo', 'error');
    } catch(e) { console.error(e); }
}

function escapeHtml(unsafe) {
    if(!unsafe) return "";
    return String(unsafe).replace(/[&<"'>]/g, function (m) {
        return {'&': '&amp;', '<': '&lt;', '>': '&gt;', '"': '&quot;', "'": '&#039;'}[m];
    });
}
