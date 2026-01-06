// Notification system for address operations
function showAddressNotification(message, type) {
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

    return { element: notification, timeout: timeout };
}

// Add global AJAX listener for all JSF AJAX responses
function setupGlobalJSFAjaxListener() {
    try {
        console.log('=== Setting up global JSF AJAX listener ===');
        if (typeof jsf !== 'undefined' && jsf.ajax && typeof jsf.ajax.addOnEvent === 'function') {
            jsf.ajax.addOnEvent(function(data) {
                try {
                    console.log('=== JSF AJAX Event ===', data.status);
                    if (data.status === 'success') {
                        // Give JSF time to update DOM
                        setTimeout(function() { handleSaveAddressCompleteGlobal(); }, 100);
                    }
                } catch (e) { console.error('JSF addOnEvent handler error:', e); }
            });
            return true;
        }
    } catch (e) {
        console.error('setupGlobalJSFAjaxListener error:', e);
    }
    return false;
}

// Try to attach immediately or when DOM ready; also poll briefly for jsf object
if (document.readyState === 'loading') {
    document.addEventListener('DOMContentLoaded', function() { if (!setupGlobalJSFAjaxListener()) {
        // start polling for jsf.ajax for a short while
        var tries = 0; var maxTries = 25; var poll = setInterval(function(){ tries++; if (setupGlobalJSFAjaxListener() || tries>maxTries) clearInterval(poll); }, 200);
    }});
} else {
    if (!setupGlobalJSFAjaxListener()) {
        var tries = 0; var maxTries = 25; var poll = setInterval(function(){ tries++; if (setupGlobalJSFAjaxListener() || tries>maxTries) clearInterval(poll); }, 200);
    }
}

// Global handler for all address save AJAX responses
function handleSaveAddressCompleteGlobal() {
    try {
        var resEl = document.getElementById('addressForm:addressSaveResult') || _el('addressSaveResult');
        var sevEl = document.getElementById('addressForm:addressSaveSeverity') || _el('addressSaveSeverity');
        
        var msg = '';
        var sev = '';
        
        if (resEl) {
            msg = (resEl.value || resEl.textContent || '').toString().trim();
        }
        if (sevEl) {
            sev = (sevEl.value || sevEl.textContent || '').toString().trim();
        }
        
        console.log('=== Global AJAX Handler: msg=', msg, 'sev=', sev);
        
        if (msg) {
            var container = document.getElementById('notification-container');
            if (container) container.style.zIndex = 100100;
            
            if (sev === 'success') {
                var modal = document.getElementById('addressModal');
                if (modal) modal.classList.add('hidden');
                showAddressNotification(msg, 'success');
                setTimeout(function() { refreshAddressCards(); }, 500);
            } else {
                showAddressNotification(msg, 'error');
            }
            
            // Clear values after processing
            try {
                if (resEl) resEl.value = resEl.textContent = '';
                if (sevEl) sevEl.value = sevEl.textContent = '';
            } catch (e) {}
        }
    } catch (e) {
        console.error('handleSaveAddressCompleteGlobal error:', e);
    }
}

// Fallback: observe DOM changes for the hidden save result fields (robust if JSF AJAX event not fired)
function setupSaveResultObserver() {
    try {
        var processed = false;
        function checkAndProcess() {
            if (processed) return;
            var resEl = document.getElementById('addressForm:addressSaveResult') || _el('addressSaveResult');
            var sevEl = document.getElementById('addressForm:addressSaveSeverity') || _el('addressSaveSeverity');
            var msg = '';
            var sev = '';
            if (resEl) msg = (resEl.value || resEl.textContent || '').toString().trim();
            if (sevEl) sev = (sevEl.value || sevEl.textContent || '').toString().trim();
            if (msg) {
                processed = true;
                try { console.log('SaveResultObserver detected msg=', msg, 'sev=', sev); } catch (e) {}
                var container = document.getElementById('notification-container'); if (container) container.style.zIndex = 100100;
                if (sev === 'success') {
                    var modal = document.getElementById('addressModal'); if (modal) modal.classList.add('hidden');
                    showAddressNotification(msg, 'success');
                    setTimeout(function(){ refreshAddressCards(); }, 500);
                } else {
                    showAddressNotification(msg, 'error');
                }
                try { if (resEl) { if (typeof resEl.value !== 'undefined') resEl.value = ''; else resEl.textContent=''; } if (sevEl) { if (typeof sevEl.value !== 'undefined') sevEl.value = ''; else sevEl.textContent=''; } } catch(e){}
            }
        }

        // Run an initial check in case fields are already present
        checkAndProcess();

        // Observe document body for DOM changes
        var observer = new MutationObserver(function(muts) {
            checkAndProcess();
            // reset processed flag after short delay to allow subsequent saves
            setTimeout(function(){ processed = false; }, 1000);
        });
        observer.observe(document.body, { childList: true, subtree: true, characterData: true });
    } catch (e) {
        console.error('setupSaveResultObserver error:', e);
    }
}

// start observer immediately
try { setupSaveResultObserver(); } catch (e) {}

// Handle AJAX response from save address
function handleSaveAddressComplete(xhr, status, args) {
    console.log('=== handleSaveAddressComplete CALLED ===', {xhr: !!xhr, status: status, args: args});
    
    // Prefer reading the hidden AJAX result fields (addressSaveResult / addressSaveSeverity)
    // Try both namespaced and non-namespaced versions
    var getEl = function(id){ return _el(id); };
    var resEl = getEl('addressSaveResult') || document.getElementById('addressForm:addressSaveResult');
    console.log('resEl found:', !!resEl, 'value:', resEl ? (resEl.value || resEl.textContent) : 'N/A');
    var sevEl = getEl('addressSaveSeverity') || document.getElementById('addressForm:addressSaveSeverity');
    console.log('sevEl found:', !!sevEl, 'value:', sevEl ? (sevEl.value || sevEl.textContent) : 'N/A');
    var msg = '';
    var sev = '';
    if (resEl) {
        if (typeof resEl.value !== 'undefined') msg = (resEl.value || '').toString().trim();
        else msg = (resEl.textContent || '').toString().trim();
    }
    if (sevEl) {
        if (typeof sevEl.value !== 'undefined') sev = (sevEl.value || '').toString().trim();
        else sev = (sevEl.textContent || '').toString().trim();
    }
    console.log('Extracted msg:', msg, 'sev:', sev);

    if (msg) {
        // clear the hidden fields so subsequent calls don't re-use them
        try {
            if (resEl) { if (typeof resEl.value !== 'undefined') resEl.value = ''; else resEl.textContent = ''; }
            if (sevEl) { if (typeof sevEl.value !== 'undefined') sevEl.value = ''; else sevEl.textContent = ''; }
        } catch (e) {}

        // debug: log values
        try { console.log('handleSaveAddressComplete: found ajax result', {msg: msg, sev: sev}); } catch(e){}

        if (sev === 'success') {
            var modal = document.getElementById('addressModal');
            if (modal) modal.classList.add('hidden');
            // ensure toast is above modal by raising z-index
            var container = document.getElementById('notification-container');
            if (container) container.style.zIndex = 100100;
            showAddressNotification(msg, 'success');
            setTimeout(function(){ refreshAddressCards(); }, 500);
        } else {
            // show error but keep modal open
            var container = document.getElementById('notification-container');
            if (container) container.style.zIndex = 100100;
            showAddressNotification(msg, 'error');
        }
        return;
    }

    // Fallback A: try to read values from JSF partial-response XML (xhr.responseXML)
    try {
        if (xhr && xhr.responseXML) {
            var updates = xhr.responseXML.getElementsByTagName('update');
            var parsedMsg = '';
            var parsedSev = '';
            for (var i = 0; i < updates.length; i++) {
                var upd = updates[i];
                var id = upd.getAttribute('id') || '';
                if (id && (id.endsWith('addressSaveResult') || id.indexOf(':addressSaveResult') !== -1)) {
                    parsedMsg = (upd.textContent || '').trim();
                }
                if (id && (id.endsWith('addressSaveSeverity') || id.indexOf(':addressSaveSeverity') !== -1)) {
                    parsedSev = (upd.textContent || '').trim();
                }
            }
            if (parsedMsg) {
                try { console.log('handleSaveAddressComplete: parsed from responseXML', {msg: parsedMsg, sev: parsedSev}); } catch(e){}
                var container = document.getElementById('notification-container'); if (container) container.style.zIndex = 100100;
                if (parsedSev === 'success') {
                    var modal = document.getElementById('addressModal'); if (modal) modal.classList.add('hidden');
                    showAddressNotification(parsedMsg, 'success');
                    setTimeout(function(){ refreshAddressCards(); }, 500);
                } else {
                    showAddressNotification(parsedMsg, 'error');
                }
                return;
            }
        }
    } catch (e) { /* ignore parse fallback errors */ }

    // Fallback B: attempt to read the messages div (older behavior)
    var addressMsgsDiv = document.querySelector('[id$="addressMsgs"]');
    if (addressMsgsDiv) {
        // small defer to ensure JSF DOM updates are settled
        setTimeout(function(){
            var errorElements = addressMsgsDiv.querySelectorAll('[class*="error"]');
            var infoElements = addressMsgsDiv.querySelectorAll('[class*="info"]');
            var errorMsg = '';
            var successMsg = '';
            if (errorElements && errorElements.length > 0) {
                errorElements.forEach(function(el){ if(el && el.textContent && !errorMsg) errorMsg = el.textContent.trim(); });
            }
            if (infoElements && infoElements.length > 0 && !errorMsg) {
                infoElements.forEach(function(el){ if(el && el.textContent && !successMsg) successMsg = el.textContent.trim(); });
            }
            if (successMsg) {
                var modal = document.getElementById('addressModal'); if (modal) modal.classList.add('hidden');
                addressMsgsDiv.innerHTML = '';
                showAddressNotification(successMsg, 'success');
                setTimeout(function(){ refreshAddressCards(); }, 500);
            } else if (errorMsg) {
                addressMsgsDiv.innerHTML = '';
                addressMsgsDiv.classList.add('hidden');
                showAddressNotification(errorMsg, 'error');
            }
        }, 100);
    }
}

// Add CSS animations if not already present
(function addNotificationStyles() {
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
})();

// Helper functions for address card edit/delete
function triggerEdit(event, btnId) {
    try{
        if(event && typeof event.preventDefault === 'function') event.preventDefault();
        if(event && typeof event.stopPropagation === 'function') event.stopPropagation();
    }catch(e){}
    try {
        var clicked = null;
        // Prefer the clicked element's form submit input (JSF-generated)
        if(event && event.target){
            var src = event.target;
            if(src.tagName === 'IMG' && src.parentElement) src = src.parentElement;
            var form = src.closest('form');
            if(form){
                clicked = form.querySelector('input[type="submit"],button[type="submit"]');
                if(clicked){ try{ clicked.click(); return false; }catch(e){} }
            }
        }

        // Fallback: find by id-suffix (namespaced JSF id)
        var selector = '[id$="' + btnId + '"]';
        var btn = document.querySelector(selector) || document.getElementById(btnId);
        if(btn){ try{ btn.click(); return false; }catch(e){} }

        // Last resort: numeric suffix match
        try{
            var suffixMatch = btnId.match(/_(\d+)$/);
            if(suffixMatch){
                var suffix = suffixMatch[1];
                var nodes = document.querySelectorAll('[id*="' + suffix + '"]');
                for(var i=0;i<nodes.length;i++){
                    var n = nodes[i];
                    if(/editAddrBtn_/.test(n.id) || /editAddrForm_/.test(n.id) || n.getAttribute('onclick')){
                        try{ n.click(); return false; }catch(e){}
                    }
                }
            }
        }catch(e){ /* ignore */ }
    } catch (e) { console.error('triggerEdit error:', e); }
    return false;
}

function triggerDelete(event, btnId, addressName) {
    // Open delete confirmation modal and store which JSF button to trigger on confirm
    try { if (event && typeof event.preventDefault === 'function') event.preventDefault(); } catch(e){}
    try { if (event && typeof event.stopPropagation === 'function') event.stopPropagation(); } catch(e){}
    openDeleteConfirm(btnId, addressName, event);
    return false;
}

// Delete confirmation modal handling
var _pendingDeleteBtnId = null;
var _pendingDeleteBtnEl = null;
var _pendingDeleteFormEl = null;
function openDeleteConfirm(btnId, label, evt){
    try{
        _pendingDeleteBtnId = btnId;
        // If we were called from a click event, prefer its nearest form/submit input (this is the JSF-generated element)
        try{
            if(evt && evt.target){
                var src = evt.target;
                // if the user clicked the inner img, climb up to the button
                if(src.tagName === 'IMG' && src.parentElement) src = src.parentElement;
                var frm = src.closest('form');
                if(frm){
                    _pendingDeleteFormEl = frm;
                    // find the input[type=submit] inside this form
                    var submitInput = frm.querySelector('input[type="submit"],button[type="submit"]');
                    if(submitInput) _pendingDeleteBtnEl = submitInput;
                }
            }
        }catch(e){ console.warn('openDeleteConfirm: could not capture delete button element from event', e); }
        // fallback: try to capture by id-suffix search
        if(!_pendingDeleteBtnEl){
            try{
                var sel = '[id$="' + btnId + '"]';
                _pendingDeleteBtnEl = document.querySelector(sel) || document.getElementById(btnId) || null;
                if(_pendingDeleteBtnEl) _pendingDeleteFormEl = _pendingDeleteBtnEl.closest('form') || null;
            }catch(e){ /* ignore */ }
        }
        var modal = document.getElementById('deleteConfirmModal');
        var msgEl = document.getElementById('deleteConfirmMessage');
        var i18n = document.getElementById('i18nMessages');
        var confirmMsg = i18n ? (i18n.dataset.confirmDelete || 'Are you sure you want to delete this address?') : 'Are you sure you want to delete this address?';
        var fullText = label ? (confirmMsg + '\n"' + label + '"') : confirmMsg;
        if(msgEl) msgEl.textContent = fullText;
        if(modal) modal.classList.remove('hidden');
        // Ensure button labels are visible — prefer localized strings from `i18nMessages`
        try{
            var i18n = document.getElementById('i18nMessages');
            var cancelText = i18n?.dataset?.cancelBtn || i18n?.dataset?.cancel || (typeof window.cancelText !== 'undefined' ? window.cancelText : 'Cancel');
            var confirmText = i18n?.dataset?.confirmBtn || i18n?.dataset?.confirm || (typeof window.confirmText !== 'undefined' ? window.confirmText : 'Confirm');
            document.querySelectorAll('#deleteConfirmModal .delete-btn-text').forEach(function(span, idx){
                try{ span.innerText = (idx===0 ? cancelText : confirmText); }catch(e){}
            });
        }catch(e){ console.warn('openDeleteConfirm: could not set button labels', e); }
    }catch(e){ console.error('openDeleteConfirm error', e); }
}

function confirmDeleteNow(){
    try{
        var fired = false;
        if(_pendingDeleteBtnEl){
            try{ _pendingDeleteBtnEl.disabled = false; _pendingDeleteBtnEl.click(); fired = true; }catch(e){ console.warn('click on stored button failed', e); }
        }
        if(!fired && _pendingDeleteBtnId){
            // find button by ends-with id (JSF namespacing)
            var selector = '[id$="' + _pendingDeleteBtnId + '"]';
            var btn = document.querySelector(selector) || document.getElementById(_pendingDeleteBtnId);
            if(btn){
                try{ btn.disabled = false; btn.click(); fired = true; }catch(e){ console.warn('btn.click failed', e); }
            }
            // fallback: try to find the containing form and submit it
            var formId = null;
            try{
                // common pattern: delAddrBtn_123 belongs to form id delAddrForm_123
                formId = _pendingDeleteBtnId.replace('delAddrBtn_', 'delAddrForm_');
                var form = document.querySelector('[id$="' + formId + '"]') || document.getElementById(formId);
                if(form){
                    // try to find submit/input element inside form and click it
                    var submitEl = form.querySelector('input[type="submit"],button[type="submit"],[id$="' + _pendingDeleteBtnId + '"]');
                    if(submitEl){ try{ submitEl.click(); fired = true; }catch(e){ console.warn('submitEl.click failed', e); } }
                    try{ if(!fired){ form.submit(); fired = true; } }catch(e){ console.warn('form.submit failed', e); }
                }
            }catch(e){ console.warn('form fallback failed', e); }
            // last resort: attempt JSF AJAX request if available
            try{
                if(window.jsf && typeof window.jsf.ajax === 'object'){
                    // find a source element for the request
                    var source = document.querySelector(selector) || document.getElementById(_pendingDeleteBtnId);
                            if(source){ try{ window.jsf.ajax.request(source, null, {execute:'@form', render:'@none'}); fired = true; }catch(e){ console.warn('jsf.ajax.request failed', e); } }
                }
            }catch(e){ console.warn('jsf.ajax fallback failed', e); }
            // Additional fallback: try to find any element with numeric suffix match
            try{
                var suffixMatch = _pendingDeleteBtnId.match(/_(\d+)$/);
                if(suffixMatch){
                    var suffix = suffixMatch[1];
                    var nodes = document.querySelectorAll('[id*="' + suffix + '"]');
                    for(var i=0;i<nodes.length;i++){
                        var n = nodes[i];
                        if(/delAddrBtn_/.test(n.id) || /delAddrForm_/.test(n.id) || n.getAttribute('onclick')){
                            try{ n.click(); fired = true; break; }catch(e){}
                        }
                    }
                }
            }catch(e){ console.warn('suffix fallback failed', e); }
        }
        }catch(e){ console.error('confirmDeleteNow error', e); }
        // only close modal if we actually triggered something
        if(fired){
            var modal = document.getElementById('deleteConfirmModal'); if(modal) modal.classList.add('hidden');
            _pendingDeleteBtnId = null;
            _pendingDeleteBtnEl = null;
            _pendingDeleteFormEl = null;
            // Fallback: if JSF oncomplete wasn't wired into the generated AJAX call, ensure UI updates
            try{
                setTimeout(function(){
                    try{ if(typeof handleDeleteSuccess === 'function') handleDeleteSuccess(); }catch(e){ console.warn('handleDeleteSuccess fallback failed', e); }
                    try{ if(typeof refreshAddressCards === 'function') refreshAddressCards(); }catch(e){ console.warn('refreshAddressCards fallback failed', e); }
                }, 250);
            }catch(e){ console.warn('post-delete fallback scheduling failed', e); }
        } else {
            // notify user and keep modal open for retry
            console.warn('Delete action could not be triggered for:', _pendingDeleteBtnId);
            if(typeof showAddressNotification === 'function') showAddressNotification('Could not trigger delete action — try again or refresh the page.', 'error');
        }
}

function cancelDelete(){ var modal = document.getElementById('deleteConfirmModal'); if(modal) modal.classList.add('hidden'); _pendingDeleteBtnId = null; }

// Full region lists: Vietnam provinces/municipalities and US states (cities left as empty maps for now)
const REGION_DATA = {
    'Vietnam': {
        regions: [
            'TP HÀ NỘI','TP HỒ CHÍ MINH','TP HẢI PHÒNG','TP ĐÀ NẴNG','TP HUẾ','TP CẦN THƠ','TỈNH AN GIANG','TỈNH BẮC NINH','TỈNH CÀ MAU','TỈNH CAO BẰNG',
            'TỈNH ĐẮK LẮK','TỈNH ĐIỆN BIÊN','TỈNH ĐỒNG NAI','TỈNH GIA LAI','TỈNH HƯNG YÊN','TỈNH HÀ TĨNH','TỈNH HÀ GIANG','TỈNH LAI CHÂU','TỈNH KHÁNH HÒA','TỈNH LẠNG SƠN',
            'TỈNH LÀO CAI','TỈNH LÂM ĐỒNG','TỈNH NGHỆ AN','TỈNH NINH BÌNH','TỈNH PHÚ THỌ','TỈNH QUẢNG NGÃI','TỈNH QUẢNG NINH','TỈNH QUẢNG TRỊ','TỈNH SƠN LA','TỈNH TÂY NINH',
            'TỈNH THÁI NGUYÊN','TỈNH THANH HÓA','TỈNH TUYÊN QUANG','TỈNH VĨNH LONG','TỈNH ĐỒNG THÁP'
        ],
        cities: {},
        wards: {}
    },
    'United States': {
        regions: [
            'Alabama','Alaska','Arizona','Arkansas','California','Colorado','Connecticut','Delaware','Florida','Georgia',
            'Hawaii','Idaho','Illinois','Indiana','Iowa','Kansas','Kentucky','Louisiana','Maine','Maryland',
            'Massachusetts','Michigan','Minnesota','Mississippi','Missouri','Montana','Nebraska','Nevada','New Hampshire','New Jersey',
            'New Mexico','New York','North Carolina','North Dakota','Ohio','Oklahoma','Oregon','Pennsylvania','Rhode Island','South Carolina',
            'South Dakota','Tennessee','Texas','Utah','Vermont','Virginia','Washington','West Virginia','Wisconsin','Wyoming','District of Columbia'
        ],
        cities: {},
        wards: {}
    }
};

function _el(id){ return document.querySelector('[id$="'+id+'"]'); }

// Determine application context path by locating this script's src
function getContextPath(){
    try{
        const scripts = document.getElementsByTagName('script');
        for(let i=0;i<scripts.length;i++){
            const s = scripts[i];
            if(s.src && s.src.indexOf('/resources/js/profile-address.js') !== -1){
                return s.src.split('/resources/js/profile-address.js')[0];
            }
        }
    }catch(e){}
    return '';
}

function onTypeChange(){
    const t = _el('addr_type_ui').value;
    const hiddenType = _el('addr_type');
    if(hiddenType) hiddenType.value = t;
}

function filterOptions(selectId, query){
    const q = (query||'').toLowerCase();
    const select = document.getElementById(selectId);
    if(!select) return;
    const all = select._allOptions || [];
    select.innerHTML='';
    all.filter(o=>o.toLowerCase().includes(q)).forEach(v=>{
        const opt = document.createElement('option'); opt.value = v; opt.textContent = v; select.appendChild(opt);
    });
}

// Clear map if it's currently shown
function clearMapIfShown(){
    const mapWrapper = _el('step_map_wrapper');
    const btn = document.getElementById('select_map_btn');
    if(mapWrapper && !mapWrapper.classList.contains('hidden')){
        mapWrapper.classList.add('hidden');
        if(btn) btn.style.display = '';
    }
    // Clear map coordinates
    const lat = _el('addr_lat'); if(lat) lat.value = '';
    const lng = _el('addr_lng'); if(lng) lng.value = '';
    const mapSel = _el('addr_map_selected'); if(mapSel) mapSel.value = '';
}

// Toggle visual handler for default checkbox (clicking the visual element)
function toggleDefaultCheckbox(){
    const cb = document.getElementById('addr_is_default');
    if(!cb) return;
    cb.checked = !cb.checked;
    // update hidden binding immediately
    const h = _el('addr_is_default_hidden'); if(h) h.value = cb.checked ? 'true' : 'false';
    // trigger change event if needed
    try{ cb.dispatchEvent(new Event('change')); }catch(e){}
}

function showFieldError(fieldId, msg){
    try{
        const err = document.getElementById(fieldId + '_error');
        if(err){ err.textContent = msg || ''; err.classList.remove('hidden'); }
        const inp = document.getElementById(fieldId);
        if(inp) inp.classList.add('border-red-500');
    }catch(e){}
}

function clearFieldError(fieldId){
    try{
        const err = document.getElementById(fieldId + '_error');
        if(err){ err.textContent = ''; err.classList.add('hidden'); }
        const inp = document.getElementById(fieldId);
        if(inp) inp.classList.remove('border-red-500');
    }catch(e){}
}

function clearAllErrors(){
    ['addr_type_ui','addr_country_ui','addr_region_input','addr_city_input','addr_zip_ui','addr_street_ui','addr_house_ui','addr_map'].forEach(id=>{ clearFieldError(id); });
}

function onCountryChange(){
    const country = document.getElementById('addr_country_ui').value;
    const hiddenCountry = _el('addr_country'); if(hiddenCountry) hiddenCountry.value = country;
    
    // reset downstream UI
    _el('step_region').classList.toggle('hidden', !country);
    _el('step_city').classList.add('hidden');
    _el('step_zip').classList.add('hidden');
    _el('step_street').classList.add('hidden');
    _el('step_house').classList.add('hidden');
    document.getElementById('map_button_row').classList.add('hidden');
    clearMapIfShown(); // Clear map if it was previously shown
    
    // clear all region/city/street/house/zip values from inputs AND Tom Select
    const regionInput = document.getElementById('addr_region_input');
    const cityInput = document.getElementById('addr_city_input');
    
    if(regionInput && regionInput.tom){ try{ regionInput.tom.clear(); regionInput.tom.clearOptions(); }catch(e){} }
    if(cityInput && cityInput.tom){ try{ cityInput.tom.clear(); cityInput.tom.clearOptions(); }catch(e){} }
    
    const clears = ['addr_region_input','addr_city_input','addr_zip_ui','addr_street_ui','addr_house_ui'];
    clears.forEach(id=>{ const el = document.getElementById(id); if(el) el.value = ''; });
    const hClears = ['addr_region','addr_city','addr_street','addr_house','addr_lat','addr_lng','addr_map_selected'];
    hClears.forEach(id=>{ const hel = _el(id); if(hel) hel.value = ''; });
    
    // load detailed region/city data on-demand
    loadRegionData(country).then(()=>{
        // after loading, Tom Select will use the updated REGION_DATA
        // refresh region TomSelect options if present
        const regionInputEl = document.getElementById('addr_region_input');
        if(regionInputEl && regionInputEl.tom){
            try{
                regionInputEl.tom.clearOptions();
                regionInputEl.tom.clear();
                if(typeof regionInputEl.tom.refreshOptions === 'function') regionInputEl.tom.refreshOptions(true);
                // trigger a load with empty query then open
                try{ regionInputEl.tom.load(''); }catch(e){}
                regionInputEl.tom.open();
            }catch(e){console.error('refresh region tomselect failed', e);} 
        }
    }).catch(()=>{
        // even if JSON load fails, fallback data should be available
    });
    
    // adjust labels
    if(country === 'Vietnam'){
        const regionLabel = _el('region_label');
        const cityLabel = _el('city_label');
        const wardLabel = _el('ward_label');
        if(regionLabel) regionLabel.textContent = 'Province';
        if(cityLabel) cityLabel.textContent = 'District';
        if(wardLabel) wardLabel.textContent = 'Ward';
    } else {
        const regionLabel = _el('region_label');
        const cityLabel = _el('city_label');
        const wardLabel = _el('ward_label');
        if(regionLabel) regionLabel.textContent = 'State';
        if(cityLabel) cityLabel.textContent = 'City / Town';
        if(wardLabel) wardLabel.textContent = 'Ward';
    }
}

// Load additional data files for detailed subdivisions if present
function loadRegionData(country){
    return new Promise((resolve, reject)=>{
        try{
                if(country === 'Vietnam'){
                // fetch full Vietnam provinces JSON
                fetch(getContextPath() + '/resources/data/vn-provinces.json')
                .then(r=>{ if(!r.ok) throw new Error('no data'); return r.json(); })
                .then(obj=>{
                    // obj structure: { "Hà Nội": { "districts": [...] }, ... }
                    const provinces = Object.keys(obj).sort();
                    REGION_DATA['Vietnam'] = {
                        regions: provinces,
                        cities: {},
                        wards: {}
                    };
                    provinces.forEach(p=>{
                        const districts = obj[p].districts || [];
                        // normalize to objects with optional zip
                        REGION_DATA['Vietnam'].cities[p] = districts.map(d=>({name: d, zip: ''}));
                        districts.forEach(d=>{ REGION_DATA['Vietnam'].wards[d] = [d]; });
                    });
                    // attempt to load detailed vn-districts (with zipcodes) if present
                    fetch(getContextPath() + '/resources/data/vn-districts.json')
                    .then(r=>{ if(!r.ok) throw new Error('no vn-districts'); return r.json(); })
                    .then(dobj=>{
                        // dobj expected: { "Province": [{"name":"District","zip":"xxxxx"}, ...], ... }
                        Object.keys(dobj).forEach(p=>{
                            const list = dobj[p] || [];
                            if(list && list.length){
                                // normalize to {name, zip} objects; keep ALL districts (even without zip)
                                const mapped = list.map(it=>({name: it.name, zip: (it.zip||'').toString().trim()}));
                                REGION_DATA['Vietnam'].cities[p] = mapped;
                                // fill wards mapping per district
                                mapped.forEach(it=>{ if(it.name) REGION_DATA['Vietnam'].wards[it.name] = [it.name]; });
                            }
                        });
                        console.log('VN districts loaded:', Object.keys(dobj).length, 'provinces');
                        resolve();
                    }).catch(()=>{
                        // no detailed file; resolve with normalized data
                        resolve();
                    });
                    return;
                    console.log('Vietnam data loaded:', REGION_DATA['Vietnam'].regions.length, 'provinces');
                    resolve();
                }).catch(e=>{ console.error('Error loading VN data:', e); reject(e); });
                return;
            }
            if(country === 'United States'){
                // fetch full US states/cities JSON
                fetch(getContextPath() + '/resources/data/us-cities.json')
                .then(r=>{ if(!r.ok) throw new Error('no data'); return r.json(); })
                .then(obj=>{
                    // obj structure: { "Alabama": ["city1", "city2", ...], ... } 
                    // We need to convert this to { "state": { "cities": [...] } } format
                    const states = Object.keys(obj).sort();
                    REGION_DATA['United States'] = {
                        regions: states,
                        cities: {},
                        wards: {}
                    };
                    states.forEach(s=>{ 
                        const cities = obj[s];
                        REGION_DATA['United States'].cities[s] = Array.isArray(cities) ? cities : (cities.cities || []);
                    });
                    console.log('US data loaded:', REGION_DATA['United States'].regions.length, 'states');
                    resolve();
                }).catch(e=>{ console.error('Error loading US data:', e); reject(e); });
                return;
            }
            resolve();
        } catch(e){ reject(e); }
    });
}

function onRegionChange(){
    const country = document.getElementById('addr_country_ui').value;
    const sel = document.getElementById('addr_region_select');
    const region = sel.value;
    const hiddenRegion = _el('addr_region'); if(hiddenRegion) hiddenRegion.value = region;
    const citySel = document.getElementById('addr_city_select'); citySel.innerHTML='';
    // populate cities/districts
    if(REGION_DATA[country] && REGION_DATA[country].cities && REGION_DATA[country].cities[region]){
        citySel._allOptions = REGION_DATA[country].cities[region].slice();
        REGION_DATA[country].cities[region].forEach(c=>{ const o=document.createElement('option'); o.value=c; o.textContent=c; citySel.appendChild(o); });
    }
    try{ const sc = _el('step_city'); if(sc) sc.classList.toggle('hidden', !region); }catch(e){}
    try{ const sw = _el('step_ward'); if(sw) sw.classList.add('hidden'); }catch(e){}
    try{ const sz = _el('step_zip'); if(sz) sz.classList.add('hidden'); }catch(e){}
    try{ const ss = _el('step_street'); if(ss) ss.classList.add('hidden'); }catch(e){}
    try{ const sh = _el('step_house'); if(sh) sh.classList.add('hidden'); }catch(e){}
    try{ const mbr = document.getElementById('map_button_row'); if(mbr) mbr.classList.add('hidden'); }catch(e){}
    // clear downstream values when region changes
    ['addr_city_select','addr_ward_select','addr_zip_ui','addr_street_ui','addr_house_ui'].forEach(id=>{ const el=document.getElementById(id); if(el) el.value=''; });
    ['addr_city','addr_state','addr_street','addr_house','addr_lat','addr_lng','addr_map_selected'].forEach(id=>{ const hel=_el(id); if(hel) hel.value=''; });
}

function onCityChange(){
    const country = document.getElementById('addr_country_ui').value;
    const citySel = document.getElementById('addr_city_select');
    const city = citySel.value;
    const hiddenCity = _el('addr_city'); if(hiddenCity) hiddenCity.value = city;
    // Vietnam: try to populate ward list if available
    if(country === 'Vietnam'){
        const wards = (REGION_DATA['Vietnam'].wards && REGION_DATA['Vietnam'].wards[city]) || [];
        const wardSel = document.getElementById('addr_ward_select'); wardSel.innerHTML='';
        if(wards.length){
            wardSel._allOptions = wards.slice();
            wards.forEach(w=>{ const o=document.createElement('option'); o.value=w; o.textContent=w; wardSel.appendChild(o); });
            try{ const sw = _el('step_ward'); if(sw) sw.classList.remove('hidden'); }catch(e){}
            try{ const ss = _el('step_street'); if(ss) ss.classList.add('hidden'); }catch(e){}
            try{ const sh = _el('step_house'); if(sh) sh.classList.add('hidden'); }catch(e){}
            try{ const mbr = document.getElementById('map_button_row'); if(mbr) mbr.classList.add('hidden'); }catch(e){}
            // clear downstream when city changes
            ['addr_ward_select','addr_zip_ui','addr_street_ui','addr_house_ui'].forEach(id=>{ const el=document.getElementById(id); if(el) el.value=''; });
            ['addr_state','addr_street','addr_house','addr_lat','addr_lng','addr_map_selected'].forEach(id=>{ const hel=_el(id); if(hel) hel.value=''; });
            return;
        }
    }
    // United States: show ZIP input
    if(country === 'United States'){
        try{ const sz = _el('step_zip'); if(sz) sz.classList.remove('hidden'); }catch(e){}
        try{ const ss = _el('step_street'); if(ss) ss.classList.add('hidden'); }catch(e){}
        try{ const sh = _el('step_house'); if(sh) sh.classList.add('hidden'); }catch(e){}
        try{ const mbr = document.getElementById('map_button_row'); if(mbr) mbr.classList.add('hidden'); }catch(e){}
        return;
    }
    // default: show street and house
    try{ const ss = _el('step_street'); if(ss) ss.classList.toggle('hidden', !city); }catch(e){}
    try{ const sh = _el('step_house'); if(sh) sh.classList.toggle('hidden', !city); }catch(e){}
    try{ const mbr = document.getElementById('map_button_row'); if(mbr) mbr.classList.toggle('hidden', !city); }catch(e){}
}

function onWardChange(){
    const wardSel = document.getElementById('addr_ward_select'); const ward = wardSel.value;
    const hState = _el('addr_state'); if(hState) hState.value = ward; // store ward in state column
    try{ const ss = _el('step_street'); if(ss) ss.classList.toggle('hidden', !ward); }catch(e){}
    try{ const sh = _el('step_house'); if(sh) sh.classList.toggle('hidden', !ward); }catch(e){}
    try{ const mbr = document.getElementById('map_button_row'); if(mbr) mbr.classList.toggle('hidden', !ward); }catch(e){}
}

function onZipChange(){
    const z = _el('addr_zip_ui').value;
    const hState = _el('addr_state'); if(hState) hState.value = z; // store zip in state column
    // Clear map when ZIP changes
    clearMapIfShown();
    try{ const ss = _el('step_street'); if(ss) ss.classList.toggle('hidden', !z); }catch(e){}
    try{ const sh = _el('step_house'); if(sh) sh.classList.toggle('hidden', !z); }catch(e){}
    try{ const mbr = document.getElementById('map_button_row'); if(mbr) mbr.classList.toggle('hidden', !z); }catch(e){}
}

let addrMap, addrMarker;
function initAddressMap(){
    if(window.addrMapInited) return; 
    window.addrMapInited=true;
    addrMap = L.map('addr_map', {center:[21.0285,105.8542], zoom:12});
    L.tileLayer('https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png',{maxZoom:19}).addTo(addrMap);
    addrMarker = L.marker([21.0285,105.8542], {draggable:true}).addTo(addrMap);
    addrMarker.on('dragend', function(e){
        const latlng = addrMarker.getLatLng();
        const hiddenLat = _el('addr_lat'); 
        if(hiddenLat) hiddenLat.value = latlng.lat;
        const hiddenLng = _el('addr_lng'); 
        if(hiddenLng) hiddenLng.value = latlng.lng;
        const mapSel = _el('addr_map_selected'); if(mapSel) mapSel.value = '1';
    });
    // allow clicking on map to move marker
    addrMap.on('click', function(e){
        addrMarker.setLatLng(e.latlng);
        const hiddenLat = _el('addr_lat'); if(hiddenLat) hiddenLat.value = e.latlng.lat;
        const hiddenLng = _el('addr_lng'); if(hiddenLng) hiddenLng.value = e.latlng.lng;
        const mapSel = _el('addr_map_selected'); if(mapSel) mapSel.value = '1';
    });
}

// Confirm and trigger delete action for an address row.
// Called from profile.xhtml buttons: triggerDelete(event, 'delAddrBtn_<id>', '<label>')
function triggerDelete(evt, jsfButtonId, label){
    try{ if(evt && typeof evt.preventDefault === 'function') evt.preventDefault(); }catch(e){}
    openDeleteConfirm(jsfButtonId, label, evt);
    return false;
}

// Initialize Tom Select autocompletes for region/city/ward
function initAutocompletes(){
    // region autocomplete
    if(window.TomSelect){
        const regionInput = document.getElementById('addr_region_input');
        if(regionInput && !regionInput.tom){
            regionInput.tom = new TomSelect(regionInput, {
                valueField: 'name', 
                labelField: 'label', 
                searchField: 'label',
                maxItems: 1,  // single selection only
                    maxOptions: 100,  // allow showing full list (VN provinces or US states)
                    openOnFocus: true,
                    onFocus: function(){
                        const country = document.getElementById('addr_country_ui').value;
                        if(country){ try{ this.load(''); this.open(); }catch(e){} }
                    },
                shouldLoad: function(query) { return true; },  // always load suggestions
                load: function(query, callback){
                    // provide region list from REGION_DATA (already loaded)
                    const country = document.getElementById('addr_country_ui').value;
                    if(!country || !REGION_DATA[country] || !REGION_DATA[country].regions) {
                        console.warn('No region data for country:', country);
                        return callback();
                    }
                    const list = REGION_DATA[country].regions.map(r=>({name:r,label:r}));
                    // filter based on query; if empty, show all
                    const q = (query||'').toLowerCase();
                    const out = q ? list.filter(o=>o.name.toLowerCase().includes(q)) : list;
                    console.log('Region suggestions for', country, ':', out.length, 'items');
                    callback(out);
                },
                onChange:function(value){
                    const h = _el('addr_region'); if(h) h.value = value || '';
                    // clear downstream
                    ['addr_city_input','addr_ward_input','addr_zip_ui','addr_street_ui','addr_house_ui'].forEach(id=>{ 
                        const el=document.getElementById(id); 
                        if(el) el.value=''; 
                        if(el && el.tom) el.tom.clear(); 
                    });
                    ['addr_city','addr_state','addr_street','addr_house','addr_lat','addr_lng','addr_map_selected'].forEach(id=>{ const hel=_el(id); if(hel) hel.value=''; });
                    
                    // populate city (district) TomSelect options immediately for selected region
                    try{
                        const country = document.getElementById('addr_country_ui').value;
                        const cityInputEl = document.getElementById('addr_city_input');
                        if(cityInputEl && cityInputEl.tom){
                            // clear old options
                            try{ cityInputEl.tom.clearOptions(); }catch(e){}
                            // if we have city list for this region, add them
                            if(REGION_DATA[country] && REGION_DATA[country].cities && REGION_DATA[country].cities[value]){
                                const list = REGION_DATA[country].cities[value];
                                list.forEach(c=>{
                                    const name = (c && c.name) ? c.name : c;
                                    const zip = (c && c.zip) ? c.zip : '';
                                    const label = zip ? (name + ' — ' + zip) : name;
                                    try{ cityInputEl.tom.addOption({name: name, label: label, zip: zip}); }catch(e){}
                                });
                            }
                            try{ cityInputEl.tom.open(); }catch(e){}
                        }
                    }catch(e){console.error('populate city options error', e);} 

                    // show next step
                    try{ const sc = _el('step_city'); if(sc) sc.classList.toggle('hidden', !value); }catch(e){}
                    try{ const sw = _el('step_ward'); if(sw) sw.classList.add('hidden'); }catch(e){}
                    try{ const sz = _el('step_zip'); if(sz) sz.classList.add('hidden'); }catch(e){}
                    try{ const mbr = document.getElementById('map_button_row'); if(mbr) mbr.classList.add('hidden'); }catch(e){}
                }
            });
        }

        const cityInput = document.getElementById('addr_city_input');
        if(cityInput && !cityInput.tom){
            cityInput.tom = new TomSelect(cityInput, {
            valueField: 'name', 
            labelField: 'label', 
            searchField: 'label',
                maxItems: 1,  // single selection only
                    maxOptions: 100,  // allow showing larger city lists
                    openOnFocus: true,
                    onFocus: function(){
                        const region = document.getElementById('addr_region_input').value;
                        if(region){ try{ this.load(''); this.open(); }catch(e){} }
                    },
                shouldLoad: function(query) { return true; },
                load: function(query, callback){
                    const country = document.getElementById('addr_country_ui').value;
                    const region = document.getElementById('addr_region_input').value;
                    if(!country || !region) {
                        console.warn('No country or region selected');
                        return callback();
                    }
                    
                    // If country has detailed cities map, use it; otherwise use Nominatim
                    if(REGION_DATA[country] && REGION_DATA[country].cities && REGION_DATA[country].cities[region]){
                        // ensure cities are in object form {name, zip}
                        const raw = REGION_DATA[country].cities[region];
                        const list = raw.map(c=>({name: c.name || c, label: c.zip ? ( (c.name||c) + ' — ' + c.zip) : (c.name||c), zip: c.zip || ''}));
                        const q = (query||'').toLowerCase();
                        const out = q ? list.filter(o=>o.label.toLowerCase().includes(q)) : list;
                        console.log('City suggestions for', region, ':', out.length, 'items');
                        return callback(out);
                    }
                    
                    // fallback: query Nominatim for city suggestions (only if query provided and has min length)
                    if(!query || query.length < 2) return callback();
                    const q = encodeURIComponent(query + (region?(', '+region):'') + (country?(', '+country):''));
                    fetch('https://nominatim.openstreetmap.org/search?q='+q+'&format=json&limit=10&addressdetails=1')
                        .then(r=>r.json()).then(data=>{
                            const items = (data||[]).map(it=>({name: it.display_name}));
                            callback(items);
                        }).catch(()=>callback());
                },
                onChange:function(value){
                    // value is the district name (actual valueField)
                    const h = _el('addr_city'); if(h) h.value = value || '';
                    // Clear map when city changes
                    clearMapIfShown();
                    // try to set zipcode if available
                    try{
                        const country = document.getElementById('addr_country_ui').value;
                        const region = document.getElementById('addr_region_input').value;
                        const raw = REGION_DATA[country] && REGION_DATA[country].cities && REGION_DATA[country].cities[region] ? REGION_DATA[country].cities[region] : [];
                        const obj = raw.find(it=> (it.name===value) || (it===value) );
                        if(obj && obj.zip){ const zipEl = document.getElementById('addr_zip_ui'); if(zipEl) zipEl.value = obj.zip; const hState = _el('addr_state'); if(hState) hState.value = obj.zip; }
                    }catch(e){ }
                    // When city/district chosen, show Street Address and Label inputs
                    const country = document.getElementById('addr_country_ui').value;
                    
                    // For USA: show ZIP code input after city selection
                    if(country === 'United States'){
                        try{ const sz = _el('step_zip'); if(sz) sz.classList.remove('hidden'); }catch(e){}
                        try{ const ss = _el('step_street'); if(ss) ss.classList.add('hidden'); }catch(e){}
                        try{ const sh = _el('step_house'); if(sh) sh.classList.add('hidden'); }catch(e){}
                        try{ const mbr = document.getElementById('map_button_row'); if(mbr) mbr.classList.add('hidden'); }catch(e){}
                    } else {
                        // For Vietnam: show street and house inputs for both Vietnam and US
                        try{ const ss = _el('step_street'); if(ss) ss.classList.toggle('hidden', !value); }catch(e){}
                        try{ const sh = _el('step_house'); if(sh) sh.classList.toggle('hidden', !value); }catch(e){}
                        try{ const mbr = document.getElementById('map_button_row'); if(mbr) mbr.classList.toggle('hidden', !value); }catch(e){}
                    }
                }
            });
        }

        const wardInput = document.getElementById('addr_ward_input');
        if(wardInput && !wardInput.tom){
            wardInput.tom = new TomSelect(wardInput, {
                valueField: 'name', 
                labelField: 'name', 
                searchField: 'name',
                maxItems: 1,  // single selection only
                    maxOptions: 100,
                    openOnFocus: true,
                    onFocus: function(){ try{ this.load(''); this.open(); }catch(e){} },
                shouldLoad: function(query) { return true; },
                load: function(query, callback){
                    // wards are keyed by district name in REGION_DATA
                    const district = document.getElementById('addr_city_input').value;
                    if(!district || !REGION_DATA['Vietnam'] || !REGION_DATA['Vietnam'].wards[district]) {
                        return callback();
                    }
                    const list = REGION_DATA['Vietnam'].wards[district].map(w=>({name:w}));
                    const q = (query||'').toLowerCase();
                    const out = q ? list.filter(o=>o.name.toLowerCase().includes(q)) : list;
                    callback(out);
                },
                onChange:function(value){
                    const h = _el('addr_state'); if(h) h.value = value || '';
                    _el('step_street').classList.toggle('hidden', !value);
                    _el('step_house').classList.toggle('hidden', !value);
                    document.getElementById('map_button_row').classList.toggle('hidden', !value);
                }
            });
        }
    }
}

function geocodeAddressAndSetMarker(){
    const country = document.getElementById('addr_country_ui').value;
    const region = document.getElementById('addr_region_input') ? document.getElementById('addr_region_input').value : '';
    const city = document.getElementById('addr_city_input') ? document.getElementById('addr_city_input').value : '';
    
    // Build address string from selected region (province/state) and city (district/town)
    // This centers the map on the selected city/town in the province/state
    let q;
    if(city && region){
        q = encodeURIComponent(city + ', ' + region + ', ' + country);
    } else if(region){
        q = encodeURIComponent(region + ', ' + country);
    } else if(country){
        q = encodeURIComponent(country);
    } else {
        return;
    }
    
    fetch('https://nominatim.openstreetmap.org/search?q='+q+'&format=json&limit=1')
        .then(r=>r.json())
        .then(data=>{
            if(data && data.length){
                const lat = parseFloat(data[0].lat), lon=parseFloat(data[0].lon);
                initAddressMap();
                addrMap.setView([lat,lon], 13);
                addrMarker.setLatLng([lat,lon]);
                const hiddenLat = _el('addr_lat'); if(hiddenLat) hiddenLat.value = lat;
                const hiddenLng = _el('addr_lng'); if(hiddenLng) hiddenLng.value = lon;
                const mapSel = _el('addr_map_selected'); if(mapSel) mapSel.value = '1';
            }
        }).catch(()=>{});
}

function openMapPicker(){
    // hide the select button and show map area then init
    const btn = document.getElementById('select_map_btn'); if(btn) btn.style.display = 'none';
    document.getElementById('step_map_wrapper').classList.remove('hidden');
    setTimeout(()=>{ initAddressMap(); geocodeAddressAndSetMarker(); },100);
}

function prepareAddressAndSubmit(){
    clearAllErrors();
    let valid = true;

    const type = (document.getElementById('addr_type_ui') || {}).value || '';
    const country = (document.getElementById('addr_country_ui') || {}).value || '';
    const region = (document.getElementById('addr_region_input') || {}).value || '';
    const city = (document.getElementById('addr_city_input') || {}).value || '';
    const zip = (document.getElementById('addr_zip_ui') || {}).value || '';
    const street = (document.getElementById('addr_street_ui') || {}).value || '';
    const house = (document.getElementById('addr_house_ui') || {}).value || '';

    if(!type){ showFieldError('addr_type_ui','This field is required'); valid = false; }
    if(!country){ showFieldError('addr_country_ui','This field is required'); valid = false; }
    if(!region){ showFieldError('addr_region_input','This field is required'); valid = false; }
    if(!city){ showFieldError('addr_city_input','This field is required'); valid = false; }
    if(country === 'United States' && !zip){ showFieldError('addr_zip_ui','ZIP code is required for United States'); valid = false; }
    if(!street){ showFieldError('addr_street_ui','Street address is required'); valid = false; }
    if(!house){ showFieldError('addr_house_ui','This field is required'); valid = false; }

    const mapSel = _el('addr_map_selected');
    const lat = _el('addr_lat') ? _el('addr_lat').value : '';
    const lng = _el('addr_lng') ? _el('addr_lng').value : '';
    if(!(mapSel && mapSel.value === '1') && !(lat && lng)){
        showFieldError('addr_map','Please select your place on map');
        valid = false;
    }

    if(!valid) return false;

    // CRITICAL: Sync all UI values to hidden JSF inputs BEFORE form submission
    console.log('=== ADDRESS SUBMIT STARTING ===');
    console.log('UI Form Values:', { type, country, region, city, zip, street, house, lat, lng });

    const hType = _el('addr_type');
    if(hType) hType.value = type;
    
    const hCountry = _el('addr_country');
    if(hCountry) hCountry.value = country;
    
    const hRegion = _el('addr_region');
    if(hRegion) hRegion.value = region;
    
    const hCity = _el('addr_city');
    if(hCity) hCity.value = city;
    
    const hStreet = _el('addr_street');
    if(hStreet) hStreet.value = street;
    
    const hHouse = _el('addr_house');
    if(hHouse) hHouse.value = house;
    
    // For US: ZIP in state field; For VN: ward in state field
    const hState = _el('addr_state');
    if(hState) hState.value = zip || '';

    // Copy latitude and longitude to hidden fields
    const hLat = _el('addr_lat');
    const hLng = _el('addr_lng');
    if(hLat) hLat.value = lat || '';
    if(hLng) hLng.value = lng || '';

    // Set map selected flag
    if(mapSel) mapSel.value = '1';

    // Handle default address checkbox
    const isDefaultCheckbox = document.getElementById('addr_is_default');
    const hIsDefault = _el('addr_is_default_hidden');
    if(isDefaultCheckbox && hIsDefault){
        hIsDefault.value = isDefaultCheckbox.checked ? 'true' : 'false';
    }

    // LOG AFTER SETTING TO VERIFY
    console.log('=== HIDDEN INPUTS AFTER SYNC ===', {
        hiddenType: hType?.value,
        hiddenCountry: hCountry?.value,
        hiddenRegion: hRegion?.value,
        hiddenCity: hCity?.value,
        hiddenStreet: hStreet?.value,
        hiddenHouse: hHouse?.value,
        hiddenState: hState?.value,
        hiddenLat: hLat?.value,
        hiddenLng: hLng?.value,
        hiddenMapSelected: mapSel?.value,
        hiddenIsDefault: hIsDefault?.value
    });

    return true;
}

// Handle delete success from f:ajax - show notification and refresh card list
function handleDeleteSuccess(data) {
    console.log('Delete success callback');
    const i18nEl = document.getElementById('i18nMessages');
    const successMsg = i18nEl?.getAttribute('data-delete-success') || 'Address deleted successfully';
    showAddressNotification(successMsg, 'success');
}

// Refresh address cards by reloading the addressGrid section
function refreshAddressCards(){
    console.log('Refreshing address cards');
    const addressGrid = document.getElementById('addressGrid');
    if(addressGrid){
        // Fetch the current profile page and replace the addressGrid HTML
        try {
            var url = window.location.pathname + window.location.search;
            // append cache buster
            url += (url.indexOf('?') === -1 ? '?' : '&') + '_ts=' + Date.now();
            fetch(url, { credentials: 'same-origin', headers: { 'X-Requested-With': 'XMLHttpRequest' } })
            .then(function(res){ if(!res.ok) throw new Error('Failed to fetch profile page: ' + res.status); return res.text(); })
            .then(function(html){
                try {
                    var parser = new DOMParser();
                    var doc = parser.parseFromString(html, 'text/html');
                    var newGrid = doc.getElementById('addressGrid');
                    if(newGrid) {
                        addressGrid.innerHTML = newGrid.innerHTML;
                        // re-run map init and any binding
                        setTimeout(function(){ initAddressCardMaps(); bindAddressCardEvents(); }, 300);
                        console.log('Address grid refreshed via XHR');
                        return;
                    }
                } catch(e){ console.error('Error parsing refreshed HTML', e); }
                // fallback: reload page
                console.log('Falling back to full reload to refresh address cards');
                window.location.reload();
            }).catch(function(err){ console.error('refreshAddressCards fetch error', err); window.location.reload(); });
        } catch(e){ console.error('refreshAddressCards error', e); window.location.reload(); }
    }
}

// Bind events for newly injected address cards (edit/delete buttons)
function bindAddressCardEvents(){
    try {
        document.querySelectorAll('[data-addr-id]').forEach(card=>{
            // edit button clickable area uses onclick triggerEdit in markup; nothing to bind here
            const editBtn = card.querySelector('button[title="Edit address"]');
            if(editBtn) editBtn.addEventListener('click', function(e){
                // find corresponding hidden command button id in the card
                const form = card.querySelector('form');
                if(form){
                    const cmd = form.querySelector('button[id^="editAddrBtn_"]');
                    if(cmd) cmd.click();
                }
            });
        });
    } catch(e){ console.error('bindAddressCardEvents error', e); }
}

// Edit address handler - trigger AJAX submit to open edit modal
function editAddressHandler_v2(event){
    event.preventDefault();
    event.stopPropagation();
    const form = event.target.closest('form');
    if(form){
        const cmdBtn = form.querySelector('[type="submit"]');
        if(cmdBtn){
            console.log('Triggering edit action');
            cmdBtn.click();
        }
    }
    return false;
}

// Delete address handler - confirm then trigger AJAX delete
function deleteAddressHandler_v2(event, streetName){
    event.preventDefault();
    event.stopPropagation();
    
    const i18nEl = document.getElementById('i18nMessages');
    const confirmMsg = i18nEl?.getAttribute('data-confirm-delete') || 'Are you sure you want to delete this address?';
    
    if(confirm(confirmMsg + '\n\n' + streetName)){
        const form = event.target.closest('form');
        if(form){
            const cmdBtn = form.querySelector('[type="submit"]');
            if(cmdBtn){
                console.log('Triggering delete action');
                cmdBtn.click();
            }
        }
    }
    return false;
}

// Open Add Address Modal (new address, clear form, update title)
function openAddAddressModal(){
    const modal = document.getElementById('addressModal');
    const title = modal.querySelector('h3');
    const subtitle = modal.querySelector('h3').nextElementSibling;
    
    // Update title
    if(title) title.textContent = 'Add New Address';
    if(subtitle) subtitle.textContent = 'Enter your new address';
    
    // Store mode
    modal.dataset.mode = 'add';
    
    // Clear form fields
    clearAddressForm();
    
    // Show modal
    modal.classList.remove('hidden');
    
    // Initialize autocompletes
    setTimeout(() => {
        initAutocompletes();
    }, 100);
}

// Open Edit Address Modal (existing address, populate form, update title)
function openEditAddressModal(){
    const modal = document.getElementById('addressModal');
    const title = modal.querySelector('h3');
    const subtitle = modal.querySelector('h3').nextElementSibling;
    
    // Update title
    if(title) title.textContent = 'Edit Address';
    if(subtitle) subtitle.textContent = 'Update your address details';
    
    // Store mode
    modal.dataset.mode = 'edit';
    
    // Show modal
    modal.classList.remove('hidden');
    
    // Populate form with existing data
    setTimeout(populateAddressForm, 300);
}

function closeAddressModal(){ document.getElementById('addressModal').classList.add('hidden'); }

// Edit address handler - open modal and populate form
function editAddressHandler(event, form){
    event.preventDefault();
    // Submit the form to trigger startEdit action
    form.submit();
    // Open modal after submission
    setTimeout(() => {
        document.getElementById('addressModal').classList.remove('hidden');
        setTimeout(populateAddressForm, 300);
    }, 200);
    return false;
}

// Delete address handler - show confirmation and submit
function deleteAddressHandler(event, form, streetName){
    event.preventDefault();
    const i18nEl = document.getElementById('i18nMessages');
    const confirmMsg = i18nEl?.getAttribute('data-confirm-delete') || 'Are you sure you want to delete this address?';
    
    if(confirm(confirmMsg + '\n\n' + streetName)){
        form.submit();
        // Show success notification after delete
        setTimeout(() => {
            const msg = i18nEl?.getAttribute('data-delete-success') || 'Address deleted successfully';
            showAddressNotification(msg, 'success');
        }, 500);
    }
    return false;
}

// i18n support for delete confirmation - get messages from data attributes
function confirmDeleteAddress(addressId, streetName){
    const i18nEl = document.getElementById('i18nMessages');
    const confirmMsg = i18nEl?.getAttribute('data-confirm-delete') || 'Are you sure you want to delete this address?';
    
    const confirmed = confirm(confirmMsg + '\n\n' + streetName);
    return confirmed;
}

// Clear address form for new address entry
function clearAddressForm(){
    // Clear all UI input fields
    const formElements = [
        'addr_country_ui', 'addr_type_ui', 'addr_region_input', 'addr_city_input',
        'addr_zip_ui', 'addr_street_ui', 'addr_house_ui'
    ];
    formElements.forEach(id=>{
        const el = document.getElementById(id);
        if(el) el.value = '';
        if(el && el.tom) {
            try{ el.tom.clear(); }catch(e){}
        }
    });

    // Clear hidden input fields
    const hiddenElements = [
        'addr_country', 'addr_type', 'addr_region', 'addr_city', 'addr_state',
        'addr_street', 'addr_house', 'addr_lat', 'addr_lng', 'addr_map_selected'
    ];
    hiddenElements.forEach(id=>{
        const el = _el(id);
        if(el) el.value = '';
    });

    // Reset checkbox
    const isDefaultCheckbox = document.getElementById('addr_is_default');
    if(isDefaultCheckbox) isDefaultCheckbox.checked = false;
    const hIsDefault = _el('addr_is_default_hidden');
    if(hIsDefault) hIsDefault.value = 'false';

    // Hide all step fields
    ['step_region', 'step_city', 'step_ward', 'step_zip', 'step_street', 'step_house', 'step_map_wrapper'].forEach(id=>{
        const el = _el(id);
        if(el) el.classList.add('hidden');
    });
    const mapButtonRow = document.getElementById('map_button_row');
    if(mapButtonRow) mapButtonRow.classList.add('hidden');

    // Reset map state
    window.addrMapInited = false;
    clearMapIfShown();
}

// Populate form fields with existing address data for editing
function populateAddressForm(){
    // Retrieve values from hidden input fields (set by AddressController via JSF binding)
    const countryVal = _el('addr_country') ? _el('addr_country').value : '';
    const regionVal = _el('addr_region') ? _el('addr_region').value : '';
    const cityVal = _el('addr_city') ? _el('addr_city').value : '';
    const stateVal = _el('addr_state') ? _el('addr_state').value : '';
    const streetVal = _el('addr_street') ? _el('addr_street').value : '';
    const houseVal = _el('addr_house') ? _el('addr_house').value : '';
    const typeVal = _el('addr_type') ? _el('addr_type').value : '';
    const latVal = _el('addr_lat') ? _el('addr_lat').value : '';
    const lngVal = _el('addr_lng') ? _el('addr_lng').value : '';
    const isDefaultVal = _el('addr_is_default_hidden') ? _el('addr_is_default_hidden').value : 'false';

    console.log('populateAddressForm called with:', {countryVal, regionVal, cityVal, stateVal, streetVal, houseVal, typeVal, latVal, lngVal, isDefaultVal});

    // If no values found, it's a new address - clear the form
    if(!countryVal && !typeVal && !regionVal){
        clearAddressForm();
        return;
    }

    // Set UI input values
    const countrySelect = document.getElementById('addr_country_ui');
    if(countrySelect) countrySelect.value = countryVal;

    const typeSelect = document.getElementById('addr_type_ui');
    if(typeSelect) typeSelect.value = typeVal;

    const regionInput = document.getElementById('addr_region_input');
    const cityInput = document.getElementById('addr_city_input');
    const streetInput = document.getElementById('addr_street_ui');
    const houseInput = document.getElementById('addr_house_ui');
    const zipInput = document.getElementById('addr_zip_ui');
    const isDefaultCheckbox = document.getElementById('addr_is_default');

    if(regionInput) regionInput.value = regionVal;
    if(cityInput) cityInput.value = cityVal;
    if(streetInput) streetInput.value = streetVal;
    if(houseInput) houseInput.value = houseVal;
    if(zipInput && stateVal) zipInput.value = stateVal;
    if(isDefaultCheckbox) isDefaultCheckbox.checked = (isDefaultVal === 'true');

    // Set map if coordinates are present
    if(latVal && lngVal && !isNaN(latVal) && !isNaN(lngVal)){
        const lat = parseFloat(latVal);
        const lng = parseFloat(lngVal);
        const mapDiv = document.getElementById('mapPickerDiv');
        if(mapDiv && mapDiv.style.display !== 'none'){
            // Map is visible - update it
            setTimeout(()=>{
                if(window.addressMapInstance){
                    window.addressMapInstance.setView([lat, lng], 13);
                    if(window.addressMarker){
                        window.addressMarker.setLatLng([lat, lng]);
                    }else{
                        window.addressMarker = L.marker([lat, lng]).addTo(window.addressMapInstance);
                    }
                }
                const hLat = _el('addr_lat');
                const hLng = _el('addr_lng');
                if(hLat) hLat.value = lat;
                if(hLng) hLng.value = lng;
                const mapSel = _el('addr_map_selected');
                if(mapSel) mapSel.value = '1';
            }, 200);
        }else{
            // Map not visible yet, just set hidden fields
            _el('addr_lat').value = lat;
            _el('addr_lng').value = lng;
            _el('addr_map_selected').value = '1';
        }
    }

    // Manually trigger country change handler FIRST to load region/city data and show appropriate fields
    if(countryVal) {
        onCountryChange();
        // Then load region data and populate TomSelects
        loadRegionData(countryVal).then(()=>{
            // Update TomSelect instances if they exist
            setTimeout(()=>{
                if(regionInput && regionInput.tom && regionVal){
                    try{ regionInput.tom.setValue(regionVal, true); }catch(e){}
                }
                if(cityInput && cityInput.tom && cityVal){
                    try{ cityInput.tom.setValue(cityVal, true); }catch(e){}
                }
            }, 100);
        });
    }
    
    if(typeVal) onTypeChange();
}

// Monitor modal visibility and populate form when it opens
const addressModalObserver = new MutationObserver(function(mutations){
    mutations.forEach(function(mutation){
        if(mutation.attributeName === 'class'){
            const modal = document.getElementById('addressModal');
            if(modal && !modal.classList.contains('hidden')){
                // Modal opened - populate form
                setTimeout(populateAddressForm, 100);
            }
        }
    });
});

// Initialize address card maps function - MUST be defined before DOMContentLoaded calls it
function initAddressCardMaps(){
    console.log('initAddressCardMaps: scanning for .addr-leaflet-map elements');
    document.querySelectorAll('.addr-leaflet-map').forEach(mapEl=>{
        const lat = parseFloat(mapEl.dataset.lat);
        const lng = parseFloat(mapEl.dataset.lng);
        const mapId = mapEl.dataset.id;
        // If this container already looks like a Leaflet container, attempt to clean it before re-init
        try{
            if(mapEl.classList.contains('leaflet-container') || mapEl.querySelector('.leaflet-pane')){
                console.log('initAddressCardMaps: found existing leaflet container on', mapEl, ' - cleaning up before re-init');
                // remove leaflet-specific classes
                Array.from(mapEl.classList).forEach(function(c){ if(c && c.indexOf('leaflet-')===0) mapEl.classList.remove(c); });
                // remove all children inserted by leaflet
                while(mapEl.firstChild){ mapEl.removeChild(mapEl.firstChild); }
                // reset inline styles often applied by Leaflet
                mapEl.style.position = '';
                mapEl.style.width = '';
                mapEl.style.height = '';
            }
        }catch(e){ console.warn('initAddressCardMaps: cleanup of existing leaflet container failed', e); }
        console.log('initAddressCardMaps: element', mapEl, 'data-lat=', mapEl.dataset.lat, 'data-lng=', mapEl.dataset.lng, 'data-id=', mapEl.dataset.id);
        
        if(!isNaN(lat) && !isNaN(lng) && lat >= -90 && lat <= 90 && lng >= -180 && lng <= 180){
            try{
                const map = L.map(mapEl, {
                    center: [lat, lng],
                    zoom: 13,
                    zoomControl: false,
                    dragging: false,
                    scrollWheelZoom: false,
                    doubleClickZoom: false,
                    boxZoom: false,
                    tap: false
                });
                
                L.tileLayer('https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png', {
                    attribution: '© OpenStreetMap contributors',
                    maxZoom: 19
                }).addTo(map);
                
                L.marker([lat, lng]).addTo(map);
                
                setTimeout(function(){ try{ map.invalidateSize(); }catch(e){} }, 200);
            }catch(e){
                console.error('initAddressCardMaps: failed to init map for', mapEl, e);
                mapEl.innerHTML = '<div style="width:100%; height:100%; background:#e5e7eb; display:flex; align-items:center; justify-content:center; font-size:12px;">Map unavailable</div>';
            }
        }else{
            console.warn('initAddressCardMaps: invalid coords for', mapEl, 'lat=', mapEl.dataset.lat, 'lng=', mapEl.dataset.lng);
            mapEl.innerHTML = '<div style="width:100%; height:100%; background:#f3f4f6; display:flex; align-items:center; justify-content:center; font-size:12px;">No coordinates</div>';
        }
    });
}

document.addEventListener('DOMContentLoaded', function(){
    // Set up modal observer
    const addressModal = document.getElementById('addressModal');
    if(addressModal){
        addressModalObserver.observe(addressModal, { attributes: true, attributeFilter: ['class'] });
    }

    // Move modals to document.body to avoid stacking-context issues
    try {
        ['addressModal','deleteConfirmModal'].forEach(function(id){
            var el = document.getElementById(id);
            if(el && el.parentNode !== document.body){
                document.body.appendChild(el);
                // ensure overlay covers full viewport and inner has higher z
                el.style.position = 'fixed';
                el.style.top = '0'; el.style.left = '0'; el.style.width = '100%'; el.style.height = '100%';
                el.style.zIndex = '100400';
                var inner = el.firstElementChild;
                if(inner) inner.style.zIndex = '100500';
            }
        });
    } catch(e){ console.error('move modals to body error', e); }

    const streetEl = document.getElementById('addr_street_ui');
    if(streetEl) {
        streetEl.addEventListener('change', function(){ 
            /* no-op - street change doesn't auto-show map; user must click Select on map */ 
            clearMapIfShown();
        });
        streetEl.addEventListener('input', function(){ 
            clearMapIfShown();
        });
    }
    
    const houseEl = document.getElementById('addr_house_ui');
    if(houseEl) {
        houseEl.addEventListener('change', function(){ clearMapIfShown(); });
        houseEl.addEventListener('input', function(){ clearMapIfShown(); });
    }
    
    const regionEl = document.getElementById('addr_region_input');
    if(regionEl) {
        regionEl.addEventListener('change', function(){ clearMapIfShown(); });
    }
    
    const cityEl = document.getElementById('addr_city_input');
    if(cityEl) {
        cityEl.addEventListener('change', function(){ clearMapIfShown(); });
    }
    
    const zipEl = document.getElementById('addr_zip_ui');
    if(zipEl) {
        zipEl.addEventListener('change', function(){ clearMapIfShown(); });
    }
    
    // Initialize default address checkbox state
    const isDefaultCheckbox = document.getElementById('addr_is_default');
    const hIsDefault = _el('addr_is_default_hidden');
    if(isDefaultCheckbox && hIsDefault){
        const currentValue = hIsDefault.value;
        isDefaultCheckbox.checked = currentValue === 'true' || currentValue === '1';
        // update hidden when user toggles
        isDefaultCheckbox.addEventListener('change', function(){ if(hIsDefault) hIsDefault.value = this.checked ? 'true' : 'false'; });
    }
    
    // preload default country data and initialize autocompletes
    const countrySelect = document.getElementById('addr_country_ui');
    const defaultCountry = (countrySelect && countrySelect.value) ? countrySelect.value : 'Vietnam';
    
    loadRegionData(defaultCountry).then(()=>{
        console.log('Default country data loaded:', defaultCountry);
        initAutocompletes();
        // Try to populate form if in edit mode
        setTimeout(populateAddressForm, 100);
    }).catch(err=>{
        console.error('Error loading default country data:', err);
        // still initialize even if load fails (fallback to hardcoded data)
        initAutocompletes();
        // Try to populate form anyway
        setTimeout(populateAddressForm, 100);
    });
    
    // Initialize Leaflet maps for address cards (on page load)
    setTimeout(initAddressCardMaps, 500);
});
