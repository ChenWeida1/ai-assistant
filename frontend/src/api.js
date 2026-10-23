import axios from 'axios'

const http = axios.create({ baseURL: '/api', timeout: 120000 })

http.interceptors.response.use(
  (response) => {
    const body = response.data
    if (body && body.code !== 200) {
      return Promise.reject(new Error(body.message || '请求失败'))
    }
    return body.data
  },
  (error) => Promise.reject(new Error(error.response?.data?.message || error.message || '网络错误'))
)

export function fetchHealth() {
  return http.get('/health')
}

export function fetchOrders() {
  return http.get('/orders')
}

export function fetchDocuments() {
  return http.get('/knowledge/documents')
}

export function uploadDocument(file) {
  const form = new FormData()
  form.append('file', file)
  return http.post('/knowledge/documents', form)
}

export function deleteDocument(id) {
  return http.delete(`/knowledge/documents/${id}`)
}

export function sendChat(sessionId, message) {
  return http.post('/chat', { sessionId, message })
}
