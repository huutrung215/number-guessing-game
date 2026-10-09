if (!Api.requireAuth()) {
    throw new Error('Chưa đăng nhập');
}

async function loadLeaderboard() {
    const loading = document.getElementById('loading');
    const list = document.getElementById('leaderboard');

    try {
        const data = await Api.leaderboard();
        loading.style.display = 'none';

        if (!data || data.length === 0) {
            list.innerHTML = '<p style="text-align:center;color:var(--text-muted);">Chưa có ai chơi. Hãy là người đầu tiên!</p>';
            return;
        }

        list.innerHTML = data.map(entry => {
            const rankClass = entry.rank <= 3 ? `top-${entry.rank}` : '';
            return `
                <div class="leaderboard-item ${rankClass}">
                    <div class="rank">${entry.rank}</div>
                    <div class="leaderboard-user">${escapeHtml(entry.username)}</div>
                    <div class="leaderboard-score">${entry.score} điểm</div>
                </div>
            `;
        }).join('');
    } catch (err) {
        loading.style.display = 'none';
        list.innerHTML = `<p style="color:var(--danger);text-align:center;">${escapeHtml(err.message)}</p>`;
    }
}

function escapeHtml(str) {
    const div = document.createElement('div');
    div.textContent = str;
    return div.innerHTML;
}

loadLeaderboard();