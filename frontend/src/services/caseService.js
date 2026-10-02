import api from './api';

const caseService = {
  async list(params = {}) {
    const { data } = await api.get('/cases', { params });
    return data.data;
  },
  async get(id) {
    const { data } = await api.get(`/cases/${id}`);
    return data.data;
  },
  async create(payload) {
    const { data } = await api.post('/cases', payload);
    return data.data;
  },
  async update(id, payload) {
    const { data } = await api.put(`/cases/${id}`, payload);
    return data.data;
  },
  async updateStatus(id, status, remarks) {
    const { data } = await api.patch(`/cases/${id}/status`, { status, remarks });
    return data.data;
  },
};

export default caseService;
