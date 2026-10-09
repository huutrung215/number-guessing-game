// ============================================
// Game Đoán Số - Frontend Logic
// ============================================

// 1. Kiểm tra đăng nhập
if (!Api.requireAuth()) {
    throw new Error('Chưa đăng nhập');
}

// 2. State
let currentTurns = 0;
let isGuessing = false;

// 3. Hiển thị thông tin user
const username = Api.getUsername();
document.getElementById('username-display').textContent = username;
document.getElementById('avatar').textContent = username.charAt(0).toUpperCase();

// ============================================
// Load user info từ server
// ============================================
async function loadUserInfo() {
    try {
        const info = await Api.me();
        updateStats(info);
    } catch (err) {
        showAlert(err.message, 'error');
    }
}

// ============================================
// Update UI: score, turns, disable buttons, overlay
// ============================================
function updateStats(info) {
    // Update số liệu
    document.getElementById('score').textContent = info.score;
    document.getElementById('turns').textContent = info.turns;
    currentTurns = info.turns;

    // Disable/enable các nút số
    const btns = document.querySelectorAll('.number-btn');
    btns.forEach(b => {
        b.disabled = info.turns <= 0;
        b.classList.remove('correct', 'wrong');
    });

    // Toggle overlay "hết lượt"
    toggleNoTurnsOverlay(info.turns <= 0);
}

// ============================================
// Overlay khi hết lượt
// ============================================
function toggleNoTurnsOverlay(show) {
    const overlay = document.getElementById('no-turns-overlay');
    if (!overlay) return;
    overlay.style.display = show ? 'flex' : 'none';
}

// ============================================
// Alert (thông báo tạm thời)
// ============================================
function showAlert(message, type = 'error') {
    const el = document.getElementById('alert');
    el.textContent = message;
    el.className = `alert alert-${type} show`;
    clearTimeout(el._timer);
    el._timer = setTimeout(() => {
        el.className = 'alert';
    }, 4000);
}

// ============================================
// Hiển thị kết quả đoán
// ============================================
function showResult(guessResult, guessedNumber) {
    const resultEl = document.getElementById('result');
    const titleEl = document.getElementById('result-title');
    const messageEl = document.getElementById('result-message');

    resultEl.className = `result show ${guessResult.correct ? 'win' : 'lose'}`;
    titleEl.textContent = guessResult.correct ? '🎉 Đoán đúng!' : '😢 Chưa đúng';
    messageEl.innerHTML = `
        Bạn chọn: <strong>${guessedNumber}</strong> — 
        Server: <strong>${guessResult.serverNumber}</strong><br>
        ${guessResult.message}
    `;

    // Highlight nút vừa đoán
    const btns = document.querySelectorAll('.number-btn');
    btns.forEach(b => {
        const num = parseInt(b.textContent);
        if (num === guessedNumber) {
            b.classList.add(guessResult.correct ? 'correct' : 'wrong');
        }
    });
}

// ============================================
// Đoán số
// ============================================
async function guess(number) {
    if (isGuessing) return;

    if (currentTurns <= 0) {
        showAlert('Bạn đã hết lượt. Hãy mua thêm!', 'error');
        return;
    }

    isGuessing = true;

    // Disable tất cả nút khi đang gọi API
    const btns = document.querySelectorAll('.number-btn');
    btns.forEach(b => b.disabled = true);

    try {
        const result = await Api.guess(number);
        showResult(result, number);

        // Cập nhật UI ngay
        document.getElementById('score').textContent = result.score;
        document.getElementById('turns').textContent = result.turnsLeft;
        currentTurns = result.turnsLeft;

        // Sau 1.5s: bỏ highlight + re-enable nếu còn lượt
        setTimeout(() => {
            btns.forEach(b => {
                b.classList.remove('correct', 'wrong');
                b.disabled = currentTurns <= 0;
            });

            // Nếu vừa hết lượt → hiện overlay
            toggleNoTurnsOverlay(currentTurns <= 0);

            isGuessing = false;
        }, 1500);

    } catch (err) {
        showAlert(err.message, 'error');

        // Fallback: nếu server báo hết lượt → cập nhật UI
        if (err.message && err.message.toLowerCase().includes('hết lượt')) {
            currentTurns = 0;
            document.getElementById('turns').textContent = '0';
            toggleNoTurnsOverlay(true);
        }

        btns.forEach(b => {
            b.disabled = currentTurns <= 0;
            b.classList.remove('correct', 'wrong');
        });
        isGuessing = false;
    }
}

// ============================================
// Mua thêm lượt (VNPAY / payOS / trực tiếp)
// ============================================
async function buyTurns() {
    const btn = document.getElementById('buy-btn');
    const originalText = btn.textContent;

    btn.disabled = true;
    btn.textContent = '⏳ Đang tạo đơn hàng...';

    try {
        const data = await Api.buyTurns();

        // Case 1: Backend trả payment URL (VNPAY/payOS) → redirect
        if (data.paymentUrl) {
            window.location.href = data.paymentUrl;
            return;
        }

        // Case 2: Backend cộng turns trực tiếp (dev mode)
        if (data.turns !== undefined) {
            const added = data.turns - currentTurns;
            showAlert(`✅ Đã mua ${added} lượt!`, 'success');
            await loadUserInfo();
        }

    } catch (err) {
        showAlert(err.message, 'error');
        btn.disabled = false;
        btn.textContent = originalText;
    }
}

// ============================================
// Đăng xuất
// ============================================
function logout() {
    if (confirm('Bạn chắc chắn muốn đăng xuất?')) {
        Api.clearToken();
        window.location.href = '/index.html';
    }
}

// ============================================
// Kiểm tra kết quả thanh toán khi vừa quay về từ VNPAY
// ============================================
async function checkPaymentResult() {
    const params = new URLSearchParams(window.location.search);
    const payment = params.get('payment');
    const txnRef = params.get('txnRef');

    if (!payment) return;

    // Xóa query string khỏi URL
    window.history.replaceState({}, '', '/game.html');

    if (payment === 'success') {
        // Load lại user info để lấy turns mới
        await loadUserInfo();

        const turnsNow = currentTurns;
        showPaymentBanner(
            `✅ Thanh toán thành công! Bạn có ${turnsNow} lượt để chơi.`,
            'success'
        );
    } else if (payment === 'failed') {
        showPaymentBanner('❌ Thanh toán thất bại. Vui lòng thử lại.', 'error');
    }
}

// ============================================
// Banner thông báo thanh toán
// ============================================
function showPaymentBanner(message, type) {
    const banner = document.createElement('div');
    banner.className = `alert alert-${type} show`;
    banner.style.cssText = `
        position: fixed;
        top: 20px;
        left: 50%;
        transform: translateX(-50%);
        z-index: 9999;
        max-width: 500px;
        width: calc(100% - 40px);
        box-shadow: 0 10px 30px rgba(0,0,0,0.4);
        animation: slideDown 0.3s ease-out;
        text-align: center;
        font-weight: 600;
    `;
    banner.textContent = message;
    document.body.appendChild(banner);

    // Tự động ẩn sau 5s
    setTimeout(() => {
        banner.style.transition = 'opacity 0.3s, transform 0.3s';
        banner.style.opacity = '0';
        banner.style.transform = 'translate(-50%, -20px)';
        setTimeout(() => banner.remove(), 300);
    }, 5000);
}

// ============================================
// Init
// ============================================
(async function init() {
    // Ưu tiên xử lý payment result trước
    await checkPaymentResult();

    // Load user info (nếu chưa load trong checkPaymentResult)
    if (!document.getElementById('score').textContent ||
        document.getElementById('score').textContent === '0') {
        await loadUserInfo();
    }
})();