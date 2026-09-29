import { useState } from 'react';
import { Link } from 'react-router-dom';
import { useAsync } from '../hooks/useAsync';
import { insightsService, statsService } from '../services/apiServices';
import { PlatformFilter, RangeFilter } from '../components/Filters';
import StatCard, { StatCardSkeleton } from '../components/StatCard';
import InsightList from '../components/InsightList';
import { Alert, EmptyState, ErrorState, Loading } from '../components/States';
import {
  ChartSkeleton,
  CommentsChart,
  EngagementChart,
  EngagementRateChart,
  FollowersChart,
  PlatformSplitChart,
} from '../charts';
import { PLATFORM_COLORS, PLATFORM_LABELS, compactNumber, timeAgo } from '../utils/format';

const PRIMARY_CARDS = ['followers', 'likes', 'comments', 'engagement'];
const SECONDARY_CARDS = ['reach', 'impressions', 'shares', 'posts'];

export default function Dashboard() {
  const [range, setRange] = useState('30d');
  const [platform, setPlatform] = useState('all');

  const overview = useAsync(() => statsService.overview({ range, platform }), [range, platform]);
  const series = useAsync(() => statsService.timeSeries({ range, platform }), [range, platform]);
  const insights = useAsync(() => insightsService.list({ range, platform }), [range, platform]);

  const cards = overview.data?.cards || [];
  const primary = cards.filter((c) => PRIMARY_CARDS.includes(c.key));
  const secondary = cards.filter((c) => SECONDARY_CARDS.includes(c.key));
  const points = series.data?.points || [];
  const breakdown = overview.data?.byPlatform || [];

  if (overview.error) {
    return <ErrorState message={overview.error} onRetry={overview.reload} />;
  }

  const hasNoAccounts = overview.data && overview.data.byPlatform.length === 0;

  return (
    <>
      <div className="filter-bar">
        <RangeFilter value={range} onChange={setRange} />
        <div className="spacer" />
        <PlatformFilter value={platform} onChange={setPlatform} allLabel="All platforms" />
        <button
          type="button"
          className="btn btn-secondary btn-sm"
          onClick={() => {
            overview.reload();
            series.reload();
            insights.reload();
          }}
          disabled={overview.loading || series.loading}
        >
          Refresh
        </button>
      </div>

      {hasNoAccounts && (
        <Alert kind="info">
          No accounts connected yet. <Link to="/accounts">Connect an account</Link> to start seeing analytics.
        </Alert>
      )}

      {!hasNoAccounts && (
        <section className="mb-3">
          <div className="section-head">
            <h3 className="chart-title">Performance insights</h3>
            <span className="faint text-sm">Derived from your own history, no external model involved</span>
          </div>
          <InsightList insights={insights.data || []} loading={insights.loading && !insights.data} />
        </section>
      )}

      <div className="dashboard">
        {overview.loading && !overview.data
          ? Array.from({ length: 4 }, (_, i) => <StatCardSkeleton key={i} />)
          : primary.map((card) => <StatCard key={card.key} card={card} />)}
      </div>

      <div className="charts-grid">
        <div className="chart-card span-2">
          <div className="chart-head">
            <div>
              <div className="chart-title">Followers growth</div>
              <div className="chart-subtitle">
                {points.length ? `${points.length} day${points.length === 1 ? '' : 's'} of data` : 'Loading data...'}
              </div>
            </div>
          </div>
          <div className="chart-body">
            {series.loading && !series.data ? <ChartSkeleton /> : <FollowersChart points={points} />}
          </div>
        </div>

        <div className="chart-card">
          <div className="chart-title">Likes &amp; comments</div>
          <div className="chart-subtitle">Daily engagement volume</div>
          <div className="chart-body">
            {series.loading && !series.data ? <ChartSkeleton /> : <EngagementChart points={points} />}
          </div>
        </div>

        <div className="chart-card">
          <div className="chart-title">Comments trend</div>
          <div className="chart-subtitle">Conversations started per day</div>
          <div className="chart-body">
            {series.loading && !series.data ? <ChartSkeleton /> : <CommentsChart points={points} />}
          </div>
        </div>

        <div className="chart-card">
          <div className="chart-title">Engagement rate</div>
          <div className="chart-subtitle">Interactions as a share of impressions</div>
          <div className="chart-body">
            {series.loading && !series.data ? <ChartSkeleton /> : <EngagementRateChart points={points} />}
          </div>
        </div>

        <div className="chart-card">
          <div className="chart-title">Audience split</div>
          <div className="chart-subtitle">Followers by connected account</div>
          <div className="chart-body">
            {overview.loading && !overview.data ? <ChartSkeleton /> : <PlatformSplitChart breakdown={breakdown} />}
          </div>
        </div>
      </div>

      <div className="dashboard">
        {overview.loading && !overview.data
          ? Array.from({ length: 4 }, (_, i) => <StatCardSkeleton key={i} />)
          : secondary.map((card) => <StatCard key={card.key} card={card} />)}
      </div>

      <div className="two-col">
        <div className="card">
          <div className="chart-head mb-2">
            <div className="chart-title">Top performing posts</div>
            <Link to="/posts" className="text-sm">
              View all
            </Link>
          </div>

          {overview.loading && !overview.data ? (
            <Loading label="Loading posts..." />
          ) : (overview.data?.topPosts || []).length === 0 ? (
            <EmptyState icon="▤" title="No posts yet" description="Posts will appear once your accounts start syncing." />
          ) : (
            <div className="table-wrap">
              <table>
                <thead>
                  <tr>
                    <th>Post</th>
                    <th>Platform</th>
                    <th className="numeric">Likes</th>
                    <th className="numeric">Comments</th>
                    <th className="numeric">Reach</th>
                    <th className="numeric">Eng. rate</th>
                  </tr>
                </thead>
                <tbody>
                  {overview.data.topPosts.map((post) => (
                    <tr key={post.id}>
                      <td style={{ maxWidth: 260 }}>
                        <div style={{ overflow: 'hidden', textOverflow: 'ellipsis', whiteSpace: 'nowrap' }}>
                          {post.caption}
                        </div>
                        <div className="faint text-sm">{timeAgo(post.publishedAt)}</div>
                      </td>
                      <td>
                        <span className="platform-tag">
                          <span
                            className="platform-dot"
                            style={{ background: PLATFORM_COLORS[post.platform] || 'var(--brand)' }}
                          />
                          {PLATFORM_LABELS[post.platform] || post.platform}
                        </span>
                      </td>
                      <td className="numeric">{compactNumber(post.likes)}</td>
                      <td className="numeric">{compactNumber(post.comments)}</td>
                      <td className="numeric">{compactNumber(post.reach)}</td>
                      <td className="numeric">{post.engagementRate.toFixed(2)}%</td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          )}
        </div>

        <div className="card">
          <div className="chart-title mb-2">Performance by platform</div>
          {overview.loading && !overview.data ? (
            <Loading label="Loading breakdown..." />
          ) : breakdown.length === 0 ? (
            <EmptyState icon="◎" title="Nothing to compare" description="Connect more than one account to compare platforms." />
          ) : (
            <div className="stack" style={{ gap: 16 }}>
              {breakdown.map((row) => (
                <div key={row.platform}>
                  <div className="row" style={{ justifyContent: 'space-between', marginBottom: 6 }}>
                    <span className="platform-tag">
                      <span
                        className="platform-dot"
                        style={{ background: PLATFORM_COLORS[row.platform] || 'var(--brand)' }}
                      />
                      {PLATFORM_LABELS[row.platform] || row.platform}
                    </span>
                    <span className="mono text-sm">
                      {compactNumber(row.followers)} · {row.engagementRate.toFixed(2)}%
                    </span>
                  </div>
                  <div className="progress-track">
                    <div
                      className="progress-fill"
                      style={{
                        width: `${Math.min(100, row.shareOfFollowers)}%`,
                        background: PLATFORM_COLORS[row.platform] || 'var(--brand)',
                      }}
                    />
                  </div>
                  <div className="faint text-sm" style={{ marginTop: 4 }}>
                    {row.shareOfFollowers.toFixed(1)}% of audience · @{row.username}
                  </div>
                </div>
              ))}
            </div>
          )}
        </div>
      </div>
    </>
  );
}
