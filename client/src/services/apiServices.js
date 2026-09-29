import api from './api';

export const RANGES = [
  { value: '1d', label: '24 hours' },
  { value: '7d', label: '7 days' },
  { value: '30d', label: '30 days' },
  { value: '90d', label: '90 days' },
  { value: '365d', label: '12 months' },
  { value: 'all', label: 'All time' },
];

export const PLATFORMS = [
  { value: 'all', label: 'All platforms' },
  { value: 'INSTAGRAM', label: 'Instagram' },
  { value: 'TIKTOK', label: 'TikTok' },
  { value: 'X', label: 'X' },
  { value: 'FACEBOOK', label: 'Facebook' },
  { value: 'LINKEDIN', label: 'LinkedIn' },
  { value: 'YOUTUBE', label: 'YouTube' },
];

export const POST_TYPES = [
  { value: 'all', label: 'All types' },
  { value: 'IMAGE', label: 'Image' },
  { value: 'VIDEO', label: 'Video' },
  { value: 'CAROUSEL', label: 'Carousel' },
  { value: 'REEL', label: 'Reel' },
  { value: 'SHORT', label: 'Short' },
  { value: 'STORY', label: 'Story' },
  { value: 'TEXT', label: 'Text' },
  { value: 'LIVE', label: 'Live' },
];

export const NOTIFICATION_TYPES = [
  { value: 'all', label: 'All activity' },
  { value: 'FOLLOWER_GAIN', label: 'Follower gain' },
  { value: 'NEW_COMMENT', label: 'Comments' },
  { value: 'REPORT_READY', label: 'Reports' },
  { value: 'ACCOUNT_ISSUE', label: 'Account issues' },
  { value: 'MILESTONE', label: 'Milestones' },
];

/** Drops empty filters so the query string stays clean. */
const clean = (params) =>
  Object.fromEntries(Object.entries(params).filter(([, v]) => v !== undefined && v !== null && v !== '' && v !== 'all'));

export const statsService = {
  overview: (params) => api.get('/stats/overview', { params: clean(params) }).then((r) => r.data),
  timeSeries: (params) => api.get('/stats/timeseries', { params: clean(params) }).then((r) => r.data),
  report: (params) => api.get('/stats/reports', { params: clean(params) }).then((r) => r.data),
};

export const accountsService = {
  list: () => api.get('/accounts').then((r) => r.data),
  connect: (payload) => api.post('/accounts', payload).then((r) => r.data),
  update: (id, payload) => api.put(`/accounts/${id}`, payload).then((r) => r.data),
  disconnect: (id) => api.delete(`/accounts/${id}`).then(() => true),
  sync: (id) => api.post(`/accounts/${id}/sync`).then((r) => r.data),
  syncAll: () => api.post('/accounts/sync').then((r) => r.data),
};

export const postsService = {
  list: (params) => api.get('/posts', { params: clean(params) }).then((r) => r.data),
  hashtags: (params) => api.get('/posts/hashtags', { params: clean(params) }).then((r) => r.data),
};

export const notificationsService = {
  list: (params) => api.get('/notifications', { params: clean(params) }).then((r) => r.data),
  unread: () => api.get('/notifications/unread').then((r) => r.data),
  unreadCount: () => api.get('/notifications/unread-count').then((r) => r.data.count),
  markRead: (id) => api.patch(`/notifications/${id}/read`).then((r) => r.data),
  markAllRead: () => api.post('/notifications/read-all').then((r) => r.data.updated),
};

export const GOAL_METRICS = [
  { value: 'FOLLOWERS', label: 'Followers', unit: 'followers' },
  { value: 'LIKES', label: 'Likes', unit: 'likes' },
  { value: 'COMMENTS', label: 'Comments', unit: 'comments' },
  { value: 'REACH', label: 'Reach', unit: 'impressions reached' },
  { value: 'POSTS', label: 'Posts published', unit: 'posts' },
  { value: 'ENGAGEMENT_RATE', label: 'Engagement rate', unit: '%' },
];

export const GOAL_PERIODS = [
  { value: 'WEEKLY', label: 'Weekly', days: 7 },
  { value: 'MONTHLY', label: 'Monthly', days: 30 },
  { value: 'QUARTERLY', label: 'Quarterly', days: 90 },
];

export const goalsService = {
  list: () => api.get('/goals').then((r) => r.data),
  create: (payload) => api.post('/goals', payload).then((r) => r.data),
  update: (id, payload) => api.put(`/goals/${id}`, payload).then((r) => r.data),
  remove: (id) => api.delete(`/goals/${id}`).then(() => true),
};

export const insightsService = {
  list: (params) => api.get('/insights', { params: clean(params) }).then((r) => r.data),
};
