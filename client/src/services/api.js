import axios from 'axios';

const TOKEN_KEY = 'sd-token';

export const tokenStore = {
  get: () => localStorage.getItem(TOKEN_KEY),
  set: (token) => localStorage.setItem(TOKEN_KEY, token),
  clear: () => localStorage.removeItem(TOKEN_KEY),
};

const api = axios.create({
  baseURL: import.meta.env.VITE_API_URL || '/api',
  headers: { 'Content-Type': 'application/json' },
  timeout: 20000,
});

api.interceptors.request.use((config) => {
  const token = tokenStore.get();
  if (token) {
    config.headers.Authorization = `Bearer ${token}`;
  }
  return config;
});

/**
 * Called when the API rejects the stored token. Kept as a mutable hook so the
 * auth context can install the real handler without a circular import.
 */
let onUnauthorized = () => {};
export const setUnauthorizedHandler = (fn) => {
  onUnauthorized = fn;
};

api.interceptors.response.use(
  (response) => response,
  (error) => {
    const status = error.response?.status;
    const isAuthRoute = error.config?.url?.includes('/auth/login') || error.config?.url?.includes('/auth/register');

    if (status === 401 && !isAuthRoute) {
      tokenStore.clear();
      onUnauthorized();
    }

    const message =
      error.response?.data?.message ||
      (error.code === 'ECONNABORTED' ? 'The request timed out. Please try again.' : null) ||
      (!error.response ? 'Cannot reach the server. Is the backend running?' : null) ||
      'Something went wrong.';

    return Promise.reject(new Error(message));
  }
);

export default api;
