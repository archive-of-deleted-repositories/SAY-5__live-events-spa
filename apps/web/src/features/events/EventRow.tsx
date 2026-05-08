import type { LiveEvent } from '@/types';

const SEVERITY_COLOR: Record<string, string> = {
  info: 'text-zinc-300',
  warning: 'text-amber-300',
  critical: 'text-red-400',
};

export function EventRow({ event }: { event: LiveEvent }) {
  const ts = new Date(event.timestamp).toISOString().replace('T', ' ').slice(0, 19);
  return (
    <div
      data-testid="event-row"
      className="grid grid-cols-1 gap-1 border-b border-zinc-800 px-3 py-2 text-sm sm:grid-cols-[160px_80px_80px_140px_1fr]"
    >
      <span className="font-mono text-zinc-400">{ts}</span>
      <span className="font-medium">{event.type}</span>
      <span className={`font-medium ${SEVERITY_COLOR[event.severity] ?? 'text-zinc-300'}`}>
        {event.severity}
      </span>
      <span className="text-zinc-400">{event.source}</span>
      <span className="truncate font-mono text-xs text-zinc-500">
        {JSON.stringify(event.payload)}
      </span>
    </div>
  );
}
