import { createContext, useCallback, useContext, useEffect, useMemo, useState } from 'react';
import { authService } from '../services/authService';
import { setUnauthorizedHandler, tokenStore } from '../services/api';

const AuthContext = createContext(null);

export function AuthProvider({ children }) {
  const [user, setUser] = useState(null);
  // "loading" until the stored token has been checked, so protected routes do
  // not bounce an authenticated user to /login on a hard refresh.
  const [status, setStatus] = useState('loading');

  useEffect(() => {
    let cancelled = false;

    async function restore() {
      if (!tokenStore.get()) {
        setStatus('anonymous');
        return;
      }
      try {
        const profile = await authService.me();
        if (!cancelled) setUser(profile);
      } catch {
        tokenStore.clear();
      } finally {
        if (!cancelled) setStatus('anonymous');
      }
    }

    restore();
    return () => {
      cancelled = true;
    };
  }, []);

  useEffect(() => {
    setUnauthorizedHandler(() => {
      setUser(null);
      setStatus('anonymous');
    });
  }, []);

  const login = useCallback(async (credentials) => {
    const data = await authService.login(credentials);
    tokenStore.set(data.token);
    setUser(data.user);
    setStatus('authenticated');
    return data.user;
  }, []);

  const register = useCallback(async (payload) => {
    const data = await authService.register(payload);
    tokenStore.set(data.token);
    setUser(data.user);
    setStatus('authenticated');
    return data.user;
  }, []);

  const logout = useCallback(async () => {
    try {
      await authService.logout();
    } catch {
      // The token is dropped client-side regardless of the server response.
    }
    tokenStore.clear();
    setUser(null);
    setStatus('anonymous');
  }, []);

  const updateProfile = useCallback(async (payload) => {
    const updated = await authService.updateProfile(payload);
    setUser(updated);
    return updated;
  }, []);

  const value = useMemo(
    () => ({ user, status, login, register, logout, updateProfile, isAuthenticated: status === 'authenticated' }),
    [user, status, login, register, logout, updateProfile]
  );

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>;
}

export function useAuth() {
  const context = useContext(AuthContext);
  if (!context) {
    throw new Error('useAuth must be used inside an AuthProvider');
  }
  return context;
}
