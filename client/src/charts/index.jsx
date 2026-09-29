import { useState } from 'react';
import {
  Area,
  AreaChart,
  Bar,
  BarChart,
  CartesianGrid,
  Cell,
  Legend,
  Line,
  LineChart,
  Pie,
  PieChart,
  ResponsiveContainer,
  Tooltip,
  XAxis,
  YAxis,
} from 'recharts';
import { compactNumber, formatNumber } from '../utils/format';

const AXIS = { fontSize: 11, fill: 'var(--text-faint)' };
const MARGIN = { top: 8, right: 12, bottom: 4, left: -8 };

/** Shortens the ISO date to whatever granularity the range needs. */
function formatTick(date, span) {
  if (span > 300) return date.slice(0, 7); // YYYY-MM
  if (span > 60) return date.slice(5); // MM-DD
  return date.slice(5); // MM-DD
}

function ChartTooltip({ active, payload, label, unit = '' }) {
  if (!active || !payload?.length) return null;
  return (
    <div className="tooltip">
      <div className="tooltip-date">{label}</div>
      {payload.map((entry) => (
        <div key={entry.dataKey} className="tooltip-row">
          <span className="row" style={{ gap: 6 }}>
            <span className="legend-swatch" style={{ background: entry.color || entry.stroke || entry.fill }} />
            {entry.name}
          </span>
          <span className="val">
            {entry.dataKey === 'engagementRate'
              ? `${entry.value.toFixed(2)}%`
              : `${formatNumber(entry.value)}${unit}`}
          </span>
        </div>
      ))}
    </div>
  );
}

/** Keeps the tick count readable no matter how many points the range returns. */
function tickInterval(count) {
  if (count <= 14) return 0;
  if (count <= 40) return Math.ceil(count / 8) - 1;
  return Math.ceil(count / 10) - 1;
}

const SERIES = [
  { key: 'followers', label: 'Followers', color: 'var(--chart-1)' },
  { key: 'likes', label: 'Likes', color: 'var(--chart-2)' },
  { key: 'comments', label: 'Comments', color: 'var(--chart-3)' },
  { key: 'engagementRate', label: 'Engagement rate', color: 'var(--chart-4)' },
];

function useToggle(initial) {
  const [hidden, setHidden] = useState(() => new Set(initial));
  const toggle = (key) =>
    setHidden((prev) => {
      const next = new Set(prev);
      if (next.has(key)) next.delete(key);
      else next.add(key);
      return next;
    });
  return [hidden, toggle];
}

/** Toggling legend used by the likes/comments chart. */
function SeriesLegend({ hidden, onToggle }) {
  return (
    <div className="chart-legend">
      {SERIES.filter((s) => s.key !== 'engagementRate').map((s) => (
        <span
          key={s.key}
          className={`legend-item${hidden.has(s.key) ? ' off' : ''}`}
          onClick={() => onToggle(s.key)}
          role="button"
          tabIndex={0}
          onKeyDown={(e) => {
            if (e.key === 'Enter' || e.key === ' ') {
              e.preventDefault();
              onToggle(s.key);
            }
          }}
        >
          <span className="legend-swatch" style={{ background: s.color }} />
          {s.label}
        </span>
      ))}
    </div>
  );
}

export function FollowersChart({ points = [] }) {
  if (!points.length) return null;
  const data = points.map((p) => ({ date: p.date, followers: p.followers }));
  const id = 'grad-followers';

  return (
    <ResponsiveContainer width="100%" height="100%">
      <AreaChart data={data} margin={MARGIN}>
        <defs>
          <linearGradient id={id} x1="0" y1="0" x2="0" y2="1">
            <stop offset="0%" stopColor="var(--chart-1)" stopOpacity={0.32} />
            <stop offset="100%" stopColor="var(--chart-1)" stopOpacity={0.02} />
          </linearGradient>
        </defs>
        <CartesianGrid strokeDasharray="3 3" stroke="var(--grid)" vertical={false} />
        <XAxis
          dataKey="date"
          tickFormatter={(d) => formatTick(d, points.length)}
          tick={AXIS}
          tickLine={false}
          axisLine={false}
          interval={tickInterval(points.length)}
          minTickGap={12}
        />
        <YAxis tick={AXIS} tickLine={false} axisLine={false} tickFormatter={compactNumber} width={52} />
        <Tooltip content={<ChartTooltip />} />
        <Area
          type="monotone"
          dataKey="followers"
          name="Followers"
          stroke="var(--chart-1)"
          strokeWidth={2.5}
          fill={`url(#${id})`}
          dot={false}
          activeDot={{ r: 4 }}
        />
      </AreaChart>
    </ResponsiveContainer>
  );
}

export function EngagementChart({ points = [] }) {
  const [hidden, toggle] = useToggle([]);
  if (!points.length) return null;
  const visible = SERIES.filter((s) => s.key !== 'followers' && !hidden.has(s.key));
  const data = points.map((p) => ({ date: p.date, likes: p.likes, comments: p.comments }));

  return (
    <>
      <SeriesLegend hidden={hidden} onToggle={toggle} />
      <div style={{ height: 'calc(100% - 26px)' }}>
        <ResponsiveContainer width="100%" height="100%">
          <BarChart data={data} margin={MARGIN}>
            <CartesianGrid strokeDasharray="3 3" stroke="var(--grid)" vertical={false} />
            <XAxis
              dataKey="date"
              tickFormatter={(d) => formatTick(d, points.length)}
              tick={AXIS}
              tickLine={false}
              axisLine={false}
              interval={tickInterval(points.length)}
              minTickGap={12}
            />
            <YAxis tick={AXIS} tickLine={false} axisLine={false} tickFormatter={compactNumber} width={52} />
            <Tooltip content={<ChartTooltip />} />
            {visible.map((s) => (
              <Bar key={s.key} dataKey={s.key} name={s.label} fill={s.color} radius={[3, 3, 0, 0]} maxBarSize={22} />
            ))}
          </BarChart>
        </ResponsiveContainer>
      </div>
    </>
  );
}

export function CommentsChart({ points = [] }) {
  if (!points.length) return null;
  const data = points.map((p) => ({ date: p.date, comments: p.comments }));

  return (
    <ResponsiveContainer width="100%" height="100%">
      <LineChart data={data} margin={MARGIN}>
        <CartesianGrid strokeDasharray="3 3" stroke="var(--grid)" vertical={false} />
        <XAxis
          dataKey="date"
          tickFormatter={(d) => formatTick(d, points.length)}
          tick={AXIS}
          tickLine={false}
          axisLine={false}
          interval={tickInterval(points.length)}
          minTickGap={12}
        />
        <YAxis tick={AXIS} tickLine={false} axisLine={false} tickFormatter={compactNumber} width={52} />
        <Tooltip content={<ChartTooltip />} />
        <Line
          type="monotone"
          dataKey="comments"
          name="Comments"
          stroke="var(--chart-3)"
          strokeWidth={2.5}
          dot={false}
          activeDot={{ r: 4 }}
        />
      </LineChart>
    </ResponsiveContainer>
  );
}

export function EngagementRateChart({ points = [] }) {
  if (!points.length) return null;
  const id = 'grad-engagement';
  const data = points.map((p) => ({ date: p.date, engagementRate: p.engagementRate }));

  return (
    <ResponsiveContainer width="100%" height="100%">
      <AreaChart data={data} margin={MARGIN}>
        <defs>
          <linearGradient id={id} x1="0" y1="0" x2="0" y2="1">
            <stop offset="0%" stopColor="var(--chart-4)" stopOpacity={0.32} />
            <stop offset="100%" stopColor="var(--chart-4)" stopOpacity={0.02} />
          </linearGradient>
        </defs>
        <CartesianGrid strokeDasharray="3 3" stroke="var(--grid)" vertical={false} />
        <XAxis
          dataKey="date"
          tickFormatter={(d) => formatTick(d, points.length)}
          tick={AXIS}
          tickLine={false}
          axisLine={false}
          interval={tickInterval(points.length)}
          minTickGap={12}
        />
        <YAxis
          tick={AXIS}
          tickLine={false}
          axisLine={false}
          width={46}
          tickFormatter={(v) => `${v.toFixed(1)}%`}
          domain={['auto', 'auto']}
        />
        <Tooltip content={<ChartTooltip />} />
        <Area
          type="monotone"
          dataKey="engagementRate"
          name="Engagement rate"
          stroke="var(--chart-4)"
          strokeWidth={2.5}
          fill={`url(#${id})`}
          dot={false}
          activeDot={{ r: 4 }}
        />
      </AreaChart>
    </ResponsiveContainer>
  );
}

/** Audience share per connected account. */
export function PlatformSplitChart({ breakdown = [] }) {
  if (!breakdown.length) return null;
  const data = breakdown.map((b) => ({
    name: b.platform.charAt(0) + b.platform.slice(1).toLowerCase(),
    value: b.followers,
    color: `var(--chart-${(breakdown.indexOf(b) % 5) + 1})`,
  }));

  return (
    <ResponsiveContainer width="100%" height="100%">
      <PieChart>
        <Pie
          data={data}
          dataKey="value"
          nameKey="name"
          innerRadius="55%"
          outerRadius="80%"
          paddingAngle={2}
          stroke="var(--surface)"
          strokeWidth={2}
        >
          {data.map((entry) => (
            <Cell key={entry.name} fill={entry.color} />
          ))}
        </Pie>
        <Tooltip
          content={({ active, payload }) => {
            if (!active || !payload?.length) return null;
            const total = data.reduce((sum, d) => sum + d.value, 0);
            const share = total ? (payload[0].value / total) * 100 : 0;
            return (
              <div className="tooltip">
                <div className="tooltip-date">{payload[0].name}</div>
                <div className="tooltip-row">
                  <span>Followers</span>
                  <span className="val">{formatNumber(payload[0].value)}</span>
                </div>
                <div className="tooltip-row">
                  <span>Share</span>
                  <span className="val">{share.toFixed(1)}%</span>
                </div>
              </div>
            );
          }}
        />
        <Legend
          verticalAlign="bottom"
          height={28}
          iconType="circle"
          wrapperStyle={{ fontSize: 12, color: 'var(--text-muted)' }}
        />
      </PieChart>
    </ResponsiveContainer>
  );
}

export function ChartSkeleton() {
  return <div className="skeleton" style={{ height: '100%', width: '100%' }} />;
}
