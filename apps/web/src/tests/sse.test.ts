import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';
import { backoffDelayMs, SseClient } from '@/lib/sse';

class FakeEventSource {
  static instances: FakeEventSource[] = [];
  url: string;
  private listeners: Record<string, Array<(e: unknown) => void>> = {};
  closed = false;

  constructor(url: string) {
    this.url = url;
    FakeEventSource.instances.push(this);
  }

  addEventListener(name: string, fn: (e: unknown) => void) {
    (this.listeners[name] ??= []).push(fn);
  }

  emit(name: string, payload: unknown) {
    (this.listeners[name] ?? []).forEach((fn) => fn(payload));
  }

  close() {
    this.closed = true;
  }
}

describe('backoffDelayMs', () => {
  it('starts at 500ms and doubles', () => {
    expect(backoffDelayMs(0)).toBe(500);
    expect(backoffDelayMs(1)).toBe(1000);
    expect(backoffDelayMs(2)).toBe(2000);
  });

  it('caps at 30s', () => {
    expect(backoffDelayMs(20)).toBe(30_000);
  });
});

describe('SseClient', () => {
  beforeEach(() => {
    FakeEventSource.instances = [];
  });
  afterEach(() => {
    vi.useRealTimers();
  });

  it('parses event payloads and forwards them', () => {
    const onEvent = vi.fn();
    const client = new SseClient({
      url: '/api/events/stream',
      onEvent,
      eventSourceCtor: FakeEventSource as unknown as typeof EventSource,
    });
    client.open();
    const es = FakeEventSource.instances[0];
    es.emit('event', {
      data: JSON.stringify({
        id: 'a',
        timestamp: '2026-01-01T00:00:00Z',
        type: 'order',
        severity: 'info',
        source: 'test',
        payload: {},
      }),
      lastEventId: 'a',
    });
    expect(onEvent).toHaveBeenCalledTimes(1);
    expect(onEvent.mock.calls[0][0].id).toBe('a');
    client.close();
  });

  it('drops malformed payloads silently', () => {
    const onEvent = vi.fn();
    const client = new SseClient({
      url: '/api/events/stream',
      onEvent,
      eventSourceCtor: FakeEventSource as unknown as typeof EventSource,
    });
    client.open();
    FakeEventSource.instances[0].emit('event', { data: 'not-json', lastEventId: '' });
    expect(onEvent).not.toHaveBeenCalled();
    client.close();
  });

  it('reports connection state changes', () => {
    const onState = vi.fn();
    const client = new SseClient({
      url: '/api/events/stream',
      onEvent: () => {},
      onStateChange: onState,
      eventSourceCtor: FakeEventSource as unknown as typeof EventSource,
    });
    client.open();
    FakeEventSource.instances[0].emit('open', null);
    expect(onState).toHaveBeenCalledWith('open');
    client.close();
    expect(onState).toHaveBeenCalledWith('closed');
  });

  it('reconnects with backoff and sends Last-Event-ID', () => {
    const onSchedule = vi.fn();
    const setTimeoutFn = ((fn: () => void) => {
      fn();
      return 0;
    }) as unknown as typeof setTimeout;

    const client = new SseClient({
      url: '/api/events/stream',
      onEvent: () => {},
      onScheduleReconnect: onSchedule,
      eventSourceCtor: FakeEventSource as unknown as typeof EventSource,
      setTimeoutFn,
    });
    client.open();
    const es1 = FakeEventSource.instances[0];
    es1.emit('event', {
      data: JSON.stringify({
        id: 'last-id',
        timestamp: '2026-01-01T00:00:00Z',
        type: 'order',
        severity: 'info',
        source: 'test',
        payload: {},
      }),
      lastEventId: 'last-id',
    });
    es1.emit('error', null);

    expect(onSchedule).toHaveBeenCalled();
    expect(FakeEventSource.instances.length).toBe(2);
    expect(FakeEventSource.instances[1].url).toContain('lastEventId=last-id');
    client.close();
  });
});
