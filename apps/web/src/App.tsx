import { FilterBar } from '@/features/events/FilterBar';
import { EventList } from '@/features/events/EventList';
import { LiveIndicator } from '@/features/events/LiveIndicator';
import { useSseStream } from '@/features/events/useSseStream';

export function App() {
  useSseStream();

  return (
    <div className="flex h-full flex-col">
      <header className="flex items-center justify-between border-b border-zinc-800 bg-surface px-4 py-3">
        <h1 className="text-base font-semibold tracking-tight">Live Events</h1>
        <LiveIndicator />
      </header>
      <FilterBar />
      <main className="flex flex-1 flex-col overflow-hidden">
        <EventList />
      </main>
    </div>
  );
}
