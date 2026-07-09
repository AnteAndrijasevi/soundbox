import axios from 'axios';

export const TOKEN_KEY = 'soundbox_token';
export const USER_KEY = 'soundbox_user';

const client = axios.create({
  baseURL: '/api',
});

client.interceptors.request.use((config) => {
  const token = localStorage.getItem(TOKEN_KEY);
  if (token) {
    config.headers.Authorization = `Bearer ${token}`;
  }
  return config;
});

client.interceptors.response.use(
  (response) => response,
  (error) => {
    const status = error.response?.status;
    const url = error.config?.url ?? '';
    // Expired/invalid token: drop credentials and send the user back to login.
    // Auth endpoints are excluded so a failed login doesn't trigger a redirect loop.
    if (status === 401 && !url.startsWith('/auth')) {
      localStorage.removeItem(TOKEN_KEY);
      localStorage.removeItem(USER_KEY);
      if (window.location.pathname !== '/login') {
        window.location.assign('/login');
      }
    }
    return Promise.reject(error);
  }
);

export default client;
