import api from './api';

const evidenceService = {
  async list(params = {}) {
    const { data } = await api.get('/evidence', { params });
    return data.data;
  },
  async get(id) {
    const { data } = await api.get(`/evidence/${id}`);
    return data.data;
  },
  async upload(file, metadata, onProgress) {
    const formData = new FormData();
    formData.append('file', file);
    formData.append('metadata', JSON.stringify(metadata));
    const { data } = await api.post('/evidence', formData, {
      headers: { 'Content-Type': 'multipart/form-data' },
      onUploadProgress: (event) => {
        if (onProgress && event.total) {
          onProgress(Math.round((event.loaded * 100) / event.total));
        }
      },
    });
    return data.data;
  },
  async download(id, fileName) {
    const response = await api.get(`/evidence/${id}/download`, { responseType: 'blob' });
    const url = window.URL.createObjectURL(new Blob([response.data]));
    const link = document.createElement('a');
    link.href = url;
    link.setAttribute('download', fileName || 'evidence-file');
    document.body.appendChild(link);
    link.click();
    link.remove();
    window.URL.revokeObjectURL(url);
  },
  async verifyIntegrity(id) {
    const { data } = await api.post(`/evidence/${id}/verify`);
    return data.data;
  },
  async transfer(id, payload) {
    const { data } = await api.post(`/evidence/${id}/transfer`, payload);
    return data.data;
  },
  async chainOfCustody(id) {
    const { data } = await api.get(`/evidence/${id}/chain-of-custody`);
    return data.data;
  },
  async seal(id, remarks) {
    const { data } = await api.post(`/evidence/${id}/seal`, null, { params: { remarks } });
    return data.data;
  },
  async reopen(id, remarks) {
    const { data } = await api.post(`/evidence/${id}/reopen`, null, { params: { remarks } });
    return data.data;
  },
};

export default evidenceService;
