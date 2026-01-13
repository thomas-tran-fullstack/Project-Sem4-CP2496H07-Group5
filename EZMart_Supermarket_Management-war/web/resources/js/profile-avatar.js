(function(){
    console.log('profile-avatar.js loaded');
    var inp = document.getElementById('avatarFileInput');
    if (!inp) return;
    var base = inp.dataset && inp.dataset.contextPath ? inp.dataset.contextPath : (window._ctx || '');

    // Notification system
    function showNotification(message, type) {
        // type: 'success' (green) or 'error' (red)
        var container = document.getElementById('notification-container');
        if (!container) {
            container = document.createElement('div');
            container.id = 'notification-container';
            container.style.cssText = 'position: fixed; top: 20px; right: 20px; z-index: 9999; pointer-events: none;';
            document.body.appendChild(container);
        }

        var notification = document.createElement('div');
        notification.className = 'notification-popup';
        notification.textContent = message;
        
        var bgColor = type === 'success' ? '#10b981' : '#ef4444';
        var borderColor = type === 'success' ? '#059669' : '#dc2626';
        
        notification.style.cssText = 'background-color: ' + bgColor + '; ' +
            'color: white; ' +
            'padding: 14px 20px; ' +
            'border-radius: 8px; ' +
            'margin-bottom: 10px; ' +
            'font-size: 14px; ' +
            'font-weight: 500; ' +
            'box-shadow: 0 4px 12px rgba(0,0,0,0.15); ' +
            'border-left: 4px solid ' + borderColor + '; ' +
            'animation: slideIn 0.3s ease-out forwards; ' +
            'pointer-events: auto; ' +
            'min-width: 250px; ' +
            'max-width: 400px;';
        
        container.appendChild(notification);

        // Auto-remove after 4 seconds with fade-out animation
        var timeout = setTimeout(function() {
            notification.style.animation = 'slideOut 0.3s ease-out forwards';
            setTimeout(function() {
                try { notification.parentNode.removeChild(notification); } catch(e) {}
            }, 300);
        }, 4000);

        // Return notification object so it can be manually removed if needed
        return { element: notification, timeout: timeout };
    }

    // Add CSS animations if not already present
    function addNotificationStyles() {
        var styleId = 'notification-styles';
        if (document.getElementById(styleId)) return;
        
        var style = document.createElement('style');
        style.id = styleId;
        style.textContent = '@keyframes slideIn { ' +
            'from { transform: translateX(420px); opacity: 0; } ' +
            'to { transform: translateX(0); opacity: 1; } ' +
            '} ' +
            '@keyframes slideOut { ' +
            'from { transform: translateX(0); opacity: 1; } ' +
            'to { transform: translateX(420px); opacity: 0; } ' +
            '}';
        document.head.appendChild(style);
    }

    addNotificationStyles();

    function uploadAvatar(event) {
        try {
            var file = event.target.files && event.target.files[0];
            if (!file) return;

            var formData = new FormData();
            formData.append('avatarFile', file);

            function doFetch(retryCount) {
                return fetch(base + '/avatar-upload', {
                    method: 'POST',
                    body: formData,
                    credentials: 'include'  // Important: ensure cookies are sent
                })
                .then(function(response){
                    return response.text().then(function(text){
                        var data;
                        try {
                            data = JSON.parse(text);
                        } catch (e) {
                            // If not JSON, treat as plain text error
                            if (!response.ok) {
                                throw new Error('HTTP ' + response.status + ': ' + text);
                            }
                            throw new Error('Invalid JSON response from server');
                        }
                        if (!response.ok) {
                            // If 429 (Too Many Requests), retry after delay
                            if (response.status === 429 && retryCount < 5) {
                                console.log('Rate limited, retrying in 5 seconds... (attempt ' + (retryCount + 1) + '/5)');
                                return new Promise(function(resolve) {
                                    setTimeout(function() {
                                        resolve(doFetch(retryCount + 1));
                                    }, 5000);
                                });
                            }
                            throw new Error((data && data.error) ? data.error : ('HTTP ' + response.status));
                        }
                        return data;
                    });
                });
            }

            doFetch(0)
            .then(function(data){
                if (data && data.success) {
                    console.log('avatar-upload response', data);
                    showNotification('Avatar Update Successful', 'success');
                    
                    var rawUrl = data.avatarUrlAbsolute || data.avatarUrl || (base + '/avatar?userId=' + data.userId);
                    // force cache-bust
                    var avatarUrl = rawUrl + (rawUrl.indexOf('?') === -1 ? '?t=' : '&t=') + (data.timestamp || new Date().getTime());

                    // preload image first, then update DOM to avoid stale/broken images
                    var img = new Image();
                    img.onload = function(){
                        try {
                            var mainAvatar = document.querySelector('[data-alt="User profile picture large"]');
                            if (mainAvatar) mainAvatar.style.backgroundImage = 'url(' + avatarUrl + ')';

                            var headerAvatar = document.querySelector('[data-alt="User avatar small"]');
                            if (headerAvatar) headerAvatar.style.backgroundImage = 'url(' + avatarUrl + ')';

                            var headerAvatarImg = document.querySelector('header img[alt="avatar"], #user-dropdown-menu img[alt="avatar"]');
                            if (headerAvatarImg) headerAvatarImg.src = avatarUrl;

                            var modalImg = document.querySelector('#avatarViewModal img');
                            if (modalImg) modalImg.src = avatarUrl;
                        } catch(e){ console.error('Error updating avatar elements', e); }
                    };
                    img.onerror = function(){
                        console.error('Failed to load avatar URL', avatarUrl);
                    };
                    img.src = avatarUrl;
                } else {
                    var errorMsg = (data && data.error) ? data.error : 'Upload failed';
                    console.error('Upload response:', data);
                    showNotification('Avatar Upload Failed', 'error');
                }
            })
            .catch(function(err){
                console.error('Avatar upload error:', err);
                showNotification('Avatar Upload Failed', 'error');
            })
            .finally(function(){ try { event.target.value = ''; } catch(e){} });
        } catch(e) { console.error('uploadAvatar handler exception', e); }
    }

    try {
        inp.addEventListener('change', uploadAvatar);
    } catch(e) { console.error('Failed to attach avatar change handler', e); }
})();
