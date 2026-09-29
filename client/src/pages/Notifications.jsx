import { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { useAsync } from '../hooks/useAsync';
import { NOTIFICATION_TYPES, notificationsService } from '../services/apiServices';
import { useNotifications } from '../context/NotificationContext';
import { EmptyState, ErrorState, Loading } from '../components/States';
import { formatDateTime, timeAgo } from '../utils/format';

const TYPE_META = {
  FOLLOWER_GAIN: { icon: '📈', label: 'Follower gain', bg: 'var(--success-soft)', color: 'var(--success)' },
  NEW_COMMENT: { icon: '💬', label: 'Comment', bg: 'var(--brand-soft)', color: 'var(--brand)' },
  REPORT_READY: { icon: '📊', label: 'Report', bg: 'rgba(20,184,166,0.14)', color: 'var(--chart-3)' },
  ACCOUNT_ISSUE: { icon: '⚠', label: 'Account issue', bg: 'var(--warning-soft)', color: 'var(--warning)' },
  MILESTONE: { icon: '★', label: 'Milestone', bg: 'rgba(236,72,153,0.14)', color: 'var(--chart-2)' },
};

const PAGE_SIZE = 20;

export default function Notifications() {
  const navigate = useNavigate();
  const { markRead, markAllRead, count, refresh } = useNotifications();
  const [type, setType] = useState('all');
  const [page, setPage] = useState(0);

  const list = useAsync(
    () => notificationsService.list({ page, size: PAGE_SIZE, type }),
    [page, type]
  );

  const items = list.data?.content || [];
  const totalPages = list.data?.totalPages || 0;

  async function open(item) {
    if (!item.read) {
      await markRead(item.id);
    }
    if (item.link) {
      navigate(item.link);
    }
    list.reload();
  }

  return (
    <>
      <div className="page-header">
        <div>
          <h2>Notifications</h2>
          <p>
            {count > 0 ? `${count} unread` : 'You are all caught up'} · follower changes, comments, reports and
            account health
          </p>
        </div>
        <button
          type="button"
          className="btn btn-secondary"
          onClick={async () => {
            await markAllRead();
            list.reload();
          }}
          disabled={count === 0}
        >
          ✓ Mark all read
        </button>
      </div>

      <div className="filter-bar">
        <select
          className="select"
          value={type}
          onChange={(e) => {
            setType(e.target.value);
            setPage(0);
          }}
          aria-label="Filter by notification type"
          style={{ width: 'auto', minWidth: 170 }}
        >
          {NOTIFICATION_TYPES.map((option) => (
            <option key={option.value} value={option.value}>
              {option.label}
            </option>
          ))}
        </select>

        <button type="button" className="btn btn-secondary btn-sm" onClick={() => { refresh(); list.reload(); }}>
          ⟳ Refresh
        </button>
      </div>

      {list.error ? (
        <ErrorState message={list.error} onRetry={list.reload} />
      ) : list.loading && !list.data ? (
        <Loading label="Loading notifications..." />
      ) : items.length === 0 ? (
        <EmptyState
          icon="◔"
          title="No notifications"
          description={type === 'all' ? 'Activity from your connected accounts will appear here.' : 'No notifications of this type.'}
        />
      ) : (
        <>
          <div className="notif-list">
            {items.map((item) => {
              const meta = TYPE_META[item.type] || { icon: '◈', label: item.type, bg: 'var(--bg)', color: 'var(--text-muted)' };
              return (
                <button
                  key={item.id}
                  type="button"
                  className={`notif-item${item.read ? '' : ' unread'}`}
                  onClick={() => open(item)}
                >
                  <span
                    className="notif-icon"
                    style={{ background: meta.bg, color: meta.color }}
                    aria-hidden="true"
                  >
                    {meta.icon}
                  </span>
                  <span className="notif-body">
                    <span className="notif-title">{item.title}</span>
                    <span className="notif-message">{item.message}</span>
                    <span className="notif-time" title={formatDateTime(item.createdAt)}>
                      {timeAgo(item.createdAt)} · {meta.label}
                    </span>
                  </span>
                  {!item.read && <span className="badge badge-brand">New</span>}
                </button>
              );
            })}
          </div>

          {totalPages > 1 && (
            <div className="pagination">
              <button
                type="button"
                className="btn btn-secondary btn-sm"
                onClick={() => setPage((p) => Math.max(0, p - 1))}
                disabled={page === 0}
              >
                ← Previous
              </button>
              <span className="info">
                Page {page + 1} of {totalPages}
              </span>
              <button
                type="button"
                className="btn btn-secondary btn-sm"
                onClick={() => setPage((p) => p + 1)}
                disabled={page + 1 >= totalPages}
              >
                Next →
              </button>
            </div>
          )}
        </>
      )}
    </>
  );
}
