import { Virtuoso } from 'react-virtuoso';
import { useAppSelector } from '@/store/hooks';
import { filterEvents } from './eventsSlice';
import { EventRow } from './EventRow';

export function EventList() {
  const items = useAppSelector((s) => s.events.items);
  const filters = useAppSelector((s) => s.events.filters);
  const filtered = filterEvents(items, filters);

  if (filtered.length === 0) {
    return (
      <div
        data-testid="empty-state"
        className="flex flex-1 items-center justify-center p-6 text-sm text-muted"
      >
        No events match the current filters.
      </div>
    );
  }

  return (
    <Virtuoso
      data-testid="event-list"
      className="flex-1"
      data={filtered}
      itemContent={(_, ev) => <EventRow key={ev.id} event={ev} />}
    />
  );
}
