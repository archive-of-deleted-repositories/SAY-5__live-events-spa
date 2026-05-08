import type { LiveEvent } from '@/types';

export interface SseClientOptions {
  url: string;
  onEvent: (event: LiveEvent) => void;
  onStateChange?: (state: 'open' | 'closed' | 'error') => void;
  /** Override the EventSource constructor (used by tests). */
  eventSourceCtor?: typeof EventSource;
  /** Called when the client schedules a reconnect; exposed for tests. */
  onScheduleReconnect?: (delayMs: number, attempt: number) => void;
  /** Override the timer (used by tests). */
  setTimeoutFn?: typeof setTimeout;
  /** Override the timer-clear (used by tests). */
  clearTimeoutFn?: typeof clearTimeout;
}

/**
 * EventSource wrapper with exponential backoff reconnect + Last-Event-ID
 * resume. The browser's built-in {@code EventSource} already retries on
 * disconnect, but it does not back off and it does not surface errors in a
 * structured way. We layer that on top.
 */
export class SseClient {
  private current: EventSource | null = null;
  private attempt = 0;
  private lastEventId: string | null = null;
  private closed = false;
  private reconnectTimer: ReturnType<typeof setTimeout> | null = null;

  constructor(private readonly opts: SseClientOptions) {}

  open(): void {
    if (this.closed) return;
    const Ctor = this.opts.eventSourceCtor ?? EventSource;
    const url = this.lastEventId
      ? `${this.opts.url}${this.opts.url.includes('?') ? '&' : '?'}lastEventId=${encodeURIComponent(this.lastEventId)}`
      : this.opts.url;
    const es = new Ctor(url);
    this.current = es;

    es.addEventListener('open', () => {
      this.attempt = 0;
      this.opts.onStateChange?.('open');
    });

    es.addEventListener('event', (raw: MessageEvent) => {
      this.lastEventId = raw.lastEventId || this.lastEventId;
      try {
        const parsed = JSON.parse(raw.data) as LiveEvent;
        this.opts.onEvent(parsed);
      } catch {
        // Drop malformed payloads silently; the server is supposed to send valid JSON.
      }
    });

    es.addEventListener('error', () => {
      this.opts.onStateChange?.('error');
      this.scheduleReconnect();
    });
  }

  close(): void {
    this.closed = true;
    if (this.reconnectTimer) {
      (this.opts.clearTimeoutFn ?? clearTimeout)(this.reconnectTimer);
      this.reconnectTimer = null;
    }
    this.current?.close();
    this.current = null;
    this.opts.onStateChange?.('closed');
  }

  private scheduleReconnect(): void {
    if (this.closed) return;
    this.current?.close();
    this.current = null;
    const delay = backoffDelayMs(this.attempt);
    this.attempt += 1;
    this.opts.onScheduleReconnect?.(delay, this.attempt);
    this.reconnectTimer = (this.opts.setTimeoutFn ?? setTimeout)(() => {
      this.reconnectTimer = null;
      this.open();
    }, delay);
  }
}

/**
 * Capped exponential backoff. Attempt 0 retries quickly; subsequent attempts
 * grow up to a 30s ceiling. A small random jitter is *not* added here because
 * the production deploy will sit behind a CDN that already smooths reconnect
 * thundering herds.
 */
export function backoffDelayMs(attempt: number): number {
  const base = 500;
  const cap = 30_000;
  return Math.min(cap, base * 2 ** attempt);
}
