import api from './api';

const auditService = {
  async search(params = {}) {
    const { data } = await api.get('/audit-logs', { params });
    return data.data;
  },
};

export default auditService;
