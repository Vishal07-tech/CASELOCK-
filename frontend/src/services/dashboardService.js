import api from './api';

const dashboardService = {
  async getStats() {
    const { data } = await api.get('/dashboard/stats');
    return data.data;
  },
};

export default dashboardService;
