import api from './api';

const userService = {
  async list(params = {}) {
    const { data } = await api.get('/users', { params });
    return data.data;
  },
  async directory(params = {}) {
    const { data } = await api.get('/users/directory', { params });
    return data.data;
  },
  async get(id) {
    const { data } = await api.get(`/users/${id}`);
    return data.data;
  },
  async create(payload) {
    const { data } = await api.post('/users', payload);
    return data.data;
  },
  async update(id, payload) {
    const { data } = await api.put(`/users/${id}`, payload);
    return data.data;
  },
  async disable(id) {
    const { data } = await api.post(`/users/${id}/disable`);
    return data.data;
  },
  async enable(id) {
    const { data } = await api.post(`/users/${id}/enable`);
    return data.data;
  },
  async resetPassword(id, newPassword) {
    const { data } = await api.post(`/users/${id}/reset-password`, { newPassword });
    return data.data;
  },
  async loginHistory(id, params = {}) {
    const { data } = await api.get(`/users/${id}/login-history`, { params });
    return data.data;
  },
};

export default userService;
