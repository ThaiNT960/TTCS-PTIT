var API_URL = window.location.origin + '/api';

document.addEventListener('DOMContentLoaded', () => {
    const user = checkAuth();
    setNavAvatar(user);
    const urlParams = new URLSearchParams(window.location.search);
    let targetUsername = urlParams.get('username');
    if (!targetUsername) {
        targetUsername = user.username;
    }
    loadProfileDynamic(user, targetUsername);
    loadPosts(user, targetUsername);
});

function setNavAvatar(user) {
    const el = document.getElementById('navAvatar');
    if (!el) return;
    const initial = (user.fullName || user.username || 'U').charAt(0).toUpperCase();
    if (user.avatar) {
        el.innerHTML = `<img src="${user.avatar}" class="w-full h-full object-cover rounded-full">`;
    } else {
        el.textContent = initial;
    }
}

async function loadProfileDynamic(viewer, targetUsername) {
    try {
        const res = await fetch(`${API_URL}/users/profile/${encodeURIComponent(targetUsername)}`);
        if (!res.ok) { 
            document.querySelector('main').innerHTML = '<div class="max-w-4xl mx-auto mt-20 text-center text-gray-500">Người dùng không tồn tại hoặc đã bị xóa</div>';
            return; 
        }
        const targetData = await res.json();

        // Basic Info
        const nameEl = document.getElementById('profileName');
        const unameEl = document.getElementById('profileUsername');
        const avatarEl = document.getElementById('profileAvatar');
        const coverEl = document.getElementById('profileCover');
        const bioEl = document.getElementById('profileBio');
        const actionArea = document.getElementById('profileActionArea');
        
        if (nameEl) nameEl.textContent = targetData.fullName || targetData.username;
        if (unameEl) unameEl.textContent = '@' + targetData.username;
        if (bioEl) bioEl.textContent = targetData.bio || '';

        // Avatar
        const initial = (targetData.fullName || targetData.username || 'U').charAt(0).toUpperCase();
        if (avatarEl) {
            if (targetData.avatar) {
                avatarEl.innerHTML = `<img src="${targetData.avatar}" class="w-full h-full object-cover">`;
            } else {
                avatarEl.innerHTML = `<div class="w-full h-full bg-primary flex items-center justify-center text-white">${initial}</div>`;
            }
        }

        // Cover Photo
        if (coverEl) {
            if (targetData.coverPhoto) {
                coverEl.innerHTML = `<img src="${targetData.coverPhoto}" class="w-full h-full object-cover">`;
            } else {
                coverEl.innerHTML = `<div class="w-full h-full bg-gradient-to-br from-primary via-red-700 to-primary-dark opacity-80"></div>`;
            }
        }

        // Details
        const detailsContainer = document.getElementById('profileDetails');
        const sidebarContainer = document.getElementById('sidebarDetails');
        
        let detailsHtml = '';
        let sidebarHtml = '';

        const addDetail = (icon, label, value) => {
            if (value) {
                detailsHtml += `<div class="flex items-center gap-1.5 bg-gray-50 px-3 py-1.5 rounded-full border border-gray-100"><i class="${icon} text-primary/70"></i><span>${label}: <strong>${value}</strong></span></div>`;
                sidebarHtml += `<div class="flex items-start gap-3 text-gray-600"><i class="${icon} mt-1 text-gray-400 w-4"></i><span>${label} tại <strong class="text-gray-900">${value}</strong></span></div>`;
            }
        };

        addDetail('fas fa-briefcase', 'Làm việc', targetData.workplace);
        addDetail('fas fa-graduation-cap', 'Học vấn', targetData.education);
        addDetail('fas fa-map-marker-alt', 'Sống tại', targetData.location);

        if (detailsContainer) detailsContainer.innerHTML = detailsHtml;
        if (sidebarContainer) sidebarContainer.innerHTML = sidebarHtml || '<p class="text-gray-400 italic">Chưa có thông tin giới thiệu</p>';

        if (actionArea) {
            if (targetUsername === viewer.username) {
                actionArea.innerHTML = `<button onclick="document.getElementById('editModal').classList.remove('hidden')" class="flex items-center gap-2 border border-gray-200 rounded-full px-5 py-2 text-sm font-semibold text-gray-700 hover:bg-gray-50 transition shadow-sm"><i class="fas fa-pen"></i> Chỉnh sửa</button>`;
                document.getElementById('editFullName').value = targetData.fullName || '';
                document.getElementById('editBio').value = targetData.bio || '';
                document.getElementById('editWorkplace').value = targetData.workplace || '';
                document.getElementById('editEducation').value = targetData.education || '';
                document.getElementById('editLocation').value = targetData.location || '';
                document.getElementById('editPrivacy').value = targetData.privacySetting || 'PUBLIC';
                document.getElementById('editAvatarUrl').value = targetData.avatar || '';
                document.getElementById('editCoverUrl').value = targetData.coverPhoto || '';
            } else {
                renderStrangerActions(actionArea, targetData);
            }
        }
    } catch(e) { console.error(e); }
}

function renderStrangerActions(container, targetData) {
    let btnHtml = '';
    if (targetData.friendshipStatus === 'NONE') {
        btnHtml = `<button onclick="sendFriendRequest('${targetData.username}')" class="flex items-center gap-2 bg-primary text-white rounded-full px-6 py-2 text-sm font-bold hover:bg-primary-dark transition shadow-md"><i class="fas fa-user-plus"></i> Kết bạn</button>`;
    } else if (targetData.friendshipStatus === 'REQUEST_SENT') {
        btnHtml = `<button class="flex items-center gap-2 bg-gray-100 text-gray-500 rounded-full px-5 py-2 text-sm font-bold opacity-80 cursor-not-allowed"><i class="fas fa-clock"></i> Đã gửi yêu cầu</button>`;
    } else if (targetData.friendshipStatus === 'REQUEST_RECEIVED') {
        btnHtml = `<button onclick="window.location.href='friend.html'" class="flex items-center gap-2 bg-blue-600 text-white rounded-full px-5 py-2 text-sm font-bold hover:bg-blue-700 transition shadow-md">Phản hồi yêu cầu</button>`;
    } else if (targetData.friendshipStatus === 'FRIEND') {
        btnHtml = `<button class="flex items-center gap-2 bg-gray-100 text-gray-700 rounded-full px-5 py-2 text-sm font-bold hover:bg-gray-200 transition"><i class="fas fa-user-check text-green-600"></i> Bạn bè</button>`;
    }
    container.innerHTML = btnHtml + `<button onclick="window.location.href='chat.html?target=${targetData.username}'" class="flex items-center gap-2 border border-gray-200 rounded-full px-5 py-2 text-sm font-semibold text-gray-700 hover:bg-gray-50 transition shadow-sm"><i class="fab fa-facebook-messenger"></i> Nhắn tin</button>`;
}

async function sendFriendRequest(targetUsername) {
    const user = checkAuth();
    try {
        const res = await fetch(`${API_URL}/friends/request`, {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ receiverUsername: targetUsername })
        });
        if (res.ok) {
            alert('Đã gửi lời mời kết bạn!');
            loadProfileDynamic(user, targetUsername);
        } else {
            const errText = await res.text();
            alert('Lỗi: ' + errText);
        }
    } catch(e) { console.error(e); alert('Lỗi kết nối.'); }
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
            body: JSON.stringify({ reactionType: 'LIKE' })
        });
        const urlParams = new URLSearchParams(window.location.search);
        let targetUsername = urlParams.get('username') || user.username;
        loadPosts(user, targetUsername);
    } catch (e) { console.error(e); }
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

async function loadPosts(viewer, targetUsername) {
    try {
        const res = await fetch(`${API_URL}/posts/user/${encodeURIComponent(targetUsername)}`);
        const userPosts = await res.json();
        const container = document.getElementById('profilePosts');
        if (!container) return;
        
        container.innerHTML = '';
        if (!userPosts.length) {
            container.innerHTML = `<div class="bg-white rounded-2xl shadow-sm p-10 text-center"><p class="text-gray-400 text-sm">Chưa có bài viết nào</p></div>`;
            return;
        }

        userPosts.forEach(post => {
            const initials = (post.fullName || post.username || '?').charAt(0).toUpperCase();
            
            // Reaction Logic from PTIT
            const currentReaction = post.currentReaction;
            let totalReactionCount = 0;
            if (post.reactionCounts) {
                Object.values(post.reactionCounts).forEach(count => { totalReactionCount += count; });
            } else { totalReactionCount = post.likeCount || 0; }
            
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

            const isOwner = post.username === viewer.username;
            const liked = post.liked;
            const likeCount = post.likeCount || 0;

            div.innerHTML = `
                <div class="p-5">
                    <div class="flex items-center gap-3 mb-3">
                        <a href="profile.html?username=${post.username}" class="w-10 h-10 rounded-full bg-primary flex items-center justify-center text-white font-bold text-sm overflow-hidden hover:opacity-80 transition no-underline">
                            ${post.avatar ? `<img src="${post.avatar}" class="w-full h-full object-cover">` : initials}
                        </a>
                        <div class="flex-1 min-w-0">
                            <a href="profile.html?username=${post.username}" class="text-sm font-bold text-gray-900 hover:underline no-underline">${post.fullName}</a>
                            <p class="text-[11px] text-gray-400 font-medium uppercase">${formatTime(post.createdAt)}</p>
                        </div>
                        ${isOwner ? `<button onclick="deletePost(${post.id})" class="text-gray-300 hover:text-red-500 transition text-sm px-2"><i class="fas fa-trash"></i></button>` : ''}
                    </div>
                    <p class="text-gray-800 text-sm leading-relaxed mb-3">${post.content}</p>
                    ${mediaHtml}
                    <div class="flex items-center justify-between text-xs text-gray-500 mb-3 px-1 border-b border-gray-100 pb-3">
                        <div class="flex items-center gap-1 cursor-pointer hover:underline" onclick="showReactionList(${post.id})">
                            <span class="text-primary bg-primary bg-opacity-10 rounded-full w-5 h-5 flex items-center justify-center text-[10px]"><i class="fas fa-thumbs-up"></i></span>
                            <span>${totalReactionCount} lượt thích</span>
                        </div>
                        <div class="cursor-pointer hover:underline" onclick="toggleComments(${post.id})"><span>${(post.comments || []).length} bình luận</span></div>
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
        });
    } catch (e) { console.error(e); }
}

function toggleComments(postId) {
    const el = document.getElementById(`comments-${postId}`);
    if (el) el.classList.toggle('hidden');
}

async function reactToPost(postId, reactionType) {
    const user = checkAuth();
    try {
        await fetch(`${API_URL}/posts/${postId}/like`, {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ reactionType })
        });
        const urlParams = new URLSearchParams(window.location.search);
        let targetUsername = urlParams.get('username') || user.username;
        loadPosts(user, targetUsername);
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
            body: JSON.stringify({ content, parentCommentId: parentId }) 
        });
        input.value = '';
        input.dataset.parentId = '';
        input.placeholder = 'Viết bình luận...';
        const urlParams = new URLSearchParams(window.location.search);
        let targetUsername = urlParams.get('username') || user.username;
        loadPosts(user, targetUsername);
    } catch (e) { console.error(e); }
}

async function deletePost(postId) {
    if (!confirm('Xóa bài viết này?')) return;
    try {
        await fetch(`${API_URL}/posts/${postId}`, { method: 'DELETE' });
        document.getElementById(`post-${postId}`).remove();
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

async function uploadFile(fileInput, endpoint) {
    if (!fileInput.files || fileInput.files.length === 0) return null;
    const formData = new FormData();
    formData.append('imageFile', fileInput.files[0]);
    const res = await fetch(`${API_URL}/upload/${endpoint}`, { method: 'POST', body: formData });
    if (res.ok) {
        const data = await res.json();
        return data.imageUrl;
    }
    return null;
}

async function saveProfile() {
    const user = checkAuth();
    const btn = document.querySelector('#editModal button[onclick="saveProfile()"]');
    try {
        btn.disabled = true;
        btn.innerHTML = '<i class="fas fa-spinner fa-spin"></i>';
        
        const avatarFile = document.getElementById('editAvatarFile');
        const coverFile = document.getElementById('editCoverFile');
        
        const [newAv, newCv] = await Promise.all([
            uploadFile(avatarFile, 'avatar'), 
            uploadFile(coverFile, 'cover')
        ]);

        const updateData = {
            fullName: document.getElementById('editFullName').value,
            bio: document.getElementById('editBio').value,
            workplace: document.getElementById('editWorkplace').value,
            education: document.getElementById('editEducation').value,
            location: document.getElementById('editLocation').value,
            privacySetting: document.getElementById('editPrivacy').value,
            avatar: newAv || document.getElementById('editAvatarUrl').value || '',
            coverPhoto: newCv || document.getElementById('editCoverUrl').value || ''
        };

        const res = await fetch(`${API_URL}/users/profile`, {
            method: 'PUT',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify(updateData)
        });

        if (res.ok) {
            const result = await res.json();
            localStorage.setItem('user', JSON.stringify({ ...user, fullName: result.fullName, avatar: result.avatar }));
            window.location.reload();
        }
    } catch (e) { console.error(e); alert('Lỗi kết nối'); }
    finally { btn.disabled = false; btn.textContent = 'Lưu thay đổi'; }
}

// Thêm vào cuối file profile.js
window.switchTab = function(tabName) {
    const btnPosts = document.getElementById('tabPostsBtn');
    const btnFriends = document.getElementById('tabFriendsBtn');
    const contentPosts = document.getElementById('tabPostsContent');
    const contentFriends = document.getElementById('tabFriendsContent');

    if (tabName === 'posts') {
        btnPosts.classList.add('text-primary', 'border-primary');
        btnPosts.classList.remove('text-gray-500', 'border-transparent');
        btnFriends.classList.remove('text-primary', 'border-primary');
        btnFriends.classList.add('text-gray-500', 'border-transparent');
        contentPosts.classList.remove('hidden');
        contentFriends.classList.add('hidden');
    } else {
        btnFriends.classList.add('text-primary', 'border-primary');
        btnFriends.classList.remove('text-gray-500', 'border-transparent');
        btnPosts.classList.remove('text-primary', 'border-primary');
        btnPosts.classList.add('text-gray-500', 'border-transparent');
        contentFriends.classList.remove('hidden');
        contentPosts.classList.add('hidden');
        
        // Load friends if needed
        const urlParams = new URLSearchParams(window.location.search);
        let targetUsername = urlParams.get('username') || checkAuth().username;
        loadProfileFriends(targetUsername);
    }
};

async function loadProfileFriends(username) {
    try {
        const res = await fetch(`${API_URL}/friends?username=${encodeURIComponent(username)}`);
        const friends = await res.json();
        
        const countEl = document.getElementById('profileFriendCount');
        if(countEl) countEl.textContent = friends.length;

        const container = document.getElementById('profileFriendsList');
        if (!container) return;

        if (friends.length === 0) {
            container.innerHTML = `<p class="text-gray-400 text-sm col-span-2 text-center py-8">Chưa có bạn bè nào.</p>`;
            return;
        }

        container.innerHTML = friends.map(f => {
            const initial = (f.fullName || f.username || '?').charAt(0).toUpperCase();
            return `
                <div class="flex items-center gap-3 p-4 border border-gray-100 rounded-xl bg-gray-50/50 hover:bg-gray-50 transition">
                    <a href="profile.html?username=${f.username}" class="w-16 h-16 rounded-xl bg-primary flex items-center justify-center text-white font-bold text-xl flex-shrink-0 overflow-hidden shadow-sm hover:opacity-90">
                        ${f.avatar ? `<img src="${f.avatar}" class="w-full h-full object-cover">` : initial}
                    </a>
                    <div class="flex-1 min-w-0">
                        <a href="profile.html?username=${f.username}" class="font-bold text-gray-900 hover:underline text-base truncate block">${f.fullName}</a>
                        <p class="text-xs text-gray-500 mt-1">@${f.username}</p>
                    </div>
                </div>
            `;
        }).join('');
    } catch(e) {
        console.error(e);
        document.getElementById('profileFriendsList').innerHTML = `<p class="text-red-400 text-sm col-span-2 text-center py-8">Lỗi tải danh sách bạn bè.</p>`;
    }
}

