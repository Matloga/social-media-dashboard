import { PLATFORMS, RANGES } from '../services/apiServices';

export function RangeFilter({ value, onChange }) {
  return (
    <div className="segmented" role="group" aria-label="Date range">
      {RANGES.map((range) => (
        <button
          key={range.value}
          type="button"
          className={value === range.value ? 'active' : ''}
          onClick={() => onChange(range.value)}
          aria-pressed={value === range.value}
        >
          {range.label}
        </button>
      ))}
    </div>
  );
}

export function PlatformFilter({ value, onChange, allLabel = 'All' }) {
  return (
    <select
      className="select"
      value={value}
      onChange={(e) => onChange(e.target.value)}
      aria-label="Filter by platform"
      style={{ width: 'auto', minWidth: 150 }}
    >
      {PLATFORMS.map((platform) => (
        <option key={platform.value} value={platform.value}>
          {platform.value === 'all' ? allLabel : platform.label}
        </option>
      ))}
    </select>
  );
}
