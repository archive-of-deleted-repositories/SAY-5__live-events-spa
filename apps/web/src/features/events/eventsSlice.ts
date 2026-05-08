import { createSlice, PayloadAction } from '@reduxjs/toolkit';
import { EMPTY_FILTERS, Filters, LiveEvent } from '@/types';

export interface EventsState {
  /** Newest events first. Capped at MAX_EVENTS (FIFO eviction). */
  items: LiveEvent[];
  filters: Filters;
  /** Connection state for the SSE stream. */
  connection: 'idle' | 'open' | 'closed' | 'error';
}

export const MAX_EVENTS = 1000;

const initialState: EventsState = {
  items: [],
  filters: { ...EMPTY_FILTERS },
  connection: 'idle',
};

const eventsSlice = createSlice({
  name: 'events',
  initialState,
  reducers: {
    received(state, action: PayloadAction<LiveEvent>) {
      const incoming = action.payload;
      // Skip duplicates (e.g. on SSE reconnect that replays a few events).
      if (state.items.some((e) => e.id === incoming.id)) {
        return;
      }
      state.items.unshift(incoming);
      if (state.items.length > MAX_EVENTS) {
        state.items.length = MAX_EVENTS;
      }
    },
    receivedBatch(state, action: PayloadAction<LiveEvent[]>) {
      const seen = new Set(state.items.map((e) => e.id));
      const fresh = action.payload.filter((e) => !seen.has(e.id));
      // Newest first; merge then sort to keep ordering stable.
      state.items = [...fresh, ...state.items]
        .sort((a, b) => (a.timestamp < b.timestamp ? 1 : -1))
        .slice(0, MAX_EVENTS);
    },
    cleared(state) {
      state.items = [];
    },
    filterChanged(state, action: PayloadAction<Partial<Filters>>) {
      state.filters = { ...state.filters, ...action.payload };
    },
    filtersReset(state) {
      state.filters = { ...EMPTY_FILTERS };
    },
    connectionChanged(state, action: PayloadAction<EventsState['connection']>) {
      state.connection = action.payload;
    },
  },
});

export const { received, receivedBatch, cleared, filterChanged, filtersReset, connectionChanged } =
  eventsSlice.actions;
export const eventsReducer = eventsSlice.reducer;

/** Pure filter applied client-side over the in-memory ring buffer. */
export function filterEvents(items: LiveEvent[], filters: Filters): LiveEvent[] {
  const search = filters.search.trim().toLowerCase();
  const from = filters.from ? Date.parse(filters.from) : Number.NEGATIVE_INFINITY;
  const to = filters.to ? Date.parse(filters.to) : Number.POSITIVE_INFINITY;
  return items.filter((e) => {
    if (filters.type && e.type !== filters.type) return false;
    if (filters.severity && e.severity !== filters.severity) return false;
    const ts = Date.parse(e.timestamp);
    if (Number.isFinite(from) && ts < from) return false;
    if (Number.isFinite(to) && ts > to) return false;
    if (search) {
      const haystack =
        `${e.source} ${e.type} ${e.severity} ${JSON.stringify(e.payload)}`.toLowerCase();
      if (!haystack.includes(search)) return false;
    }
    return true;
  });
}
