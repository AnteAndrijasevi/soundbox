import client from './client';

/** Pull a human message out of a ProblemDetail (RFC 7807) response, else fall back. */
export function apiError(err, fallback) {
  const data = err?.response?.data;
  return data?.detail ?? data?.message ?? data?.error ?? fallback;
}

// Auth
export const register = (data) => client.post('/auth/register', data);
export const login = (data) => client.post('/auth/login', data);

// Users
export const getMe = () => client.get('/users/me');
export const getUser = (userId) => client.get(`/users/${userId}`);
export const updateBio = (bio) => client.patch('/users/me/bio', { bio });
export const toggleFollow = (userId) => client.post(`/users/${userId}/follow`);

// Albums
export const searchAlbums = (query, limit = 20, offset = 0) =>
  client.get('/albums/search', { params: { query, limit, offset } });
export const getAlbum = (mbid) => client.get(`/albums/${mbid}`);

// Reviews
export const getAlbumReviews = (mbid, page = 0, size = 10) =>
  client.get(`/albums/${mbid}/reviews`, { params: { page, size } });
export const getUserReviews = (userId, page = 0, size = 10) =>
  client.get(`/users/${userId}/reviews`, { params: { page, size } });
export const submitReview = (mbid, data) => client.post(`/albums/${mbid}/reviews`, data);
export const deleteReview = (reviewId) => client.delete(`/reviews/${reviewId}`);
export const toggleLike = (reviewId) => client.post(`/reviews/${reviewId}/like`);

// Listen log
export const logListen = (mbid, data) => client.post(`/albums/${mbid}/log`, data);
export const getMyLog = (page = 0, size = 12) =>
  client.get('/users/me/log', { params: { page, size } });
export const getUserLog = (userId, page = 0, size = 12) =>
  client.get(`/users/${userId}/log`, { params: { page, size } });
export const getMyRelistenHistory = (mbid) =>
  client.get(`/users/me/albums/${mbid}/history`);
export const getUserRelistenHistory = (userId, mbid) =>
  client.get(`/users/${userId}/albums/${mbid}/history`);

// Feed
export const getFeed = (page = 0, size = 10) =>
  client.get('/feed', { params: { page, size } });

// Notifications
export const getNotifications = (page = 0, size = 20) =>
  client.get('/users/me/notifications', { params: { page, size } });
export const getUnreadCount = () => client.get('/users/me/notifications/unread-count');
export const markNotificationsRead = () => client.post('/users/me/notifications/read');

// Lists
export const createList = (data) => client.post('/lists', data);
export const getUserLists = (userId) => client.get(`/users/${userId}/lists`);
export const getListDetail = (listId) => client.get(`/lists/${listId}`);
export const addAlbumToList = (listId, mbid, note) =>
  client.post(`/lists/${listId}/albums/${mbid}`, note ? { note } : {});
export const removeAlbumFromList = (listId, mbid) =>
  client.delete(`/lists/${listId}/albums/${mbid}`);
export const deleteList = (listId) => client.delete(`/lists/${listId}`);
