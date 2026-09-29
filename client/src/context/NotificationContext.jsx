import { createContext, useCallback, useContext, useEffect, useMemo, useState } from 'react';
import { notificationsService } from '../services/apiServices';
import { useAuth } from './AuthContext';

const NotificationContext = createContext(null);

const POLL_MS = 60000;

export function NotificationProvider({ children }) {
  const { isAuthenticated } = useAuth();
  const [unread, setUnread] = useState([]);
  const [count, setCount] = useState(0);

  const refresh = useCallback(async () => {
    if (!isAuthenticated) {
      setUnread([]);
      setCount(0);
      return;
    }
    try {
      const [items, total] = await Promise.all([
        notificationsService.unread(),
        notificationsService.unreadCount(),
      ]);
      setUnread(items);
      setCount(total);
    } catch {
      // A failed poll should never surface as an error banner; the next tick retries.
    }
  }, [isAuthenticated]);

  useEffect(() => {
    refresh();
    if (!isAuthenticated) return undefined;
    const timer = setInterval(refresh, POLL_MS);
    return () => clearInterval(timer);
  }, [refresh, isAuthenticated]);

  const markRead = useCallback(
    async (id) => {
      setUnread((items) => items.filter((n) => n.id !== id));
      setCount((c) => Math.max(0, c - 1));
      try {
        await notificationsService.markRead(id);
      } finally {
        refresh();
      }
    },
    [refresh]
  );

  const markAllRead = useCallback(async () => {
    setUnread([]);
    setCount(0);
    try {
      await notificationsService.markAllRead();
    } finally {
      refresh();
    }
  }, [refresh]);

  const value = useMemo(
    () => ({ unread, count, refresh, markRead, markAllRead }),
    [unread, count, refresh, markRead, markAllRead]
  );

  return <NotificationContext.Provider value={value}>{children}</NotificationContext.Provider>;
}

export function useNotifications() {
  const context = useContext(NotificationContext);
  if (!context) {
    throw new Error('useNotifications must be used inside a NotificationProvider');
  }
  return context;
}
