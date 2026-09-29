import { useState } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import { useAuth } from '../context/AuthContext';
import { Alert } from '../components/States';

const DEMO = { email: 'demo@socialdashboard.dev', password: 'demo12345' };

export default function Login() {
  const { login } = useAuth();
  const navigate = useNavigate();
  const [form, setForm] = useState({ email: '', password: '' });
  const [error, setError] = useState('');
  const [busy, setBusy] = useState(false);

  const update = (key) => (e) => setForm((f) => ({ ...f, [key]: e.target.value }));

  async function handleSubmit(event) {
    event.preventDefault();
    setError('');
    setBusy(true);
    try {
      await login({ email: form.email.trim(), password: form.password });
      navigate('/dashboard', { replace: true });
    } catch (err) {
      setError(err.message);
    } finally {
      setBusy(false);
    }
  }

  function fillDemo() {
    setForm(DEMO);
    setError('');
  }

  return (
    <div className="auth-screen">
      <div className="auth-card">
        <div className="auth-brand">
          <div className="logo">◈</div>
          <h1>Social Dashboard</h1>
        </div>
        <p className="auth-subtitle">Sign in to view your analytics</p>

        <form className="auth-form" onSubmit={handleSubmit} noValidate>
          {error && <Alert kind="error">{error}</Alert>}

          <div className="field">
            <label htmlFor="email">Email address</label>
            <input
              id="email"
              type="email"
              className="input"
              value={form.email}
              onChange={update('email')}
              placeholder="you@example.com"
              autoComplete="email"
              required
            />
          </div>

          <div className="field">
            <label htmlFor="password">Password</label>
            <input
              id="password"
              type="password"
              className="input"
              value={form.password}
              onChange={update('password')}
              placeholder="Your password"
              autoComplete="current-password"
              required
            />
          </div>

          <button type="submit" className="btn btn-primary btn-block" disabled={busy}>
            {busy ? <span className="spinner" /> : null}
            {busy ? 'Signing in...' : 'Sign in'}
          </button>
        </form>

        <div className="demo-hint">
          <button type="button" className="btn btn-sm btn-secondary" onClick={fillDemo} style={{ marginBottom: 8 }}>
            Fill demo credentials
          </button>
          <div>
            Demo account: <code>{DEMO.email}</code> / <code>{DEMO.password}</code>
          </div>
        </div>

        <div className="auth-footer">
          Don&apos;t have an account? <Link to="/register">Create one</Link>
        </div>
      </div>
    </div>
  );
}
