(function(){
    console.log('verify-otp.js loaded');

    document.addEventListener('DOMContentLoaded', function(){
        var form = document.getElementById('verifyForm');
        var inputs = Array.prototype.slice.call(document.querySelectorAll('.otp-digit'));
        var hidden = document.getElementById('otpHidden');
        var hiddenBean = document.getElementById('otpCombined');
        // flag set when a Resend control was clicked so submit validation can be bypassed
        var __otpResendFlag = false;

        if (!inputs || inputs.length === 0) {
            console.log('No OTP inputs found');
            return;
        }

        function updateHidden() {
            var val = inputs.map(function(i){ return i.value || ''; }).join('');
            if (hidden) hidden.value = val;
            // Try multiple ways to locate the JSF-rendered hidden input bound to otpCombined
            if (hiddenBean) {
                hiddenBean.value = val;
            } else {
                // common JSF rendered id/name patterns
                var hb = document.querySelector('input[id$="otpCombined"]') || document.querySelector('input[name$="otpCombined"]') || document.querySelector('input[id*="otpCombined"]');
                if (hb) {
                    hiddenBean = hb;
                    hiddenBean.value = val;
                    console.log('Located JSF hidden otpCombined via selector, id=', hb.id, 'name=', hb.name);
                } else {
                    // try explicit form:id pattern
                    try {
                        var explicit = document.getElementById('verifyForm:otpCombined');
                        if (explicit) { hiddenBean = explicit; hiddenBean.value = val; console.log('Located JSF hidden by explicit id'); }
                    } catch (e) {
                        // ignore
                    }
                }
            }
            return val;
        }

        inputs.forEach(function(inp, idx){
            inp.setAttribute('inputmode','numeric');
            inp.setAttribute('maxlength','1');

            inp.addEventListener('input', function(e){
                this.value = this.value.replace(/[^0-9]/g, '');
                if (this.value.length > 0 && idx < inputs.length - 1) {
                    inputs[idx+1].focus();
                }
                updateHidden();
            });

            inp.addEventListener('keydown', function(e){
                if (e.key === 'Backspace') {
                    if (this.value === '') {
                        if (idx > 0) inputs[idx-1].focus();
                    } else {
                        this.value = '';
                    }
                    updateHidden();
                    e.preventDefault();
                } else if (e.key === 'ArrowLeft' && idx > 0) {
                    inputs[idx-1].focus();
                    e.preventDefault();
                } else if (e.key === 'ArrowRight' && idx < inputs.length-1) {
                    inputs[idx+1].focus();
                    e.preventDefault();
                }
            });

            inp.addEventListener('paste', function(e){
                e.preventDefault();
                var text = (e.clipboardData || window.clipboardData).getData('text').replace(/[^0-9]/g,'').slice(0, inputs.length);
                for (var i=0;i<text.length;i++) inputs[i].value = text.charAt(i);
                updateHidden();
                for (var k=0;k<inputs.length;k++){
                    if (!inputs[k].value) { inputs[k].focus(); return; }
                }
                inputs[inputs.length-1].focus();
            });
        });

        if (form) {
            form.addEventListener('submit', function(e){
                var val = updateHidden();

                // Determine which element triggered the submit
                var submitter = e.submitter || document.activeElement;
                var isResend = false;

                if (submitter) {
                    try {
                        // Prefer an explicit data attribute: data-resend="true"
                        if (submitter.dataset && (submitter.dataset.resend === 'true' || submitter.dataset.action === 'resend')) {
                            isResend = true;
                        }

                        // Fallback: inspect id/name/value/text to detect common 'resend' naming
                        if (!isResend) {
                            var s = (submitter.id || submitter.name || submitter.value || submitter.innerText || '').toString().toLowerCase();
                            if (s.indexOf('resend') !== -1 || s.indexOf('send code') !== -1 || s.indexOf('send') !== -1 && s.indexOf('verify') === -1) {
                                isResend = true;
                            }
                        }
                    } catch (err) {
                        // ignore detection errors and fall back to default validation
                    }
                }

                // Also check hidden resend flag (set by onclick on Resend link)
                var resendHidden = document.getElementById('resendFlag');
                var hiddenIsResend = false;
                try { if (resendHidden && resendHidden.value === 'true') hiddenIsResend = true; } catch (err) {}

                // If this submit is NOT a resend action, enforce full-code validation
                if (!isResend && !__otpResendFlag && !hiddenIsResend && val.length !== inputs.length) {
                    e.preventDefault();
                    alert('Please enter the full ' + inputs.length + '-digit verification code.');
                }

                // If we used the hidden resend flag, reset it so subsequent submits behave normally
                try { if (hiddenIsResend && resendHidden) resendHidden.value = 'false'; } catch (err) {}
            });
        }

        // Setup detection for Resend triggers (works for links, buttons, JSF commandLink, etc.)
        // If user clicks an element that looks like a "resend" control, set a flag so submit handler can skip OTP validation.
        function markResend(e) {
            __otpResendFlag = true;
            try { if (e && e.currentTarget && e.currentTarget.dataset) e.currentTarget.dataset.resend = 'true'; } catch (err) {}
            setTimeout(function(){ __otpResendFlag = false; }, 2000);
        }

        // Heuristic selector: look for elements whose text/value contains 'resend'
        var potential = Array.prototype.slice.call(document.querySelectorAll('a,button,input'));
        potential.forEach(function(el){
            try {
                var txt = (el.value || el.innerText || el.textContent || '').toString().trim().toLowerCase();
                if (txt.indexOf('resend') !== -1 || txt.indexOf('send code') !== -1) {
                    el.addEventListener('click', markResend);
                }
            } catch (err) {
                // ignore
            }
        });

        // autofocus first
        inputs[0].focus();
        console.log('verify-otp initialized');
    });
})();
