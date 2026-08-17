import axios from 'axios'

const baseURL = '/api/nodes'

export const nodeApi = {
  getTree(templateId = null) {
    const url = templateId ? `${baseURL}/tree?templateId=${templateId}` : `${baseURL}/tree`
    return axios.get(url)
  },
  
  getNodeById(id) {
    return axios.get(`${baseURL}/${id}`)
  },
  
  createNode(data) {
    return axios.post(baseURL, data)
  },
  
  updateNode(id, data) {
    return axios.put(`${baseURL}/${id}`, data)
  },
  
  deleteNode(id) {
    return axios.delete(`${baseURL}/${id}`)
  },
  
  moveNode(id, direction) {
    return axios.post(`${baseURL}/${id}/move?direction=${direction}`)
  },
  
  getNodeConfig(id) {
    return axios.get(`${baseURL}/${id}/config`)
  },
  
  updateNodeConfig(id, config) {
    return axios.put(`${baseURL}/${id}/config`, config)
  },
  
  getOptions() {
    return axios.get(`${baseURL}/options`)
  }
}