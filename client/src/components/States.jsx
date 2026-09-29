export function Loading({ label = 'Loading...' }) {
  return (
    <div className="row" style={{ justifyContent: 'center', padding: '40px 0', gap: 10 }}>
      <div className="spinner dark" />
      <span className="muted">{label}</span>
    </div>
  );
}

export function ErrorState({ message, onRetry }) {
  return (
    <div className="empty-state">
      <div className="icon" aria-hidden="true">
        ⚠
      </div>
      <h3>Could not load this data</h3>
      <p>{message}</p>
      {onRetry && (
        <button type="button" className="btn btn-secondary mt-2" onClick={onRetry}>
          Try again
        </button>
      )}
    </div>
  );
}

export function EmptyState({ icon = '◌', title, description, action }) {
  return (
    <div className="empty-state">
      <div className="icon" aria-hidden="true">
        {icon}
      </div>
      <h3>{title}</h3>
      {description && <p>{description}</p>}
      {action && <div className="mt-2">{action}</div>}
    </div>
  );
}

export function Alert({ kind = 'info', children }) {
  if (!children) return null;
  return (
    <div className={`alert alert-${kind}`} role={kind === 'error' ? 'alert' : 'status'}>
      <span aria-hidden="true">{kind === 'error' ? '⚠' : kind === 'success' ? '✓' : 'ℹ'}</span>
      <span>{children}</span>
    </div>
  );
}

export function Badge({ kind = 'neutral', children }) {
  return <span className={`badge badge-${kind}`}>{children}</span>;
}
