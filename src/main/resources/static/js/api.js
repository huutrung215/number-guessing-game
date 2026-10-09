const API_BASE = '/api';

const Api = {
    getToken() {
        return localStorage.getItem('token');
    },

    setToken(token) {
        localStorage.setItem('token', token);
    },

    clearToken() {
        localStorage.removeItem('token');
        localStorage.removeItem('username');
    },

    getUsername() {
        return localStorage.getItem('username');
    },

    setUsername(username) {
        localStorage.setItem('username', username);
    },

    isLoggedIn() {
        return !!this.getToken();
    },

    requireAuth() {
        if (!this.isLoggedIn()) {
            window.location.href = '/index.html';
            return false;
        }
        return true;
    },

    async request(path, options = {}) {
        const headers = {
            'Content-Type': 'application/json',
            ...(options.headers || {}),
        };

        const token = this.getToken();
        if (token) {
            headers['Authorization'] = `Bearer ${token}`;
        }

        const response = await fetch(API_BASE + path, {
            ...options,
            headers,
        });

        if (response.status === 401) {
            this.clearToken();
            window.location.href = '/index.html';
            throw new Error('Phiên đăng nhập đã hết hạn');
        }

        const contentType = response.headers.get('content-type') || '';
        const data = contentType.includes('application/json')
            ? await response.json()
            : await response.text();

        if (!response.ok) {
            const message = typeof data === 'object' ? (data.error || data.message) : data;
            throw new Error(message || `Lỗi ${response.status}`);
        }

        return data;
    },

    register(username, password) {
        return this.request('/auth/register', {
            method: 'POST',
            body: JSON.stringify({ username, password }),
        });
    },

    login(username, password) {
        return this.request('/auth/login', {
            method: 'POST',
            body: JSON.stringify({ username, password }),
        });
    },

    guess(number) {
        return this.request('/guess', {
            method: 'POST',
            body: JSON.stringify({ number }),
        });
    },

    buyTurns() {
        return this.request('/payment/buy-turns', { method: 'POST' });
    },

    me() {
        return this.request('/me');
    },

    leaderboard() {
        return this.request('/leaderboard');
    },
};