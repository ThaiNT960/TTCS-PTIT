var API_URL = window.location.origin + '/api';
let stompClient = null;
let currentChatUser = null;
let currentConversationId = null;
let groupSubscriptions = new Map();
let conversations = [];
let allGroupDocuments = [];
let activeDocCategory = '';
let docSearchQuery = '';

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

    // Đăng ký sự kiện tìm kiếm tài liệu trong tab Nhóm
    const searchDocInput = document.getElementById('searchDocInput');
    if (searchDocInput) {
        searchDocInput.addEventListener('input', (e) => {
            docSearchQuery = e.target.value;
            renderFilteredDocuments();
        });
    }
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

function formatMessageTime(dateStr) {
    if (!dateStr) return '';
    return new Date(dateStr).toLocaleTimeString('vi-VN', { hour: '2-digit', minute: '2-digit' });
}

function escapeHtml(text) {
    const div = document.createElement('div');
    div.textContent = text;
    return div.innerHTML;
}

function connectWebSocket() {
    const socket = new SockJS('http://localhost:8080/ws');
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
                updateConversationMsg(msg.receiverUsername, false, msg);
                return;
            }
            if (msg.type === 'UNFRIENDED' || msg.type === 'UNFRIENDED_SELF') {
                if (msg.partnerUsername === currentChatUser) {
                    document.getElementById('chatInputArea').classList.add('hidden');
                    if (document.getElementById('notFriendPlaceholder')) {
                        document.getElementById('notFriendPlaceholder').textContent = 'Bạn không thể nhắn tin do không còn là bạn bè.';
                        document.getElementById('notFriendPlaceholder').classList.remove('hidden');
                    }
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
                const partner = msg.senderUsername === user.username ? msg.receiverUsername : msg.senderUsername;
                updateConversationMsg(partner, false, msg);
                return;
            }

            if (msg.senderUsername === currentChatUser) {
                appendMessage(msg, 'received');
            } else {
                showNotification(msg);
            }
            updateConversationMsg(msg.senderUsername, false, msg);
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
        new Notification(`Tin nhắn từ ${msg.senderUsername}`, { body: msg.content });
    }
}

function renderConversations() {
    const list = document.getElementById('friendsList');
    if (!list) return;
    list.innerHTML = '';

    if (!conversations.length) {
        list.innerHTML = `<p class="text-center text-gray-400 text-sm py-8">Chưa có bạn bè và nhóm.<br>Hãy kết bạn hoặc tạo nhóm!</p>`;
        return;
    }

    conversations.forEach(conv => {
        const initial = (conv.name || '?').charAt(0).toUpperCase();
        const div = document.createElement('div');
        
        let activeClass = '';
        if (conv.isGroup) {
            if (currentConversationId == conv.id) activeClass = 'bg-blue-50';
        } else {
            if (currentChatUser === conv.username) activeClass = 'bg-red-50';
        }
            
        div.className = `chat-contact-item flex items-center gap-3 p-3 rounded-xl cursor-pointer hover:bg-gray-50 transition ${activeClass}`;
        
        let avatarHtml = '';
        if (conv.isGroup) {
            avatarHtml = conv.avatar 
                ? `<div class="relative w-10 h-10 flex-shrink-0"><img src="${escapeHtml(conv.avatar)}" class="w-10 h-10 rounded-full object-cover"></div>`
                : `<div class="w-10 h-10 rounded-full bg-blue-100 text-blue-600 flex items-center justify-center font-bold text-sm flex-shrink-0">${initial}</div>`;
            div.dataset.groupId = conv.id;
            div.onclick = () => selectGroupChat(conv.id, conv.name);
        } else {
            let onlineDot = '';
            if (conv.isOnline) {
                onlineDot = `<span class="absolute bottom-0 right-0 w-3 h-3 bg-green-500 rounded-full border-2 border-white"></span>`;
            }
            avatarHtml = conv.avatar
                ? `<div class="relative w-10 h-10 flex-shrink-0"><img src="${escapeHtml(conv.avatar)}" class="w-10 h-10 rounded-full object-cover">${onlineDot}</div>`
                : `<div class="relative w-10 h-10 flex-shrink-0"><div class="w-10 h-10 rounded-full bg-primary flex items-center justify-center text-white font-bold text-sm">${initial}</div>${onlineDot}</div>`;
            div.dataset.username = conv.username;
            div.onclick = () => selectChat(conv.username, conv.name);
        }
        
        let unreadHtml = '';
        if (conv.unread) {
            unreadHtml = `<div class="unread-dot w-2.5 h-2.5 bg-primary rounded-full ml-auto flex-shrink-0"></div>`;
        }

        const subtext = conv.lastMessage || (conv.isGroup ? "Nhóm trò chuyện" : `@${conv.username}`);

        div.innerHTML = `
            ${avatarHtml}
            <div class="flex-1 min-w-0">
                <p class="font-semibold text-sm text-gray-900 truncate">${conv.name}</p>
                <p class="text-xs text-gray-400 truncate">${escapeHtml(subtext)}</p>
            </div>
            ${unreadHtml}
        `;
        list.appendChild(div);
    });
}

function updateConversationMsg(idOrUsername, isGroup, msg) {
    const conv = conversations.find(c => {
        if (isGroup) {
            return c.isGroup && c.id == idOrUsername;
        } else {
            return !c.isGroup && c.username === idOrUsername;
        }
    });

    if (conv) {
        let preview = msg.content;
        if (msg.isRevoked || msg.type === 'MESSAGE_RECALLED') {
            preview = "[Tin nhắn đã bị thu hồi]";
        } else if (msg.imageUrl && (!msg.content || msg.content.trim() === '')) {
            preview = "[Hình ảnh]";
        }
        conv.lastMessage = preview;
        conv.lastTimestamp = msg.timestamp ? new Date(msg.timestamp) : new Date();
        
        if (isGroup) {
            if (currentConversationId != conv.id) {
                conv.unread = true;
            }
        } else {
            if (currentChatUser !== conv.username) {
                conv.unread = true;
            }
        }

        conversations.sort((a, b) => b.lastTimestamp - a.lastTimestamp);
        renderConversations();
    } else {
        loadFriends();
    }
}

async function loadFriends() {
    try {
        const res = await fetch(`${API_URL}/chat/contacts`);
        const friends = await res.json();
        
        const groupRes = await fetch(`${API_URL}/chat/groups`);
        let groups = [];
        if (groupRes.ok) groups = await groupRes.json();

        conversations = [];

        groups.forEach(group => {
            subscribeToGroup(group.id);
            conversations.push({
                id: group.id,
                isGroup: true,
                name: group.name,
                avatar: group.avatar,
                lastMessage: group.lastMessage || "Nhóm trò chuyện",
                lastTimestamp: group.lastTimestamp ? new Date(group.lastTimestamp) : new Date(0),
                unread: false
            });
        });

        friends.forEach(friend => {
            conversations.push({
                username: friend.username,
                isGroup: false,
                name: friend.fullName,
                avatar: friend.avatar,
                lastMessage: friend.lastMessage,
                lastTimestamp: friend.lastTimestamp ? new Date(friend.lastTimestamp) : new Date(0),
                isOnline: friend.isOnline,
                unread: false
            });
        });

        conversations.sort((a, b) => b.lastTimestamp - a.lastTimestamp);
        renderConversations();
    } catch (e) { console.error(e); }
}

function subscribeToGroup(convId) {
    if (stompClient && stompClient.connected && !groupSubscriptions.has(convId)) {
        const sub = stompClient.subscribe(`/topic/conversation/${convId}`, (message) => {
            const msg = JSON.parse(message.body);
            if (msg.type === 'MESSAGE_RECALLED') {
                const msgEl = document.getElementById(`msg-${msg.messageId}`);
                if (msgEl) {
                    const contentDiv = msgEl.querySelector('.rounded-2xl');
                    if (contentDiv) {
                        const timeEl = contentDiv.querySelector('.msg-time');
                        const timeHtml = timeEl ? timeEl.outerHTML : '';
                        contentDiv.innerHTML = `<div class="italic text-gray-500 text-xs py-1">Tin nhắn đã bị thu hồi</div>` + timeHtml;
                        contentDiv.className = 'max-w-xs lg:max-w-md px-4 py-2.5 rounded-2xl text-sm leading-relaxed bg-gray-100 border border-gray-200 text-gray-500';
                        const btn = msgEl.querySelector('button');
                        if (btn) btn.remove();
                    }
                }
                updateConversationMsg(msg.conversationId, true, msg);
                return;
            }
            if (msg.type === 'GROUP_UPDATED') {
                const newName = msg.content;
                const newAvatar = msg.imageUrl;
                const groupId = msg.conversationId;
                
                updateGroupAvatarUI(groupId, newAvatar, newName);
                
                if (groupId == currentConversationId) {
                    document.getElementById('chatTitle').textContent = newName;
                    sessionStorage.setItem('currentChatFullName', newName);
                    
                    const sidebarName = document.getElementById('sidebarGroupName');
                    if (sidebarName) sidebarName.textContent = newName;
                    
                    const nameInput = document.getElementById('groupNameEditInput');
                    if (nameInput) nameInput.value = newName;
                }
                
                const conv = conversations.find(c => c.isGroup && c.id == groupId);
                if (conv) {
                    conv.name = newName;
                    conv.avatar = newAvatar;
                    renderConversations();
                }
                return;
            }
            if (msg.conversationId == currentConversationId) {
                appendMessage(msg, msg.senderUsername === checkAuth().username ? 'sent' : 'received');
            } else {
                showUnreadBadgeForGroup(msg.conversationId);
                showNotification(msg);
            }
            updateConversationMsg(msg.conversationId, true, msg);
        });
        groupSubscriptions.set(convId, sub);
    }
}

function showUnreadBadgeForGroup(convId) {
    const conv = conversations.find(c => c.isGroup && c.id == convId);
    if (conv) {
        conv.unread = true;
        renderConversations();
    }
}

function showUnreadBadge(username) {
    const conv = conversations.find(c => !c.isGroup && c.username === username);
    if (conv) {
        conv.unread = true;
        renderConversations();
    }
}

async function selectChat(username, fullName) {
    currentChatUser = username;
    currentConversationId = null; // Clear group chat
    sessionStorage.setItem('currentChatUser', username);
    sessionStorage.removeItem('currentConversationId');
    sessionStorage.setItem('currentChatFullName', fullName);

    // Update header
    document.getElementById('chatTitle').textContent = fullName;
    const partnerAv = document.getElementById('chatPartnerAvatar');
    if (partnerAv) {
        partnerAv.textContent = (fullName || username).charAt(0).toUpperCase();
        partnerAv.className = 'w-9 h-9 bg-primary rounded-full flex items-center justify-center text-white font-bold text-sm';
    }
    document.getElementById('chatWindowHeader').classList.remove('hidden');
    
    // Hide group actions
    if (document.getElementById('addMemberBtn')) document.getElementById('addMemberBtn').classList.add('hidden');
    if (document.getElementById('leaveGroupBtn')) document.getElementById('leaveGroupBtn').classList.add('hidden');
    if (document.getElementById('groupInfoBtn')) document.getElementById('groupInfoBtn').classList.add('hidden');
    if (document.getElementById('groupInfoSidebar')) document.getElementById('groupInfoSidebar').classList.add('hidden');

    // Fetch history and friend status
    const user = checkAuth();
    try {
        const res = await fetch(`${API_URL}/chat/history?user2=${username}`);
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

    const conv = conversations.find(c => !c.isGroup && c.username === username);
    if (conv) {
        conv.unread = false;
    }
    renderConversations();

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
        partnerAv.textContent = (groupName || '?').charAt(0).toUpperCase();
        partnerAv.className = 'w-9 h-9 bg-blue-600 rounded-full flex items-center justify-center text-white font-bold text-sm';
    }
    document.getElementById('chatWindowHeader').classList.remove('hidden');
    
    // Show group actions
    if (document.getElementById('addMemberBtn')) document.getElementById('addMemberBtn').classList.remove('hidden');
    if (document.getElementById('leaveGroupBtn')) document.getElementById('leaveGroupBtn').classList.remove('hidden');
    if (document.getElementById('groupInfoBtn')) document.getElementById('groupInfoBtn').classList.remove('hidden');
    if (document.getElementById('groupInfoSidebar')) document.getElementById('groupInfoSidebar').classList.add('hidden');

    document.getElementById('chatInputArea').classList.remove('hidden');
    if (document.getElementById('notFriendPlaceholder')) {
        document.getElementById('notFriendPlaceholder').classList.add('hidden');
    }

    const msgs = document.getElementById('chatMessages');
    msgs.innerHTML = '<p class="text-center text-gray-400 text-xs py-4">Đang tải lịch sử...</p>';

    const conv = conversations.find(c => c.isGroup && c.id == groupId);
    if (conv) {
        conv.unread = false;
    }
    renderConversations();

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

    // Fetch and apply actual group avatar/name
    try {
        const group = await apiGet(`/api/group/${groupId}`);
        document.getElementById('chatTitle').textContent = group.name;
        sessionStorage.setItem('currentChatFullName', group.name);
        if (partnerAv) {
            if (group.avatar) {
                partnerAv.innerHTML = `<img src="${escapeHtml(group.avatar)}" class="w-full h-full object-cover rounded-full">`;
                partnerAv.className = "w-9 h-9 rounded-full overflow-hidden flex items-center justify-center text-white font-bold text-sm";
            } else {
                partnerAv.textContent = group.name.charAt(0).toUpperCase();
                partnerAv.className = 'w-9 h-9 bg-blue-600 rounded-full flex items-center justify-center text-white font-bold text-sm';
            }
        }
        
        // Update contact item as well to keep it fresh
        const contactItem = document.querySelector(`.chat-contact-item[data-group-id='${groupId}']`);
        if (contactItem) {
            const nameEl = contactItem.querySelector('.font-semibold');
            if (nameEl) nameEl.textContent = group.name;
            const oldAv = contactItem.firstElementChild;
            if (oldAv) {
                if (group.avatar) {
                    const newImg = document.createElement('img');
                    newImg.src = group.avatar;
                    newImg.className = "w-10 h-10 rounded-full object-cover flex-shrink-0";
                    contactItem.replaceChild(newImg, oldAv);
                } else {
                    const newDiv = document.createElement('div');
                    newDiv.className = "w-10 h-10 rounded-full bg-blue-100 text-blue-600 flex items-center justify-center font-bold text-sm flex-shrink-0";
                    newDiv.textContent = group.name.charAt(0).toUpperCase();
                    contactItem.replaceChild(newDiv, oldAv);
                }
            }
        }
        
        // If sidebar is visible, reload it too
        const sidebar = document.getElementById('groupInfoSidebar');
        if (sidebar && !sidebar.classList.contains('hidden')) {
            loadGroupInfoSidebar();
        }
    } catch (e) {
        console.error("Lỗi khi tải thông tin nhóm ở selectGroupChat:", e);
    }
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
        const img = message.imageUrl ? `<img src="${message.imageUrl}" class="rounded-lg mb-2 max-w-full" style="max-height: 200px; object-fit: cover;">` : '';
        const txt = message.content ? `<div>${message.content}</div>` : '';
        contentHtml = img + txt;
    }

    let senderHtml = '';
    if (type === 'received' && currentConversationId && message.senderUsername) {
        senderHtml = `<div class="text-[10px] text-gray-400 mb-0.5 ml-1">@${message.senderUsername}</div>`;
    }

    const recallBtn = (type === 'sent' && !message.isRevoked)
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
    try {
        await fetch(`${API_URL}/chat/revoke/${messageId}`, { method: 'PUT' });
    } catch (e) { console.error("Recall error:", e); }
}

async function clearHistory() {
    if (!currentChatUser && !currentConversationId) return;
    
    const isGroup = !!currentConversationId;
    const targetName = isGroup ? document.getElementById('chatTitle').textContent : currentChatUser;
    
    if (!confirm(`Bạn có chắc muốn xóa toàn bộ lịch sử trò chuyện với ${targetName}? Lưu ý: Chỉ xóa phía bạn, các thành viên khác vẫn thấy.`)) return;
    
    try {
        if (isGroup) {
            await fetch(`${API_URL}/chat/group-history/${currentConversationId}`, { method: 'DELETE' });
            document.getElementById('chatMessages').innerHTML = '';
            selectGroupChat(currentConversationId, targetName);
        } else {
            await fetch(`${API_URL}/chat/history/${currentChatUser}`, { method: 'DELETE' });
            document.getElementById('chatMessages').innerHTML = '';
            selectChat(currentChatUser, targetName);
        }
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

function closeGroupModal() { document.getElementById("groupModal").classList.add("hidden"); }

async function loadFriendsForSelection() {
    const user = checkAuth();
    const container = document.getElementById("friendsSelectionList");
    try {
        const res = await fetch(`${API_URL}/chat/contacts`);
        const friends = await res.json();
        container.innerHTML = friends.map(f => `
            <label class="flex items-center gap-3 p-2 hover:bg-gray-50 rounded-lg cursor-pointer transition">
                <input type="checkbox" name="groupMember" value="${f.username}" class="w-4 h-4 rounded text-primary border-gray-300">
                <div class="w-8 h-8 rounded-full bg-gray-200 flex items-center justify-center text-xs font-bold">${(f.fullName||f.username).charAt(0).toUpperCase()}</div>
                <span class="text-sm font-medium text-gray-700">${f.fullName}</span>
            </label>
        `).join("");
    } catch(e) { console.error(e); }
}

async function submitCreateGroup() {
    const user = checkAuth();
    const name = document.getElementById("groupNameInput").value.trim();
    const usernames = Array.from(document.querySelectorAll("input[name='groupMember']:checked")).map(cb => cb.value);
    if (!name) { alert("Vui lòng nhập tên nhóm."); return; }
    if (usernames.length < 2) { alert("Nhóm chat phải có từ 3 người trở lên (bao gồm cả bạn). Vui lòng chọn ít nhất 2 bạn bè."); return; }
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

async function leaveGroup() {
    if (!currentConversationId) return;
    const groupName = document.getElementById('chatTitle').textContent;
    if (!confirm(`Bạn có chắc chắn muốn rời khỏi nhóm "${groupName}"?`)) return;
    try {
        const res = await fetch(`${API_URL}/chat/group/${currentConversationId}/leave`, { method: 'POST' });
        if (res.ok) {
            currentConversationId = null;
            sessionStorage.removeItem('currentConversationId');
            document.getElementById('chatWindowHeader').classList.add('hidden');
            document.getElementById('chatMessages').innerHTML = `
                <div class="flex flex-col items-center justify-center h-full text-gray-400 gap-3">
                    <i class="fas fa-comment-dots text-5xl opacity-30"></i>
                    <p class="text-sm">Chọn một cuộc hội thoại để bắt đầu</p>
                </div>`;
            document.getElementById('chatInputArea').classList.add('hidden');
            loadFriends(); // Refresh sidebar list
        }
    } catch (e) { console.error("Leave group error:", e); }
}

function openAddMemberModal() {
    if (!currentConversationId) return;
    document.getElementById("addMemberModal").classList.remove("hidden");
    loadFriendsForAddSelection();
}

function closeAddMemberModal() {
    document.getElementById("addMemberModal").classList.add("hidden");
}

async function loadFriendsForAddSelection() {
    const container = document.getElementById("addMemberSelectionList");
    container.innerHTML = '<p class="text-center text-gray-400 text-xs py-4">Đang tải danh sách...</p>';
    try {
        const res = await fetch(`${API_URL}/chat/group/${currentConversationId}/non-members`);
        const friends = await res.json();
        if (friends.length === 0) {
            container.innerHTML = '<p class="text-center text-gray-400 text-sm py-4">Tất cả bạn bè của bạn đã có mặt trong nhóm!</p>';
            return;
        }
        container.innerHTML = friends.map(f => `
            <label class="flex items-center gap-3 p-2 hover:bg-gray-50 rounded-lg cursor-pointer transition">
                <input type="checkbox" name="addMemberItem" value="${f.username}" class="w-4 h-4 rounded text-primary border-gray-300">
                <div class="w-8 h-8 rounded-full bg-gray-200 flex items-center justify-center text-xs font-bold">${(f.fullName||f.username).charAt(0).toUpperCase()}</div>
                <span class="text-sm font-medium text-gray-700">${f.fullName}</span>
            </label>
        `).join("");
    } catch(e) { console.error(e); }
}

async function submitAddMember() {
    const usernames = Array.from(document.querySelectorAll("input[name='addMemberItem']:checked")).map(cb => cb.value);
    if (usernames.length === 0) { alert("Vui lòng chọn ít nhất 1 thành viên để thêm."); return; }
    try {
        const res = await fetch(`${API_URL}/chat/group/${currentConversationId}/add-members`, {
            method: "POST",
            headers: { "Content-Type": "application/json" },
            body: JSON.stringify({ usernames })
        });
        if (res.ok) {
            closeAddMemberModal();
            alert("Đã thêm thành viên thành công!");
        }
    } catch(e) { console.error(e); }
}

function toggleGroupSidebar() {

    const sidebar = document.getElementById('groupInfoSidebar');

    if (sidebar.classList.contains('hidden')) {

        sidebar.classList.remove('hidden');

        loadGroupInfoSidebar();

    } else {

        sidebar.classList.add('hidden');

    }

}



function switchGroupTab(tabName, btnElement) {

    document.querySelectorAll('.group-tab-btn').forEach(btn => {

        btn.classList.remove('text-primary', 'bg-primary/10');

        btn.classList.add('text-gray-500');

    });

    btnElement.classList.remove('text-gray-500');

    btnElement.classList.add('text-primary', 'bg-primary/10');



    document.getElementById('tabGroupMembers').classList.add('hidden');

    document.getElementById('tabGroupDocuments').classList.add('hidden');

    document.getElementById('tabGroupRequests').classList.add('hidden');



    if (tabName === 'members') {

        document.getElementById('tabGroupMembers').classList.remove('hidden');

        loadGroupMembersTab();

    } else if (tabName === 'documents') {

        document.getElementById('tabGroupDocuments').classList.remove('hidden');

        loadGroupDocumentsTab();

    } else if (tabName === 'requests') {

        document.getElementById('tabGroupRequests').classList.remove('hidden');

        loadGroupRequestsTab();

    }

}



async function loadGroupInfoSidebar() {

    if (!currentConversationId) return;

    loadGroupMembersTab();

    

    try {

        const group = await apiGet(`/api/group/${currentConversationId}`);

        document.getElementById('sidebarGroupName').textContent = group.name;

        document.getElementById('sidebarMemberCount').textContent = group.memberCount + ' thành viên';

        

        // Update sidebar avatar

        const sidebarAv = document.getElementById('sidebarGroupAvatar');

        if (sidebarAv) {

            if (group.avatar) {

                sidebarAv.innerHTML = `<img src="${escapeHtml(group.avatar)}" class="w-full h-full object-cover rounded-full">`;

                sidebarAv.className = "w-full h-full rounded-full overflow-hidden shadow-sm";

            } else {

                sidebarAv.innerHTML = group.name.charAt(0).toUpperCase();

                sidebarAv.className = "w-full h-full bg-gradient-to-br from-indigo-500 to-purple-600 rounded-full flex items-center justify-center text-white font-bold text-3xl shadow-sm";

            }

        }

        

        // Also update the chat header avatar here since we have the group object

        const partnerAv = document.getElementById('chatPartnerAvatar');

        if (partnerAv) {

            if (group.avatar) {

                partnerAv.innerHTML = `<img src="${escapeHtml(group.avatar)}" class="w-full h-full object-cover rounded-full">`;

                partnerAv.className = "w-9 h-9 rounded-full overflow-hidden flex items-center justify-center text-white font-bold text-sm";

            } else {

                partnerAv.innerHTML = group.name.charAt(0).toUpperCase();

                partnerAv.className = "w-9 h-9 bg-blue-600 rounded-full flex items-center justify-center text-white font-bold text-sm";

            }

        }

        

        document.getElementById('groupNameEditInput').value = group.name;

    } catch (e) {

        console.error("Lỗi tải thông tin chi tiết nhóm:", e);

    }

}



async function loadGroupMembersTab() {

    const list = document.getElementById('groupInfoMembersList');

    list.innerHTML = '<p class="text-center text-xs text-gray-400 py-4">Đang tải...</p>';

    try {

        const res = await apiGet(`/api/chat/group/${currentConversationId}/members`);

        list.innerHTML = '';

        const currentUser = checkAuth();

        let currentUserRole = 'MEMBER';

        

        // Find current user role first

        const me = res.find(m => m.username === currentUser.username);

        if (me && me.role) currentUserRole = me.role;

        window.currentUserGroupRole = currentUserRole;



        // Show/hide edit buttons based on role

        const canEdit = currentUserRole === 'ADMIN' || currentUserRole === 'CO_ADMIN';

        const editNameBtn = document.getElementById('editGroupNameBtn');

        const editAvatarBtn = document.getElementById('editGroupAvatarBtn');

        if (editNameBtn) editNameBtn.classList.toggle('hidden', !canEdit);

        if (editAvatarBtn) editAvatarBtn.classList.toggle('hidden', !canEdit);



        // Update member count in the sidebar

        const sidebarMemberCount = document.getElementById('sidebarMemberCount');

        if (sidebarMemberCount) sidebarMemberCount.textContent = res.length + ' thành viên';



        if (currentUserRole === 'ADMIN') {

            list.innerHTML = `

                <div class="mb-3">

                    <button onclick="disbandGroup()" class="w-full bg-red-50 hover:bg-red-100 text-red-600 font-bold py-2 rounded-xl transition flex items-center justify-center gap-2">

                        <i class="fas fa-trash-alt"></i> Giải tán nhóm

                    </button>

                </div>

            `;

        }



        res.forEach(member => {

            const initial = escapeHtml((member.fullName || member.username || '?').charAt(0).toUpperCase());

            const avatar = member.avatar ? `<img src="${escapeHtml(member.avatar)}" class="w-8 h-8 rounded-full object-cover">` 

                                         : `<div class="w-8 h-8 rounded-full bg-primary flex items-center justify-center text-white font-bold text-xs">${initial}</div>`;

            

            let roleBadge = '';

            if (member.role === 'ADMIN') roleBadge = '<span class="text-[9px] bg-red-50 text-primary px-2 py-0.5 rounded-full font-bold inline-block mt-0.5">Trưởng nhóm</span>';

            else if (member.role === 'CO_ADMIN') roleBadge = '<span class="text-[9px] bg-orange-50 text-orange-600 px-2 py-0.5 rounded-full font-bold inline-block mt-0.5">Phó nhóm</span>';



            let actionMenu = '';

            if (member.username !== currentUser.username) {

                if (currentUserRole === 'ADMIN') {

                    const isCoAdmin = member.role === 'CO_ADMIN';

                    const roleBtn = isCoAdmin 

                        ? `<button onclick="changeMemberRole('${member.username}', 'MEMBER')" class="w-full text-left px-3 py-2 text-xs hover:bg-gray-50 text-gray-700">Giáng chức</button>`

                        : `<button onclick="changeMemberRole('${member.username}', 'CO_ADMIN')" class="w-full text-left px-3 py-2 text-xs hover:bg-gray-50 text-gray-700">Phong phó nhóm</button>`;

                    actionMenu = `

                        <div class="relative">

                            <button onclick="toggleMemberDropdown(event, '${member.username}')" class="text-gray-400 hover:text-gray-600 p-1 focus:outline-none"><i class="fas fa-ellipsis-v text-xs"></i></button>

                            <div id="member-dropdown-${member.username}" class="member-action-dropdown absolute right-0 top-full mt-1 bg-white border border-gray-150 shadow-lg rounded-xl w-32 hidden z-20 overflow-hidden">

                                ${roleBtn}

                                <button onclick="kickMember('${member.username}')" class="w-full text-left px-3 py-2 text-xs hover:bg-red-50 text-red-600 font-semibold">Xóa khỏi nhóm</button>

                            </div>

                        </div>

                    `;

                } else if (currentUserRole === 'CO_ADMIN' && member.role === 'MEMBER') {

                    actionMenu = `

                        <button onclick="kickMember('${member.username}')" class="text-gray-400 hover:text-red-500 p-1 focus:outline-none" title="Xóa khỏi nhóm"><i class="fas fa-user-minus text-xs"></i></button>

                    `;

                }

            }



            list.innerHTML += `

                <div class="flex items-center gap-3 p-2.5 hover:bg-white rounded-xl transition border border-transparent hover:border-gray-100 shadow-sm relative">

                    ${avatar}

                    <div class="flex-1 min-w-0">

                        <a href="profile.html?username=${encodeURIComponent(member.username)}" class="font-semibold text-xs text-gray-900 hover:underline truncate block">

                            ${escapeHtml(member.fullName)}

                        </a>

                        ${roleBadge ? `<div>${roleBadge}</div>` : `<p class="text-[10px] text-gray-500 truncate">@${escapeHtml(member.username)}</p>`}

                    </div>

                    ${actionMenu}

                </div>

            `;

        });

    } catch (e) {

        list.innerHTML = '<p class="text-center text-red-500 text-xs py-4">Lỗi tải dữ liệu</p>';

    }

}



async function disbandGroup() {

    if (!confirm('Bạn có chắc chắn muốn giải tán nhóm này? Hành động này không thể hoàn tác!')) return;

    try {

        await fetch(`${API_URL}/group/${currentConversationId}`, { method: 'DELETE' });

        // After disbanding, clear UI

        currentConversationId = null;

        sessionStorage.removeItem('currentConversationId');

        document.getElementById('chatWindowHeader').classList.add('hidden');

        document.getElementById('chatMessages').innerHTML = `

            <div class="flex flex-col items-center justify-center h-full text-gray-400 gap-3">

                <i class="fas fa-comment-dots text-5xl opacity-30"></i>

                <p class="text-sm">Chọn một cuộc hội thoại để bắt đầu</p>

            </div>`;

        document.getElementById('chatInputArea').classList.add('hidden');

        document.getElementById('groupInfoSidebar').classList.add('hidden');

        loadFriends(); // Refresh sidebar list

    } catch (err) { alert('Lỗi khi giải tán nhóm'); }

}



async function loadGroupDocumentsTab() {

    const list = document.getElementById('groupInfoDocumentsList');

    list.innerHTML = '<p class="text-center text-xs text-gray-400 py-4">Đang tải...</p>';

    try {

        const res = await apiGet(`/api/document/group/${currentConversationId}`);

        allGroupDocuments = res;

        renderFilteredDocuments();

    } catch (e) {

        list.innerHTML = '<p class="text-center text-red-500 text-xs py-4">Lỗi tải dữ liệu</p>';

    }

}



function getFileExtInfo(filename, fileUrl, fileType) {

    let ext = '';

    if (fileUrl && fileUrl.includes('.')) {

        ext = fileUrl.split('.').pop().toLowerCase();

    } else if (filename && filename.includes('.')) {

        ext = filename.split('.').pop().toLowerCase();

    }

    

    if (!ext && fileType) {

        if (fileType.includes('pdf')) ext = 'pdf';

        else if (fileType.includes('sheet') || fileType.includes('excel') || fileType.includes('ms-excel')) ext = 'xlsx';

        else if (fileType.includes('presentation') || fileType.includes('powerpoint') || fileType.includes('presentationml')) ext = 'pptx';

        else if (fileType.includes('document') || fileType.includes('word') || fileType.includes('msword')) ext = 'docx';

        else if (fileType.includes('image/')) ext = 'img';

        else if (fileType.includes('zip') || fileType.includes('compressed')) ext = 'zip';

    }

    

    ext = ext || 'file';

    let extText = ext.toUpperCase();

    if (extText.length > 4) extText = extText.substring(0, 4);

    

    if (ext === 'pdf') {

        return { extText: 'PDF', bgClass: 'bg-red-50 text-red-600 shadow-sm border border-red-100' };

    } else if (ext === 'ppt' || ext === 'pptx') {

        return { extText: 'PPT', bgClass: 'bg-yellow-50 text-yellow-600 shadow-sm border border-yellow-100' };

    } else if (ext === 'xls' || ext === 'xlsx') {

        return { extText: 'XLS', bgClass: 'bg-green-50 text-green-600 shadow-sm border border-green-100' };

    } else if (ext === 'doc' || ext === 'docx') {

        return { extText: 'DOC', bgClass: 'bg-blue-50 text-blue-600 shadow-sm border border-blue-100' };

    } else if (ext === 'png' || ext === 'jpg' || ext === 'jpeg' || ext === 'gif') {

        return { extText: 'IMG', bgClass: 'bg-purple-50 text-purple-600 shadow-sm border border-purple-100' };

    } else if (ext === 'zip' || ext === 'rar') {

        return { extText: 'ZIP', bgClass: 'bg-orange-50 text-orange-600 shadow-sm border border-orange-100' };

    } else {

        return { extText: extText, bgClass: 'bg-gray-50 text-gray-600 shadow-sm border border-gray-100' };

    }

}



function getCategoryColorClass(category) {

    if (category === 'Đề thi') return 'text-red-500';

    if (category === 'Bài giảng') return 'text-blue-500';

    if (category === 'Bài tập') return 'text-green-500';

    if (category === 'Ghi chú') return 'text-purple-500';

    return 'text-gray-500';

}



function getCategoryBadge(category) {

    let badgeClass = 'bg-gray-100 text-gray-600';

    if (category === 'Đề thi') badgeClass = 'bg-red-50 text-red-600 border border-red-100';

    else if (category === 'Bài giảng') badgeClass = 'bg-blue-50 text-blue-600 border border-blue-100';

    else if (category === 'Bài tập') badgeClass = 'bg-green-50 text-green-600 border border-green-100';

    else if (category === 'Ghi chú') badgeClass = 'bg-purple-50 text-purple-600 border border-purple-100';

    return `<span class="px-1.5 py-0.5 rounded text-[9px] font-bold ${badgeClass}">${escapeHtml(category)}</span>`;

}



function renderFilteredDocuments() {

    const list = document.getElementById('groupInfoDocumentsList');

    if (!list) return;

    

    let filtered = allGroupDocuments || [];

    

    // Filter by category

    if (activeDocCategory) {

        filtered = filtered.filter(doc => doc.category === activeDocCategory);

    }

    

    // Filter by search query

    if (docSearchQuery) {

        const query = docSearchQuery.toLowerCase();

        filtered = filtered.filter(doc => 

            (doc.name && doc.name.toLowerCase().includes(query)) ||

            (doc.description && doc.description.toLowerCase().includes(query))

        );

    }

    

    list.innerHTML = '';

    if (filtered.length === 0) {

        list.innerHTML = '<p class="text-center text-xs text-gray-400 py-4">Không tìm thấy tài liệu phù hợp</p>';

        return;

    }

    

    filtered.forEach(doc => {

        const size = (doc.sizeBytes / 1024 / 1024).toFixed(2) + ' MB';

        const date = new Date(doc.createdAt).toLocaleDateString('vi-VN');

        const fileInfo = getFileExtInfo(doc.name, doc.fileUrl, doc.fileType);

        const catColorClass = getCategoryColorClass(doc.category);

        

        const currentUser = checkAuth();

        const isUploader = doc.uploader && doc.uploader.username === currentUser.username;

        const canDelete = isUploader || (window.currentUserGroupRole === 'ADMIN' || window.currentUserGroupRole === 'CO_ADMIN');

        

        const deleteBtn = canDelete ? `

            <button onclick="deleteDocument(${doc.id})" class="text-gray-400 hover:text-red-500 p-1.5 transition flex-shrink-0" title="Xóa tài liệu">

                <i class="fas fa-trash-alt text-sm"></i>

            </button>

        ` : '';

        

        const pinBtn = (window.currentUserGroupRole === 'ADMIN' || window.currentUserGroupRole === 'CO_ADMIN') ? `

            <button onclick="togglePinDocument(${doc.id})" class="p-1.5 transition flex-shrink-0 ${doc.isPinned ? 'text-yellow-500 hover:text-yellow-600' : 'text-gray-400 hover:text-gray-600'}" title="${doc.isPinned ? 'Bỏ ghim' : 'Ghim tài liệu'}">

                <i class="fas fa-thumbtack text-sm"></i>

            </button>

        ` : '';



        const downloadBtn = `

            <a href="${doc.fileUrl}" target="_blank" onclick="incrementDownload(${doc.id})" class="text-gray-400 hover:text-blue-500 p-1.5 transition flex-shrink-0" title="Tải xuống">

                <i class="fas fa-download text-sm"></i>

            </a>

        `;



        list.innerHTML += `
            <div class="bg-white p-2.5 rounded-xl border border-gray-150 hover:border-gray-200 shadow-sm flex items-center gap-2.5 transition ${doc.isPinned ? 'border-yellow-300 bg-yellow-50/10' : ''}">
                <div class="w-9 h-9 rounded-lg ${fileInfo.bgClass} flex items-center justify-center text-[10px] font-extrabold flex-shrink-0">
                    ${fileInfo.extText}
                </div>
                <div class="flex-1 min-w-0">
                    <div class="flex items-center gap-1.5">
                        ${doc.isPinned ? '<i class="fas fa-thumbtack text-yellow-500 text-[10px]" title="Đã ghim"></i>' : ''}
                        <h4 class="font-semibold text-sm text-gray-900 truncate" title="${doc.name}">${escapeHtml(doc.name)}</h4>
                    </div>
                    <div class="text-[10px] text-gray-400 font-medium mt-0.5 leading-normal break-words">
                        <span class="${catColorClass} font-semibold">${escapeHtml(doc.category || 'Khác')}</span>
                        <span class="text-gray-300"> · </span>
                        <span>${size}</span>
                        <span class="text-gray-300"> · </span>
                        <span>${doc.downloads || 0} lượt tải</span>
                        <span class="text-gray-300"> · </span>
                        <span class="text-gray-500 font-semibold">${escapeHtml(doc.uploader.fullName)}</span>
                    </div>
                </div>
                <div class="flex items-center gap-0.5 flex-shrink-0">
                    ${pinBtn}
                    ${downloadBtn}
                    ${deleteBtn}
                </div>

            </div>

        `;

    });

}



function filterDocsByCategory(category, btnElement) {

    activeDocCategory = category;

    

    document.querySelectorAll('.doc-chip-btn').forEach(btn => {

        btn.className = 'doc-chip-btn px-3 py-1 bg-white hover:bg-gray-50 border border-gray-200 text-gray-500 rounded-full text-[10px] font-semibold transition whitespace-nowrap';

    });

    

    if (btnElement) {

        btnElement.className = 'doc-chip-btn active px-3 py-1 bg-primary text-white border border-transparent rounded-full text-[10px] font-semibold transition whitespace-nowrap';

    }

    

    renderFilteredDocuments();

}



async function deleteDocument(docId) {

    if (!confirm('Bạn có chắc muốn xóa tài liệu này?')) return;

    try {

        const res = await fetch(`${API_URL}/document/${docId}`, { method: 'DELETE' });

        if (res.ok) {

            alert('Đã xóa tài liệu thành công!');

            loadGroupDocumentsTab();

        } else {

            const err = await res.json();

            alert(err.error || 'Lỗi khi xóa tài liệu');

        }

    } catch (e) {

        console.error(e);

        alert('Lỗi khi xóa tài liệu');

    }

}



async function togglePinDocument(docId) {

    try {

        const res = await fetch(`${API_URL}/document/${docId}/pin`, { method: 'POST' });

        if (res.ok) {

            loadGroupDocumentsTab();

        } else {

            const err = await res.json();

            alert(err.error || 'Lỗi khi ghim tài liệu');

        }

    } catch (e) {

        console.error(e);

        alert('Lỗi khi ghim tài liệu');

    }

}



async function loadGroupRequestsTab() {

    const list = document.getElementById('groupInfoRequestsList');

    list.innerHTML = '<p class="text-center text-xs text-gray-400 py-4">Đang tải...</p>';

    try {

        const res = await apiGet(`/api/group/${currentConversationId}/requests`);

        document.getElementById('requestCountBadge').classList.toggle('hidden', res.length === 0);

        

        list.innerHTML = '';

        if (res.length === 0) {

            list.innerHTML = '<p class="text-center text-xs text-gray-400 py-4">Không có yêu cầu nào</p>';

            return;

        }

        

        res.forEach(req => {

            const initial = escapeHtml((req.user.fullName || req.user.username || '?').charAt(0).toUpperCase());

            const avatar = req.user.avatar ? `<img src="${escapeHtml(req.user.avatar)}" class="w-8 h-8 rounded-full object-cover">` 

                                         : `<div class="w-8 h-8 rounded-full bg-gray-200 flex items-center justify-center text-gray-500 font-bold text-xs">${initial}</div>`;

            

            list.innerHTML += `

                <div class="bg-white p-3 rounded-xl border border-gray-100 shadow-sm flex items-center gap-3">

                    ${avatar}

                    <div class="flex-1 min-w-0">

                        <h4 class="font-semibold text-xs text-gray-900 truncate">${escapeHtml(req.user.fullName)}</h4>

                        <p class="text-[10px] text-gray-500 truncate">@${escapeHtml(req.user.username)}</p>

                    </div>

                    <div class="flex gap-1">

                        <button onclick="processJoinRequest(${req.id}, 'APPROVE')" class="w-7 h-7 rounded-full bg-green-50 text-green-600 hover:bg-green-100 flex items-center justify-center transition"><i class="fas fa-check text-xs"></i></button>

                        <button onclick="processJoinRequest(${req.id}, 'REJECT')" class="w-7 h-7 rounded-full bg-red-50 text-red-600 hover:bg-red-100 flex items-center justify-center transition"><i class="fas fa-times text-xs"></i></button>

                    </div>

                </div>

            `;

        });

    } catch (e) {

        list.innerHTML = '<p class="text-center text-red-500 text-xs py-4">Chỉ quản trị viên mới xem được.</p>';

        document.getElementById('requestCountBadge').classList.add('hidden');

    }

}



async function changeMemberRole(username, role) {

    if (!confirm('Bạn có chắc muốn thay đổi quyền của người này?')) return;

    try {

        await apiPost(`/api/group/${currentConversationId}/role`, { targetUsername: username, role: role });

        loadGroupMembersTab();

    } catch (err) { alert(err.message || 'Lỗi thay đổi quyền'); }

}



async function kickMember(username) {

    if (!confirm('Bạn có chắc muốn xóa thành viên này khỏi nhóm?')) return;

    try {

        await apiPost(`/api/group/${currentConversationId}/kick`, { targetUsername: username });

        loadGroupMembersTab();

    } catch (err) { alert(err.message || 'Lỗi khi xóa thành viên'); }

}



async function processJoinRequest(reqId, action) {

    try {

        await apiPost(`/api/group/${currentConversationId}/requests/${reqId}`, { action: action });

        loadGroupRequestsTab();

        loadGroupMembersTab();

    } catch (err) { alert(err.message || 'Lỗi xử lý yêu cầu'); }

}



// ======================= DOCUMENT UPLOAD MODAL ======================= //



function openUploadDocModal() { document.getElementById('uploadDocModal').classList.remove('hidden'); }

function closeUploadDocModal() { document.getElementById('uploadDocModal').classList.add('hidden'); }



async function submitUploadDoc() {

    const fileInput = document.getElementById('docFileInput');

    if (!fileInput.files || fileInput.files.length === 0) {

        alert("Vui lòng chọn file tài liệu.");

        return;

    }

    const file = fileInput.files[0];

    const name = document.getElementById('docNameInput').value.trim() || file.name;

    const desc = "";

    

    if (file.size > 50 * 1024 * 1024) {

        alert("Kích thước file không được vượt quá 50MB.");

        return;

    }



    const category = document.getElementById('docCategoryInput').value || 'Khác';



    const formData = new FormData();

    formData.append('file', file);

    formData.append('name', name);

    formData.append('description', desc);

    formData.append('fileType', file.type || 'application/octet-stream');

    formData.append('category', category);

    formData.append('conversationId', currentConversationId);



    try {

        const res = await fetch(`${API_URL}/document/upload`, {

            method: 'POST',

            body: formData

        });

        

        if (!res.ok) {

            const errorText = await res.text();

            throw new Error(errorText);

        }

        

        closeUploadDocModal();

        fileInput.value = '';

        document.getElementById('docFileNameDisplay').textContent = 'Nhấn để chọn file';

        document.getElementById('docNameInput').value = '';

        

        alert("Tải lên thành công!");

        loadGroupDocumentsTab();

    } catch (err) {

        alert("Lỗi khi tải tài liệu lên: " + err.message);

    }

}



async function incrementDownload(docId) {

    try {

        await apiPost(`/api/document/${docId}/download`, {});

    } catch (e) {}

}



// ======================= GROUP RENAME & CLICK-BASED DROPDOWN ======================= //



function startRenameGroup() {

    const currentName = document.getElementById('sidebarGroupName').textContent;

    document.getElementById('groupNameEditInput').value = currentName;

    document.getElementById('groupNameDisplayArea').classList.add('hidden');

    document.getElementById('groupNameEditArea').classList.remove('hidden');

    document.getElementById('groupNameEditInput').focus();

}



function cancelRenameGroup() {

    document.getElementById('groupNameEditArea').classList.add('hidden');

    document.getElementById('groupNameDisplayArea').classList.remove('hidden');

}



async function saveRenameGroup() {

    const input = document.getElementById('groupNameEditInput');

    const newName = input.value.trim();

    if (!newName) {

        alert("Tên nhóm không được để trống.");

        return;

    }

    try {

        const res = await apiPost(`/api/group/${currentConversationId}/rename`, { name: newName });

        

        // Cập nhật UI Sidebar

        document.getElementById('sidebarGroupName').textContent = res.name;

        document.getElementById('sidebarGroupAvatar').textContent = res.name.charAt(0).toUpperCase();

        

        // Cập nhật Header chat hiện tại

        document.getElementById('chatTitle').textContent = res.name;

        const partnerAv = document.getElementById('chatPartnerAvatar');

        if (partnerAv) {

            partnerAv.textContent = res.name.charAt(0).toUpperCase();

        }

        

        // Cập nhật danh sách liên lạc bên trái

        const contactItem = document.querySelector(`.chat-contact-item[data-group-id='${currentConversationId}']`);

        if (contactItem) {

            const nameEl = contactItem.querySelector('.font-semibold');

            if (nameEl) nameEl.textContent = res.name;

            const avatarEl = contactItem.querySelector('.rounded-full');

            if (avatarEl) avatarEl.textContent = res.name.charAt(0).toUpperCase();

        }

        

        sessionStorage.setItem('currentChatFullName', res.name);

        cancelRenameGroup();

    } catch (err) {

        alert(err.message || "Lỗi khi đổi tên nhóm.");

    }

}



function toggleMemberDropdown(event, username) {

    event.stopPropagation();

    const dropdown = document.getElementById(`member-dropdown-${username}`);

    const isHidden = dropdown.classList.contains('hidden');

    

    // Ẩn tất cả dropdown khác trước

    document.querySelectorAll('.member-action-dropdown').forEach(d => d.classList.add('hidden'));

    

    if (isHidden) {

        dropdown.classList.remove('hidden');

    }

}



// Lắng nghe click ngoài màn hình để đóng các dropdown

document.addEventListener('click', () => {

    document.querySelectorAll('.member-action-dropdown').forEach(d => d.classList.add('hidden'));

});



async function uploadGroupAvatar() {

    const fileInput = document.getElementById('groupAvatarFileInput');

    if (!fileInput || !fileInput.files || fileInput.files.length === 0) return;

    

    const file = fileInput.files[0];

    if (file.size > 5 * 1024 * 1024) {

        alert("Kích thước ảnh đại diện không được vượt quá 5MB.");

        fileInput.value = '';

        return;

    }

    

    const formData = new FormData();

    formData.append('imageFile', file);

    

    try {

        const res = await fetch(`${API_URL}/group/${currentConversationId}/avatar`, {

            method: 'POST',

            body: formData

        });

        

        if (!res.ok) {

            const err = await res.json();

            throw new Error(err.error || 'Lỗi khi upload ảnh');

        }

        

        const data = await res.json();

        

        // Cập nhật UI ngay lập tức cho người thực hiện

        updateGroupAvatarUI(currentConversationId, data.avatar, sessionStorage.getItem('currentChatFullName'));

        

        fileInput.value = '';

        alert("Cập nhật ảnh đại diện nhóm thành công!");

    } catch (err) {

        alert(err.message || "Lỗi khi đổi ảnh đại diện nhóm");

        fileInput.value = '';

    }

}



function updateGroupAvatarUI(groupId, avatarUrl, groupName) {

    // 1. Update sidebar avatar

    if (groupId == currentConversationId) {

        const sidebarAv = document.getElementById('sidebarGroupAvatar');

        if (sidebarAv) {

            if (avatarUrl) {

                sidebarAv.innerHTML = `<img src="${escapeHtml(avatarUrl)}" class="w-full h-full object-cover rounded-full">`;

                sidebarAv.className = "w-full h-full rounded-full overflow-hidden shadow-sm";

            } else {

                const initial = (groupName || '?').charAt(0).toUpperCase();

                sidebarAv.innerHTML = initial;

                sidebarAv.className = "w-full h-full bg-gradient-to-br from-indigo-500 to-purple-600 rounded-full flex items-center justify-center text-white font-bold text-3xl shadow-sm";

            }

        }

        

        // 2. Update chat header avatar

        const partnerAv = document.getElementById('chatPartnerAvatar');

        if (partnerAv) {

            if (avatarUrl) {

                partnerAv.innerHTML = `<img src="${escapeHtml(avatarUrl)}" class="w-full h-full object-cover rounded-full">`;

                partnerAv.className = "w-9 h-9 rounded-full overflow-hidden flex items-center justify-center text-white font-bold text-sm";

            } else {

                const initial = (groupName || '?').charAt(0).toUpperCase();

                partnerAv.innerHTML = initial;

                partnerAv.className = "w-9 h-9 bg-blue-600 rounded-full flex items-center justify-center text-white font-bold text-sm";

            }

        }

    }

    

    // 3. Update left contact list item avatar

    const contactItem = document.querySelector(`.chat-contact-item[data-group-id='${groupId}']`);

    if (contactItem) {

        const oldAv = contactItem.firstElementChild;

        if (oldAv) {

            if (avatarUrl) {

                const newImg = document.createElement('img');

                newImg.src = avatarUrl;

                newImg.className = "w-10 h-10 rounded-full object-cover flex-shrink-0";

                contactItem.replaceChild(newImg, oldAv);

            } else {

                const initial = (groupName || '?').charAt(0).toUpperCase();

                const newDiv = document.createElement('div');

                newDiv.className = "w-10 h-10 rounded-full bg-blue-100 text-blue-600 flex items-center justify-center font-bold text-sm flex-shrink-0";

                newDiv.textContent = initial;

                contactItem.replaceChild(newDiv, oldAv);

            }

        }

    }

}
