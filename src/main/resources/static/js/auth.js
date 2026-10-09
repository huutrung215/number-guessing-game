let mode = 'login';

// Nếu đã đăng nhập → chuyển sang game
if (Api.isLoggedIn()) {
    window.location.href = '/game.html';
}

function switchTab(newMode) {
    mode = newMode;
    document.querySelectorAll('.tab').forEach((t, i) => {
        t.classList.toggle('active', (i === 0) === (newMode === 'login'));
    });
    document.getElementById('submit-btn').textContent =
        newMode === 'login' ? 'Đăng nhập' : 'Đăng ký';
    document.getElementById('password').autocomplete =
        newMode === 'login' ? 'current-password' : 'new-password';
    hideAlert();
}

function showAlert(message, type = 'error') {
    const el = document.getElementById('alert');
    el.textContent = message;
    el.className = `alert alert-${type} show`;
}

function hideAlert() {
    document.getElementById('alert').className = 'alert';
}

async function handleSubmit(event) {
    event.preventDefault();
    const btn = document.getElementById('submit-btn');
    const username = document.getElementById('username').value.trim();
    const password = document.getElementById('password').value;

    btn.disabled = true;
    btn.textContent = mode === 'login' ? 'Đang đăng nhập...' : 'Đang đăng ký...';
    hideAlert();

    try {
        const data = mode === 'login'
            ? await Api.login(username, password)
            : await Api.register(username, password);

        Api.setToken(data.token);
        Api.setUsername(data.username);

        showAlert('Thành công! Đang chuyển hướng...', 'success');
        setTimeout(() => {
            window.location.href = '/game.html';
        }, 500);
    } catch (err) {
        showAlert(err.message);
        btn.disabled = false;
        btn.textContent = mode === 'login' ? 'Đăng nhập' : 'Đăng ký';
    }
}