import { useState } from 'react';
import { useAuth } from '../context/AuthContext';
import { useTheme } from '../context/ThemeContext';
import { Alert } from '../components/States';
import { formatDate, initials } from '../utils/format';

export default function Profile() {
  const { user, updateProfile } = useAuth();
  const { isDark, toggle } = useTheme();

  const [form, setForm] = useState({
    name: user?.name || '',
    bio: user?.bio || '',
    newPassword: '',
    currentPassword: '',
  });
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState('');
  const [success, setSuccess] = useState('');

  const update = (key) => (e) => setForm((f) => ({ ...f, [key]: e.target.value }));

  async function handleSubmit(event) {
    event.preventDefault();
    setError('');
    setSuccess('');
    setBusy(true);
    try {
      const payload = { name: form.name.trim(), bio: form.bio.trim() };
      if (form.newPassword) {
        payload.newPassword = form.newPassword;
        payload.currentPassword = form.currentPassword;
      }
      await updateProfile(payload);
      setForm((f) => ({ ...f, newPassword: '', currentPassword: '' }));
      setSuccess('Profile updated.');
    } catch (err) {
      setError(err.message);
    } finally {
      setBusy(false);
    }
  }

  return (
    <>
      <div className="page-header">
        <div>
          <h2>Profile settings</h2>
          <p>Update your display name, bio or password.</p>
        </div>
      </div>

      <div className="two-col">
        <div className="card">
          <form className="stack" onSubmit={handleSubmit}>
            {error && <Alert kind="error">{error}</Alert>}
            {success && <Alert kind="success">{success}</Alert>}

            <div className="field">
              <label htmlFor="name">Display name</label>
              <input
                id="name"
                className="input"
                value={form.name}
                onChange={update('name')}
                maxLength={80}
                required
              />
            </div>

            <div className="field">
              <label htmlFor="bio">Bio</label>
              <textarea
                id="bio"
                className="textarea"
                value={form.bio}
                onChange={update('bio')}
                placeholder="A short description of your work"
                maxLength={500}
              />
              <span className="faint text-sm">{form.bio.length}/500</span>
            </div>

            <hr style={{ border: 'none', borderTop: '1px solid var(--border)', margin: '4px 0' }} />

            <div className="field">
              <label htmlFor="newPassword">New password</label>
              <input
                id="newPassword"
                type="password"
                className="input"
                value={form.newPassword}
                onChange={update('newPassword')}
                placeholder="Leave blank to keep your current password"
                autoComplete="new-password"
                minLength={8}
              />
            </div>

            {form.newPassword && (
              <div className="field">
                <label htmlFor="currentPassword">Current password</label>
                <input
                  id="currentPassword"
                  type="password"
                  className="input"
                  value={form.currentPassword}
                  onChange={update('currentPassword')}
                  placeholder="Confirm it is you"
                  autoComplete="current-password"
                />
              </div>
            )}

            <button type="submit" className="btn btn-primary" disabled={busy}>
              {busy ? <span className="spinner" /> : '✓'} Save changes
            </button>
          </form>
        </div>

        <div className="stack">
          <div className="card">
            <div className="row mb-2" style={{ gap: 14 }}>
              <div
                className="avatar"
                style={{
                  width: 52,
                  height: 52,
                  borderRadius: '50%',
                  display: 'grid',
                  placeItems: 'center',
                  color: '#fff',
                  fontWeight: 700,
                  fontSize: 18,
                  background: user?.avatarColor || 'var(--brand)',
                }}
              >
                {initials(user?.name)}
              </div>
              <div>
                <strong style={{ fontSize: 16 }}>{user?.name}</strong>
                <div className="muted text-sm">{user?.email}</div>
              </div>
            </div>

            <div className="stack" style={{ gap: 9 }}>
              <div className="row" style={{ justifyContent: 'space-between' }}>
                <span className="muted text-sm">Connected accounts</span>
                <strong>{user?.accountCount ?? 0}</strong>
              </div>
              <div className="row" style={{ justifyContent: 'space-between' }}>
                <span className="muted text-sm">Total posts</span>
                <strong>{user?.postCount ?? 0}</strong>
              </div>
              <div className="row" style={{ justifyContent: 'space-between' }}>
                <span className="muted text-sm">Member since</span>
                <strong>{formatDate(user?.createdAt)}</strong>
              </div>
            </div>
          </div>

          <div className="card">
            <div className="chart-title mb-2">Appearance</div>
            <div className="row" style={{ justifyContent: 'space-between' }}>
              <span className="muted text-sm">Theme</span>
              <button type="button" className="btn btn-secondary btn-sm" onClick={toggle}>
                {isDark ? '☀ Light mode' : '☾ Dark mode'}
              </button>
            </div>
          </div>
        </div>
      </div>
    </>
  );
}
