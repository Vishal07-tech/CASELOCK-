import api from './api';

const notificationService = {
  async list(params = {}) {
    const { data } = await api.get('/notifications', { params });
    return data.data;
  },
  async unreadCount() {
    const { data } = await api.get('/notifications/unread-count');
    return data.data;
  },
  async markRead(id) {
    const { data } = await api.post(`/notifications/${id}/read`);
    return data.data;
  },
  async markAllRead() {
    await api.post('/notifications/read-all');
  },
};

export default notificationService;
