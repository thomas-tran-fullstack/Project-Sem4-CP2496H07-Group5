/**
 * EZMart Chat JavaScript - For real-time 1:1 messaging
 * Provides Messenger-like chat experience
 */

// Chat state
const ChatJS = {
    webSocket: null,
    currentConversationId: null,
    currentUserId: null,
    currentUserRole: null,
    isTyping: false,
    reconnectAttempts: 0,
    maxReconnectAttempts: 5,

    /**
     * Initialize chat functionality
     */
    init: function(options = {}) {
        this.currentUserId = options.userId || this.getUserIdFromSession();
        this.currentUserRole = options.userRole || this.getUserRoleFromSession();
        this.serverUrl = options.serverUrl || this.getServerUrl();
        
        if (this.currentUserId && this.currentUserId > 0) {
            this.connect();
            this.startHeartbeat();
        } else {
            console.log('ChatJS: User not logged in, chat disabled');
        }
    },

    /**
     * Connect to WebSocket server
     */
    connect: function() {
        if (this.webSocket && this.webSocket.readyState === WebSocket.OPEN) {
            return;
        }

        const wsUrl = this.getWebSocketUrl();
        console.log('ChatJS: Connecting to', wsUrl);

        try {
            this.webSocket = new WebSocket(wsUrl);

            this.webSocket.onopen = (e) => {
                console.log('ChatJS: Connected');
                this.reconnectAttempts = 0;
                this.onConnect();
            };

            this.webSocket.onmessage = (e) => {
                this.handleMessage(JSON.parse(e.data));
            };

            this.webSocket.onerror = (e) => {
                console.error('ChatJS: Error', e);
            };

            this.webSocket.onclose = (e) => {
                console.log('ChatJS: Disconnected');
                this.handleDisconnect();
            };
        } catch (e) {
            console.error('ChatJS: Connection failed', e);
        }
    },

    /**
     * Get WebSocket URL
     */
    getWebSocketUrl: function() {
        const protocol = window.location.protocol === 'https:' ? 'wss:' : 'ws:';
        const convId = this.currentConversationId || 0;
        return `${protocol}//${window.location.host}/EZMart_Supermarket_Management-war/chat/${convId}/${this.currentUserId}`;
    },

    /**
     * Handle incoming WebSocket messages
     */
    handleMessage: function(message) {
        switch (message.type) {
            case 'NEW_MESSAGE':
                this.onNewMessage(message);
                break;
            case 'TYPING':
                this.onTyping(message);
                break;
            case 'READ_RECEIPT':
                this.onReadReceipt(message);
                break;
            case 'CONNECTED':
                console.log('ChatJS: Server confirmed connection');
                break;
            default:
                console.log('ChatJS: Unknown message type', message.type);
        }
    },

    /**
     * Handle new message
     */
    onNewMessage: function(message) {
        // Emit custom event for page to handle
        const event = new CustomEvent('chat:newMessage', { detail: message });
        document.dispatchEvent(event);

        // Play notification if from another user
        if (message.senderId !== this.currentUserId) {
            this.playNotification();
        }

        // Update unread count if chat is closed
        const chatWindow = document.getElementById('chatWindow');
        if (!chatWindow || !chatWindow.classList.contains('active')) {
            this.updateUnreadBadge();
        }
    },

    /**
     * Handle typing indicator
     */
    onTyping: function(message) {
        if (message.senderId !== this.currentUserId) {
            const event = new CustomEvent('chat:typing', { detail: message });
            document.dispatchEvent(event);
        }
    },

    /**
     * Handle read receipt
     */
    onReadReceipt: function(message) {
        const event = new CustomEvent('chat:read', { detail: message });
        document.dispatchEvent(event);
    },

    /**
     * Send a message
     */
    sendMessage: function(content, conversationId) {
        const convId = conversationId || this.currentConversationId;
        if (!convId || !content.trim()) return false;

        const message = {
            type: 'MESSAGE',
            conversationId: convId,
            senderId: this.currentUserId,
            senderRole: this.currentUserRole,
            content: content.trim(),
            timestamp: Date.now()
        };

        if (this.webSocket && this.webSocket.readyState === WebSocket.OPEN) {
            this.webSocket.send(JSON.stringify(message));
            return true;
        }

        return false;
    },

    /**
     * Send typing indicator
     */
    sendTyping: function(isTyping) {
        if (!this.currentConversationId) return;

        if (this.webSocket && this.webSocket.readyState === WebSocket.OPEN) {
            this.webSocket.send(JSON.stringify({
                type: 'TYPING',
                conversationId: this.currentConversationId,
                senderId: this.currentUserId,
                typing: isTyping
            }));
        }
    },

    /**
     * Mark messages as read
     */
    markAsRead: function(conversationId) {
        const convId = conversationId || this.currentConversationId;
        if (!convId) return;

        if (this.webSocket && this.webSocket.readyState === WebSocket.OPEN) {
            this.webSocket.send(JSON.stringify({
                type: 'READ',
                conversationId: convId,
                readerId: this.currentUserId
            }));
        }
    },

    /**
     * Switch to a different conversation
     */
    switchConversation: function(conversationId) {
        this.currentConversationId = conversationId;
        this.markAsRead(conversationId);
        this.connect(); // Reconnect with new conversation
    },

    /**
     * Handle connection established
     */
    onConnect: function() {
        // Emit connect event
        const event = new CustomEvent('chat:connect');
        document.dispatchEvent(event);
    },

    /**
     * Handle disconnection
     */
    handleDisconnect: function() {
        // Try to reconnect
        if (this.reconnectAttempts < this.maxReconnectAttempts) {
            this.reconnectAttempts++;
            const delay = Math.min(1000 * Math.pow(2, this.reconnectAttempts), 30000);
            console.log(`ChatJS: Reconnecting in ${delay}ms...`);
            setTimeout(() => this.connect(), delay);
        } else {
            console.log('ChatJS: Max reconnection attempts reached');
        }

        const event = new CustomEvent('chat:disconnect');
        document.dispatchEvent(event);
    },

    /**
     * Start heartbeat to keep connection alive
     */
    startHeartbeat: function() {
        setInterval(() => {
            if (this.webSocket && this.webSocket.readyState === WebSocket.OPEN) {
                this.webSocket.send('{"type":"PING"}');
            }
        }, 30000);
    },

    /**
     * Update unread badge count
     */
    updateUnreadBadge: function() {
        // This would typically fetch from server
        fetch(`/api/chat/unread-count?userId=${this.currentUserId}`)
            .then(response => response.json())
            .then(data => {
                const badge = document.querySelector('.chat-badge');
                if (badge) {
                    if (data.count > 0) {
                        badge.textContent = data.count > 99 ? '99+' : data.count;
                        badge.style.display = 'flex';
                    } else {
                        badge.style.display = 'none';
                    }
                }
            })
            .catch(e => console.error('ChatJS: Failed to get unread count', e));
    },

    /**
     * Play notification sound
     */
    playNotification: function() {
        try {
            const audio = new Audio('/resources/sounds/notification.mp3');
            audio.volume = 0.5;
            audio.play().catch(e => console.log('ChatJS: Audio play failed', e));
        } catch (e) {
            // Audio not supported
        }
    },

    /**
     * Toggle chat widget
     */
    toggleChat: function() {
        const meta = document.querySelector('#conversationMeta[data-conversation-id]');
        const convId = meta ? parseInt(meta.getAttribute('data-conversation-id') || '0') : 0;
        if (convId && convId !== this.currentConversationId) {
            this.switchConversation(convId);
        }

        const chatWindow = document.getElementById('chatWindow');
        if (chatWindow) {
            chatWindow.classList.toggle('active');
            if (chatWindow.classList.contains('active')) {
                this.markAsRead();
                scrollToBottom();
            }
        }
    },

    /**
     * Close chat widget
     */
    closeChat: function() {
        const chatWindow = document.getElementById('chatWindow');
        if (chatWindow) {
            chatWindow.classList.remove('active');
        }
    },

    /**
     * Get user ID from session
     */
    getUserIdFromSession: function() {
        try {
            const session = document.body.dataset.sessionUserId;
            return session ? parseInt(session) : null;
        } catch (e) {
            return null;
        }
    },

    /**
     * Get user role from session
     */
    getUserRoleFromSession: function() {
        try {
            return document.body.dataset.sessionUserRole || null;
        } catch (e) {
            return null;
        }
    },

    /**
     * Get server URL
     */
    getServerUrl: function() {
        return window.location.origin;
    },

    /**
     * Send message via AJAX fallback (if WebSocket unavailable)
     */
    sendMessageAjax: function(content, conversationId) {
        const convId = conversationId || this.currentConversationId;
        if (!convId || !content.trim()) return;

        const formData = new FormData();
        formData.append('conversationId', convId);
        formData.append('content', content.trim());

        fetch('/api/chat/send-message', {
            method: 'POST',
            body: formData
        })
        .then(response => response.json())
        .then(data => {
            if (data.success) {
                const event = new CustomEvent('chat:sent', { detail: data.message });
                document.dispatchEvent(event);
            }
        })
        .catch(e => console.error('ChatJS: Failed to send message', e));
    },

    /**
     * Send new message (creates new conversation)
     */
    sendNewMessage: function(content) {
        if (!content.trim()) return;

        // For customers, we need to create a conversation first via AJAX, then use WebSocket
        this.createConversationAndSendMessage(content.trim());
    },

    /**
     * Create conversation and send first message
     */
    createConversationAndSendMessage: function(content) {
        // First, create conversation via AJAX
        const formData = new FormData();
        formData.append('chatController:newMessage', content);
        formData.append('javax.faces.ViewState', document.getElementById('javax.faces.ViewState')?.value || '');
        formData.append('chatController', 'chatController');

        fetch(window.location.href, {
            method: 'POST',
            body: formData,
            headers: {
                'Faces-Request': 'partial/ajax',
                'X-Requested-With': 'XMLHttpRequest'
            }
        })
        .then(response => response.text())
        .then(data => {
            // Clear input
            const input = document.getElementById('messageInput');
            if (input) input.value = '';

            // Update the chat content area with the response
            const chatContent = document.getElementById('chatContent');
            if (chatContent && data) {
                // Extract the updated content from the AJAX response
                const parser = new DOMParser();
                const doc = parser.parseFromString(data, 'text/html');
                const newContent = doc.querySelector('#chatContent');
                if (newContent) {
                    chatContent.innerHTML = newContent.innerHTML;
                }
            }

            // Scroll to bottom
            setTimeout(() => {
                const container = document.getElementById('messagesContainer');
                if (container) container.scrollTop = container.scrollHeight;
            }, 100);

            // Now connect to WebSocket for real-time updates
            // Extract conversation ID from the updated content
            const conversationElement = doc.querySelector('[data-conversation-id]');
            if (conversationElement) {
                const convId = conversationElement.getAttribute('data-conversation-id');
                if (convId) {
                    this.currentConversationId = parseInt(convId);
                    this.connect(); // Connect to WebSocket
                }
            }
        })
        .catch(error => {
            console.error('Error sending message:', error);
        });
    },

    /**
     * Disconnect WebSocket
     */
    disconnect: function() {
        if (this.webSocket) {
            this.webSocket.close();
            this.webSocket = null;
        }
    }
};

// Make ChatJS available globally
window.ChatJS = ChatJS;

// Utility functions
function scrollToBottom() {
    const container = document.getElementById('messagesContainer');
    if (container) {
        container.scrollTop = container.scrollHeight;
    }
}

function formatMessageTime(date) {
    const now = new Date();
    const messageDate = new Date(date);
    
    if (messageDate.toDateString() === now.toDateString()) {
        return messageDate.toLocaleTimeString('vi-VN', { hour: '2-digit', minute: '2-digit' });
    } else if (messageDate.toDateString() === new Date(now - 86400000).toDateString()) {
        return 'Hôm qua ' + messageDate.toLocaleTimeString('vi-VN', { hour: '2-digit', minute: '2-digit' });
    } else {
        return messageDate.toLocaleDateString('vi-VN', { day: '2-digit', month: '2-digit', hour: '2-digit', minute: '2-digit' });
    }
}

function escapeHtml(text) {
    const div = document.createElement('div');
    div.textContent = text;
    return div.innerHTML;
}

// Auto-initialize if user data is available
document.addEventListener('DOMContentLoaded', function() {
    const userId = document.body.dataset.sessionUserId;
    const userRole = document.body.dataset.sessionUserRole;
    
    if (userId && userRole) {
        ChatJS.init({
            userId: parseInt(userId),
            userRole: userRole
        });
    }
});
