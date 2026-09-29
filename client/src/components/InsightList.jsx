import { Link } from 'react-router-dom';

const SEVERITY = {
  POSITIVE: { icon: '↑', className: 'insight-positive', label: 'Opportunity' },
  NEGATIVE: { icon: '↓', className: 'insight-negative', label: 'Declining' },
  WARNING: { icon: '⚠', className: 'insight-warning', label: 'Watch this' },
  NEUTRAL: { icon: '•', className: 'insight-neutral', label: 'For info' },
};

/**
 * Renders the insight cards returned by /api/insights. The copy is generated on
 * the server so the same wording is available to any other client; this
 * component only decides how each severity is presented.
 */
export default function InsightList({ insights = [], loading = false }) {
  if (loading) {
    return (
      <div className="insight-grid">
        {Array.from({ length: 3 }, (_, i) => (
          <div key={i} className="card insight-card">
            <div className="skeleton" style={{ height: 14, width: '55%', marginBottom: 10 }} />
            <div className="skeleton" style={{ height: 10, width: '100%', marginBottom: 6 }} />
            <div className="skeleton" style={{ height: 10, width: '75%' }} />
          </div>
        ))}
      </div>
    );
  }

  if (!insights.length) return null;

  return (
    <div className="insight-grid">
      {insights.map((insight, i) => {
        const meta = SEVERITY[insight.severity] || SEVERITY.NEUTRAL;
        const body = (
          <>
            <div className="insight-head">
              <span className={`insight-icon ${meta.className}`} aria-hidden="true">
                {meta.icon}
              </span>
              <div style={{ minWidth: 0 }}>
                <div className="insight-title">{insight.title}</div>
                <div className="insight-label">{meta.label}</div>
              </div>
            </div>
            <p className="insight-detail">{insight.detail}</p>
            {insight.link && (
              <span className="insight-link">
                Investigate <span aria-hidden="true">→</span>
              </span>
            )}
          </>
        );

        return insight.link ? (
          <Link key={`${insight.title}-${i}`} to={insight.link} className="card insight-card link">
            {body}
          </Link>
        ) : (
          <div key={`${insight.title}-${i}`} className="card insight-card">
            {body}
          </div>
        );
      })}
    </div>
  );
}
