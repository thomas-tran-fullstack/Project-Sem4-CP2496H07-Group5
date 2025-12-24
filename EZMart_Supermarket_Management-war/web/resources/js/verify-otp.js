(function(){
    console.log('verify-otp.js loaded');

    document.addEventListener('DOMContentLoaded', function(){
        var form = document.getElementById('verifyForm');
        var inputs = Array.prototype.slice.call(document.querySelectorAll('.otp-digit'));
        var hidden = document.getElementById('otpHidden');
        var hiddenBean = document.getElementById('otpCombined');

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
                if (val.length !== inputs.length) {
                    e.preventDefault();
                    alert('Please enter the full ' + inputs.length + '-digit verification code.');
                }
            });
        }

        // autofocus first
        inputs[0].focus();
        console.log('verify-otp initialized');
    });
})();
