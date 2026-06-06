var API_URL = window.location.origin + '/api';

function debounce(func, wait) {
    let timeout;
    return function(...args) {
        clearTimeout(timeout);
        timeout = setTimeout(() => func.apply(this, args), wait);
    };
}

document.addEventListener('DOMContentLoaded', () => {
    const user = checkAuth();
    if (user.role !== 'ROLE_ADMIN') {
        alert('Bạn không có quyền truy cập trang này');
        window.location.href = 'home.html';
        return;
    }
    
    // Set admin display name
    var adminNameEl = document.getElementById('adminName');
    if (adminNameEl) {
        adminNameEl.textContent = 'Admin PTIT';
    }

    // Set current date string
    var currentDateTimeEl = document.getElementById('currentDateTime');
    if (currentDateTimeEl) {
        var options = { weekday: 'long', year: 'numeric', month: 'long', day: 'numeric' };
        currentDateTimeEl.textContent = new Date().toLocaleDateString('vi-VN', options);
    }

    // Cập nhật lời chào mừng động theo thời gian thực tế
    var adminGreetingEl = document.getElementById('adminGreeting');
    if (adminGreetingEl) {
        var hour = new Date().getHours();
        var greeting = 'Chào buổi tối';
        if (hour >= 5 && hour < 11) {
            greeting = 'Chào buổi sáng';
        } else if (hour >= 11 && hour < 18) {
            greeting = 'Chào buổi trưa';
        }
        adminGreetingEl.textContent = `${greeting}, Admin PTIT! 👋`;
    }

    // Wire up search inputs with debounce
    const userSearchInput = document.getElementById('userSearchInput');
    if (userSearchInput) {
        userSearchInput.addEventListener('input', debounce(() => {
            loadUsers();
        }, 300));
    }

    const postSearchInput = document.getElementById('postSearchInput');
    if (postSearchInput) {
        postSearchInput.addEventListener('input', debounce(() => {
            loadPosts();
        }, 300));
    }

    const announcementSearchInput = document.getElementById('announcementSearchInput');
    if (announcementSearchInput) {
        announcementSearchInput.addEventListener('input', debounce(() => {
            loadAnnouncements();
        }, 300));
    }

    loadDashboardStats();
    loadUsers();
    loadPosts();
    loadAnnouncements();
    checkAiService();
});

var currentTab = 'all';
var myChart = null;
var currentChartType = 'posts';
var chartDataObj = null;

function showToast(message, type) {
    var toast = document.getElementById('toast');
    if (!toast) return;
    var icon = document.getElementById('toastIcon');
    var iconBg = document.getElementById('toastIconBg');
    var msg = document.getElementById('toastMessage');
    
    msg.textContent = message;
    
    if (type === 'success') {
        icon.className = 'fas fa-check text-emerald-600';
        iconBg.className = 'w-8 h-8 rounded-full flex items-center justify-center bg-emerald-50';
    } else if (type === 'error') {
        icon.className = 'fas fa-exclamation-triangle text-red-600';
        iconBg.className = 'w-8 h-8 rounded-full flex items-center justify-center bg-red-50';
    } else {
        icon.className = 'fas fa-info text-blue-600';
        iconBg.className = 'w-8 h-8 rounded-full flex items-center justify-center bg-blue-50';
    }
    
    toast.classList.remove('hidden');
    setTimeout(function() { toast.classList.add('hidden'); }, 3000);
}

function changeView(view) {
    // Hide all views
    document.querySelectorAll('.view-section').forEach(el => el.classList.add('hidden'));
    
    // Show selected view
    var targetSec = document.getElementById('view-' + view);
    if (targetSec) targetSec.classList.remove('hidden');

    // Update active nav item class
    document.querySelectorAll('.nav-item').forEach(el => el.classList.remove('active'));
    var activeNav = document.getElementById('nav-' + view);
    if (activeNav) activeNav.classList.add('active');

    // Update title
    var viewTitles = {
        'dashboard': 'Dashboard',
        'users': 'Quản lý Người dùng',
        'posts': 'Quản lý Bài viết',
        'announcements': 'Quản lý Thông báo'
    };
    var titleEl = document.getElementById('currentViewTitle');
    if (titleEl && viewTitles[view]) {
        titleEl.textContent = viewTitles[view];
    }
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
            
            // Statistics counters
            document.getElementById('statUsers').textContent = data.usersCount ?? '-';
            document.getElementById('statPosts').textContent = data.postsCount ?? '-';
            document.getElementById('statMessages').textContent = data.messagesCount ?? '-';
            document.getElementById('statLockedUsers').textContent = data.lockedUsersCount ?? '0';
            document.getElementById('statPending').textContent = data.pendingCount ?? '-';
            document.getElementById('statRejected').textContent = data.rejectedCount ?? '-';
            
            // Post view counts
            document.getElementById('tabAllCount').textContent = data.postsCount ?? 0;
            document.getElementById('tabPendingCount').textContent = data.pendingCount ?? 0;
            document.getElementById('tabRejectedCount').textContent = data.rejectedCount ?? 0;
            
            // Shortcuts badges
            document.getElementById('shortcutLockedCount').textContent = (data.lockedUsersCount ?? 0) + ' bị khóa';
            document.getElementById('shortcutPendingCount').textContent = (data.pendingCount ?? 0) + ' chờ';
            
            updateModeUI(data.moderationMode);
            
            // Render line chart
            chartDataObj = data.chartData;
            if (chartDataObj) {
                renderChart();
            }
            
            // Render top tables
            renderTopPosts(data.topPosts);
            renderTopUsers(data.topUsers);
        }
    } catch(e) { console.error(e); }
}

function renderChart() {
    if (!chartDataObj) return;
    
    var labels = chartDataObj.labels || [];
    var data = [];
    var labelName = '';
    var color = '';
    var fillColor = '';
    
    if (currentChartType === 'posts') {
        data = chartDataObj.posts || [];
        labelName = 'Bài viết mới';
        color = '#ef4444';
        fillColor = 'rgba(239, 68, 68, 0.05)';
    } else if (currentChartType === 'messages') {
        data = chartDataObj.messages || [];
        labelName = 'Tin nhắn mới';
        color = '#10b981';
        fillColor = 'rgba(16, 185, 129, 0.05)';
    } else {
        data = chartDataObj.users || [];
        labelName = 'Người dùng mới';
        color = '#3b82f6';
        fillColor = 'rgba(59, 130, 246, 0.05)';
    }
    
    var ctx = document.getElementById('activityChart');
    if (!ctx) return;
    
    if (myChart) {
        myChart.destroy();
    }
    
    myChart = new Chart(ctx.getContext('2d'), {
        type: 'line',
        data: {
            labels: labels,
            datasets: [{
                label: labelName,
                data: data,
                borderColor: color,
                backgroundColor: fillColor,
                borderWidth: 2.5,
                fill: true,
                tension: 0.35,
                pointRadius: 4,
                pointHoverRadius: 6,
                pointBackgroundColor: color,
                pointBorderColor: '#ffffff',
                pointBorderWidth: 1.5
            }]
        },
        options: {
            responsive: true,
            maintainAspectRatio: false,
            plugins: {
                legend: { display: false },
                tooltip: {
                    backgroundColor: '#1e293b',
                    titleFont: { size: 12, weight: 'bold' },
                    bodyFont: { size: 12 },
                    padding: 10,
                    cornerRadius: 8,
                    displayColors: false
                }
            },
            scales: {
                y: {
                    grid: { color: 'rgba(0, 0, 0, 0.03)' },
                    ticks: { font: { size: 11 } }
                },
                x: {
                    grid: { display: false },
                    ticks: { font: { size: 11 } }
                }
            }
        }
    });
}

function updateChartDataset(type) {
    currentChartType = type;
    document.querySelectorAll('.chart-tab').forEach(b => b.classList.remove('active'));
    var activeBtn = document.getElementById('chart-tab-' + type);
    if (activeBtn) {
        activeBtn.classList.add('active');
    }
    renderChart();
}

function renderTopPosts(posts) {
    const tbody = document.getElementById('topPostsTableBody');
    if (!posts || posts.length === 0) {
        tbody.innerHTML = `<tr><td colspan="3" class="text-center text-gray-400 py-6">Không có dữ liệu bài viết nổi bật.</td></tr>`;
        return;
    }
    
    tbody.innerHTML = posts.map(p => `
        <tr class="hover:bg-gray-50/50">
            <td class="py-3 font-semibold text-gray-900">${escapeHtml(p.authorName)}</td>
            <td class="py-3 text-gray-600 max-w-[220px] truncate">${escapeHtml(p.content)}</td>
            <td class="py-3 text-right font-bold text-primary">${p.interactions} 🔥</td>
        </tr>
    `).join('');
}

function renderTopUsers(users) {
    const tbody = document.getElementById('topUsersTableBody');
    if (!users || users.length === 0) {
        tbody.innerHTML = `<tr><td colspan="3" class="text-center text-gray-400 py-6">Không có dữ liệu người dùng hoạt động.</td></tr>`;
        return;
    }
    
    tbody.innerHTML = users.map(u => `
        <tr class="hover:bg-gray-50/50">
            <td class="py-3">
                <div class="flex items-center gap-2">
                    <div class="w-6 h-6 bg-red-100 text-primary rounded-full flex items-center justify-center text-[10px] font-bold">
                        ${(u.fullName || u.username || '?').charAt(0).toUpperCase()}
                    </div>
                    <span class="font-semibold text-gray-900">${escapeHtml(u.fullName || u.username)}</span>
                </div>
            </td>
            <td class="py-3 text-center text-xs text-gray-500 font-medium">
                ${u.postsCount} bài viết / ${u.commentsCount} bình luận
            </td>
            <td class="py-3 text-right font-bold text-emerald-600">${u.score} pts</td>
        </tr>
    `).join('');
}

async function loadUsers() {
    try {
        const searchInput = document.getElementById('userSearchInput');
        const search = searchInput ? searchInput.value.trim() : '';
        const url = search ? `${API_URL}/admin/users?search=${encodeURIComponent(search)}` : `${API_URL}/admin/users`;
        const res = await fetch(url);
        const users = await res.json();
        const tbody = document.getElementById('usersTableBody');
        tbody.innerHTML = users.map((u, index) => {
            let statusBadge = '';
            let lockBtn = '';
            
            if (u.locked) {
                statusBadge = `<span class="status-badge bg-rose-50 text-rose-600"><i class="fas fa-user-slash mr-1"></i>Bị khóa</span>`;
                lockBtn = `<button onclick="toggleLockUser(${u.id})" class="text-xs text-emerald-500 hover:text-emerald-700 transition px-2.5 py-1.5 rounded-lg hover:bg-emerald-50 font-semibold"><i class="fas fa-unlock"></i> Mở khóa</button>`;
            } else {
                statusBadge = `<span class="status-badge bg-emerald-50 text-emerald-600"><i class="fas fa-check-circle mr-1"></i>Hoạt động</span>`;
                lockBtn = `<button onclick="toggleLockUser(${u.id})" class="text-xs text-amber-500 hover:text-amber-700 transition px-2.5 py-1.5 rounded-lg hover:bg-amber-50 font-semibold"><i class="fas fa-lock"></i> Khóa</button>`;
            }
            
            let deleteBtn = '';
            if (u.role !== 'ROLE_ADMIN') {
                deleteBtn = `<button onclick="deleteUser(${u.id}, this)" class="text-xs text-gray-400 hover:text-red-500 transition px-2.5 py-1.5 rounded-lg hover:bg-red-50 font-semibold"><i class="fas fa-trash"></i> Xóa</button>`;
            } else {
                deleteBtn = `<span class="text-xs text-gray-300 px-2.5 py-1.5 font-medium">—</span>`;
            }
            
            return `
            <tr class="hover:bg-gray-50/50">
                <td class="py-3.5 text-sm text-gray-500 font-medium">${index + 1}</td>
                <td class="py-3.5">
                     <div class="flex items-center gap-2">
                         <div class="w-8 h-8 bg-red-100 text-primary rounded-full flex items-center justify-center text-xs font-bold flex-shrink-0 shadow-sm">
                             ${(u.fullName || u.username || '?').charAt(0).toUpperCase()}
                         </div>
                         <span class="text-sm font-semibold text-gray-900">${escapeHtml(u.fullName || '-')}</span>
                     </div>
                </td>
                <td class="py-3.5 text-sm text-gray-600 font-mono">@${escapeHtml(u.username)}</td>
                <td class="py-3.5 text-sm text-gray-600 font-mono">${escapeHtml(u.email || '-')}</td>
                <td class="py-3.5">${statusBadge}</td>
                <td class="py-3.5">
                     <span class="text-xs font-bold px-2.5 py-1 rounded-full ${u.role === 'ROLE_ADMIN' ? 'bg-red-50 text-primary' : 'bg-gray-100 text-gray-500'}">
                         ${u.role === 'ROLE_ADMIN' ? 'Admin' : 'User'}
                     </span>
                </td>
                <td class="py-3.5 text-right"><div class="flex items-center justify-end gap-1.5">${lockBtn}${deleteBtn}</div></td>
            </tr>`;
        }).join('');
    } catch (e) { console.error(e); }
}

async function toggleLockUser(userId) {
    try {
        const res = await fetch(`${API_URL}/admin/users/${userId}/toggle-lock`, { method: 'POST' });
        if (res.ok) {
            const data = await res.json();
            var statusWord = data.locked ? 'Khóa' : 'Mở khóa';
            showToast(`Đã ${statusWord.toLowerCase()} tài khoản thành công!`, 'success');
            loadDashboardStats();
            loadUsers();
        } else {
            showToast('Lỗi khi cập nhật trạng thái tài khoản', 'error');
        }
    } catch(e) {
        console.error(e);
        showToast('Lỗi kết nối đến máy chủ', 'error');
    }
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
            showToast('Đã thêm tài khoản thành viên mới', 'success');
            loadDashboardStats();
            loadUsers();
        } else { showToast('Lỗi khi tạo người dùng', 'error'); }
    } catch (e) { showToast('Lỗi kết nối', 'error'); }
}

async function deleteUser(userId, btn) {
    if (!confirm('Xóa người dùng này? Thao tác này sẽ xóa mọi dữ liệu liên quan!')) return;
    try {
        await fetch(`${API_URL}/admin/users/${userId}`, { method: 'DELETE' });
        btn.closest('tr').remove();
        showToast('Đã xóa người dùng thành công', 'success');
        loadDashboardStats();
    } catch (e) { console.error(e); showToast('Lỗi xóa người dùng', 'error'); }
}

async function setModerationMode(mode) {
    try {
        const res = await fetch(`${API_URL}/admin/moderation/mode`, {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ mode: mode })
        });
        if(res.ok) {
            updateModeUI(mode);
            var modeNames = { 'NONE': 'Không kiểm duyệt', 'MANUAL': 'Kiểm duyệt thủ công', 'AUTO_AI': 'Tự động (AI)' };
            showToast('Đã chuyển sang: ' + modeNames[mode], 'success');
            loadDashboardStats();
        } else {
            const errText = await res.text();
            console.error('Lỗi đổi chế độ:', errText);
            showToast('Lỗi đổi chế độ kiểm duyệt', 'error');
        }
    } catch(e) { console.error(e); showToast('Lỗi kết nối', 'error'); }
}

async function checkAiService(triggerToast = false) {
    try {
        const res = await fetch(`${API_URL}/admin/moderation/ai-status`);
        const data = await res.json();
        var dot = document.getElementById('aiStatusDot');
        var text = document.getElementById('aiStatusText');
        if(dot && text) {
            if (data.available) {
                dot.className = 'ai-status-dot ai-online';
                text.className = 'text-emerald-600 font-semibold';
                text.textContent = 'AI Service: Đang hoạt động';
                if (triggerToast) {
                    showToast('AI Service đang hoạt động!', 'success');
                }
            } else {
                dot.className = 'ai-status-dot ai-offline';
                text.className = 'text-red-500 font-semibold';
                text.textContent = 'AI Service: Không khả dụng';
                if (triggerToast) {
                    showToast('AI Service không khả dụng! (' + (data.url || '') + ')', 'error');
                }
            }
        }
    } catch(e) {
        console.error(e);
        var dot = document.getElementById('aiStatusDot');
        var text = document.getElementById('aiStatusText');
        if(dot) dot.className = 'ai-status-dot ai-offline';
        if(text) { text.className = 'text-red-500 font-semibold'; text.textContent = 'AI Service: Lỗi kết nối'; }
        if (triggerToast) {
            showToast('Lỗi kiểm tra AI Service', 'error');
        }
    }
}

function switchTab(tab, btn) {
    currentTab = tab;
    document.querySelectorAll('#view-posts .tab-btn').forEach(function(b) { b.classList.remove('active'); });
    
    if (btn) {
        btn.classList.add('active');
    } else {
        var foundBtn = document.querySelector(`#view-posts button[onclick*="'${tab}'"]`) || 
                       document.querySelector(`#view-posts button[onclick*="${tab}"]`);
        if (foundBtn) foundBtn.classList.add('active');
    }

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
        const searchInput = document.getElementById('postSearchInput');
        const search = searchInput ? searchInput.value.trim() : '';
        const url = search ? `${API_URL}/admin/posts?search=${encodeURIComponent(search)}` : `${API_URL}/admin/posts`;
        const res = await fetch(url);
        const posts = await res.json();
        const tbody = document.getElementById('postsTableBody');
        
        if(!posts || posts.length === 0) {
            tbody.innerHTML = '<tr><td colspan="8" class="text-center text-gray-400 py-6">Không có bài viết nào.</td></tr>';
            return;
        }

        tbody.innerHTML = posts.map((p, index) => {
            let statusBadge = '';
            const st = p.status || 'APPROVED';
            if (st === 'PENDING') statusBadge = '<span class="status-badge status-pending"><i class="fas fa-hourglass-half mr-1"></i>Chờ duyệt</span>';
            else if (st === 'REJECTED') statusBadge = '<span class="status-badge status-rejected"><i class="fas fa-ban mr-1"></i>Từ chối</span>';
            else statusBadge = '<span class="status-badge status-approved"><i class="fas fa-check-circle mr-1"></i>Đã duyệt</span>';

            let aiLabelHtml = '<span class="text-gray-300 text-xs">—</span>';
            if (p.moderationLabel) {
                if (p.moderationLabel === 'CLEAN') aiLabelHtml = '<span class="text-emerald-600 font-semibold text-xs flex items-center gap-1"><i class="fas fa-leaf text-[10px]"></i>Clean</span>';
                else if (p.moderationLabel === 'OFFENSIVE') aiLabelHtml = '<span class="text-amber-500 font-semibold text-xs flex items-center gap-1"><i class="fas fa-exclamation-triangle text-[10px]"></i>Offensive</span>';
                else if (p.moderationLabel === 'HATE') aiLabelHtml = '<span class="text-rose-600 font-semibold text-xs flex items-center gap-1"><i class="fas fa-skull-crossbones text-[10px]"></i>Hate</span>';
                
                if (p.moderationConfidence) {
                    aiLabelHtml += `<span class="text-gray-400 text-[10px] ml-1 font-mono">${(p.moderationConfidence * 100).toFixed(0)}%</span>`;
                }
            }

            let actionsHtml = '';
            if (st === 'PENDING') {
                actionsHtml += `<button onclick="approvePost(${p.id})" class="text-[11px] text-emerald-500 hover:text-emerald-700 transition px-2.5 py-1.5 rounded-lg hover:bg-emerald-50 font-semibold flex items-center gap-1"><i class="fas fa-check"></i> Duyệt</button>`;
                actionsHtml += `<button onclick="rejectPost(${p.id})" class="text-[11px] text-rose-500 hover:text-rose-700 transition px-2.5 py-1.5 rounded-lg hover:bg-rose-50 font-semibold flex items-center gap-1"><i class="fas fa-times"></i> Hủy</button>`;
            } else if (st === 'REJECTED') {
                actionsHtml += `<button onclick="approvePost(${p.id})" class="text-[11px] text-emerald-500 hover:text-emerald-700 transition px-2.5 py-1.5 rounded-lg hover:bg-emerald-50 font-semibold flex items-center gap-1"><i class="fas fa-check"></i> Duyệt lại</button>`;
            }
            actionsHtml += `<button onclick="deletePost(${p.id}, this)" class="text-[11px] text-gray-400 hover:text-red-500 transition px-2.5 py-1.5 rounded-lg hover:bg-red-50 font-medium"><i class="fas fa-trash"></i> Xóa</button>`;

            const imageBadge = p.imageUrl ? `<div class="mt-1"><span class="inline-flex items-center gap-1 bg-red-50 text-red-500 text-[10px] font-bold px-2 py-0.5 rounded-full"><i class="far fa-image"></i> 1 ảnh</span></div>` : '';

            const interactionsHtml = `
                <div class="flex items-center gap-2.5 text-xs text-gray-500 font-medium">
                    <span class="flex items-center gap-1"><i class="fas fa-heart text-rose-500"></i> ${p.likeCount || 0}</span>
                    <span class="flex items-center gap-1"><i class="fas fa-comment text-blue-500"></i> ${(p.comments || []).length}</span>
                </div>
            `;

            return `
            <tr class="hover:bg-gray-50/50 post-row" data-status="${st}">
                <td class="py-3.5 text-sm text-gray-500 font-medium">${index + 1}</td>
                <td class="py-3.5">
                    <div class="flex items-center gap-2">
                        <span class="text-sm font-semibold text-gray-900">${escapeHtml(p.userFullName || p.username)}</span>
                    </div>
                </td>
                <td class="py-3.5 text-sm text-gray-600 max-w-xs">
                    <div class="truncate">${escapeHtml(p.content)}</div>
                    ${imageBadge}
                </td>
                <td class="py-3.5">${interactionsHtml}</td>
                <td class="py-3.5">${statusBadge}</td>
                <td class="py-3.5 text-sm">${aiLabelHtml}</td>
                <td class="py-3.5 text-sm text-gray-500">${formatDateDisplay(p.createdAt)}</td>
                <td class="py-3.5 text-right"><div class="flex items-center justify-end gap-1">${actionsHtml}</div></td>
            </tr>`;
        }).join('');
        
        // Re-apply tab filtering
        switchTab(currentTab, null);
    } catch(e) { console.error(e); }
}

async function approvePost(postId) {
    try {
        const res = await fetch(`${API_URL}/admin/posts/${postId}/approve`, { method: 'POST' });
        if(res.ok) {
            showToast('Đã phê duyệt bài viết #' + postId, 'success');
            loadDashboardStats();
            loadPosts();
        } else showToast('Lỗi khi phê duyệt bài viết', 'error');
    } catch(e) { console.error(e); }
}

async function rejectPost(postId) {
    if(!confirm('Từ chối bài viết #' + postId + '?')) return;
    try {
        const res = await fetch(`${API_URL}/admin/posts/${postId}/reject`, { method: 'POST' });
        if(res.ok) {
            showToast('Đã từ chối bài viết #' + postId, 'success');
            loadDashboardStats();
            loadPosts();
        } else showToast('Lỗi khi từ chối bài viết', 'error');
    } catch(e) { console.error(e); }
}

async function approveAllPending() {
    if(!confirm('Duyệt tất cả bài đang chờ duyệt?')) return;
    try {
        const res = await fetch(`${API_URL}/admin/posts/approve-all`, { method: 'POST' });
        if(res.ok) {
            const data = await res.json();
            showToast(`Đã phê duyệt tất cả ${data.count} bài viết!`, 'success');
            loadDashboardStats();
            loadPosts();
        } else showToast('Lỗi khi phê duyệt hàng loạt', 'error');
    } catch(e) { console.error(e); }
}

async function deletePost(postId, btn) {
    if(!confirm('Xóa bài viết này?')) return;
    try {
        const res = await fetch(`${API_URL}/admin/posts/${postId}`, { method: 'DELETE' });
        if (res.ok) {
            showToast('Đã xóa bài viết khỏi cơ sở dữ liệu', 'success');
            loadDashboardStats();
            loadPosts();
        } else showToast('Lỗi khi xóa bài viết', 'error');
    } catch(e) { console.error(e); }
}

async function loadAnnouncements() {
    try {
        const searchInput = document.getElementById('announcementSearchInput');
        const search = searchInput ? searchInput.value.trim() : '';
        const url = search ? `${API_URL}/announcements?search=${encodeURIComponent(search)}` : `${API_URL}/announcements`;
        const res = await fetch(url);
        const anns = await res.json();
        const tbody = document.getElementById('announcementsTableBody');
        if(!anns || anns.length === 0) {
            tbody.innerHTML = '<tr><td colspan="6" class="text-center text-gray-400 py-6">Chưa có thông báo nào.</td></tr>';
            return;
        }

        tbody.innerHTML = anns.map((ann, index) => `
            <tr class="hover:bg-gray-50/50">
                <td class="py-3.5 text-sm text-gray-500 font-medium">${index + 1}</td>
                <td class="py-3.5 font-semibold text-gray-900">${escapeHtml(ann.title)}</td>
                <td class="py-3.5 text-sm text-gray-600 max-w-md truncate" title="${escapeHtml(ann.content)}">${escapeHtml(ann.content)}</td>
                <td class="py-3.5 text-sm text-gray-500">${escapeHtml(ann.adminName || 'Admin')}</td>
                <td class="py-3.5 text-sm text-gray-500">${formatDateDisplay(ann.createdAt)}</td>
                <td class="py-3.5 text-right">
                    <button onclick="deleteAnnouncement(${ann.id})" class="text-xs text-gray-400 hover:text-red-500 transition px-2.5 py-1.5 rounded-lg hover:bg-red-50 font-semibold">
                        <i class="fas fa-trash"></i> Xóa
                    </button>
                </td>
            </tr>
        `).join('');
    } catch(e) { console.error(e); }
}

async function createAnnouncement() {
    const title = document.getElementById('annTitle').value.trim();
    const content = document.getElementById('annContent').value.trim();
    if(!title || !content) { alert('Vui lòng điền đủ Tiêu đề và Nội dung'); return; }
    
    try {
        const res = await fetch(`${API_URL}/announcements`, {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ title, content })
        });
        if(res.ok) {
            document.getElementById('addAnnouncementModal').classList.add('hidden');
            document.getElementById('annTitle').value = '';
            document.getElementById('annContent').value = '';
            showToast('Đã đăng tải thông báo thành công!', 'success');
            loadAnnouncements();
        } else showToast('Lỗi khi tạo thông báo', 'error');
    } catch(e) { console.error(e); }
}

async function deleteAnnouncement(annId) {
    if(!confirm('Xóa thông báo này?')) return;
    try {
        const res = await fetch(`${API_URL}/announcements/${annId}`, { method: 'DELETE' });
        if (res.ok) {
            showToast('Đã gỡ bỏ thông báo thành công', 'success');
            loadAnnouncements();
        } else showToast('Lỗi khi xóa thông báo', 'error');
    } catch(e) { console.error(e); }
}

function escapeHtml(unsafe) {
    if(!unsafe) return "";
    return String(unsafe).replace(/[&<"'>]/g, function (m) {
        return {'&': '&amp;', '<': '&lt;', '>': '&gt;', '"': '&quot;', "'": '&#039;'}[m];
    });
}
