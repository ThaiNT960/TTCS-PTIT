var API_URL = 'http://localhost:8080/api';
let stompClient = null;
let currentChatUser = null;

document.addEventListener('DOMContentLoaded', () => {
    const user = checkAuth();
    setNavAvatar(user);
    loadFriends();
    connectWebSocket();
    setupMessageForm();

    if ('Notification' in window && Notification.permission === 'default') {
        Notification.requestPermission();
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

function connectWebSocket() {
    const socket = new SockJS('http://localhost:8080/ws');
    stompClient = Stomp.over(socket);
    stompClient.debug = null;
    stompClient.connect({}, () => {
        const user = checkAuth();
        stompClient.subscribe(`/topic/messages/${user.username}`, (message) => {
            const msg = JSON.parse(message.body);
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
        if (savedUser && savedName) selectChat(savedUser, savedName);
    }, (err) => {
        setTimeout(connectWebSocket, 3000);
    });
}

function showNotification(msg) {
    if ('Notification' in window && Notification.permission === 'granted') {
        new Notification(`Tin nhắn từ ${msg.senderUsername}`, { body: msg.content });
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
        const res = await fetch(`${API_URL}/friends?username=${user.username}`);
        const friends = await res.json();
        list.innerHTML = '';

        if (!friends.length) {
            list.innerHTML = `<p class="text-center text-gray-400 text-sm py-8">Chưa có bạn bè.<br>Hãy thêm bạn bè trước!</p>`;
            return;
        }

        friends.forEach(friend => {
            const initial = (friend.fullName || friend.username || '?').charAt(0).toUpperCase();
            const div = document.createElement('div');
            div.className = 'chat-contact-item flex items-center gap-3 p-3 rounded-xl cursor-pointer hover:bg-gray-50 transition';
            div.dataset.username = friend.username;
            div.onclick = () => selectChat(friend.username, friend.fullName);
            div.innerHTML = `
                <div class="w-10 h-10 rounded-full bg-primary flex items-center justify-center text-white font-bold text-sm flex-shrink-0">${initial}</div>
                <div class="flex-1 min-w-0">
                    <p class="font-semibold text-sm text-gray-900 truncate">${friend.fullName}</p>
                    <p class="text-xs text-gray-400 truncate">@${friend.username}</p>
                </div>
            `;
            list.appendChild(div);
        });
    } catch (e) { console.error(e); }
}

async function selectChat(username, fullName) {
    currentChatUser = username;
    sessionStorage.setItem('currentChatUser', username);
    sessionStorage.setItem('currentChatFullName', fullName);

    // Update header
    document.getElementById('chatTitle').textContent = fullName;
    const partnerAv = document.getElementById('chatPartnerAvatar');
    if (partnerAv) partnerAv.textContent = (fullName || username).charAt(0).toUpperCase();
    document.getElementById('chatWindowHeader').classList.remove('hidden');
    document.getElementById('chatInputArea').classList.remove('hidden');

    // Clear messages
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
        }
    });

    // Load history
    const user = checkAuth();
    try {
        const res = await fetch(`${API_URL}/chat/history?user1=${user.username}&user2=${username}`);
        const history = await res.json();
        history.forEach(msg => {
            const type = msg.senderUsername === user.username ? 'sent' : 'received';
            appendMessage(msg, type);
        });
    } catch (e) { console.error(e); }
}

function appendMessage(message, type) {
    const msgs = document.getElementById('chatMessages');
    const timeStr = formatMessageTime(message.timestamp);
    const div = document.createElement('div');
    div.className = `flex ${type === 'sent' ? 'justify-end' : 'justify-start'} mb-1`;
    div.innerHTML = `
        <div class="max-w-xs lg:max-w-md px-4 py-2.5 rounded-2xl text-sm leading-relaxed
            ${type === 'sent'
            ? 'bg-primary text-white rounded-br-sm'
            : 'bg-white text-gray-800 shadow-sm rounded-bl-sm'}">
            <div>${message.content}</div>
            <div class="text-xs mt-1 ${type === 'sent' ? 'text-red-200' : 'text-gray-400'}">${timeStr}</div>
        </div>`;
    msgs.appendChild(div);
    msgs.scrollTop = msgs.scrollHeight;
}

function setupMessageForm() {
    const form = document.getElementById('messageForm');
    if (!form) return;
    form.addEventListener('submit', e => {
        e.preventDefault();
        if (!currentChatUser) return;
        const user = checkAuth();
        const content = document.getElementById('messageInput').value.trim();
        if (!content || !stompClient) return;

        const message = {
            senderUsername: user.username,
            receiverUsername: currentChatUser,
            content,
            timestamp: new Date().toISOString()
        };
        stompClient.send('/app/chat', {}, JSON.stringify(message));
        appendMessage(message, 'sent');
        document.getElementById('messageInput').value = '';
    });
}
