import { useState } from 'react';
import { useAsync } from '../hooks/useAsync';
import { GOAL_METRICS, GOAL_PERIODS, PLATFORMS, goalsService } from '../services/apiServices';
import { PlatformFilter } from '../components/Filters';
import { Alert, EmptyState, ErrorState, Loading } from '../components/States';
import { compactNumber, formatDate, formatNumber, timeAgo } from '../utils/format';

const STATUS_META = {
  ACHIEVED: { label: 'Achieved', kind: 'success', icon: '✓' },
  ON_TRACK: { label: 'On track', kind: 'brand', icon: '→' },
  BEHIND: { label: 'Behind pace', kind: 'warning', icon: '◐' },
  AT_RISK: { label: 'At risk', kind: 'danger', icon: '⚠' },
  EXPIRED: { label: 'Expired', kind: 'neutral', icon: '✕' },
};

function defaultEndDate(period) {
  const days = GOAL_PERIODS.find((p) => p.value === period)?.days || 30;
  const d = new Date();
  // Both endpoints are inclusive server-side, so back off one day to keep a
  // "monthly" goal at exactly 30 days.
  d.setDate(d.getDate() + days - 1);
  return d.toISOString().slice(0, 10);
}

export default function Goals() {
  const [showForm, setShowForm] = useState(false);
  const [error, setError] = useState('');
  const [busy, setBusy] = useState(false);
  const [form, setForm] = useState({
    metric: 'FOLLOWERS',
    platform: 'all',
    title: '',
    targetValue: '',
    period: 'MONTHLY',
  });

  const goals = useAsync(() => goalsService.list(), []);
  const items = goals.data || [];

  const metricLabel = (key) => GOAL_METRICS.find((m) => m.value === key)?.label || key;
  const isRate = (key) => key === 'ENGAGEMENT_RATE';
  const set = (key) => (e) => setForm((f) => ({ ...f, [key]: e.target.value }));

  async function createGoal(event) {
    event.preventDefault();
    setError('');
    setBusy(true);
    try {
      await goalsService.create({
        metric: form.metric,
        platform: form.platform === 'all' ? '' : form.platform,
        title: form.title.trim(),
        targetValue: Number(form.targetValue),
        period: form.period,
        endDate: defaultEndDate(form.period),
      });
      setForm({ metric: 'FOLLOWERS', platform: 'all', title: '', targetValue: '', period: 'MONTHLY' });
      setShowForm(false);
      goals.reload();
    } catch (err) {
      setError(err.message);
    } finally {
      setBusy(false);
    }
  }

  async function remove(id) {
    await goalsService.remove(id);
    goals.reload();
  }

  async function toggleActive(goal) {
    await goalsService.update(goal.id, { active: !goal.active });
    goals.reload();
  }

  return (
    <>
      <div className="page-header">
        <div>
          <h2>Goals</h2>
          <p>Set a target and track how you are tracking against the calendar.</p>
        </div>
        <button type="button" className="btn btn-primary" onClick={() => setShowForm((s) => !s)}>
          {showForm ? '✕ Cancel' : '+ New goal'}
        </button>
      </div>

      {showForm && (
        <div className="card mb-3">
          <form className="stack" onSubmit={createGoal}>
            {error && <Alert kind="error">{error}</Alert>}

            <div className="filter-bar" style={{ marginBottom: 0, border: 'none', padding: 0 }}>
              <div className="field" style={{ marginBottom: 0 }}>
                <label htmlFor="metric">Metric</label>
                <select id="metric" className="select" value={form.metric} onChange={set('metric')}>
                  {GOAL_METRICS.map((m) => (
                    <option key={m.value} value={m.value}>
                      {m.label}
                    </option>
                  ))}
                </select>
              </div>

              <div className="field" style={{ marginBottom: 0 }}>
                <label htmlFor="period">Period</label>
                <select id="period" className="select" value={form.period} onChange={set('period')}>
                  {GOAL_PERIODS.map((p) => (
                    <option key={p.value} value={p.value}>
                      {p.label}
                    </option>
                  ))}
                </select>
              </div>

              <div className="field" style={{ marginBottom: 0 }}>
                <label htmlFor="platform">Platform</label>
                <select id="platform" className="select" value={form.platform} onChange={set('platform')}>
                  {PLATFORMS.map((p) => (
                    <option key={p.value} value={p.value}>
                      {p.label}
                    </option>
                  ))}
                </select>
              </div>

              <div className="field" style={{ marginBottom: 0, maxWidth: 170 }}>
                <label htmlFor="target">
                  Target{isRate(form.metric) ? ' (%)' : ''}
                </label>
                <input
                  id="target"
                  className="input"
                  type="number"
                  min="0.1"
                  step="any"
                  required
                  value={form.targetValue}
                  onChange={set('targetValue')}
                  placeholder="2000"
                />
              </div>
            </div>

            <div className="field">
              <label htmlFor="title">Title (optional)</label>
              <input
                id="title"
                className="input"
                value={form.title}
                onChange={set('title')}
                maxLength={200}
                placeholder="Leave blank to name it automatically"
              />
            </div>

            <button type="submit" className="btn btn-primary" disabled={busy}>
              {busy ? <span className="spinner" /> : '✓'} Create goal
            </button>
          </form>
        </div>
      )}

      {goals.error ? (
        <ErrorState message={goals.error} onRetry={goals.reload} />
      ) : goals.loading && !goals.data ? (
        <Loading label="Loading goals..." />
      ) : items.length === 0 ? (
        <EmptyState
          icon="◎"
          title="No goals yet"
          description="Set a follower, engagement or publishing target and this page will track your pace for you."
          action={
            <button type="button" className="btn btn-primary" onClick={() => setShowForm(true)}>
              + Create your first goal
            </button>
          }
        />
      ) : (
        <div className="goal-grid">
          {items.map((goal) => {
            const meta = STATUS_META[goal.status] || STATUS_META.ON_TRACK;
            const width = Math.min(100, Math.max(0, goal.progress));
            const paceWidth = Math.min(100, Math.max(0, goal.timeProgress));
            const unit = isRate(goal.metric) ? '%' : '';
            const value = (n) => (isRate(goal.metric) ? `${Number(n).toFixed(2)}${unit}` : formatNumber(n));

            return (
              <article key={goal.id} className={`card goal-card${goal.active ? '' : ' inactive'}`}>
                <div className="row" style={{ justifyContent: 'space-between', alignItems: 'flex-start' }}>
                  <div style={{ minWidth: 0 }}>
                    <h3 className="goal-title">{goal.title}</h3>
                    <div className="faint text-sm">
                      {metricLabel(goal.metric)}
                      {goal.platform ? ` · ${goal.platform}` : ' · all platforms'} ·{' '}
                      {goal.period.toLowerCase()}
                    </div>
                  </div>
                  <span className={`badge badge-${meta.kind}`}>
                    {meta.icon} {meta.label}
                  </span>
                </div>

                <div className="goal-numbers">
                  <div>
                    <span className="goal-achieved">{value(goal.achieved)}</span>
                    <span className="goal-of"> of {value(goal.targetValue)}</span>
                  </div>
                  <span className={`goal-pct ${goal.progress >= 100 ? 'done' : ''}`}>
                    {Math.round(goal.progress)}%
                  </span>
                </div>

                <div
                  className="progress"
                  role="progressbar"
                  aria-valuenow={Math.round(goal.progress)}
                  aria-valuemin={0}
                  aria-valuemax={100}
                  aria-label={`${goal.title} progress`}
                >
                  <div
                    className={`progress-bar ${goal.status === 'BEHIND' || goal.status === 'AT_RISK' || goal.status === 'EXPIRED' ? 'danger' : ''}`}
                    style={{ width: `${width}%` }}
                  />
                  <div className="progress-pace" style={{ left: `${paceWidth}%` }} title="Where you should be by now" />
                </div>

                <div className="goal-foot">
                  <span className="faint text-sm">
                    Day {goal.daysElapsed} of {goal.daysTotal} · ends {formatDate(goal.endDate)}
                  </span>
                  <span
                    className={`text-sm ${goal.paceDelta >= 0 ? 'stat-delta up' : 'stat-delta down'}`}
                    title="Progress compared with how much of the period has elapsed"
                  >
                    {goal.paceDelta >= 0 ? '+' : ''}
                    {Math.round(goal.paceDelta)} pts vs pace
                  </span>
                </div>

                <div className="row mt-2" style={{ justifyContent: 'space-between' }}>
                  <span className="faint text-sm" title={new Date(goal.createdAt).toISOString()}>
                    created {timeAgo(goal.createdAt)}
                  </span>
                  <span className="row" style={{ gap: 6 }}>
                    <button type="button" className="btn btn-ghost btn-sm" onClick={() => toggleActive(goal)}>
                      {goal.active ? 'Pause' : 'Resume'}
                    </button>
                    <button
                      type="button"
                      className="btn btn-ghost btn-sm danger"
                      onClick={() => remove(goal.id)}
                      aria-label={`Delete ${goal.title}`}
                    >
                      Delete
                    </button>
                  </span>
                </div>
              </article>
            );
          })}
        </div>
      )}

      <div className="card mt-3">
        <div className="chart-title mb-2">How progress is measured</div>
        <ul className="muted text-sm" style={{ margin: 0, paddingLeft: 18, lineHeight: 1.8 }}>
          <li>
            <strong>Followers</strong> is treated as growth, so the baseline is your audience on the start date and
            the target is new followers gained.
          </li>
          <li>
            <strong>Likes, comments, reach and posts</strong> are treated as totals, so progress runs from zero
            across the period.
          </li>
          <li>
            The marker on each bar shows where a steady pace would have put you today. Being ahead of it means the
            target is still reachable.
          </li>
          <li>Progress is recalculated from your analytics on every load, so it survives a re-sync.</li>
        </ul>
        <div className="faint text-sm mt-2">
          Combined reach right now: {compactNumber(items.reduce((sum, g) => sum + g.currentValue, 0))}
        </div>
      </div>
    </>
  );
}
