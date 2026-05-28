var API_URL = '/api';

document.addEventListener('DOMContentLoaded', () => {
    const user = checkAuth();

    // Set navbar avatar initial
    const navAvatar = document.getElementById('navAvatar');
    if (navAvatar && user.fullName) navAvatar.textContent = user.fullName.charAt(0).toUpperCase();
    if (user.avatar) navAvatar.innerHTML = `<img src="${escapeHtml(user.avatar)}" class="w-full h-full object-cover" onerror="this.parentElement.textContent='${escapeHtml(user.fullName ? user.fullName.charAt(0).toUpperCase() : 'U')}'">`;

    loadPosts(user);
    setupPostForm(user);
    loadAnnouncements();

    // Image preview setup
    const postImageFile = document.getElementById('postImageFile');
    const previewContainer = document.getElementById('createPostImagePreviewContainer');
    const previewImg = document.getElementById('createPostImagePreview');
    const cancelBtn = document.getElementById('cancelPostImagePreview');

    if (postImageFile) {
        postImageFile.addEventListener('change', function() {
            if (this.files && this.files[0]) {
                const reader = new FileReader();
                reader.onload = function(e) {
                    previewImg.src = e.target.result;
                    previewContainer.classList.remove('hidden');
                }
                reader.readAsDataURL(this.files[0]);
            }
        });
        if (cancelBtn) {
            cancelBtn.addEventListener('click', function() {
                postImageFile.value = '';
                previewImg.src = '';
                previewContainer.classList.add('hidden');
            });
        }
    }
});

let currentUserObj = null;

function escapeHtml(unsafe) {
    if (!unsafe) return "";
    return String(unsafe)
         .replace(/&/g, "&amp;")
         .replace(/</g, "&lt;")
         .replace(/>/g, "&gt;")
         .replace(/"/g, "&quot;")
         .replace(/'/g, "&#039;");
}

var TAG_COLORS = {
    '#just-for-fun': 'hashtag-blue',
    '#quan-trọng':   'hashtag-red',
    '#hỏi-đáp':     'hashtag-green',
    '#chia-sẻ':     'hashtag-purple',
    '#học-tập':     'hashtag-yellow'
};

function renderContent(content) {
    if (!content) return '';
    var escaped = escapeHtml(content);
    escaped = escaped.replace(/(#[\w\u00C0-\u024F\u1E00-\u1EFF-]+)/g, function(match) {
        var lower = match.toLowerCase();
        var colorClass = TAG_COLORS[lower] || 'hashtag-default';
        return '<span class="hashtag-tag ' + colorClass + '">' + match + '</span>';
    });
    return escaped;
}

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

let allPostsData = [];

function searchPosts() {
    var input = document.getElementById('searchInput');
    var query = input ? input.value.trim() : '';
    if (!query) return;
    document.getElementById('searchIndicator').classList.remove('hidden');
    document.getElementById('searchQueryText').textContent = query;
    document.getElementById('clearSearchBtn').classList.remove('hidden');
    if (currentUserObj) {
        loadPosts(currentUserObj, query);
    }
}

function clearSearch() {
    var input = document.getElementById('searchInput');
    if (input) input.value = '';
    document.getElementById('searchIndicator').classList.add('hidden');
    document.getElementById('clearSearchBtn').classList.add('hidden');
    if (currentUserObj) {
        loadPosts(currentUserObj);
    }
}

let currentPage = 0;

async function loadPosts(user, searchQuery = '', append = false) {
    currentUserObj = user;
    try {
        if (!append) currentPage = 0;
        let url = `${API_URL}/posts?page=${currentPage}&size=10`;
        if (searchQuery) url += `&search=${encodeURIComponent(searchQuery)}`;
        
        const res = await fetch(url);
        const data = await res.json();
        const posts = data.content || [];
        const feed = document.getElementById('postsFeed');
        
        if (!append) {
            feed.innerHTML = '';
            allPostsData = posts || [];
        } else {
            allPostsData = allPostsData.concat(posts);
        }

        if (!allPostsData.length) {
            feed.innerHTML = `<div class="bg-white rounded-2xl shadow-sm p-8 text-center text-gray-400 text-sm">` + (searchQuery ? `Không tìm thấy bài viết nào phù hợp.` : `Chưa có bài viết nào. Hãy đăng bài đầu tiên!`) + `</div>`;
        } else {
            posts.forEach(post => renderPost(post, user, feed));
        }

        const oldBtn = document.getElementById('loadMoreBtn');
        if (oldBtn) oldBtn.remove();

        if (data.totalPages && currentPage < data.totalPages - 1) {
            const btn = document.createElement('button');
            btn.id = 'loadMoreBtn';
            btn.className = 'w-full py-3 bg-gray-50 hover:bg-gray-100 text-gray-600 rounded-xl text-sm font-semibold transition mt-2 mb-6';
            btn.textContent = 'Xem thêm bài viết';
            btn.onclick = () => {
                currentPage++;
                loadPosts(user, searchQuery, true);
            };
            feed.appendChild(btn);
        }
        
        if (!append) extractTopics(allPostsData);
    } catch (e) {
        console.error(e);
    }
}

function renderPost(post, user, container) {
    const initials = (post.userFullName || post.username || '?').charAt(0).toUpperCase();
    const isOwner = post.username === user.username;
    const isAdmin = user.role === 'ROLE_ADMIN';
    const liked = post.liked;
    const commentCount = (post.comments || []).length;
    
    // Logic Reaction từ PTIT
    const currentReaction = post.currentReaction;
    let totalReactionCount = post.likeCount || 0;
    const reactionIconMap = { 
        'LIKE': '<i class="fas fa-thumbs-up"></i> Thích', 
        'HAHA': '<i class="far fa-laugh-squint"></i> Haha', 
        'SAD': '<i class="far fa-sad-tear"></i> Buồn', 
        'ANGRY': '<i class="far fa-angry"></i> Phẫn nộ' 
    };
    let currentReactHtml = '<i class="far fa-heart"></i> Thích';
    let currentBtnClass = 'text-gray-500';
    if (currentReaction && currentReaction !== 'NONE') {
        currentReactHtml = reactionIconMap[currentReaction] || `<i class="fas fa-thumbs-up"></i> ${currentReaction}`;
        currentBtnClass = `curr-${currentReaction}`;
    }

    const div = document.createElement('div');
    div.id = `post-${post.id}`;
    div.className = 'bg-white rounded-2xl shadow-sm mb-4 overflow-hidden fade-up';
    div.innerHTML = `
        <div class="p-5">
            <div class="flex items-center gap-3 mb-3">
                <a href="profile.html?username=${escapeHtml(post.username)}" class="w-10 h-10 rounded-full bg-primary flex items-center justify-center text-white font-bold text-sm flex-shrink-0 overflow-hidden hover:opacity-80 transition no-underline">
                    ${post.userAvatar ? `<img src="${escapeHtml(post.userAvatar)}" class="w-full h-full object-cover">` : escapeHtml(initials)}
                </a>
                <div class="flex-1 min-w-0">
                    <a href="profile.html?username=${escapeHtml(post.username)}" class="font-semibold text-gray-900 text-sm hover:underline">${escapeHtml(post.userFullName || post.username)}</a>
                    <p class="text-xs text-gray-400">${formatTime(post.createdAt)}</p>
                </div>
                ${(isOwner || isAdmin) ? `
                <button onclick="deletePost(${post.id})" class="text-gray-300 hover:text-red-500 transition text-sm px-2">
                    <i class="fas fa-trash"></i>
                </button>` : ''}
            </div>
            <p class="text-gray-800 text-sm leading-relaxed mb-3" style="white-space: pre-wrap;">${renderContent(post.content)}</p>
            ${post.imageUrl ? `<img src="${escapeHtml(post.imageUrl)}" alt="Post image" class="w-full rounded-xl mb-3 max-h-96 object-cover" onerror="this.style.display='none'">` : ''}
            <div class="flex items-center justify-between text-xs text-gray-400 mb-3 px-1 border-b border-gray-100 pb-3">
                <div class="flex items-center gap-1 cursor-pointer hover:underline" onclick="showReactionList(${post.id})">
                    <span class="text-primary bg-primary bg-opacity-10 rounded-full w-5 h-5 flex items-center justify-center text-[10px]"><i class="fas fa-thumbs-up"></i></span>
                    <span>${totalReactionCount} lượt thích</span>
                </div>
                <div class="cursor-pointer hover:underline" onclick="toggleComments(${post.id})">
                    <span>${commentCount} bình luận</span>
                </div>
            </div>
        </div>
        <div class="flex px-2 py-1">
            <div class="flex-1 reaction-container">
                <div class="reaction-menu">
                    <span class="reaction-icon emoji-like" onclick="reactToPost(${post.id}, 'LIKE')"><i class="fas fa-thumbs-up"></i></span>
                    <span class="reaction-icon emoji-haha" onclick="reactToPost(${post.id}, 'HAHA')"><i class="far fa-laugh-squint"></i></span>
                    <span class="reaction-icon emoji-sad" onclick="reactToPost(${post.id}, 'SAD')"><i class="far fa-sad-tear"></i></span>
                    <span class="reaction-icon emoji-angry" onclick="reactToPost(${post.id}, 'ANGRY')"><i class="far fa-angry"></i></span>
                </div>
                <button onclick="reactToPost(${post.id}, '${currentReaction === 'LIKE' ? 'NONE' : 'LIKE'}')" 
                    class="w-full flex items-center justify-center gap-2 py-2 rounded-xl text-sm font-semibold transition hover:bg-gray-50 ${currentBtnClass}">
                    ${currentReactHtml}
                </button>
            </div>
            <button onclick="toggleComments(${post.id})" class="flex-1 flex items-center justify-center gap-2 py-2.5 rounded-xl text-sm font-medium text-gray-500 hover:bg-gray-50 transition">
                <i class="far fa-comment"></i> Bình luận
            </button>
        </div>
        <div id="comments-${post.id}" class="hidden border-t border-gray-100 p-4 bg-gray-50">
            <div id="comments-list-${post.id}">
                ${renderCommentsHtml(post.id, post.comments)}
            </div>
            <div class="flex gap-2 mt-2">
                <input type="text" id="comment-input-${post.id}" placeholder="Viết bình luận..."
                    class="flex-1 bg-white border border-gray-200 rounded-full px-4 py-2 text-sm focus:outline-none focus:border-primary transition"
                    onkeyup="if(event.key==='Enter') submitComment(${post.id})">
                <button onclick="submitComment(${post.id})" class="bg-primary hover:bg-primary-dark text-white text-xs font-semibold px-4 py-2 rounded-full transition">Gửi</button>
            </div>
        </div>
    `;
    container.appendChild(div);
}

function toggleComments(postId) {
    const el = document.getElementById(`comments-${postId}`);
    el.classList.toggle('hidden');
}

async function reactToPost(postId, reactionType) {
    const user = checkAuth();
    try {
        const res = await fetch(`${API_URL}/posts/${postId}/like`, {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ username: user.username, reactionType: reactionType })
        });
        const data = await res.json();
        if (data.success || data.liked !== undefined) {
            loadPosts(user, document.getElementById('searchInput') ? document.getElementById('searchInput').value.trim() : '');
        }
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
        const iconMap = { 
            'LIKE': '<i class="fas fa-thumbs-up text-blue-500"></i>', 
            'HAHA': '<i class="fas fa-laugh-squint text-yellow-500"></i>', 
            'SAD': '<i class="fas fa-sad-tear text-yellow-500"></i>', 
            'ANGRY': '<i class="fas fa-angry text-red-500"></i>' 
        };
        body.innerHTML = users.map(u => `
            <div class="flex items-center justify-between mb-3">
                <a href="profile.html?username=${escapeHtml(u.username)}" class="flex items-center gap-3 no-underline hover:opacity-80">
                    <div class="w-10 h-10 rounded-full bg-primary text-white flex items-center justify-center font-bold relative overflow-hidden">
                        ${u.avatar ? `<img src="${escapeHtml(u.avatar)}" class="w-full h-full object-cover">` : escapeHtml((u.fullName||u.username).charAt(0).toUpperCase())}
                        <div class="absolute -bottom-1 -right-1 bg-white rounded-full p-0.5 text-[10px]">${iconMap[u.reactionType] || iconMap['LIKE']}</div>
                    </div>
                    <span class="font-semibold text-sm text-gray-900">${escapeHtml(u.fullName || u.username)}</span>
                </a>
            </div>`).join('');
    } catch (e) { body.innerHTML = '<p class="text-center text-red-400 text-sm">Lỗi tải dữ liệu</p>'; }
}

async function deletePost(postId) {
    if (!confirm('Xóa bài viết này?')) return;
    const user = checkAuth();
    try {
        await fetch(`${API_URL}/posts/${postId}`, { method: 'DELETE' });
        const el = document.getElementById(`post-${postId}`);
        if (el) el.remove();
    } catch (e) { console.error(e); }
}

async function submitComment(postId) {
    const user = checkAuth();
    const input = document.getElementById(`comment-input-${postId}`);
    const content = input.value.trim();
    const parentId = input.dataset.parentId || null;
    if (!content) return;
    try {
        await fetch(`${API_URL}/posts/${postId}/comments`, {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ username: user.username, content, parentId })
        });
        input.value = '';
        delete input.dataset.parentId;
        input.placeholder = 'Viết bình luận...';
        loadPosts(user, document.getElementById('searchInput') ? document.getElementById('searchInput').value.trim() : '');
    } catch (e) { console.error(e); }
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
                    <a href="profile.html?username=${escapeHtml(c.username)}" class="${avatarSize} rounded-full bg-primary flex items-center justify-center text-white font-bold flex-shrink-0 overflow-hidden hover:opacity-80 transition no-underline">
                         ${escapeHtml(c.fullName ? c.fullName.charAt(0).toUpperCase() : 'U')}
                    </a>
                    <div class="flex-1 min-w-0">
                        <div class="bg-${isReply ? 'gray-100' : 'white'} rounded-xl px-3 py-2 shadow-sm inline-block max-w-full">
                            <a href="profile.html?username=${escapeHtml(c.username)}" class="font-semibold text-xs text-gray-700 mb-0.5 hover:underline">${escapeHtml(c.fullName || c.username)}</a>
                            <p class="text-sm text-gray-800 break-words">${renderContent(c.content)}</p>
                        </div>
                        <div class="text-[11px] text-gray-500 mt-1 ml-2 flex gap-3">
                            <button onclick="reactToComment(${c.id})" class="font-semibold hover:underline transition ${likeBtnClass}">Thích ${likeCountStr}</button>
                            <button onclick="setReply(${postId}, ${c.id}, '${escapeHtml(c.fullName || c.username)}')" class="font-semibold hover:underline text-gray-500 transition">Trả lời</button>
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
            body: JSON.stringify({ username: user.username })
        });
        loadPosts(user, document.getElementById('searchInput') ? document.getElementById('searchInput').value.trim() : '');
    } catch (e) { console.error(e); }
}

function showModerationNotice(type, message) {
    var feed = document.getElementById('postsFeed');
    if (!feed) return;
    var bgColor = type === 'rejected' ? 'bg-red-50 border-red-200' : 'bg-yellow-50 border-yellow-200';
    var iconColor = type === 'rejected' ? 'text-red-500' : 'text-yellow-500';
    var icon = type === 'rejected' ? 'fa-exclamation-triangle' : 'fa-hourglass-half';
    var textColor = type === 'rejected' ? 'text-red-700' : 'text-yellow-700';

    var notice = document.createElement('div');
    notice.className = bgColor + ' border rounded-2xl p-4 mb-4 flex items-start gap-3 fade-up';
    notice.innerHTML = '<i class="fas ' + icon + ' ' + iconColor + ' text-lg mt-0.5"></i>'
        + '<div class="flex-1"><p class="' + textColor + ' text-sm font-medium">' + message + '</p></div>'
        + '<button onclick="this.parentElement.remove()" class="text-gray-400 hover:text-gray-600 text-sm"><i class="fas fa-times"></i></button>';

    feed.insertBefore(notice, feed.firstChild);
    setTimeout(function() { if (notice.parentNode) notice.remove(); }, 8000);
}

function setupPostForm(user) {
    const form = document.getElementById('postForm');
    const submitBtn = document.getElementById('postSubmitBtn');
    if (!form) return;
    form.addEventListener('submit', async (e) => {
        e.preventDefault();
        const content = document.getElementById('postContent').value.trim();
        const imageFile = document.getElementById('postImageFile') ? document.getElementById('postImageFile').files[0] : null;
        if (!content && !imageFile) return;
        
        if (submitBtn) {
            submitBtn.disabled = true;
            submitBtn.textContent = 'Đang đăng...';
        }
        
        let imageUrl = null;
        if (imageFile) {
            const formData = new FormData();
            formData.append('imageFile', imageFile);
            try {
                const uploadRes = await fetch(`${API_URL}/upload/post-image`, { method: 'POST', body: formData });
                if (uploadRes.ok) {
                    const uploadData = await uploadRes.json();
                    if (uploadData.status === 'ok') imageUrl = uploadData.imageUrl;
                }
            } catch (err) { console.error('Lỗi upload', err); }
        }

        try {
            const res = await fetch(`${API_URL}/posts`, {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify({ content, imageUrl: imageUrl, username: user.username })
            });
            if (res.ok) {
                const result = await res.json();
                document.getElementById('postContent').value = '';
                if (document.getElementById('postImageFile')) {
                    document.getElementById('postImageFile').value = '';
                    document.getElementById('createPostImagePreview').src = '';
                    document.getElementById('createPostImagePreviewContainer').classList.add('hidden');
                }
                
                if (result.status === 'PENDING') {
                    showModerationNotice('pending', result.message || 'Bài viết đang chờ kiểm duyệt.');
                } else if (result.status === 'REJECTED') {
                    showModerationNotice('rejected', result.message || 'Bài viết bị từ chối do nội dung không phù hợp.');
                } else {
                    loadPosts(user);
                }
            }
        } catch (e) {
            console.error(e);
        } finally {
            if (submitBtn) {
                submitBtn.disabled = false;
                submitBtn.innerHTML = '<i class="fas fa-paper-plane"></i> Đăng';
            }
        }
    });
}

// Announcements & Topics
async function loadAnnouncements() {
    const box = document.getElementById('announcementsBox');
    if (!box) return;
    try {
        const res = await fetch(`${API_URL}/announcements`);
        const data = await res.json();
        
        if (!data || !data.length) {
            box.innerHTML = '<p class="text-xs text-gray-400 text-center py-4">Chưa có thông báo nào.</p>';
            return;
        }
        box.innerHTML = '';
        data.forEach(ann => {
            const div = document.createElement('div');
            div.className = 'border-b border-gray-100 py-3 px-1 last:border-0 hover:bg-gray-50 transition rounded-lg cursor-default';
            div.innerHTML = `<div class="flex items-center gap-2 mb-1">
                                <span class="text-xs text-gray-400">${formatTime(ann.createdAt)}</span>
                             </div>
                             <p class="text-sm font-semibold text-gray-900 mb-0.5">${escapeHtml(ann.title)}</p>
                             <p class="text-xs text-gray-500 line-clamp-2">${escapeHtml(ann.content)}</p>`;
            box.appendChild(div);
        });
    } catch (e) {
        box.innerHTML = '<p class="text-xs text-gray-400 text-center py-4">Lỗi tải thông báo.</p>';
    }
}

function extractTopics(posts) {
    const box = document.getElementById('topicsBox');
    if (!box) return;

    const tagCounts = {};
    (posts || []).forEach(post => {
        if (!post.content) return;
        const matches = post.content.match(/#[\w\u00C0-\u024F\u1E00-\u1EFF-]+/g);
        if (matches) {
            matches.forEach(tag => {
                const lower = tag.toLowerCase();
                tagCounts[lower] = (tagCounts[lower] || 0) + 1;
            });
        }
    });

    const sorted = Object.keys(tagCounts).sort((a, b) => tagCounts[b] - tagCounts[a]);

    if (sorted.length === 0) {
        box.innerHTML = '<p class="text-xs text-gray-400 text-center py-4 w-full">Chưa có chủ đề nào.</p>';
        return;
    }

    box.innerHTML = '';
    const colors = [
        'bg-blue-50 text-blue-600 hover:bg-blue-100',
        'bg-green-50 text-green-600 hover:bg-green-100',
        'bg-purple-50 text-purple-600 hover:bg-purple-100',
        'bg-orange-50 text-orange-600 hover:bg-orange-100',
        'bg-pink-50 text-pink-600 hover:bg-pink-100',
        'bg-cyan-50 text-cyan-600 hover:bg-cyan-100',
        'bg-yellow-50 text-yellow-700 hover:bg-yellow-100',
        'bg-red-50 text-red-500 hover:bg-red-100'
    ];

    sorted.slice(0, 15).forEach((tag, idx) => {
        const colorClass = colors[idx % colors.length];
        const btn = document.createElement('button');
        btn.className = 'text-xs font-medium px-3 py-1.5 rounded-full transition cursor-pointer ' + colorClass;
        btn.textContent = tag + ' (' + tagCounts[tag] + ')';
        btn.onclick = () => {
            const input = document.getElementById('searchInput');
            if (input) input.value = tag;
            searchPosts();
        };
        box.appendChild(btn);
    });
}
