let currentCategory = '';
let currentSearch = '';

document.addEventListener('DOMContentLoaded', () => {
    checkAuth();
    loadCommunities();
    
    // Filter click handlers
    document.querySelectorAll('.filter-btn').forEach(btn => {
        btn.addEventListener('click', (e) => {
            document.querySelectorAll('.filter-btn').forEach(b => {
                b.classList.remove('active', 'bg-primary', 'text-white', 'border-transparent', 'shadow-sm');
                b.classList.add('bg-white', 'hover:bg-gray-50', 'border', 'border-gray-200', 'text-gray-600');
            });
            
            const target = e.currentTarget;
            target.classList.remove('bg-white', 'hover:bg-gray-50', 'border', 'border-gray-200', 'text-gray-600');
            target.classList.add('active', 'bg-primary', 'text-white', 'border-transparent', 'shadow-sm');
            
            currentCategory = target.dataset.category;
            loadCommunities();
        });
    });
    
    // Search input handler
    const searchInput = document.getElementById('searchGroupInput');
    let timeout = null;
    searchInput.addEventListener('keyup', (e) => {
        clearTimeout(timeout);
        timeout = setTimeout(() => {
            currentSearch = e.target.value.trim();
            loadCommunities();
        }, 500);
    });
});

async function loadCommunities() {
    try {
        let url = `/api/group/search?name=${encodeURIComponent(currentSearch)}`;
        if (currentCategory) {
            url += `&category=${encodeURIComponent(currentCategory)}`;
        }
        
        const res = await apiGet(url);
        renderCommunities(res);
    } catch (err) {
        console.error('Lỗi tải danh sách cộng đồng:', err);
    }
}

function renderCommunities(groups) {
    const grid = document.getElementById('communityGrid');
    const emptyState = document.getElementById('emptyState');
    
    grid.innerHTML = '';
    
    if (!groups || groups.length === 0) {
        grid.classList.add('hidden');
        emptyState.classList.remove('hidden');
        emptyState.style.display = 'flex';
        return;
    }
    
    grid.classList.remove('hidden');
    emptyState.classList.add('hidden');
    emptyState.style.display = 'none';
    
    groups.forEach(group => {
        const isRequiresApproval = group.privacy === 'REQUIRES_APPROVAL';
        const privacyBadge = isRequiresApproval ? 
            `<span class="flex items-center gap-1 bg-yellow-50 text-yellow-600 border border-yellow-100 text-[10px] font-semibold px-2 py-0.5 rounded-full whitespace-nowrap">
                <i class="fas fa-lock text-[9px]"></i> Cần duyệt
             </span>` : 
            `<span class="flex items-center gap-1 bg-green-50 text-green-600 border border-green-100 text-[10px] font-semibold px-2 py-0.5 rounded-full whitespace-nowrap">
                <i class="fas fa-lock-open text-[9px]"></i> Công khai
             </span>`;
            
        let actionBtnHtml = '';
        if (group.isMember) {
            actionBtnHtml = `
                <button onclick="enterGroup(${group.id}, '${escapeJsString(group.name)}')" 
                    class="border border-gray-200 hover:bg-gray-50 text-gray-600 font-bold text-xs px-4 py-2 rounded-xl transition flex-shrink-0">
                    Vào nhóm
                </button>`;
        } else if (group.isPending) {
            actionBtnHtml = `
                <button disabled 
                    class="bg-yellow-50 text-yellow-600 border border-yellow-100 font-bold text-xs px-4 py-2 rounded-xl flex-shrink-0 cursor-not-allowed">
                    Đang duyệt
                </button>`;
        } else {
            actionBtnHtml = `
                <button onclick="joinGroup(${group.id}, this)" 
                    class="bg-primary text-white hover:bg-primary-dark font-bold text-xs px-5 py-2 rounded-xl transition flex-shrink-0 shadow-sm">
                    Tham gia
                </button>`;
        }
            
        const card = document.createElement('div');
        card.className = 'bg-white rounded-3xl border border-gray-100 p-6 shadow-sm hover:shadow-md transition duration-200 flex flex-col h-full relative';
        card.innerHTML = `
            <div class="flex gap-4 items-start mb-4">
                <div class="w-12 h-12 bg-red-50 text-primary rounded-full flex items-center justify-center flex-shrink-0">
                    <i class="fas fa-users text-lg"></i>
                </div>
                <div class="flex-1 min-w-0">
                    <h3 class="font-bold text-base text-gray-900 mb-1.5 truncate" title="${escapeHtml(group.name)}">${escapeHtml(group.name)}</h3>
                    <div class="flex flex-wrap gap-1.5 items-center">
                        <span class="flex items-center gap-1 bg-gray-50 border border-gray-100/50 text-gray-500 text-[10px] font-semibold px-2 py-0.5 rounded-full">
                            <i class="fas fa-user-friends text-[9px]"></i> ${group.memberCount}
                        </span>
                        ${privacyBadge}
                    </div>
                </div>
            </div>
            
            <p class="text-xs text-gray-500 leading-relaxed mb-5 line-clamp-2 flex-1">${escapeHtml(group.description) || 'Chưa có mô tả'}</p>
            
            <div class="flex justify-between items-center mt-auto pt-2.5 border-t border-gray-50">
                <span class="bg-red-50 text-primary text-[9px] font-bold tracking-wider uppercase px-2.5 py-1 rounded-md max-w-[110px] truncate" title="${escapeHtml(group.category || 'Khác')}">
                    ${escapeHtml(group.category || 'Khác')}
                </span>
                ${actionBtnHtml}
            </div>
        `;
        grid.appendChild(card);
    });
}

function enterGroup(groupId, groupName) {
    sessionStorage.setItem('currentConversationId', groupId);
    sessionStorage.setItem('currentChatFullName', groupName);
    sessionStorage.removeItem('currentChatUser');
    window.location.href = 'chat.html';
}

async function joinGroup(groupId, btn) {
    if (btn) btn.disabled = true;
    try {
        const res = await apiPost(`/api/group/${groupId}/join`, {});
        if (res.status === 'joined') {
            alert('Bạn đã tham gia nhóm thành công! Có thể vào Tin nhắn để xem.');
        } else if (res.status === 'requested') {
            alert('Yêu cầu tham gia đã được gửi tới Quản trị viên nhóm.');
        }
        loadCommunities();
    } catch (err) {
        if (err.message) {
            alert(err.message);
        } else {
            alert('Có lỗi xảy ra hoặc bạn đã tham gia nhóm này rồi.');
        }
    } finally {
        if (btn) btn.disabled = false;
    }
}

function escapeHtml(unsafe) {
    if (!unsafe) return "";
    return String(unsafe)
         .replace(/&/g, "&amp;")
         .replace(/</g, "&lt;")
         .replace(/>/g, "&gt;")
         .replace(/"/g, "&quot;")
         .replace(/'/g, "&#039;");
}

function escapeJsString(str) {
    if (!str) return "";
    return str.replace(/'/g, "\\'").replace(/"/g, '\\"');
}
