import api from './api';

const reportService = {
  async get(caseId) {
    const { data } = await api.get(`/reports/${caseId}`);
    return data.data;
  },
  async downloadPdf(caseId, caseNumber) {
    const response = await api.get(`/reports/${caseId}/pdf`, { responseType: 'blob' });
    const url = window.URL.createObjectURL(new Blob([response.data], { type: 'application/pdf' }));
    const link = document.createElement('a');
    link.href = url;
    link.setAttribute('download', `case-${caseNumber || caseId}-report.pdf`);
    document.body.appendChild(link);
    link.click();
    link.remove();
    window.URL.revokeObjectURL(url);
  },
};

export default reportService;
