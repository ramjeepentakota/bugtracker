import axios from 'axios';
import toast from 'react-hot-toast';

// Create axios instance with base configuration
const api = axios.create({
  baseURL: '/api',
  timeout: 10000,
  headers: {
    'Content-Type': 'application/json',
  },
});

// Request interceptor to add auth token
api.interceptors.request.use(
  (config) => {
    const token = localStorage.getItem('token');
    if (token) {
      config.headers.Authorization = `Bearer ${token}`;
    }
    return config;
  },
  (error) => {
    return Promise.reject(error);
  }
);

// Response interceptor to handle errors
api.interceptors.response.use(
  (response) => response,
  (error) => {
    if (error.response?.status === 401) {
      localStorage.removeItem('token');
      window.location.href = '/login';
    } else if (error.response?.status === 403) {
      toast.error('Access denied. You do not have permission to access this resource.');
      // Redirect to dashboard if trying to access restricted pages
      if (window.location.pathname !== '/dashboard') {
        window.location.href = '/dashboard';
      }
    }
    return Promise.reject(error);
  }
);

// Auth API
export const authAPI = {
  login: (credentials) => api.post('/auth/login', credentials),
  register: (userData) => api.post('/auth/register', userData),
  validateToken: () => api.get('/auth/validate'),
};

// Dashboard API
export const dashboardAPI = {
  getStats: () => api.get('/dashboard/stats'),
  getDefectsByApplication: () => api.get('/dashboard/defects-by-application'),
  getMonthlyTrends: () => api.get('/dashboard/monthly-trends'),
  getClientsWithMostDefects: () => api.get('/dashboard/clients-most-defects'),
};

// Clients API
export const clientsAPI = {
  getAll: () => api.get('/clients'),
  getPaged: (page = 0, size = 10) => api.get(`/clients/paged?page=${page}&size=${size}`),
  getById: (id) => api.get(`/clients/${id}`),
  create: (client) => api.post('/clients', client),
  update: (id, client) => api.put(`/clients/${id}`, client),
  delete: (id) => api.delete(`/clients/${id}`),
  search: (query) => api.get(`/clients/search?query=${query}`),
  getStats: (id) => api.get(`/clients/${id}/stats`),
};

// Applications API
export const applicationsAPI = {
  getAll: () => api.get('/applications'),
  getPaged: (page = 0, size = 10) => api.get(`/applications/paged?page=${page}&size=${size}`),
  getById: (id) => api.get(`/applications/${id}`),
  getByClient: (clientId) => api.get(`/applications/client/${clientId}`),
  create: (application) => api.post('/applications', application),
  update: (id, application) => api.put(`/applications/${id}`, application),
  delete: (id) => api.delete(`/applications/${id}`),
  searchByClient: (clientId, search) => api.get(`/applications/client/${clientId}/search?search=${search}`),
};

// Test Plans API
export const testPlansAPI = {
  getAll: () => api.get('/test-plans'),
  getPaged: (page = 0, size = 10) => api.get(`/test-plans/paged?page=${page}&size=${size}`),
  getById: (id) => api.get(`/test-plans/${id}`),
  getByTestCaseId: (testCaseId) => api.get(`/test-plans/test-case/${testCaseId}`),
  create: (testPlan) => api.post('/test-plans', testPlan),
  update: (id, testPlan) => api.put(`/test-plans/${id}`, testPlan),
  delete: (id) => api.delete(`/test-plans/${id}`),
  search: (query) => api.get(`/test-plans/search?query=${query}`),
};

// Defects API
export const defectsAPI = {
  getAll: () => api.get('/defects'),
  getPaged: (page = 0, size = 10) => api.get(`/defects/paged?page=${page}&size=${size}`),
  getById: (id) => api.get(`/defects/${id}`),
  getByDefectId: (defectId) => api.get(`/defects/defect-id/${defectId}`),
  getByClient: (clientId) => api.get(`/defects/client/${clientId}`),
  searchByClient: (clientId, search, page = 0, size = 10) =>
    api.get(`/defects/client/${clientId}/search?search=${search}&page=${page}&size=${size}`),
  create: (defect) => api.post('/defects', defect),
  update: (id, defect) => api.put(`/defects/${id}`, defect),
  delete: (id) => api.delete(`/defects/${id}`),
  getHistory: (id) => api.get(`/defects/${id}/history`),
};

export default api;