import { formatCardValue, formatNumber, formatPercent } from '../utils/format';

const ICONS = {
  followers: '👥',
  likes: '❤️',
  comments: '💬',
  engagement: '📈',
  posts: '📊',
  reach: '🎯',
  impressions: '👁',
  shares: '🔁',
};

const TINTS = {
  followers: ['rgba(99,102,241,0.14)', 'var(--chart-1)'],
  likes: ['rgba(236,72,153,0.14)', 'var(--chart-2)'],
  comments: ['rgba(20,184,166,0.14)', 'var(--chart-3)'],
  engagement: ['rgba(245,158,11,0.14)', 'var(--chart-4)'],
  posts: ['rgba(139,92,246,0.14)', 'var(--chart-5)'],
  reach: ['rgba(99,102,241,0.14)', 'var(--chart-1)'],
  impressions: ['rgba(20,184,166,0.14)', 'var(--chart-3)'],
  shares: ['rgba(236,72,153,0.14)', 'var(--chart-2)'],
};

const ARROWS = { up: '↑', down: '↓', flat: '→' };

export default function StatCard({ card }) {
  const [tint, accent] = TINTS[card.key] || TINTS.followers;
  const { delta } = card;

  return (
    <div className="card stat-card">
      <div className="stat-head">
        <span className="stat-label">{card.label}</span>
        <span className="stat-icon" style={{ background: tint, color: accent }} aria-hidden="true">
          {ICONS[card.key] || '◈'}
        </span>
      </div>

      <div className="stat-value">{formatCardValue(card.value, card.format)}</div>

      <div className={`stat-delta ${delta.direction}`}>
        <span aria-hidden="true">{ARROWS[delta.direction] || '→'}</span>
        <span>{formatPercent(delta.percent)}</span>
        <span className="vs">vs prev</span>
      </div>
    </div>
  );
}

export function StatCardSkeleton() {
  return (
    <div className="card stat-card">
      <div className="skeleton" style={{ height: 12, width: '55%', marginBottom: 14 }} />
      <div className="skeleton" style={{ height: 28, width: '70%', marginBottom: 10 }} />
      <div className="skeleton" style={{ height: 16, width: '40%' }} />
    </div>
  );
}

/** Compact key/value list, used on the report and account detail screens. */
export function MetricList({ items }) {
  return (
    <div className="stack" style={{ gap: 10 }}>
      {items.map((item) => (
        <div key={item.label} className="row" style={{ justifyContent: 'space-between', gap: 12 }}>
          <span className="muted text-sm">{item.label}</span>
          <strong className="mono" style={{ fontSize: 14 }}>
            {typeof item.value === 'number' ? formatNumber(item.value) : item.value}
          </strong>
        </div>
      ))}
    </div>
  );
}
