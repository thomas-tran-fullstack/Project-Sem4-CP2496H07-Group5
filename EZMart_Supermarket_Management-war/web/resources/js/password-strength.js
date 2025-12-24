document.addEventListener('DOMContentLoaded', function() {
    var newPass = document.getElementById('newPass');
    var confirmPass = document.getElementById('confirmPass');
    var strengthLabel = document.getElementById('strengthLabel');

    function scorePassword(pw) {
        var score = 0;
        if (!pw) return score;
        if (pw.length >= 8) score += 1;
        if (pw.match(/[a-z]/) && pw.match(/[A-Z]/)) score += 1;
        if (pw.match(/\d/)) score += 1;
        if (pw.match(/[^A-Za-z0-9]/)) score += 1;
        return score;
    }

    function updateStrength() {
        var pw = newPass ? newPass.value : '';
        var s = scorePassword(pw);
        var text = 'Very Weak';
        if (s >= 4) text = 'Strong';
        else if (s === 3) text = 'Medium';
        else if (s === 2) text = 'Weak';
        strengthLabel.textContent = text;
    }

    if (newPass) newPass.addEventListener('input', updateStrength);
    updateStrength();
});