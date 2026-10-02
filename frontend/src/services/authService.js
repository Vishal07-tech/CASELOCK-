import api, { tokenStorage } from './api';

const authService = {
  async login(username, password) {
    const { data } = await api.post('/auth/login', { username, password });
    const { accessToken, refreshToken, user } = data.data;
    tokenStorage.setTokens(accessToken, refreshToken);
    return user;
  },

  async register(payload) {
    const { data } = await api.post('/auth/register', payload);
    return data.data;
  },

  async logout() {
    try {
      await api.post('/auth/logout');
    } finally {
      tokenStorage.clear();
    }
  },

  async me() {
    const { data } = await api.get('/auth/me');
    return data.data;
  },

  isAuthenticated() {
    return Boolean(tokenStorage.getAccessToken());
  },
};

export default authService;
