var API_URL = '/api';
let stompClient = null;
let currentChatUser = null;
let currentConversationId = null;
let groupSubscriptions = new Map();

document.addEventListener('DOMContentLoaded', () => {
    const user = checkAuth();
    setNavAvatar(user);
    connectWebSocket();
    setupMessageForm();

    if ('Notification' in window && Notification.permission === 'default') {
        Notification.requestPermission();
    }

    // Image preview setup
    const chatImageInput = document.getElementById('chatImageInput');
    const previewContainer = document.getElementById('imagePreviewContainer');
    const previewImg = document.getElementById('chatImagePreview');
    const cancelBtn = document.getElementById('cancelImagePreview');
    
    if (chatImageInput) {
        chatImageInput.addEventListener('change', function() {
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
                chatImageInput.value = '';
                previewImg.src = '';
                previewContainer.classList.add('hidden');
            });
        }
    }
});

function setNavAvatar(user) {
    const el = document.getElementById('navAvatar');
    if (!el) return;
    const initial = escapeHtml((user.fullName || user.username || 'U').charAt(0).toUpperCase());
    if (user.avatar) {
        el.innerHTML = `<img src="${escapeHtml(user.avatar)}" class="w-full h-full object-cover rounded-full" onerror="this.parentElement.textContent='${initial}'">`;
    } else {
        el.textContent = initial;
    }
}

function formatMessageTime(dateStr) {
    if (!dateStr) return '';
    return new Date(dateStr).toLocaleTimeString('vi-VN', { hour: '2-digit', minute: '2-digit' });
}

function connectWebSocket() {
    const socket = new SockJS('/ws');
    stompClient = Stomp.over(socket);
    stompClient.debug = null;
    stompClient.connect({}, () => {
        const user = checkAuth();
        stompClient.subscribe(`/topic/messages/${user.username}`, (message) => {
            const msg = JSON.parse(message.body);
            const user = checkAuth();
            
            // Xử lý xác nhận ID từ server cho tin nhắn vừa gửi (Personal Chat)
            if (msg.senderUsername === user.username && !msg.conversationId && !msg.type) {
                const pending = document.querySelector('.pending-msg');
                if (pending) {
                    pending.id = `msg-${msg.id}`;
                    pending.classList.remove('pending-msg');
                    const btn = pending.querySelector('button');
                    if (btn) btn.setAttribute('onclick', `recallMessage(${msg.id})`);
                }
                return;
            }
            if (msg.type === 'UNFRIENDED' || msg.type === 'UNFRIENDED_SELF') {
                if (msg.partnerUsername === currentChatUser) {
                    document.getElementById('chatInputArea').classList.add('hidden');
                    document.getElementById('notFriendPlaceholder').classList.remove('hidden');
                }
                return;
            }
            if (msg.type === 'MESSAGE_RECALLED') {
                const msgEl = document.getElementById(`msg-${msg.messageId}`);
                if (msgEl) {
                    const contentDiv = msgEl.querySelector('.rounded-2xl');
                    if (contentDiv) {
                        const timeEl = contentDiv.querySelector('.msg-time');
                        const timeHtml = timeEl ? timeEl.outerHTML : '';
                        contentDiv.innerHTML = `<div class="italic text-gray-500 text-xs py-1">Tin nhắn đã bị thu hồi</div>` + timeHtml;
                        contentDiv.className = 'max-w-xs lg:max-w-md px-4 py-2.5 rounded-2xl text-sm leading-relaxed bg-gray-100 border border-gray-200 text-gray-500';
                        // Ẩn nút thu hồi nếu còn hiện
                        const btn = msgEl.querySelector('button');
                        if (btn) btn.remove();
                    }
                }
                return;
            }

            if (msg.senderUsername === currentChatUser) {
                appendMessage(msg, 'received');
            } else {
                showUnreadBadge(msg.senderUsername);
                showNotification(msg);
            }
        });

        // Restore last chat on reload
        const savedUser = sessionStorage.getItem('currentChatUser');
        const savedName = sessionStorage.getItem('currentChatFullName');
        const savedGroup = sessionStorage.getItem('currentConversationId');
        if (savedGroup) {
            selectGroupChat(savedGroup, savedName);
        } else if (savedUser && savedName) {
            selectChat(savedUser, savedName);
        }

        // Subscribe to groups after connection
        loadFriends();
    }, (err) => {
        setTimeout(connectWebSocket, 3000);
    });
}

function showNotification(msg) {
    if ('Notification' in window && Notification.permission === 'granted') {
        new Notification(`Tin nhắn từ ${escapeHtml(msg.senderUsername)}`, { body: msg.content });
    }
}

function showUnreadBadge(username) {
    const items = document.querySelectorAll('.chat-contact-item');
    items.forEach(item => {
        if (item.dataset.username === username) {
            if (!item.querySelector('.unread-dot')) {
                const dot = document.createElement('div');
                dot.className = 'unread-dot w-2.5 h-2.5 bg-primary rounded-full ml-auto flex-shrink-0';
                item.appendChild(dot);
            }
        }
    });
}

async function loadFriends() {
    const user = checkAuth();
    const list = document.getElementById('friendsList');
    try {
        const res = await fetch(`${API_URL}/chat/contacts`);
        const friends = await res.json();
        
        const groupRes = await fetch(`${API_URL}/chat/groups`);
        let groups = [];
        if (groupRes.ok) groups = await groupRes.json();

        list.innerHTML = '';

        if (!friends.length && !groups.length) {
            list.innerHTML = `<p class="text-center text-gray-400 text-sm py-8">Chưa có bạn bè và nhóm.<br>Hãy kết bạn hoặc tạo nhóm!</p>`;
            return;
        }

        groups.forEach(group => {
            subscribeToGroup(group.id);
            const initial = escapeHtml((group.name || '?').charAt(0).toUpperCase());
            const div = document.createElement('div');
            div.className = 'chat-contact-item flex items-center gap-3 p-3 rounded-xl cursor-pointer hover:bg-gray-50 transition';
            div.dataset.groupId = group.id;
            div.onclick = () => selectGroupChat(group.id, group.name);
            div.innerHTML = `
                <div class="w-10 h-10 rounded-full bg-blue-100 text-blue-600 flex items-center justify-center font-bold text-sm flex-shrink-0">${initial}</div>
                <div class="flex-1 min-w-0">
                    <p class="font-semibold text-sm text-gray-900 truncate">${escapeHtml(group.name)}</p>
                    <p class="text-xs text-gray-400 truncate">Nhóm trò chuyện</p>
                </div>
            `;
            list.appendChild(div);
        });

        friends.forEach(friend => {
            const initial = escapeHtml((friend.fullName || friend.username || '?').charAt(0).toUpperCase());
            const div = document.createElement('div');
            div.className = 'chat-contact-item flex items-center gap-3 p-3 rounded-xl cursor-pointer hover:bg-gray-50 transition';
            div.dataset.username = friend.username;
            div.onclick = () => selectChat(friend.username, friend.fullName);
            div.innerHTML = `
                <div class="w-10 h-10 rounded-full bg-primary flex items-center justify-center text-white font-bold text-sm flex-shrink-0">${initial}</div>
                <div class="flex-1 min-w-0">
                    <p class="font-semibold text-sm text-gray-900 truncate">${escapeHtml(friend.fullName)}</p>
                    <p class="text-xs text-gray-400 truncate">@${escapeHtml(friend.username)}</p>
                </div>
            `;
            list.appendChild(div);
        });
    } catch (e) { console.error(e); }
}

function subscribeToGroup(convId) {
    if (stompClient && stompClient.connected && !groupSubscriptions.has(convId)) {
        const sub = stompClient.subscribe(`/topic/conversation/${convId}`, (message) => {
            const msg = JSON.parse(message.body);
            if (msg.conversationId == currentConversationId) {
                appendMessage(msg, msg.senderUsername === checkAuth().username ? 'sent' : 'received');
            } else {
                showUnreadBadgeForGroup(msg.conversationId);
                showNotification(msg);
            }
        });
        groupSubscriptions.set(convId, sub);
    }
}

function showUnreadBadgeForGroup(convId) {
    const items = document.querySelectorAll('.chat-contact-item');
    items.forEach(item => {
        if (item.dataset.groupId == convId) {
            if (!item.querySelector('.unread-dot')) {
                const dot = document.createElement('div');
                dot.className = 'unread-dot w-2.5 h-2.5 bg-primary rounded-full ml-auto flex-shrink-0';
                item.appendChild(dot);
            }
        }
    });
}

async function selectChat(username, fullName) {
    currentChatUser = username;
    currentConversationId = null; // Clear group chat
    sessionStorage.setItem('currentChatUser', username);
    sessionStorage.removeItem('currentConversationId');
    sessionStorage.setItem('currentChatFullName', fullName);

    // Update header
    document.getElementById('chatTitle').textContent = fullName;
    document.getElementById('chatTitle').parentElement.setAttribute('href', `profile.html?username=${encodeURIComponent(username)}`);
    const partnerAv = document.getElementById('chatPartnerAvatar');
    if (partnerAv) {
        partnerAv.textContent = (fullName || username).charAt(0).toUpperCase();
        partnerAv.className = 'w-9 h-9 bg-primary rounded-full flex items-center justify-center text-white font-bold text-sm';
    }
    const btn = document.getElementById('groupMembersBtn');
    if (btn) btn.classList.add('hidden');
    document.getElementById('chatWindowHeader').classList.remove('hidden');
    // Fetch history and friend status
    const user = checkAuth();
    try {
        const res = await fetch(`${API_URL}/chat/history?targetUsername=${encodeURIComponent(username)}`);
        const data = await res.json();
        const history = data.messages || [];

        // Update UI based on friend status
        if (data.isFriend === false) {
            document.getElementById('chatInputArea').classList.add('hidden');
            if (document.getElementById('notFriendPlaceholder')) {
                document.getElementById('notFriendPlaceholder').classList.remove('hidden');
            }
        } else {
            document.getElementById('chatInputArea').classList.remove('hidden');
            if (document.getElementById('notFriendPlaceholder')) {
                document.getElementById('notFriendPlaceholder').classList.add('hidden');
            }
        }

        // Clear messages right before appending to prevent overlap
        const msgs = document.getElementById('chatMessages');
        msgs.innerHTML = '';

    // Highlight active contact
    document.querySelectorAll('.chat-contact-item').forEach(item => {
        if (item.dataset.username === username) {
            item.classList.add('bg-red-50');
            const dot = item.querySelector('.unread-dot');
            if (dot) dot.remove();
        } else {
            item.classList.remove('bg-red-50');
            item.classList.remove('bg-blue-50');
        }
    });

        history.forEach(msg => {
            const type = msg.senderUsername === user.username ? 'sent' : 'received';
            appendMessage(msg, type);
        });
    } catch (e) { console.error(e); }
}

async function selectGroupChat(groupId, groupName) {
    currentConversationId = groupId;
    currentChatUser = null; // Clear personal chat
    sessionStorage.setItem('currentConversationId', groupId);
    sessionStorage.removeItem('currentChatUser');
    sessionStorage.setItem('currentChatFullName', groupName);

    // Update header
    document.getElementById('chatTitle').textContent = groupName;
    const partnerAv = document.getElementById('chatPartnerAvatar');
    if (partnerAv) {
        partnerAv.textContent = escapeHtml((groupName || '?').charAt(0).toUpperCase());
        partnerAv.className = 'w-9 h-9 bg-blue-600 rounded-full flex items-center justify-center text-white font-bold text-sm';
    }
    const btn = document.getElementById('groupMembersBtn');
    if (btn) btn.classList.remove('hidden');
    document.getElementById('chatWindowHeader').classList.remove('hidden');
    document.getElementById('chatInputArea').classList.remove('hidden');
    if (document.getElementById('notFriendPlaceholder')) {
        document.getElementById('notFriendPlaceholder').classList.add('hidden');
    }

    const msgs = document.getElementById('chatMessages');
    msgs.innerHTML = '<p class="text-center text-gray-400 text-xs py-4">Đang tải lịch sử...</p>';

    // Highlight active contact
    document.querySelectorAll('.chat-contact-item').forEach(item => {
        if (item.dataset.groupId == groupId) {
            item.classList.add('bg-blue-50');
            const dot = item.querySelector('.unread-dot');
            if (dot) dot.remove();
        } else {
            item.classList.remove('bg-blue-50');
            item.classList.remove('bg-red-50'); // clear others
        }
    });

    try {
        const res = await fetch(`${API_URL}/chat/group-history?conversationId=${groupId}`);
        const data = await res.json();
        const history = data.messages || [];

        msgs.innerHTML = '';
        history.forEach(msg => {
            const user = checkAuth();
            const type = msg.senderUsername === user.username ? 'sent' : 'received';
            appendMessage(msg, type);
        });
    } catch (e) { console.error(e); }
}

function appendMessage(message, type) {
    const msgs = document.getElementById('chatMessages');
    const timeStr = formatMessageTime(message.timestamp);
    const div = document.createElement('div');
    div.className = `flex ${type === 'sent' ? 'justify-end' : 'justify-start'} mb-1 relative group/msg`;
    if (message.id) div.id = `msg-${message.id}`;
    else if (type === 'sent') div.classList.add('pending-msg');

    let contentHtml = "";
    if (message.isRevoked) {
        contentHtml = `<div class="italic text-gray-500 text-xs py-1">Tin nhắn đã bị thu hồi</div>`;
    } else {
        const img = message.imageUrl ? `<img src="${escapeHtml(message.imageUrl)}" class="rounded-lg mb-2 max-w-full" style="max-height: 200px; object-fit: cover;">` : '';
        const txt = message.content ? `<div>${escapeHtml(message.content)}</div>` : '';
        contentHtml = img + txt;
    }

    let senderHtml = '';
    if (type === 'received' && currentConversationId && message.senderUsername) {
        senderHtml = `<div class="text-[10px] text-gray-400 mb-0.5 ml-1">@${escapeHtml(message.senderUsername)}</div>`;
    }

    const recallBtn = (type === 'sent' && !message.isRevoked && !currentConversationId)
        ? `<button onclick="recallMessage(${message.id})" class="absolute top-1/2 -left-8 -translate-y-1/2 opacity-0 group-hover/msg:opacity-100 text-gray-400 hover:text-red-500 transition p-1" title="Thu hồi tin nhắn">
             <i class="fas fa-undo-alt text-xs"></i>
           </button>`
        : '';

    div.innerHTML = `
        <div class="relative flex flex-col ${type === 'sent' ? 'items-end' : 'items-start'}">
            ${senderHtml}
            <div class="relative">
                ${recallBtn}
                <div class="max-w-xs lg:max-w-md px-4 py-2.5 rounded-2xl text-sm leading-relaxed
                    ${message.isRevoked ? 'bg-gray-100 border border-gray-200 text-gray-500' : (type === 'sent' ? 'bg-primary text-white rounded-br-sm' : 'bg-white text-gray-800 shadow-sm rounded-bl-sm')}">
                    ${contentHtml}
                    <div class="msg-time text-[11px] mt-1 ${type === 'sent' ? 'text-red-200' : 'text-gray-400'}">${timeStr}</div>
                </div>
            </div>
        </div>`;
    msgs.appendChild(div);
    msgs.scrollTop = msgs.scrollHeight;
}

async function recallMessage(messageId) {
    if (!confirm("Bạn có chắc chắn muốn thu hồi tin nhắn này? Cả hai phía đều sẽ không thấy nội dung này nữa.")) return;
    const user = checkAuth();
    try {
        await fetch(`${API_URL}/chat/revoke/${messageId}`, { method: 'PUT' });
        // WebSocket event MESSAGE_RECALLED will trigger UI update, no need to call selectChat() here
    } catch (e) { console.error("Recall error:", e); }
}

async function clearHistory() {
    if (!currentChatUser) return;
    if (!confirm(`Bạn có chắc muốn xóa toàn bộ lịch sử trò chuyện với ${currentChatUser}? Lưu ý: Chỉ xóa phía bạn, đối phương vẫn thấy.`)) return;
    const user = checkAuth();
    try {
        await fetch(`${API_URL}/chat/history/${currentChatUser}`, { method: 'DELETE' });
        document.getElementById('chatMessages').innerHTML = '';
        selectChat(currentChatUser, document.getElementById('chatTitle').textContent);
    } catch (e) { console.error("Clear history error:", e); }
}

function setupMessageForm() {
    const form = document.getElementById('messageForm');
    if (!form) return;
    form.addEventListener('submit', async e => {
        e.preventDefault();
        if (!currentChatUser && !currentConversationId) return;
        const user = checkAuth();
        const content = document.getElementById('messageInput').value.trim();
        const imageFile = document.getElementById('chatImageInput') ? document.getElementById('chatImageInput').files[0] : null;
        
        if (!content && !imageFile) return;

        let imageUrl = null;
        if (imageFile) {
             const formData = new FormData();
             formData.append('imageFile', imageFile);
             try {
                 const uploadRes = await fetch(`${API_URL}/upload/chat-image`, { method: 'POST', body: formData });
                 if (uploadRes.ok) {
                     const uploadData = await uploadRes.json();
                     if (uploadData.status === 'ok') imageUrl = uploadData.imageUrl;
                 }
             } catch (err) {}
        }

        if (!stompClient) return;

        const message = {
            senderUsername: user.username,
            receiverUsername: currentChatUser,
            conversationId: currentConversationId,
            content,
            imageUrl: imageUrl,
            timestamp: new Date().toISOString()
        };
        const topic = currentConversationId ? '/app/chat/group' : '/app/chat';
        stompClient.send(topic, {}, JSON.stringify(message));
        // Chỉ append ngay lập tức nếu là chat cá nhân
        if (!currentConversationId) {
            appendMessage(message, 'sent');
        }
        document.getElementById('messageInput').value = '';
        
        if (document.getElementById('chatImageInput')) {
            document.getElementById('chatImageInput').value = '';
            document.getElementById('chatImagePreview').src = '';
            document.getElementById('imagePreviewContainer').classList.add('hidden');
        }
    });
}

function openCreateGroupModal() {
    document.getElementById("groupModal").classList.remove("hidden");
    loadFriendsForSelection();
}

function closeGroupModal() {
    document.getElementById('groupModal').classList.add('hidden');
}

async function openGroupMembersModal() {
    if (!currentConversationId) return;
    document.getElementById('groupMembersModal').classList.remove('hidden');
    const list = document.getElementById('groupMembersList');
    list.innerHTML = '<p class="text-center text-gray-400 text-sm py-4">Đang tải...</p>';
    
    try {
        const res = await fetch(`${API_URL}/chat/group/${currentConversationId}/members`);
        if (res.ok) {
            const members = await res.json();
            document.getElementById('groupMembersCount').textContent = members.length;
            list.innerHTML = '';
            members.forEach(member => {
                const initial = escapeHtml((member.fullName || member.username || '?').charAt(0).toUpperCase());
                const time = new Date(member.joinedAt).toLocaleDateString('vi-VN');
                const avatar = member.avatar ? `<img src="${escapeHtml(member.avatar)}" class="w-10 h-10 rounded-full object-cover flex-shrink-0">` 
                                             : `<div class="w-10 h-10 rounded-full bg-primary flex items-center justify-center text-white font-bold text-sm flex-shrink-0">${initial}</div>`;
                
                list.innerHTML += `
                    <div class="flex items-center gap-3 p-2 hover:bg-gray-50 rounded-xl transition">
                        ${avatar}
                        <div class="flex-1 min-w-0">
                            <a href="profile.html?username=${encodeURIComponent(member.username)}" class="font-semibold text-sm text-gray-900 hover:underline truncate block">
                                ${escapeHtml(member.fullName)}
                            </a>
                            <p class="text-xs text-gray-500 truncate">@${escapeHtml(member.username)} · Đã tham gia: ${time}</p>
                        </div>
                    </div>
                `;
            });
        } else {
            list.innerHTML = '<p class="text-center text-red-500 text-sm py-4">Lỗi khi tải danh sách thành viên</p>';
        }
    } catch (e) {
        console.error(e);
        list.innerHTML = '<p class="text-center text-red-500 text-sm py-4">Lỗi khi tải danh sách thành viên</p>';
    }
}

function closeGroupMembersModal() {
    document.getElementById('groupMembersModal').classList.add('hidden');
}


async function loadFriendsForSelection() {
    const user = checkAuth();
    const container = document.getElementById("friendsSelectionList");
    try {
        const res = await fetch(`${API_URL}/chat/contacts`);
        const friends = await res.json();
        container.innerHTML = friends.map(f => `
            <label class="flex items-center gap-3 p-2 hover:bg-gray-50 rounded-lg cursor-pointer transition">
                <input type="checkbox" name="groupMember" value="${escapeHtml(f.username)}" class="w-4 h-4 rounded text-primary border-gray-300">
                <div class="w-8 h-8 rounded-full bg-gray-200 flex items-center justify-center text-xs font-bold">${escapeHtml((f.fullName||f.username).charAt(0).toUpperCase())}</div>
                <span class="text-sm font-medium text-gray-700">${escapeHtml(f.fullName)}</span>
            </label>
        `).join("");
    } catch(e) { console.error(e); }
}

async function submitCreateGroup() {
    const user = checkAuth();
    const name = document.getElementById("groupNameInput").value.trim();
    const usernames = Array.from(document.querySelectorAll("input[name='groupMember']:checked")).map(cb => cb.value);
    if (!name || usernames.length < 1) { alert("Vui lng nh?p tn v ch?n t nh?t 1 b?n"); return; }
    usernames.push(user.username);
    try {
        const res = await fetch(`${API_URL}/chat/group`, {
            method: "POST",
            headers: { "Content-Type": "application/json" },
            body: JSON.stringify({ name, usernames })
        });
        if (res.ok) { 
            closeGroupModal(); 
            document.getElementById("groupNameInput").value = "";
            loadFriends(); 
        }
    } catch(e) { console.error(e); }
}

