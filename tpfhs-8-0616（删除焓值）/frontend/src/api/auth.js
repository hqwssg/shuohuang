import axios from 'axios';

const API_BASE_URL = '/api';

export const authApi = {
  login(username, password) {
    return axios.post(`${API_BASE_URL}/user/login`, { username, password });
  }
};

export const templateApi = {
  getAllTemplates() {
    return axios.get(`${API_BASE_URL}/template/list`);
  },
  
  getTemplateById(id) {
    return axios.get(`${API_BASE_URL}/template/${id}`);
  },
  
  createTemplate(name, createdBy, description = '') {
    return axios.post(`${API_BASE_URL}/template/create`, { name, createdBy, description });
  },
  
  updateTemplate(id, name, updatedBy) {
    return axios.put(`${API_BASE_URL}/template/${id}`, { name, updatedBy });
  },
  
  updateTemplateProperties(id, description, enabled, taskConfig, updatedBy) {
    return axios.put(`${API_BASE_URL}/template/${id}/properties`, { description, enabled, taskConfig, updatedBy });
  },
  
  copyTemplate(sourceId, newName, createdBy) {
    return axios.post(`${API_BASE_URL}/template/copy/${sourceId}`, { newName, createdBy });
  },
  
  deleteTemplate(id) {
    return axios.delete(`${API_BASE_URL}/template/${id}`);
  }
};

export const nodeApi = {
  getTree(templateId = null) {
    const url = templateId ? `${API_BASE_URL}/nodes/tree?templateId=${templateId}` : `${API_BASE_URL}/nodes/tree`;
    return axios.get(url);
  },
  
  getNodeById(id) {
    return axios.get(`${API_BASE_URL}/nodes/${id}`);
  },
  
  createNode(data) {
    return axios.post(`${API_BASE_URL}/nodes`, data);
  },
  
  updateNode(id, data) {
    return axios.put(`${API_BASE_URL}/nodes/${id}`, data);
  },
  
  deleteNode(id) {
    return axios.delete(`${API_BASE_URL}/nodes/${id}`);
  },
  
  moveNode(id, direction) {
    return axios.post(`${API_BASE_URL}/nodes/${id}/move?direction=${direction}`);
  },
  
  getNodeConfig(id) {
    return axios.get(`${API_BASE_URL}/nodes/${id}/config`);
  },
  
  updateNodeConfig(id, config) {
    return axios.put(`${API_BASE_URL}/nodes/${id}/config`, config);
  },
  
  getOptions() {
    return axios.get(`${API_BASE_URL}/nodes/options`);
  }
};
