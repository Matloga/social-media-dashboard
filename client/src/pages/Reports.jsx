import { useState } from 'react';
import { useAsync } from '../hooks/useAsync';
import { RANGES, statsService } from '../services/apiServices';
import api from '../services/api';
import { PlatformFilter, RangeFilter } from '../components/Filters';
import { MetricList } from '../components/StatCard';
import { Alert, ErrorState, Loading } from '../components/States';
import { FollowersChart, EngagementChart } from '../charts';
import { PLATFORM_COLORS, PLATFORM_LABELS, compactNumber, formatDate, formatNumber, formatPercent } from '../utils/format';

export default function Reports() {
  const [range, setRange] = useState('30d');
  const [platform, setPlatform] = useState('all');
  const [exportError, setExportError] = useState('');

  const report = useAsync(() => statsService.report({ range, platform }), [range, platform]);
  const data = report.data;

  async function downloadCsv() {
    setExportError('');
    // The endpoint needs the Authorization header, so fetch it and hand the
    // blob to a temporary anchor rather than navigating to the URL directly.
    try {
      const response = await api.get('/reports/export.csv', {
        params: { range, platform: platform === 'all' ? '' : platform },
        responseType: 'blob',
      });
      const url = URL.createObjectURL(new Blob([response.data], { type: 'text/csv' }));
      const link = document.createElement('a');
      link.href = url;
      link.download = `social-report-${range}-${platform}.csv`;
      document.body.appendChild(link);
      link.click();
      link.remove();
      URL.revokeObjectURL(url);
    } catch (err) {
      setExportError(err.message);
    }
  }

  if (report.error) {
    return <ErrorState message={report.error} onRetry={report.reload} />;
  }

  return (
    <>
      <div className="page-header">
        <div>
          <h2>Analytics report</h2>
          <p>Exportable summary of reach, impressions, engagement and audience growth.</p>
        </div>
        <button type="button" className="btn btn-primary" onClick={downloadCsv} disabled={!data}>
          ⭳ Export CSV
        </button>
      </div>

      {exportError && <Alert kind="error">{exportError}</Alert>}

      <div className="filter-bar">
        <RangeFilter value={range} onChange={setRange} />
        <div className="spacer" />
        <PlatformFilter value={platform} onChange={setPlatform} allLabel="All platforms" />
        {data && (
          <span className="faint text-sm">
            {formatDate(data.from)} → {formatDate(data.to)}
          </span>
        )}
      </div>

      {report.loading && !data ? (
        <Loading label="Building report..." />
      ) : !data ? null : (
        <>
          <div className="dashboard">
            <div className="card stat-card">
              <div className="stat-head">
                <span className="stat-label">Total reach</span>
                <span className="stat-icon" style={{ background: 'var(--brand-soft)', color: 'var(--chart-1)' }}>
                  🎯
                </span>
              </div>
              <div className="stat-value">{formatNumber(data.totalReach)}</div>
            </div>

            <div className="card stat-card">
              <div className="stat-head">
                <span className="stat-label">Impressions</span>
                <span className="stat-icon" style={{ background: 'rgba(20,184,166,0.14)', color: 'var(--chart-3)' }}>
                  👁
                </span>
              </div>
              <div className="stat-value">{formatNumber(data.totalImpressions)}</div>
            </div>

            <div className="card stat-card">
              <div className="stat-head">
                <span className="stat-label">Follower growth</span>
                <span className="stat-icon" style={{ background: 'rgba(245,158,11,0.14)', color: 'var(--chart-4)' }}>
                  📈
                </span>
              </div>
              <div className="stat-value">
                {data.followerGrowth >= 0 ? '+' : ''}
                {formatNumber(data.followerGrowth)}
              </div>
              <div className={`stat-delta ${data.followerGrowth >= 0 ? 'up' : 'down'}`}>
                {data.totalFollowers > 0
                  ? formatPercent((data.followerGrowth / data.totalFollowers) * 100)
                  : '0%'}
                <span className="vs">of audience</span>
              </div>
            </div>

            <div className="card stat-card">
              <div className="stat-head">
                <span className="stat-label">Engagement rate</span>
                <span className="stat-icon" style={{ background: 'rgba(236,72,153,0.14)', color: 'var(--chart-2)' }}>
                  ❤️
                </span>
              </div>
              <div className="stat-value">{data.engagementRate.toFixed(2)}%</div>
            </div>
          </div>

          <div className="charts-grid">
            <div className="chart-card">
              <div className="chart-title">Follower growth</div>
              <div className="chart-subtitle">{data.rangeLabel}</div>
              <div className="chart-body">
                <FollowersChart points={data.trend} />
              </div>
            </div>
            <div className="chart-card">
              <div className="chart-title">Engagement volume</div>
              <div className="chart-subtitle">Likes and comments per day</div>
              <div className="chart-body">
                <EngagementChart points={data.trend} />
              </div>
            </div>
          </div>

          <div className="two-col">
            <div className="card">
              <div className="chart-title mb-2">Breakdown by platform</div>
              {data.byPlatform.length === 0 ? (
                <p className="muted">No data for this selection.</p>
              ) : (
                <div className="table-wrap">
                  <table>
                    <thead>
                      <tr>
                        <th>Platform</th>
                        <th>Handle</th>
                        <th className="numeric">Followers</th>
                        <th className="numeric">Likes</th>
                        <th className="numeric">Comments</th>
                        <th className="numeric">Eng. rate</th>
                        <th className="numeric">Share</th>
                      </tr>
                    </thead>
                    <tbody>
                      {data.byPlatform.map((row) => (
                        <tr key={row.platform}>
                          <td>
                            <span className="platform-tag">
                              <span
                                className="platform-dot"
                                style={{ background: PLATFORM_COLORS[row.platform] || 'var(--brand)' }}
                              />
                              {PLATFORM_LABELS[row.platform] || row.platform}
                            </span>
                          </td>
                          <td className="muted">@{row.username}</td>
                          <td className="numeric">{compactNumber(row.followers)}</td>
                          <td className="numeric">{compactNumber(row.likes)}</td>
                          <td className="numeric">{compactNumber(row.comments)}</td>
                          <td className="numeric">{row.engagementRate.toFixed(2)}%</td>
                          <td className="numeric">{row.shareOfFollowers.toFixed(1)}%</td>
                        </tr>
                      ))}
                    </tbody>
                  </table>
                </div>
              )}
            </div>

            <div className="card">
              <div className="chart-title mb-2">Totals</div>
              <MetricList
                items={[
                  { label: 'Total likes', value: data.totalLikes },
                  { label: 'Total comments', value: data.totalComments },
                  { label: 'Total shares', value: data.totalShares },
                  { label: 'Posts published', value: data.postsPublished },
                  { label: 'Audience', value: data.totalFollowers },
                  { label: 'Report ID', value: data.id },
                ]}
              />

              <div className="alert alert-info mt-3" style={{ fontSize: 12.5 }}>
                <span aria-hidden="true">ℹ</span>
                <span>
                  {RANGES.find((r) => r.value === range)?.label || range} · generated{' '}
                  {formatDate(data.generatedAt)}
                </span>
              </div>
            </div>
          </div>
        </>
      )}
    </>
  );
}
