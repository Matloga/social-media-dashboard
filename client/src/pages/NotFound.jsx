import { Link } from 'react-router-dom';

export default function NotFound() {
  return (
    <div className="auth-screen">
      <div className="auth-card" style={{ textAlign: 'center' }}>
        <div className="auth-brand">
          <div className="logo">◈</div>
          <h1>404</h1>
        </div>
        <p className="auth-subtitle">That page does not exist.</p>
        <Link to="/dashboard" className="btn btn-primary btn-block">
          Back to dashboard
        </Link>
      </div>
    </div>
  );
}
