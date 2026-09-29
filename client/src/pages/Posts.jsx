import { useState } from 'react';
import { useAsync } from '../hooks/useAsync';
import { PLATFORMS, POST_TYPES, postsService } from '../services/apiServices';
import { PlatformFilter } from '../components/Filters';
import { EmptyState, ErrorState, Loading } from '../components/States';
import {
  PLATFORM_COLORS,
  PLATFORM_LABELS,
  POST_TYPE_ICONS,
  compactNumber,
  formatDateTime,
} from '../utils/format';

const SORTS = [
  { value: 'publishedAt', label: 'Newest' },
  { value: 'engagement', label: 'Most engaged' },
  { value: 'likes', label: 'Most liked' },
  { value: 'comments', label: 'Most comments' },
  { value: 'reach', label: 'Most reach' },
  { value: 'impressions', label: 'Most impressions' },
];

const PAGE_SIZE = 12;

export default function Posts() {
  const [page, setPage] = useState(0);
  const [platform, setPlatform] = useState('all');
  const [type, setType] = useState('all');
  const [sort, setSort] = useState('publishedAt');
  const [direction, setDirection] = useState('desc');

  const posts = useAsync(
    () => postsService.list({ page, size: PAGE_SIZE, platform, type, sort, direction }),
    [page, platform, type, sort, direction]
  );

  const hashtags = useAsync(() => postsService.hashtags({ platform, limit: 12 }), [platform]);

  const data = posts.data;
  const items = data?.content || [];
  const totalPages = data?.totalPages || 0;
  const totalElements = data?.totalElements || 0;

  // Any filter change invalidates the current page number.
  function resetPage(setter) {
    return (value) => {
      setter(value);
      setPage(0);
    };
  }

  return (
    <>
      <div className="page-header">
        <div>
          <h2>Posts</h2>
          <p>
            {totalElements > 0
              ? `${totalElements.toLocaleString()} post${totalElements === 1 ? '' : 's'} across your connected accounts.`
              : 'Browse and filter every post across your connected accounts.'}
          </p>
        </div>
      </div>

      <div className="filter-bar">
        <div className="segmented" role="group" aria-label="Sort posts">
          {SORTS.map((option) => (
            <button
              key={option.value}
              type="button"
              className={sort === option.value ? 'active' : ''}
              onClick={() => resetPage(setSort)(option.value)}
              aria-pressed={sort === option.value}
            >
              {option.label}
            </button>
          ))}
        </div>

        <div className="spacer" />

        <select
          className="select"
          value={type}
          onChange={(e) => resetPage(setType)(e.target.value)}
          aria-label="Filter by post type"
          style={{ width: 'auto', minWidth: 130 }}
        >
          {POST_TYPES.map((option) => (
            <option key={option.value} value={option.value}>
              {option.label}
            </option>
          ))}
        </select>

        <PlatformFilter value={platform} onChange={resetPage(setPlatform)} />

        <button
          type="button"
          className="btn btn-secondary btn-sm"
          onClick={() => setDirection((d) => (d === 'desc' ? 'asc' : 'desc'))}
          title="Toggle sort direction"
        >
          {direction === 'desc' ? '↓ Desc' : '↑ Asc'}
        </button>
      </div>

      {posts.error ? (
        <ErrorState message={posts.error} onRetry={posts.reload} />
      ) : posts.loading && !posts.data ? (
        <Loading label="Loading posts..." />
      ) : items.length === 0 ? (
        <EmptyState
          icon="▤"
          title="No posts match these filters"
          description="Try a different platform, post type or sort order."
        />
      ) : (
        <>
          <div className="post-grid">
            {items.map((post) => (
              <article key={post.id} className="post-card">
                <div className="post-head">
                  <span className="post-type-icon" aria-hidden="true">
                    {POST_TYPE_ICONS[post.type] || '▤'}
                  </span>
                  <div style={{ minWidth: 0, flex: 1 }}>
                    <div className="platform-tag text-sm">
                      <span
                        className="platform-dot"
                        style={{ background: PLATFORM_COLORS[post.platform] || 'var(--brand)' }}
                      />
                      {PLATFORM_LABELS[post.platform] || post.platform}
                    </div>
                    <div className="faint text-sm">
                      @{post.username} · {formatDateTime(post.publishedAt)}
                    </div>
                  </div>
                  <span className="badge badge-brand">{post.type}</span>
                </div>

                <p className="post-caption">{post.caption}</p>

                {post.hashtags?.length > 0 && (
                  <div>
                    {post.hashtags.map((tag) => (
                      <span key={tag} className="tag">
                        #{tag}
                      </span>
                    ))}
                  </div>
                )}

                <div className="post-metrics">
                  <div className="post-metric">
                    <div className="n">{compactNumber(post.likes)}</div>
                    <div className="l">Likes</div>
                  </div>
                  <div className="post-metric">
                    <div className="n">{compactNumber(post.comments)}</div>
                    <div className="l">Comments</div>
                  </div>
                  <div className="post-metric">
                    <div className="n">{compactNumber(post.shares)}</div>
                    <div className="l">Shares</div>
                  </div>
                  <div className="post-metric">
                    <div className="n">{compactNumber(post.reach)}</div>
                    <div className="l">Reach</div>
                  </div>
                </div>

                <div className="row" style={{ justifyContent: 'space-between' }}>
                  <span className="faint text-sm">Engagement</span>
                  <span className="badge badge-success">{post.engagementRate.toFixed(2)}%</span>
                </div>
              </article>
            ))}
          </div>

          <div className="pagination">
            <button
              type="button"
              className="btn btn-secondary btn-sm"
              onClick={() => setPage((p) => Math.max(0, p - 1))}
              disabled={page === 0 || posts.loading}
            >
              ← Previous
            </button>
            <span className="info">
              Page {page + 1} of {Math.max(totalPages, 1)}
            </span>
            <button
              type="button"
              className="btn btn-secondary btn-sm"
              onClick={() => setPage((p) => p + 1)}
              disabled={page + 1 >= totalPages || posts.loading}
            >
              Next →
            </button>
          </div>
        </>
      )}

      {hashtags.data?.length > 0 && (
        <div className="card mt-3">
          <div className="chart-title mb-2">Top hashtags</div>
          <p className="muted text-sm mb-2">Ranked by total engagement across your posts.</p>
          <div className="table-wrap">
            <table>
              <thead>
                <tr>
                  <th>Hashtag</th>
                  <th className="numeric">Likes</th>
                  <th className="numeric">Comments</th>
                  <th className="numeric">Shares</th>
                  <th className="numeric">Total engagement</th>
                </tr>
              </thead>
              <tbody>
                {hashtags.data.map((tag) => (
                  <tr key={tag.name}>
                    <td>
                      <strong>{tag.tag}</strong>
                    </td>
                    <td className="numeric">{compactNumber(tag.likes)}</td>
                    <td className="numeric">{compactNumber(tag.comments)}</td>
                    <td className="numeric">{compactNumber(tag.shares)}</td>
                    <td className="numeric">
                      <strong>{compactNumber(tag.totalEngagement)}</strong>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        </div>
      )}

      <p className="faint text-sm mt-2">
        {PLATFORMS.length - 1} platforms supported. Filter by platform to focus the hashtag ranking.
      </p>
    </>
  );
}
