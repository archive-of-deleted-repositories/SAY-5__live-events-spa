import { describe, it, expect } from 'vitest';
import {
  cleared,
  connectionChanged,
  eventsReducer,
  filterChanged,
  filterEvents,
  filtersReset,
  MAX_EVENTS,
  received,
  receivedBatch,
} from '@/features/events/eventsSlice';
import type { LiveEvent } from '@/types';

function ev(over: Partial<LiveEvent> = {}): LiveEvent {
  return {
    id: over.id ?? crypto.randomUUID(),
    timestamp: over.timestamp ?? '2026-01-01T00:00:00Z',
    type: over.type ?? 'order',
    severity: over.severity ?? 'info',
    source: over.source ?? 'test',
    payload: over.payload ?? { ok: true },
  };
}

describe('eventsSlice', () => {
  it('starts empty with default filters', () => {
    const s = eventsReducer(undefined, { type: 'init' });
    expect(s.items).toEqual([]);
    expect(s.connection).toBe('idle');
    expect(s.filters.type).toBe('');
  });

  it('prepends received events', () => {
    const a = ev({ id: 'a', timestamp: '2026-01-01T00:00:00Z' });
    const b = ev({ id: 'b', timestamp: '2026-01-01T00:00:01Z' });
    let s = eventsReducer(undefined, received(a));
    s = eventsReducer(s, received(b));
    expect(s.items.map((e) => e.id)).toEqual(['b', 'a']);
  });

  it('drops duplicate ids', () => {
    const a = ev({ id: 'a' });
    let s = eventsReducer(undefined, received(a));
    s = eventsReducer(s, received(a));
    expect(s.items.length).toBe(1);
  });

  it('caps at MAX_EVENTS via FIFO eviction', () => {
    let s = eventsReducer(undefined, { type: 'init' });
    for (let i = 0; i < MAX_EVENTS + 50; i++) {
      s = eventsReducer(
        s,
        received(ev({ id: `id-${i}`, timestamp: new Date(i * 1000).toISOString() })),
      );
    }
    expect(s.items.length).toBe(MAX_EVENTS);
    // Oldest entries (id-0..id-49) should be evicted.
    expect(s.items.find((e) => e.id === 'id-0')).toBeUndefined();
    expect(s.items.find((e) => e.id === 'id-49')).toBeUndefined();
  });

  it('receivedBatch merges and sorts newest-first', () => {
    const a = ev({ id: 'a', timestamp: '2026-01-01T00:00:00Z' });
    const b = ev({ id: 'b', timestamp: '2026-01-01T00:00:02Z' });
    const c = ev({ id: 'c', timestamp: '2026-01-01T00:00:01Z' });
    let s = eventsReducer(undefined, received(a));
    s = eventsReducer(s, receivedBatch([b, c]));
    expect(s.items.map((e) => e.id)).toEqual(['b', 'c', 'a']);
  });

  it('cleared empties the buffer', () => {
    let s = eventsReducer(undefined, received(ev()));
    s = eventsReducer(s, cleared());
    expect(s.items).toEqual([]);
  });

  it('filterChanged merges partial updates', () => {
    let s = eventsReducer(undefined, filterChanged({ type: 'order' }));
    s = eventsReducer(s, filterChanged({ severity: 'critical' }));
    expect(s.filters.type).toBe('order');
    expect(s.filters.severity).toBe('critical');
  });

  it('filtersReset restores empty filters', () => {
    let s = eventsReducer(undefined, filterChanged({ type: 'order', search: 'foo' }));
    s = eventsReducer(s, filtersReset());
    expect(s.filters.type).toBe('');
    expect(s.filters.search).toBe('');
  });

  it('connectionChanged updates state', () => {
    const s = eventsReducer(undefined, connectionChanged('open'));
    expect(s.connection).toBe('open');
  });
});

describe('filterEvents', () => {
  const items = [
    ev({ id: '1', type: 'order', severity: 'info', source: 'shop', payload: { sku: 'A' } }),
    ev({ id: '2', type: 'alert', severity: 'critical', source: 'monitor', payload: { code: 500 } }),
    ev({
      id: '3',
      type: 'payment',
      severity: 'warning',
      source: 'stripe',
      payload: { amount: 42, currency: 'usd' },
      timestamp: '2026-06-01T00:00:00Z',
    }),
  ];

  it('returns all when no filter is set', () => {
    expect(
      filterEvents(items, { type: '', severity: '', search: '', from: '', to: '' }).length,
    ).toBe(3);
  });

  it('filters by type', () => {
    expect(
      filterEvents(items, { type: 'alert', severity: '', search: '', from: '', to: '' }).map(
        (e) => e.id,
      ),
    ).toEqual(['2']);
  });

  it('filters by severity', () => {
    expect(
      filterEvents(items, { type: '', severity: 'warning', search: '', from: '', to: '' }).map(
        (e) => e.id,
      ),
    ).toEqual(['3']);
  });

  it('search matches across source, type and payload', () => {
    expect(
      filterEvents(items, { type: '', severity: '', search: 'stripe', from: '', to: '' }).map(
        (e) => e.id,
      ),
    ).toEqual(['3']);
    expect(
      filterEvents(items, { type: '', severity: '', search: 'sku', from: '', to: '' }).map(
        (e) => e.id,
      ),
    ).toEqual(['1']);
  });

  it('respects time range', () => {
    expect(
      filterEvents(items, {
        type: '',
        severity: '',
        search: '',
        from: '2026-05-01T00:00:00Z',
        to: '',
      }).map((e) => e.id),
    ).toEqual(['3']);
  });
});
