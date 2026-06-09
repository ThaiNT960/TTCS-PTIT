var API_URL = window.location.origin + '/api';

function debounce(func, wait) {
    let timeout;
    return function (...args) {
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
    setTimeout(function () { toast.classList.add('hidden'); }, 3000);
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
    document.querySelectorAll('.mode-card').forEach(function (card) {
        card.classList.remove('active');
        card.querySelector('.mode-icon').classList.remove('bg-primary', 'text-white');
        card.querySelector('.mode-icon').classList.add('bg-gray-200', 'text-gray-500');
    });
    var activeCard = document.getElementById('mode-' + mode);
    if (activeCard) {
        activeCard.classList.add('active');
        activeCard.querySelector('.mode-icon').classList.remove('bg-gray-200', 'text-gray-500');
        activeCard.querySelector('.mode-icon').classList.add('bg-primary', 'text-white');
    }
}

async function loadDashboardStats() {
    try {
        const data = await apiGet(`${API_URL}/admin/dashboard`);

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
    } catch (e) { console.error(e); }
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
        const users = await apiGet(url);
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
        const data = await apiPost(`${API_URL}/admin/users/${userId}/toggle-lock`);
        var statusWord = data.locked ? 'Khóa' : 'Mở khóa';
        showToast(`Đã ${statusWord.toLowerCase()} tài khoản thành công!`, 'success');
        loadDashboardStats();
        loadUsers();
    } catch (e) {
        console.error(e);
        showToast(e.message || 'Lỗi kết nối đến máy chủ', 'error');
    }
}

async function createUser() {
    const email = document.getElementById('newEmail').value.trim();
    const fullName = document.getElementById('newFullName').value.trim();
    const password = document.getElementById('newPassword').value.trim();
    if (!email || !fullName || !password) { alert('Vui lòng điền đầy đủ'); return; }

    // Kiểm tra định dạng email cơ bản
    const emailRegex = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;
    if (!emailRegex.test(email)) {
        alert('Email không đúng định dạng!');
        return;
    }

    try {
        await apiPost(`${API_URL}/admin/users`, { email, fullName, password });
        document.getElementById('addUserModal').classList.add('hidden');
        document.getElementById('newEmail').value = '';
        document.getElementById('newFullName').value = '';
        document.getElementById('newPassword').value = '';
        // Reset icon con mắt về mặc định ẩn
        const pwdInput = document.getElementById('newPassword');
        const icon = document.getElementById('toggle-icon-newPassword');
        if (pwdInput && icon) {
            pwdInput.type = 'password';
            icon.className = 'far fa-eye-slash';
        }
        showToast('Đã thêm tài khoản thành viên mới', 'success');
        loadDashboardStats();
        loadUsers();
    } catch (e) { showToast(e.message || 'Lỗi kết nối', 'error'); }
}

async function deleteUser(userId, btn) {
    if (!confirm('Xóa người dùng này? Thao tác này sẽ xóa mọi dữ liệu liên quan!')) return;
    try {
        await apiDelete(`${API_URL}/admin/users/${userId}`);
        btn.closest('tr').remove();
        showToast('Đã xóa người dùng thành công', 'success');
        loadDashboardStats();
    } catch (e) { console.error(e); showToast(e.message || 'Lỗi xóa người dùng', 'error'); }
}

async function setModerationMode(mode) {
    try {
        await apiPost(`${API_URL}/admin/moderation/mode`, { mode: mode });
        updateModeUI(mode);
        var modeNames = { 'NONE': 'Không kiểm duyệt', 'MANUAL': 'Kiểm duyệt thủ công', 'AUTO_AI': 'Tự động (AI)' };
        showToast('Đã chuyển sang: ' + modeNames[mode], 'success');
        loadDashboardStats();
    } catch (e) { console.error(e); showToast(e.message || 'Lỗi kết nối', 'error'); }
}

async function checkAiService(triggerToast = false) {
    try {
        const data = await apiGet(`${API_URL}/admin/moderation/ai-status`);
        var dot = document.getElementById('aiStatusDot');
        var text = document.getElementById('aiStatusText');
        if (dot && text) {
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
    } catch (e) {
        console.error(e);
        var dot = document.getElementById('aiStatusDot');
        var text = document.getElementById('aiStatusText');
        if (dot) dot.className = 'ai-status-dot ai-offline';
        if (text) { text.className = 'text-red-500 font-semibold'; text.textContent = 'AI Service: Lỗi kết nối'; }
        if (triggerToast) {
            showToast(e.message || 'Lỗi kiểm tra AI Service', 'error');
        }
    }
}

function switchTab(tab, btn) {
    currentTab = tab;
    document.querySelectorAll('#view-posts .tab-btn').forEach(function (b) { b.classList.remove('active'); });

    if (btn) {
        btn.classList.add('active');
    } else {
        var foundBtn = document.querySelector(`#view-posts button[onclick*="'${tab}'"]`) ||
            document.querySelector(`#view-posts button[onclick*="${tab}"]`);
        if (foundBtn) foundBtn.classList.add('active');
    }

    var bulkActions = document.getElementById('bulkActions');
    if (bulkActions) {
        bulkActions.style.display = tab === 'pending' ? 'flex' : 'none';
    }

    document.querySelectorAll('.post-row').forEach(function (row) {
        if (tab === 'all') {
            row.style.display = '';
        } else {
            row.style.display = row.dataset.status === tab.toUpperCase() ? '' : 'none';
        }
    });
}

function formatDateDisplay(dateStr) {
    if (!dateStr) return '';
    try {
        return new Date(dateStr).toLocaleString('vi-VN');
    } catch (e) { return dateStr; }
}

async function loadPosts() {
    try {
        const searchInput = document.getElementById('postSearchInput');
        const search = searchInput ? searchInput.value.trim() : '';
        const url = search ? `${API_URL}/admin/posts?search=${encodeURIComponent(search)}` : `${API_URL}/admin/posts`;
        const posts = await apiGet(url);
        const tbody = document.getElementById('postsTableBody');

        if (!posts || posts.length === 0) {
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
                let icon = '';
                let colorClass = '';
                let labelText = '';
                if (p.moderationLabel === 'CLEAN') {
                    icon = 'fa-leaf';
                    colorClass = 'text-emerald-600';
                    labelText = 'Clean';
                } else if (p.moderationLabel === 'OFFENSIVE') {
                    icon = 'fa-exclamation-triangle';
                    colorClass = 'text-amber-500';
                    labelText = 'Offensive';
                } else if (p.moderationLabel === 'HATE') {
                    icon = 'fa-skull-crossbones';
                    colorClass = 'text-rose-600';
                    labelText = 'Hate';
                }

                const confidenceHtml = p.moderationConfidence
                    ? `<span class="text-gray-400 text-[10px] ml-1 font-mono">${(p.moderationConfidence * 100).toFixed(0)}%</span>`
                    : '';

                aiLabelHtml = `<div class="flex items-center justify-center gap-1.5 text-xs font-semibold ${colorClass}">
                    <i class="fas ${icon} text-[10px]"></i>
                    <span>${labelText}</span>
                    ${confidenceHtml}
                </div>`;
            }

            let actionsHtml = '';
            if (st === 'PENDING') {
                actionsHtml += `<button onclick="approvePost(${p.id})" class="text-[11px] text-emerald-500 hover:text-emerald-700 transition px-2.5 py-1.5 rounded-lg hover:bg-emerald-50 font-semibold flex items-center gap-1"><i class="fas fa-check"></i> Duyệt</button>`;
                actionsHtml += `<button onclick="rejectPost(${p.id})" class="text-[11px] text-rose-500 hover:text-rose-700 transition px-2.5 py-1.5 rounded-lg hover:bg-rose-50 font-semibold flex items-center gap-1"><i class="fas fa-times"></i> Hủy</button>`;
            } else if (st === 'REJECTED') {
                actionsHtml += `<button onclick="approvePost(${p.id})" class="text-[11px] text-emerald-500 hover:text-emerald-700 transition px-2.5 py-1.5 rounded-lg hover:bg-emerald-50 font-semibold flex items-center gap-1"><i class="fas fa-check"></i> Duyệt lại</button>`;
            }
            actionsHtml += `<button onclick="deletePost(${p.id}, this)" class="text-[11px] text-gray-400 hover:text-red-500 transition px-2.5 py-1.5 rounded-lg hover:bg-red-50 font-medium"><i class="fas fa-trash"></i> Xóa</button>`;

            const imageBadge = p.imageUrl ? `
                <div class="mt-1">
                    <a href="${p.imageUrl}" target="_blank" class="inline-flex items-center gap-1 bg-red-50 hover:bg-red-100 text-red-500 text-[10px] font-bold px-2 py-0.5 rounded-full transition cursor-pointer" title="Bấm để xem ảnh gốc">
                        <i class="far fa-image"></i> 1 ảnh
                    </a>
                </div>
            ` : '';

            const interactionsHtml = `
                <div class="flex items-center justify-center gap-2 text-[11px] font-bold">
                    <span class="inline-flex items-center gap-1.5 bg-rose-50 text-rose-600 px-2.5 py-1 rounded-lg">
                        <i class="fas fa-heart text-rose-500 text-xs"></i> ${p.likeCount || 0}
                    </span>
                    <span class="inline-flex items-center gap-1.5 bg-rose-50 text-rose-600 px-2.5 py-1 rounded-lg">
                        <i class="far fa-comment text-rose-500 text-xs"></i> ${(p.comments || []).length}
                    </span>
                </div>
            `;

            const isLongText = p.content && p.content.length > 50;
            const contentHtml = isLongText
                ? `<div class="truncate cursor-pointer hover:text-gray-900 transition duration-150" onclick="this.classList.toggle('truncate'); this.classList.toggle('whitespace-normal'); this.classList.toggle('break-words');" title="Bấm để xem đầy đủ / thu gọn">${escapeHtml(p.content)}</div>`
                : `<div>${escapeHtml(p.content)}</div>`;

            return `
            <tr class="hover:bg-gray-50/50 post-row" data-status="${st}">
                <td class="py-3.5 text-sm text-gray-500 font-medium">${index + 1}</td>
                <td class="py-3.5 pl-3">
                    <span class="text-sm font-semibold text-gray-900 truncate block max-w-[75px]" title="${escapeHtml(p.userFullName || p.username)}">${escapeHtml(p.userFullName || p.username)}</span>
                </td>
                <td class="py-3.5 text-sm text-gray-600 max-w-xs">
                    ${contentHtml}
                    ${imageBadge}
                </td>
                <td class="py-3.5">${interactionsHtml}</td>
                <td class="py-3.5 text-center">${statusBadge}</td>
                <td class="py-3.5 text-sm text-center">${aiLabelHtml}</td>
                <td class="py-3.5 text-sm text-gray-500 text-center">${formatDateDisplay(p.createdAt)}</td>
                <td class="py-3.5 text-center"><div class="flex items-center justify-center gap-1">${actionsHtml}</div></td>
            </tr>`;
        }).join('');

        // Re-apply tab filtering
        switchTab(currentTab, null);
    } catch (e) { console.error(e); }
}

async function approvePost(postId) {
    try {
        await apiPost(`${API_URL}/admin/posts/${postId}/approve`);
        showToast('Đã phê duyệt bài viết #' + postId, 'success');
        loadDashboardStats();
        loadPosts();
    } catch (e) { console.error(e); showToast(e.message || 'Lỗi khi phê duyệt bài viết', 'error'); }
}

async function rejectPost(postId) {
    if (!confirm('Từ chối bài viết #' + postId + '?')) return;
    try {
        await apiPost(`${API_URL}/admin/posts/${postId}/reject`);
        showToast('Đã từ chối bài viết #' + postId, 'success');
        loadDashboardStats();
        loadPosts();
    } catch (e) { console.error(e); showToast(e.message || 'Lỗi khi từ chối bài viết', 'error'); }
}

async function approveAllPending() {
    if (!confirm('Duyệt tất cả bài đang chờ duyệt?')) return;
    try {
        const data = await apiPost(`${API_URL}/admin/posts/approve-all`);
        showToast(`Đã phê duyệt tất cả ${data.count} bài viết!`, 'success');
        loadDashboardStats();
        loadPosts();
    } catch (e) { console.error(e); showToast(e.message || 'Lỗi khi phê duyệt hàng loạt', 'error'); }
}

async function deletePost(postId, btn) {
    if (!confirm('Xóa bài viết này?')) return;
    try {
        await apiDelete(`${API_URL}/admin/posts/${postId}`);
        showToast('Đã xóa bài viết khỏi cơ sở dữ liệu', 'success');
        loadDashboardStats();
        loadPosts();
    } catch (e) { console.error(e); showToast(e.message || 'Lỗi khi xóa bài viết', 'error'); }
}

async function loadAnnouncements() {
    try {
        const searchInput = document.getElementById('announcementSearchInput');
        const search = searchInput ? searchInput.value.trim() : '';
        const url = search ? `${API_URL}/announcements?search=${encodeURIComponent(search)}` : `${API_URL}/announcements`;
        const anns = await apiGet(url);
        const tbody = document.getElementById('announcementsTableBody');
        if (!anns || anns.length === 0) {
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
    } catch (e) { console.error(e); }
}

async function createAnnouncement() {
    const title = document.getElementById('annTitle').value.trim();
    const content = document.getElementById('annContent').value.trim();
    if (!title || !content) { alert('Vui lòng điền đủ Tiêu đề và Nội dung'); return; }

    try {
        await apiPost(`${API_URL}/admin/announcements`, { title, content });
        document.getElementById('addAnnouncementModal').classList.add('hidden');
        document.getElementById('annTitle').value = '';
        document.getElementById('annContent').value = '';
        showToast('Đã đăng tải thông báo thành công!', 'success');
        loadAnnouncements();
    } catch (e) { console.error(e); showToast(e.message || 'Lỗi khi tạo thông báo', 'error'); }
}

async function deleteAnnouncement(annId) {
    if (!confirm('Xóa thông báo này?')) return;
    try {
        await apiDelete(`${API_URL}/admin/announcements/${annId}`);
        showToast('Đã gỡ bỏ thông báo thành công', 'success');
        loadAnnouncements();
    } catch (e) { console.error(e); showToast(e.message || 'Lỗi khi xóa thông báo', 'error'); }
}

function escapeHtml(unsafe) {
    if (!unsafe) return "";
    return String(unsafe).replace(/[&<"'>]/g, function (m) {
        return { '&': '&amp;', '<': '&lt;', '>': '&gt;', '"': '&quot;', "'": '&#039;' }[m];
    });
}

function togglePasswordVisibility(id) {
    const input = document.getElementById(id);
    const icon = document.getElementById('toggle-icon-' + id);
    if (input && icon) {
        if (input.type === 'password') {
            input.type = 'text';
            icon.classList.remove('fa-eye-slash');
            icon.classList.add('fa-eye');
        } else {
            input.type = 'password';
            icon.classList.remove('fa-eye');
            icon.classList.add('fa-eye-slash');
        }
    }
}
