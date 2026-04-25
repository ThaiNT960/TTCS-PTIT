var API_URL = 'http://localhost:8080/api';
let stompClient = null;
let currentConversationId = null;
let currentChatPartner = null; 
let subscriptions = new Map(); // Track by conversation ID

document.addEventListener('DOMContentLoaded', async () => {
    console.log("Chat JS loaded");
    const user = checkAuth();
    if (!user) {
        console.error("No user found, redirecting to login");
        window.location.href = 'login.html';
        return;
    }
    setNavAvatar(user);
    
    // Connect first, then load data
    connectWebSocket();
    await loadConversations();
    setupMessageForm();

    const urlParams = new URLSearchParams(window.location.search);
    const targetUsername = urlParams.get('target');
    if (targetUsername && targetUsername !== user.username) {
        try {
            const res = await fetch(`${API_URL}/chat/conversation-with?user1=${encodeURIComponent(user.username)}&user2=${encodeURIComponent(targetUsername)}`);
            if (res.ok) {
                const conv = await res.json();
                // Ensure it exists in the UI list if it's brand new
                const item = document.querySelector(`.chat-contact-item[data-id="${conv.id}"]`);
                if (!item) await loadConversations();
                selectConversation(conv);
            }
        } catch (e) { console.error("Error loading target conversation", e); }
    }

    if ('Notification' in window && Notification.permission === 'default') {
        Notification.requestPermission();
    }
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

function formatMessageTime(dateStr) {
    if (!dateStr) return '';
    return new Date(dateStr).toLocaleTimeString('vi-VN', { hour: '2-digit', minute: '2-digit' });
}

function connectWebSocket() {
    console.log("Connecting WebSocket...");
    const socket = new SockJS('http://localhost:8080/ws');
    stompClient = Stomp.over(socket);
    stompClient.debug = null;
    stompClient.connect({}, () => {
        console.log("WebSocket connected");
        const user = checkAuth();
        
        // Subscribe to personal topic
        stompClient.subscribe(`/topic/messages/${user.username}`, (message) => {
            const msg = JSON.parse(message.body);
            console.log("Received private message", msg);
            handleIncomingMessage(msg);
        });
    }, (err) => {
        console.error("WebSocket error", err);
        setTimeout(connectWebSocket, 5000);
    });
}

function handleIncomingMessage(msg) {
    if (msg.conversationId == currentConversationId) {
        appendMessage(msg, msg.senderUsername === checkAuth().username ? 'sent' : 'received');
    } else {
        showUnreadBadge(msg.conversationId);
        showNotification(msg);
        // Refresh list content only, don't re-subscribe everything here
        updateConversationListUI(); 
    }
}

function showNotification(msg) {
    if ('Notification' in window && Notification.permission === 'granted') {
        new Notification(`Tin nhắn mới`, { body: msg.content });
    }
}

function showUnreadBadge(convId) {
    const item = document.querySelector(`.chat-contact-item[data-id="${convId}"]`);
    if (item && !item.querySelector('.unread-dot')) {
        const dot = document.createElement('div');
        dot.className = 'unread-dot w-2.5 h-2.5 bg-primary rounded-full ml-auto flex-shrink-0';
        item.appendChild(dot);
    }
}

async function loadConversations() {
    console.log("Loading conversations...");
    const user = checkAuth();
    const list = document.getElementById('conversationsList');
    try {
        const res = await fetch(`${API_URL}/chat/conversations?username=${user.username}`);
        if (!res.ok) throw new Error("API error: " + res.status);
        const conversations = await res.json();
        console.log("Conversations loaded", conversations);
        
        list.innerHTML = '';
        if (!conversations.length) {
            list.innerHTML = `<p class="text-center text-gray-400 text-sm py-12 px-6">Chưa có cuộc hội thoại nào.</p>`;
            return;
        }

        conversations.forEach(conv => {
            // Subscribe to group topics once
            subscribeToConversation(conv.id);
            renderConversationItem(conv, list);
        });
    } catch (e) {
        console.error("Load conversations failed", e);
        list.innerHTML = `<p class="text-center text-red-400 text-sm py-12 px-6">Lỗi tải dữ liệu</p>`;
    }
}

function subscribeToConversation(convId) {
    if (stompClient && stompClient.connected && !subscriptions.has(convId)) {
        console.log("Subscribing to conversation topic:", convId);
        const sub = stompClient.subscribe(`/topic/conversation/${convId}`, (message) => {
            const msg = JSON.parse(message.body);
            handleIncomingMessage(msg);
        });
        subscriptions.set(convId, sub);
    }
}

function renderConversationItem(conv, container) {
    const initial = (conv.name || '?').charAt(0).toUpperCase();
    const div = document.createElement('div');
    div.className = 'chat-contact-item flex items-center gap-3 p-4 cursor-pointer hover:bg-gray-50 transition border-b border-gray-50 last:border-0';
    div.dataset.id = conv.id;
    div.onclick = () => selectConversation(conv);
    
    let lastMsgText = conv.lastMessage || 'Chưa có tin nhắn';
    if (lastMsgText.length > 30) lastMsgText = lastMsgText.substring(0, 27) + '...';

    div.innerHTML = `
        <div class="w-12 h-12 rounded-full ${conv.isGroupChat ? 'bg-blue-100 text-blue-600' : 'bg-primary text-white'} flex items-center justify-center font-bold text-lg flex-shrink-0">
            ${conv.avatar ? `<img src="${conv.avatar}" class="w-full h-full rounded-full object-cover">` : initial}
        </div>
        <div class="flex-1 min-w-0">
            <div class="flex justify-between items-baseline mb-0.5">
                <p class="font-bold text-sm text-gray-900 truncate">${conv.name}</p>
                ${conv.lastTimestamp ? `<span class="text-[10px] text-gray-400">${formatMessageTime(conv.lastTimestamp)}</span>` : ''}
            </div>
            <p class="text-xs text-gray-500 truncate last-msg-text">${lastMsgText}</p>
        </div>
    `;
    container.appendChild(div);
}

// Minimal UI update without re-rendering everything
async function updateConversationListUI() {
    const user = checkAuth();
    try {
        const res = await fetch(`${API_URL}/chat/conversations?username=${user.username}`);
        const conversations = await res.json();
        conversations.forEach(conv => {
            const item = document.querySelector(`.chat-contact-item[data-id="${conv.id}"]`);
            if (item) {
                const textEl = item.querySelector('.last-msg-text');
                if (textEl) textEl.textContent = conv.lastMessage || '...';
                // Move to top logic could be added here
            } else {
                // New conversation appeared
                loadConversations();
            }
        });
    } catch(e) {}
}

async function selectConversation(conv) {
    currentConversationId = conv.id;
    currentChatPartner = conv.otherUsername || null;
    console.log("Selected conversation", conv.id);

    document.getElementById('chatTitle').textContent = conv.name;
    const partnerAv = document.getElementById('chatPartnerAvatar');
    if (partnerAv) {
        partnerAv.textContent = (conv.name || '?').charAt(0).toUpperCase();
        partnerAv.className = `w-9 h-9 rounded-full flex items-center justify-center text-white font-bold text-sm ${conv.isGroupChat ? 'bg-blue-600' : 'bg-primary'}`;
    }
    document.getElementById('chatWindowHeader').classList.remove('hidden');
    document.getElementById('chatInputArea').classList.remove('hidden');

    const msgs = document.getElementById('chatMessages');
    msgs.innerHTML = '<p class="text-center text-gray-400 text-xs py-4">Đang tải lịch sử...</p>';

    document.querySelectorAll('.chat-contact-item').forEach(item => {
        item.classList.toggle('bg-primary/5', item.dataset.id == conv.id);
        if (item.dataset.id == conv.id) {
            const dot = item.querySelector('.unread-dot');
            if (dot) dot.remove();
        }
    });

    try {
        const res = await fetch(`${API_URL}/chat/history?conversationId=${conv.id}`);
        const history = await res.json();
        msgs.innerHTML = '';
        history.forEach(msg => appendMessage(msg, msg.senderUsername === checkAuth().username ? 'sent' : 'received'));
    } catch (e) { console.error(e); }
}

function appendMessage(message, type) {
    const msgs = document.getElementById('chatMessages');
    if (message.id && document.getElementById(`msg-${message.id}`)) return;

    const timeStr = formatMessageTime(message.timestamp);
    const div = document.createElement('div');
    if (message.id) div.id = `msg-${message.id}`;
    div.className = `flex ${type === 'sent' ? 'justify-end' : 'justify-start'} mb-1.5`;
    
    let senderHtml = '';
    if (type === 'received' && message.senderUsername) {
        senderHtml = `<p class="text-[10px] text-gray-400 mb-0.5 ml-1">${message.senderUsername}</p>`;
    }

    let imageHtml = '';
    if (message.imageUrl) {
        imageHtml = `<div class="mb-1"><img src="${message.imageUrl}" class="rounded-lg max-w-full h-auto object-contain max-h-48 border ${type === 'sent' ? 'border-primary-dark/20' : 'border-gray-200'}"></div>`;
    }
    
    let contentHtml = message.content ? `<div>${message.content}</div>` : '';

    div.innerHTML = `
        <div class="flex flex-col ${type === 'sent' ? 'items-end' : 'items-start'}">
            ${senderHtml}
            <div class="max-w-xs lg:max-w-md px-4 py-2.5 rounded-2xl text-sm leading-relaxed shadow-sm
                ${type === 'sent' ? 'bg-primary text-white rounded-br-sm' : 'bg-white text-gray-800 rounded-bl-sm'}">
                ${imageHtml}
                ${contentHtml}
                <div class="text-[10px] mt-1 opacity-70 text-right">${timeStr}</div>
            </div>
        </div>`;
    msgs.appendChild(div);
    msgs.scrollTop = msgs.scrollHeight;
}

let selectedChatFile = null;

function previewChatImage(input) {
    const container = document.getElementById('chatImagePreviewContainer');
    const preview = document.getElementById('chatImagePreview');
    if (input.files && input.files[0]) {
        selectedChatFile = input.files[0];
        const reader = new FileReader();
        reader.onload = function(e) {
            preview.src = e.target.result;
            container.classList.remove('hidden');
        };
        reader.readAsDataURL(selectedChatFile);
    } else {
        removeChatImage();
    }
}

function removeChatImage() {
    selectedChatFile = null;
    const input = document.getElementById('chatImageFile');
    if (input) input.value = '';
    document.getElementById('chatImagePreviewContainer').classList.add('hidden');
    document.getElementById('chatImagePreview').src = '';
}

function setupMessageForm() {
    const form = document.getElementById('messageForm');
    if (!form) return;
    form.addEventListener('submit', async e => {
        e.preventDefault();
        const input = document.getElementById('messageInput');
        const content = input.value.trim();
        const submitBtn = form.querySelector('button[type="submit"]');

        if (!currentConversationId || !stompClient) return;
        if (!content && !selectedChatFile) return;

        submitBtn.disabled = true;
        let imageUrl = null;

        try {
            if (selectedChatFile) {
                const formData = new FormData();
                formData.append('file', selectedChatFile);
                const res = await fetch(`${API_URL}/upload`, { method: 'POST', body: formData });
                if (res.ok) {
                    const data = await res.json();
                    imageUrl = data.url;
                }
            }

            const user = checkAuth();
            const message = {
                senderUsername: user.username,
                conversationId: currentConversationId,
                receiverUsername: currentChatPartner, 
                content,
                imageUrl: imageUrl,
                timestamp: new Date().toISOString()
            };
            stompClient.send('/app/chat', {}, JSON.stringify(message));
            
            input.value = '';
            removeChatImage();
        } catch (err) {
            console.error('Error sending message:', err);
            alert('Có lỗi xảy ra khi gửi tin nhắn');
        } finally {
            submitBtn.disabled = false;
        }
    });
}

function openCreateGroupModal() {
    document.getElementById('groupModal').classList.remove('hidden');
    loadFriendsForSelection();
}
function closeGroupModal() { document.getElementById('groupModal').classList.add('hidden'); }

async function loadFriendsForSelection() {
    const user = checkAuth();
    const container = document.getElementById('friendsSelectionList');
    try {
        const res = await fetch(`${API_URL}/friends?username=${user.username}`);
        const friends = await res.json();
        container.innerHTML = friends.map(f => `
            <label class="flex items-center gap-3 p-2 hover:bg-gray-50 rounded-lg cursor-pointer transition">
                <input type="checkbox" name="groupMember" value="${f.username}" class="w-4 h-4 rounded text-primary border-gray-300">
                <div class="w-8 h-8 rounded-full bg-gray-200 flex items-center justify-center text-xs font-bold">${f.fullName.charAt(0)}</div>
                <span class="text-sm font-medium text-gray-700">${f.fullName}</span>
            </label>
        `).join('');
    } catch(e) { console.error(e); }
}

async function submitCreateGroup() {
    const user = checkAuth();
    const name = document.getElementById('groupNameInput').value.trim();
    const usernames = Array.from(document.querySelectorAll('input[name="groupMember"]:checked')).map(cb => cb.value);
    if (!name || usernames.length < 1) { alert('Vui lòng nhập tên và chọn ít nhất 1 bạn'); return; }
    usernames.push(user.username);
    try {
        const res = await fetch(`${API_URL}/chat/group`, {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ name, usernames })
        });
        if (res.ok) { closeGroupModal(); loadConversations(); }
    } catch(e) { console.error(e); }
}
