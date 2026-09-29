import { useState } from 'react';
import { Link } from 'react-router-dom';
import { useAsync } from '../hooks/useAsync';
import { PLATFORMS, accountsService } from '../services/apiServices';
import { Alert, Badge, EmptyState, ErrorState, Loading } from '../components/States';
import { PLATFORM_COLORS, PLATFORM_LABELS, compactNumber, formatDateTime, timeAgo } from '../utils/format';

const STATUS_KIND = { ACTIVE: 'success', WARNING: 'warning', ERROR: 'danger', DISCONNECTED: 'neutral' };
const PLATFORM_GLYPH = {
  INSTAGRAM: '◎',
  TIKTOK: '♪',
  X: '𝕏',
  FACEBOOK: 'f',
  LINKEDIN: 'in',
  YOUTUBE: '▶',
};

export default function Accounts() {
  const accounts = useAsync(() => accountsService.list(), []);
  const [form, setForm] = useState({ platform: 'INSTAGRAM', username: '' });
  const [busy, setBusy] = useState(false);
  const [syncingId, setSyncingId] = useState(null);
  const [error, setError] = useState('');
  const [success, setSuccess] = useState('');

  const list = accounts.data || [];

  async function handleConnect(event) {
    event.preventDefault();
    setError('');
    setSuccess('');
    if (!form.username.trim()) {
      setError('Enter a username to connect.');
      return;
    }
    setBusy(true);
    try {
      const account = await accountsService.connect({
        platform: form.platform,
        username: form.username.trim(),
      });
      setForm((f) => ({ ...f, username: '' }));
      setSuccess(`${PLATFORM_LABELS[account.platform]} account @${account.username} connected and syncing.`);
      accounts.reload();
    } catch (err) {
      setError(err.message);
    } finally {
      setBusy(false);
    }
  }

  async function handleSync(id) {
    setSyncingId(id);
    setError('');
    try {
      await accountsService.sync(id);
      accounts.reload();
    } catch (err) {
      setError(err.message);
    } finally {
      setSyncingId(null);
    }
  }

  async function handleSyncAll() {
    setSyncingId('all');
    setError('');
    try {
      await accountsService.syncAll();
      accounts.reload();
    } catch (err) {
      setError(err.message);
    } finally {
      setSyncingId(null);
    }
  }

  async function handleDisconnect(account) {
    const confirmed = window.confirm(
      `Disconnect @${account.username}? Its posts and analytics history will be removed.`
    );
    if (!confirmed) return;
    setError('');
    try {
      await accountsService.disconnect(account.id);
      accounts.reload();
    } catch (err) {
      setError(err.message);
    }
  }

  return (
    <>
      <div className="page-header">
        <div>
          <h2>Connected accounts</h2>
          <p>Link each social profile to pull its analytics into one dashboard.</p>
        </div>
        {list.length > 0 && (
          <button
            type="button"
            className="btn btn-secondary"
            onClick={handleSyncAll}
            disabled={syncingId === 'all'}
          >
            {syncingId === 'all' ? <span className="spinner dark" /> : '⟳'}
            Sync all
          </button>
        )}
      </div>

      {error && <Alert kind="error">{error}</Alert>}
      {success && <Alert kind="success">{success}</Alert>}

      <div className="two-col">
        <div className="stack">
          {accounts.loading && !accounts.data ? (
            <Loading label="Loading accounts..." />
          ) : accounts.error ? (
            <ErrorState message={accounts.error} onRetry={accounts.reload} />
          ) : list.length === 0 ? (
            <EmptyState
              icon="◎"
              title="No accounts connected"
              description="Connect your first account to start tracking performance."
            />
          ) : (
            list.map((account) => (
              <div key={account.id} className="card">
                <div className="account-card">
                  <div
                    className="avatar"
                    style={{ background: PLATFORM_COLORS[account.platform] || 'var(--brand)' }}
                    aria-hidden="true"
                  >
                    {PLATFORM_GLYPH[account.platform] || '◎'}
                  </div>

                  <div className="info">
                    <div className="row" style={{ gap: 8 }}>
                      <strong>{PLATFORM_LABELS[account.platform] || account.platform}</strong>
                      <span className="muted">{account.handle}</span>
                      <Badge kind={STATUS_KIND[account.status] || 'neutral'}>{account.status}</Badge>
                    </div>
                    <div className="muted text-sm">
                      {compactNumber(account.followers)} followers · {compactNumber(account.following)} following
                    </div>
                    {account.statusMessage && account.status !== 'ACTIVE' && (
                      <div className="text-sm" style={{ color: 'var(--warning)', marginTop: 4 }}>
                        {account.statusMessage}
                      </div>
                    )}
                  </div>

                  <div style={{ textAlign: 'right' }}>
                    <div className="faint text-sm">Last synced</div>
                    <div className="text-sm">{timeAgo(account.lastSyncedAt)}</div>
                  </div>
                </div>

                <div className="row mt-2" style={{ justifyContent: 'flex-end', gap: 8 }}>
                  <button
                    type="button"
                    className="btn btn-secondary btn-sm"
                    onClick={() => handleSync(account.id)}
                    disabled={syncingId === account.id}
                  >
                    {syncingId === account.id ? <span className="spinner dark" /> : '⟳'} Sync
                  </button>
                  <button
                    type="button"
                    className="btn btn-danger btn-sm"
                    onClick={() => handleDisconnect(account)}
                  >
                    Disconnect
                  </button>
                </div>
              </div>
            ))
          )}
        </div>

        <div className="card">
          <div className="chart-title mb-2">Connect an account</div>
          <p className="muted text-sm mb-2">
            A year of analytics history is generated as soon as the account connects, so charts are never empty.
          </p>

          <form className="stack" onSubmit={handleConnect}>
            <div className="field">
              <label htmlFor="platform">Platform</label>
              <select
                id="platform"
                className="select"
                value={form.platform}
                onChange={(e) => setForm((f) => ({ ...f, platform: e.target.value }))}
              >
                {PLATFORMS.filter((p) => p.value !== 'all').map((p) => (
                  <option key={p.value} value={p.value}>
                    {p.label}
                  </option>
                ))}
              </select>
            </div>

            <div className="field">
              <label htmlFor="username">Username</label>
              <input
                id="username"
                className="input"
                value={form.username}
                onChange={(e) => setForm((f) => ({ ...f, username: e.target.value }))}
                placeholder="yourhandle"
                maxLength={80}
              />
            </div>

            <button type="submit" className="btn btn-primary" disabled={busy}>
              {busy ? <span className="spinner" /> : '+'} Connect account
            </button>
          </form>

          <div className="muted text-sm mt-3">
            Connected {list.length} account{list.length === 1 ? '' : 's'}.{' '}
            <Link to="/posts">Browse posts</Link>
          </div>

          {list.length > 0 && (
            <div className="faint text-sm mt-2">
              Earliest connection: {formatDateTime(
                list.map((a) => a.connectedAt).sort()[0]
              )}
            </div>
          )}
        </div>
      </div>
    </>
  );
}
