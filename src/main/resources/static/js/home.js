var API_URL = 'http://localhost:8080/api';
let selectedFiles = []; // Phạm vi toàn cục để dùng chung

document.addEventListener('DOMContentLoaded', () => {
    const user = checkAuth();

    // Set navbar avatar
    const navAvatar = document.getElementById('navAvatar');
    if (navAvatar && (user.fullName || user.username)) {
        const initial = (user.fullName || user.username).charAt(0).toUpperCase();
        if (user.avatar) {
            navAvatar.innerHTML = `<img src="${user.avatar}" class="w-full h-full object-cover rounded-full">`;
        } else {
            navAvatar.textContent = initial;
        }
    }

    // Show admin link
    if (user.role === 'ROLE_ADMIN') {
        const link = document.getElementById('adminNavLink');
        if (link) link.style.display = 'inline';
    }

    loadPosts(user);
    setupPostForm(user);
    
    // Notifications
    loadNotifications(user);
    setInterval(() => loadNotifications(user), 30000); 

    // Handle multi-image selection & preview
    const imageFileInput = document.getElementById('postImageFile');
    if (imageFileInput) {
        imageFileInput.addEventListener('change', (e) => {
            if (e.target.files.length > 0) {
                // Thêm file mới vào mảng thay vì ghi đè (tùy chọn, ở đây là ghi đè để giống FB)
                selectedFiles = Array.from(e.target.files);
                renderPreviews();
            }
        });
    }
});

function renderPreviews() {
    const container = document.getElementById('imagePreviewContainer');
    if (!container) return;
    container.innerHTML = '';
    if (selectedFiles.length > 0) {
        container.classList.remove('hidden');
        selectedFiles.forEach((file, idx) => {
            const reader = new FileReader();
            reader.onload = (re) => {
                const div = document.createElement('div');
                div.className = 'relative aspect-square rounded-lg overflow-hidden border border-gray-200 group';
                const isImg = file.type.startsWith('image/');
                div.innerHTML = `
                    ${isImg ? `<img src="${re.target.result}" class="w-full h-full object-cover">` : `<div class="w-full h-full bg-gray-100 flex items-center justify-center"><i class="fas fa-video text-gray-400"></i></div>`}
                    <button onclick="removePreviewFile(${idx})" class="absolute top-1 right-1 w-6 h-6 bg-black/60 text-white rounded-full text-[10px] hover:bg-black transition flex items-center justify-center">
                        <i class="fas fa-times"></i>
                    </button>
                `;
                container.appendChild(div);
            };
            reader.readAsDataURL(file);
        });
    } else {
        container.classList.add('hidden');
    }
}

window.removePreviewFile = (index) => {
    selectedFiles.splice(index, 1);
    renderPreviews();
};

function formatTime(dateStr) {
    if (!dateStr) return '';
    const now = new Date();
    const d = new Date(dateStr);
    const diffMs = now - d;
    const diffMin = Math.floor(diffMs / 60000);
    const diffHour = Math.floor(diffMin / 60);
    const diffDay = Math.floor(diffHour / 24);
    if (diffMin < 1) return 'Vừa xong';
    if (diffMin < 60) return `${diffMin} phút trước`;
    if (diffHour < 24) return `${diffHour} giờ trước`;
    if (diffDay < 30) return `${diffDay} ngày trước`;
    return d.toLocaleDateString('vi-VN');
}

// --- NOTIFICATIONS ---
function toggleNotiMenu() {
    const menu = document.getElementById('notiMenu');
    menu.classList.toggle('hidden');
    if (!menu.classList.contains('hidden')) {
        loadNotifications(checkAuth());
    }
}

async function loadNotifications(user) {
    try {
        const res = await fetch(`${API_URL}/notifications?username=${encodeURIComponent(user.username)}`);
        const notifications = await res.json();
        const badge = document.getElementById('notiBadge');
        const list = document.getElementById('notiList');
        const unreadCount = notifications.filter(n => !n.isRead).length;
        if (unreadCount > 0) {
            badge.textContent = unreadCount > 9 ? '9+' : unreadCount;
            badge.classList.remove('hidden');
        } else { badge.classList.add('hidden'); }
        
        if (notifications.length === 0) {
            list.innerHTML = `<p class="text-center text-gray-400 text-sm py-10">Không có thông báo nào</p>`;
            return;
        }
        list.innerHTML = notifications.map(n => {
            const initial = (n.senderFullName || n.senderUsername || 'U').charAt(0).toUpperCase();
            let actionText = '';
            let icon = '';
            switch(n.type) {
                case 'LIKE_POST': actionText = 'đã thích bài viết của bạn'; icon = 'fa-heart text-red-500'; break;
                case 'COMMENT_POST': actionText = 'đã bình luận về bài viết của bạn'; icon = 'fa-comment text-blue-500'; break;
                case 'FRIEND_REQUEST': actionText = 'đã gửi lời mời kết bạn'; icon = 'fa-user-plus text-primary'; break;
                default: actionText = 'có thông báo mới'; icon = 'fa-bell text-gray-400';
            }
            return `
                <div onclick="handleNotiClick(${n.id}, '${n.link}')" class="px-4 py-3 flex gap-3 hover:bg-gray-50 cursor-pointer transition border-b border-gray-50 last:border-0 ${n.isRead ? '' : 'bg-primary/5'}">
                    <div class="w-12 h-12 rounded-full bg-gray-100 flex-shrink-0 flex items-center justify-center relative">
                        ${n.senderAvatar ? `<img src="${n.senderAvatar}" class="w-full h-full rounded-full object-cover">` : `<span class="font-bold text-primary">${initial}</span>`}
                        <div class="absolute -bottom-1 -right-1 bg-white rounded-full w-5 h-5 flex items-center justify-center shadow-sm border border-gray-100">
                            <i class="fas ${icon} text-[10px]"></i>
                        </div>
                    </div>
                    <div class="flex-1 min-w-0">
                        <p class="text-sm text-gray-900 leading-snug"><span class="font-bold">${n.senderFullName || n.senderUsername}</span> ${actionText}</p>
                        <p class="text-[11px] font-medium text-primary mt-1">${formatTime(n.createdAt)}</p>
                    </div>
                    ${n.isRead ? '' : `<div class="w-2 h-2 bg-primary rounded-full mt-2 self-start flex-shrink-0 shadow-sm shadow-primary/40"></div>`}
                </div>`;
        }).join('');
    } catch (e) { console.error(e); }
}

async function handleNotiClick(id, link) {
    try {
        await fetch(`${API_URL}/notifications/${id}/read`, { method: 'PUT' });
        window.location.href = link;
    } catch (e) { console.error(e); }
}

async function markAllNotiAsRead() {
    const user = checkAuth();
    try {
        await fetch(`${API_URL}/notifications/read-all?username=${encodeURIComponent(user.username)}`, { method: 'PUT' });
        loadNotifications(user);
    } catch (e) { console.error(e); }
}

// --- POSTS ---
async function loadPosts(user) {
    try {
        const res = await fetch(`${API_URL}/posts?username=${encodeURIComponent(user.username)}`);
        const posts = await res.json();
        const feed = document.getElementById('postsFeed');
        if (!feed) return;
        feed.innerHTML = '';
        if (!posts.length) {
            feed.innerHTML = `<div class="bg-white rounded-2xl shadow-sm p-8 text-center text-gray-400 text-sm">Chưa có bài viết nào</div>`;
            return;
        }
        posts.forEach(post => renderPost(post, user, feed));
    } catch (e) { console.error(e); }
}

function renderPost(post, user, container) {
    const initials = (post.fullName || post.username || '?').charAt(0).toUpperCase();
    const isOwner = post.username === user.username;
    const isAdmin = user.role === 'ROLE_ADMIN';
    const currentReaction = post.currentReaction;
    
    let totalReactionCount = 0;
    if (post.reactionCounts) {
        Object.values(post.reactionCounts).forEach(count => { totalReactionCount += count; });
    } else { totalReactionCount = post.likeCount || 0; }
    
    const reactionIconMap = { 'LIKE': '<i class="fas fa-thumbs-up"></i> Thích', 'HAHA': '<i class="far fa-laugh-squint"></i> Haha', 'SAD': '<i class="far fa-sad-tear"></i> Buồn', 'ANGRY': '<i class="far fa-angry"></i> Phẫn nộ' };
    let currentReactHtml = '<i class="far fa-heart"></i> Thích';
    let currentBtnClass = 'text-gray-500';
    if (currentReaction && currentReaction !== 'NONE') {
        currentReactHtml = reactionIconMap[currentReaction] || `<i class="fas fa-thumbs-up"></i> ${currentReaction}`;
        currentBtnClass = `curr-${currentReaction}`;
    }

    let mediaHtml = '';
    if (post.mediaUrls && post.mediaUrls.length > 0) {
        const count = post.mediaUrls.length;
        const gridClass = count === 1 ? 'grid-cols-1' : 'grid-cols-2';
        mediaHtml = `<div class="grid ${gridClass} gap-1 mb-3 rounded-xl overflow-hidden border border-gray-100">`;
        post.mediaUrls.forEach(url => {
            const isVid = url.toLowerCase().endsWith('.mp4');
            mediaHtml += isVid ? `<video src="${url}" controls class="w-full aspect-square object-cover"></video>` : `<img src="${url}" class="w-full aspect-square object-cover">`;
        });
        mediaHtml += `</div>`;
    }

    const div = document.createElement('div');
    div.id = `post-${post.id}`;
    div.className = 'bg-white rounded-2xl shadow-sm mb-4 overflow-hidden';
    div.innerHTML = `
        <div class="p-5">
            <div class="flex items-center gap-3 mb-3">
                <a href="profile.html?username=${post.username}" class="w-10 h-10 rounded-full bg-primary flex items-center justify-center text-white font-bold text-sm overflow-hidden hover:opacity-80 transition">
                    ${post.avatar ? `<img src="${post.avatar}" class="w-full h-full object-cover">` : initials}
                </a>
                <div class="flex-1 min-w-0">
                    <a href="profile.html?username=${post.username}" class="font-bold text-gray-900 text-sm hover:underline no-underline">${post.fullName || post.username}</a>
                    <p class="text-xs text-gray-400">${formatTime(post.createdAt)}</p>
                </div>
                ${(isOwner || isAdmin) ? `<button onclick="deletePost(${post.id})" class="text-gray-300 hover:text-red-500 transition text-sm px-2"><i class="fas fa-trash"></i></button>` : ''}
            </div>
            <p class="text-gray-800 text-sm leading-relaxed mb-3">${post.content}</p>
            ${mediaHtml}
            <div class="flex items-center justify-between text-xs text-gray-500 mb-3 px-1 border-b border-gray-100 pb-3">
                <div class="flex items-center gap-1 cursor-pointer hover:underline" onclick="showReactionList(${post.id})">
                    <span class="text-primary bg-primary bg-opacity-10 rounded-full w-5 h-5 flex items-center justify-center text-[10px]"><i class="fas fa-thumbs-up"></i></span>
                    <span>${totalReactionCount}</span>
                </div>
                <div class="cursor-pointer hover:underline" onclick="toggleComments(${post.id})"><span>${(post.comments || []).length} bình luận</span></div>
            </div>
        </div>
        <div class="flex px-2 py-1">
            <div class="flex-1 reaction-container">
                <div class="reaction-menu">
                    <span class="reaction-icon emoji-like" onclick="reactToPost(${post.id}, 'LIKE')"><i class="fas fa-thumbs-up"></i></span>
                    <span class="reaction-icon emoji-haha" onclick="reactToPost(${post.id}, 'HAHA')"><i class="fas fa-laugh-squint"></i></span>
                    <span class="reaction-icon emoji-sad" onclick="reactToPost(${post.id}, 'SAD')"><i class="fas fa-sad-tear"></i></span>
                    <span class="reaction-icon emoji-angry" onclick="reactToPost(${post.id}, 'ANGRY')"><i class="fas fa-angry"></i></span>
                </div>
                <button onclick="reactToPost(${post.id}, '${currentReaction === 'LIKE' ? 'NONE' : 'LIKE'}')" class="w-full flex items-center justify-center gap-2 py-2 rounded-xl text-sm font-semibold transition hover:bg-gray-50 ${currentBtnClass}">${currentReactHtml}</button>
            </div>
            <button onclick="toggleComments(${post.id})" class="flex-1 flex items-center justify-center gap-2 py-2.5 rounded-xl text-sm font-medium text-gray-500 hover:bg-gray-50 transition"><i class="far fa-comment"></i> Bình luận</button>
        </div>
        <div id="comments-${post.id}" class="hidden border-t border-gray-100 p-4 bg-gray-50">
            <div id="comments-list-${post.id}">
                ${renderCommentsHtml(post.id, post.comments)}
            </div>
            <div class="flex gap-2 mt-2">
                <input type="text" id="comment-input-${post.id}" placeholder="Viết bình luận..." class="flex-1 bg-white border border-gray-200 rounded-full px-4 py-2 text-sm focus:outline-none focus:border-primary transition" onkeyup="if(event.key==='Enter') submitComment(${post.id})">
                <button onclick="submitComment(${post.id})" class="bg-primary hover:bg-primary-dark text-white text-xs font-semibold px-4 py-2 rounded-full transition">Gửi</button>
            </div>
        </div>`;
    container.appendChild(div);
}

window.renderCommentsHtml = function(postId, comments) {
    if (!comments || comments.length === 0) return '';
    const topLevel = comments.filter(c => !c.parentCommentId);
    const repliesMap = {};
    comments.forEach(c => {
        if (c.parentCommentId) {
            if (!repliesMap[c.parentCommentId]) repliesMap[c.parentCommentId] = [];
            repliesMap[c.parentCommentId].push(c);
        }
    });

    const renderSingleComment = (c, isReply = false) => {
        const likeBtnClass = c.currentReaction === 'LIKE' ? 'text-primary' : 'text-gray-500';
        const likeCountStr = c.likeCount > 0 ? `(${c.likeCount})` : '';
        const replyHtml = repliesMap[c.id] ? repliesMap[c.id].map(r => renderSingleComment(r, true)).join('') : '';
        const marginClass = isReply ? 'ml-8 mt-2' : 'mb-3';
        const avatarSize = isReply ? 'w-6 h-6 text-[10px]' : 'w-8 h-8 text-xs';

        return `
            <div class="flex flex-col ${marginClass}">
                <div class="flex gap-3">
                    <a href="profile.html?username=${c.username}" class="${avatarSize} rounded-full bg-primary flex items-center justify-center text-white font-bold flex-shrink-0 overflow-hidden hover:opacity-80 transition">
                         ${c.fullName ? c.fullName.charAt(0).toUpperCase() : 'U'}
                    </a>
                    <div class="flex-1 min-w-0">
                        <div class="bg-${isReply ? 'gray-100' : 'white'} rounded-xl px-3 py-2 shadow-sm inline-block max-w-full">
                            <p class="font-semibold text-xs text-gray-700 mb-0.5">${c.fullName || c.username}</p>
                            <p class="text-sm text-gray-800 break-words">${c.content}</p>
                        </div>
                        <div class="text-[11px] text-gray-500 mt-1 ml-2 flex gap-3">
                            <button onclick="reactToComment(${c.id})" class="font-semibold hover:underline transition ${likeBtnClass}">Thích ${likeCountStr}</button>
                            <button onclick="setReply(${postId}, ${c.id}, '${c.fullName || c.username}')" class="font-semibold hover:underline text-gray-500 transition">Trả lời</button>
                            <span>${formatTime(c.createdAt)}</span>
                        </div>
                    </div>
                </div>
                ${replyHtml}
            </div>
        `;
    };

    return topLevel.map(c => renderSingleComment(c)).join('');
};

window.setReply = function(postId, commentId, name) {
    const input = document.getElementById(`comment-input-${postId}`);
    if(input) {
        input.dataset.parentId = commentId;
        input.placeholder = `Trả lời ${name}...`;
        input.focus();
    }
};

async function reactToComment(commentId) {
    const user = checkAuth();
    try {
        await fetch(`${API_URL}/posts/comments/${commentId}/reaction`, {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ username: user.username, reactionType: 'LIKE' })
        });
        loadPosts(user);
    } catch (e) { console.error(e); }
}

function toggleComments(postId) {
    const el = document.getElementById(`comments-${postId}`);
    if (el) el.classList.toggle('hidden');
}

async function reactToPost(postId, reactionType) {
    const user = checkAuth();
    try {
        await fetch(`${API_URL}/posts/${postId}/reaction`, {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ username: user.username, reactionType })
        });
        loadPosts(user);
    } catch (e) { console.error(e); }
}

async function showReactionList(postId) {
    document.getElementById('usersModalTitle').textContent = 'Người đã bày tỏ cảm xúc';
    const body = document.getElementById('usersModalBody');
    body.innerHTML = '<p class="text-center text-gray-400 text-sm">Đang tải...</p>';
    document.getElementById('usersModal').classList.remove('hidden');
    try {
        const res = await fetch(`${API_URL}/posts/${postId}/reactions`);
        const users = await res.json();
        if(!users || users.length === 0) {
            body.innerHTML = '<p class="text-center text-gray-400 text-sm">Chưa có ai bày tỏ cảm xúc</p>';
            return;
        }
        const iconMap = { 'LIKE': '<i class="fas fa-thumbs-up text-blue-500"></i>', 'HAHA': '<i class="fas fa-laugh-squint text-yellow-500"></i>', 'SAD': '<i class="fas fa-sad-tear text-yellow-500"></i>', 'ANGRY': '<i class="fas fa-angry text-red-500"></i>' };
        body.innerHTML = users.map(u => `
            <div class="flex items-center justify-between mb-3">
                <a href="profile.html?username=${u.username}" class="flex items-center gap-3 no-underline hover:opacity-80">
                    <div class="w-10 h-10 rounded-full bg-primary text-white flex items-center justify-center font-bold relative overflow-hidden">
                        ${u.avatar ? `<img src="${u.avatar}" class="w-full h-full object-cover">` : (u.fullName||u.username).charAt(0).toUpperCase()}
                        <div class="absolute -bottom-1 -right-1 bg-white rounded-full p-0.5 text-[10px]">${iconMap[u.reactionType] || iconMap['LIKE']}</div>
                    </div>
                    <span class="font-semibold text-sm text-gray-900">${u.fullName || u.username}</span>
                </a>
            </div>`).join('');
    } catch (e) { body.innerHTML = '<p class="text-center text-red-400 text-sm">Lỗi tải dữ liệu</p>'; }
}

async function deletePost(postId) {
    if (!confirm('Xóa bài viết này?')) return;
    try {
        await fetch(`${API_URL}/posts/${postId}?username=${encodeURIComponent(checkAuth().username)}`, { method: 'DELETE' });
        document.getElementById(`post-${postId}`).remove();
    } catch (e) { console.error(e); }
}

async function submitComment(postId) {
    const user = checkAuth();
    const input = document.getElementById(`comment-input-${postId}`);
    const content = input.value.trim();
    if (!content) return;

    const parentId = input.dataset.parentId || null;

    try {
        await fetch(`${API_URL}/posts/${postId}/comments`, { 
            method: 'POST', 
            headers: { 'Content-Type': 'application/json' }, 
            body: JSON.stringify({ username: user.username, content, parentCommentId: parentId }) 
        });
        input.value = '';
        input.dataset.parentId = '';
        input.placeholder = 'Viết bình luận...';
        loadPosts(user);
    } catch (e) { console.error(e); }
}

function setupPostForm(user) {
    const form = document.getElementById('postForm');
    if (!form) return;
    form.addEventListener('submit', async (e) => {
        e.preventDefault();
        const content = document.getElementById('postContent').value.trim();
        const submitBtn = document.getElementById('postSubmitBtn');
        if (!content && selectedFiles.length === 0) return;
        
        submitBtn.disabled = true;
        submitBtn.innerHTML = '<i class="fas fa-spinner fa-spin mr-2"></i> Đang đăng...';

        try {
            const mediaUrls = [];
            if (selectedFiles.length > 0) {
                const uploadPromises = selectedFiles.map(async (file) => {
                    const formData = new FormData();
                    formData.append('file', file);
                    const res = await fetch(`${API_URL}/upload`, { method: 'POST', body: formData });
                    if (res.ok) {
                        const data = await res.json();
                        return data.url;
                    }
                    return null;
                });
                const results = await Promise.all(uploadPromises);
                results.forEach(url => { if(url) mediaUrls.push(url); });
            }

            const res = await fetch(`${API_URL}/posts`, {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify({ content, mediaUrls, username: user.username })
            });

            if (res.ok) {
                form.reset();
                selectedFiles = [];
                document.getElementById('imagePreviewContainer').innerHTML = '';
                document.getElementById('imagePreviewContainer').classList.add('hidden');
                loadPosts(user);
            }
        } catch (e) { console.error(e); } finally {
            submitBtn.disabled = false;
            submitBtn.textContent = 'Đăng';
        }
    });
}

// --- SEARCH LOGIC ---
async function doGlobalSearch() {
    const user = checkAuth();
    const query = document.getElementById('globalSearchInput').value.trim();
    const resultsContainer = document.getElementById('sidebarSearchResults');
    const feedHeader = document.getElementById('searchFeedHeader');
    const createPostBlock = document.getElementById('createPostBlock');
    const postsFeed = document.getElementById('postsFeed');

    if (!query) {
        clearSearch();
        return;
    }

    resultsContainer.innerHTML = '<p class="text-center text-gray-400 text-xs py-4">Đang tìm kiếm...</p>';
    feedHeader.classList.remove('hidden');
    createPostBlock.classList.add('hidden');
    postsFeed.innerHTML = '<p class="text-center text-gray-400 text-sm py-10">Đang tìm kiếm bài viết...</p>';

    try {
        // 1. Search Users
        const resUsers = await fetch(`${API_URL}/auth/users/search?keyword=${encodeURIComponent(query)}`);
        const users = await resUsers.json();
        const filteredUsers = users.filter(u => u.username !== user.username);
        
        if (filteredUsers.length === 0) {
            resultsContainer.innerHTML = '<p class="text-center text-gray-400 text-xs py-4">Không tìm thấy người dùng</p>';
        } else {
            resultsContainer.innerHTML = filteredUsers.map(u => {
                const initial = (u.fullName || u.username || '?').charAt(0).toUpperCase();
                return `
                <div class="flex items-center gap-3 p-2 hover:bg-gray-50 rounded-xl transition">
                    <a href="profile.html?username=${u.username}" class="w-10 h-10 rounded-full bg-primary flex items-center justify-center text-white font-bold flex-shrink-0 overflow-hidden hover:opacity-80 transition no-underline">
                        ${u.avatar ? `<img src="${u.avatar}" class="w-full h-full object-cover">` : initial}
                    </a>
                    <div class="flex-1 min-w-0">
                        <a href="profile.html?username=${u.username}" class="font-semibold text-sm text-gray-900 truncate hover:underline no-underline block">${u.fullName || u.username}</a>
                        <p class="text-xs text-gray-400">@${u.username}</p>
                    </div>
                </div>`;
            }).join('');
        }

        // 2. Search Posts
        const resPosts = await fetch(`${API_URL}/posts/search?keyword=${encodeURIComponent(query)}&viewer=${encodeURIComponent(user.username)}`);
        const posts = await resPosts.json();
        
        postsFeed.innerHTML = '';
        if (posts.length === 0) {
            postsFeed.innerHTML = '<div class="bg-white rounded-2xl shadow-sm p-8 text-center text-gray-400 text-sm">Không tìm thấy bài viết nào phù hợp</div>';
        } else {
            posts.forEach(post => renderPost(post, user, postsFeed));
        }

    } catch (e) {
        console.error(e);
        resultsContainer.innerHTML = '<p class="text-center text-red-400 text-xs py-4">Lỗi tìm kiếm</p>';
        postsFeed.innerHTML = '<p class="text-center text-red-400 text-sm">Lỗi tải bài viết</p>';
    }
}

window.clearSearch = function() {
    const user = checkAuth();
    document.getElementById('globalSearchInput').value = '';
    document.getElementById('sidebarSearchResults').innerHTML = '<p class="text-xs text-gray-400 text-center">Gõ từ khóa và nhấn Enter</p>';
    document.getElementById('searchFeedHeader').classList.add('hidden');
    document.getElementById('createPostBlock').classList.remove('hidden');
    loadPosts(user);
};
