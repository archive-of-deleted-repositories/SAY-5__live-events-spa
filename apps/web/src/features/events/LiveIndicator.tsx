import { useAppSelector } from '@/store/hooks';

const LABEL: Record<string, string> = {
  idle: 'idle',
  open: 'live',
  closed: 'closed',
  error: 'reconnecting',
};

const COLOR: Record<string, string> = {
  idle: 'bg-zinc-500',
  open: 'bg-emerald-400',
  closed: 'bg-zinc-500',
  error: 'bg-amber-400',
};

export function LiveIndicator() {
  const state = useAppSelector((s) => s.events.connection);
  return (
    <div data-testid="live-indicator" className="flex items-center gap-2 px-3 py-2 text-sm">
      <span
        className={`live-dot inline-block h-2.5 w-2.5 rounded-full ${COLOR[state] ?? 'bg-zinc-500'}`}
      />
      <span className="text-zinc-400">{LABEL[state] ?? state}</span>
    </div>
  );
}
