import api from './api';

const custodyService = {
  async forCase(caseId, params = {}) {
    const { data } = await api.get(`/chain-of-custody/case/${caseId}`, { params });
    return data.data;
  },
};

export default custodyService;
