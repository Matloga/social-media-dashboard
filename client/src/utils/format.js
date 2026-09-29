const compact = new Intl.NumberFormat('en-US', { notation: 'compact', maximumFractionDigits: 1 });
const plain = new Intl.NumberFormat('en-US');

/** 12345 -> "12.3K". Use for axis ticks and tight card layouts. */
export const compactNumber = (value) => compact.format(Number(value) || 0);

/** 12345 -> "12,345". Use where the exact figure matters. */
export const formatNumber = (value) => plain.format(Number(value) || 0);

/**
 * Renders a card value according to the server's "format" hint.
 * The API returns engagement rate as a real percentage (e.g. 7.23), not a
 * fraction, so it only needs the sign appended.
 */
export const formatCardValue = (value, format) => {
  if (format === 'percent') {
    return `${(Number(value) || 0).toFixed(2)}%`;
  }
  return formatNumber(value);
};

export const formatPercent = (value, digits = 1) => {
  const n = Number(value) || 0;
  return `${n > 0 ? '+' : ''}${n.toFixed(digits)}%`;
};

const dateTimeFormat = new Intl.DateTimeFormat('en-GB', {
  day: 'numeric',
  month: 'short',
  year: 'numeric',
  hour: '2-digit',
  minute: '2-digit',
});

const dateFormat = new Intl.DateTimeFormat('en-GB', {
  day: 'numeric',
  month: 'short',
  year: 'numeric',
});

export const formatDateTime = (iso) => (iso ? dateTimeFormat.format(new Date(iso)) : '-');

export const formatDate = (iso) => (iso ? dateFormat.format(new Date(iso)) : '-');

/** "3h ago" style label, for notification and post timestamps. */
export function timeAgo(iso) {
  if (!iso) return '-';
  const seconds = Math.floor((Date.now() - new Date(iso).getTime()) / 1000);
  if (seconds < 60) return 'just now';
  const units = [
    ['year', 31536000],
    ['month', 2592000],
    ['week', 604800],
    ['day', 86400],
    ['hour', 3600],
    ['minute', 60],
  ];
  for (const [name, secs] of units) {
    const value = Math.floor(seconds / secs);
    if (value >= 1) return `${value} ${name}${value > 1 ? 's' : ''} ago`;
  }
  return 'just now';
}

export const initials = (name = '') =>
  name
    .split(' ')
    .filter(Boolean)
    .slice(0, 2)
    .map((part) => part[0].toUpperCase())
    .join('') || '?';

export const PLATFORM_LABELS = {
  INSTAGRAM: 'Instagram',
  TIKTOK: 'TikTok',
  X: 'X',
  FACEBOOK: 'Facebook',
  LINKEDIN: 'LinkedIn',
  YOUTUBE: 'YouTube',
};

export const PLATFORM_COLORS = {
  INSTAGRAM: '#e1306c',
  TIKTOK: '#00f2ea',
  X: '#0f1419',
  FACEBOOK: '#1877f2',
  LINKEDIN: '#0a66c2',
  YOUTUBE: '#ff0000',
};

export const POST_TYPE_ICONS = {
  IMAGE: '▣',
  VIDEO: '▶',
  CAROUSEL: '◫',
  REEL: '↻',
  SHORT: '⇱',
  STORY: '◔',
  TEXT: '≡',
  LIVE: '◉',
};
