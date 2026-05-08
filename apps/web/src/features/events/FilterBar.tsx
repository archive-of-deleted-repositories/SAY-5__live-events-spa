import { useAppDispatch, useAppSelector } from '@/store/hooks';
import { filterChanged, filtersReset } from './eventsSlice';
import { buildCsvExportUrl } from './eventsApi';
import type { EventType, Severity } from '@/types';

const TYPES: EventType[] = ['order', 'payment', 'shipment', 'alert'];
const SEVERITIES: Severity[] = ['info', 'warning', 'critical'];

export function FilterBar() {
  const dispatch = useAppDispatch();
  const filters = useAppSelector((s) => s.events.filters);

  return (
    <div
      data-testid="filter-bar"
      className="flex flex-wrap items-center gap-2 border-b border-zinc-800 bg-surface p-3"
    >
      <select
        aria-label="Type"
        value={filters.type}
        onChange={(e) => dispatch(filterChanged({ type: e.target.value as EventType | '' }))}
        className="rounded border border-zinc-700 bg-zinc-900 px-2 py-1 text-sm"
      >
        <option value="">All types</option>
        {TYPES.map((t) => (
          <option key={t} value={t}>
            {t}
          </option>
        ))}
      </select>

      <select
        aria-label="Severity"
        value={filters.severity}
        onChange={(e) => dispatch(filterChanged({ severity: e.target.value as Severity | '' }))}
        className="rounded border border-zinc-700 bg-zinc-900 px-2 py-1 text-sm"
      >
        <option value="">All severities</option>
        {SEVERITIES.map((s) => (
          <option key={s} value={s}>
            {s}
          </option>
        ))}
      </select>

      <input
        aria-label="Search"
        type="search"
        placeholder="Search payload, source..."
        value={filters.search}
        onChange={(e) => dispatch(filterChanged({ search: e.target.value }))}
        className="flex-1 min-w-[160px] rounded border border-zinc-700 bg-zinc-900 px-2 py-1 text-sm"
      />

      <input
        aria-label="From"
        type="datetime-local"
        value={filters.from}
        onChange={(e) => dispatch(filterChanged({ from: e.target.value }))}
        className="rounded border border-zinc-700 bg-zinc-900 px-2 py-1 text-sm"
      />
      <input
        aria-label="To"
        type="datetime-local"
        value={filters.to}
        onChange={(e) => dispatch(filterChanged({ to: e.target.value }))}
        className="rounded border border-zinc-700 bg-zinc-900 px-2 py-1 text-sm"
      />

      <button
        type="button"
        onClick={() => dispatch(filtersReset())}
        className="rounded border border-zinc-700 px-3 py-1 text-sm text-zinc-300 hover:bg-zinc-800"
      >
        Reset
      </button>

      <a
        data-testid="export-csv"
        href={buildCsvExportUrl(filters)}
        className="rounded bg-accent px-3 py-1 text-sm font-medium text-zinc-900 hover:bg-blue-300"
        download
      >
        Export CSV
      </a>
    </div>
  );
}
