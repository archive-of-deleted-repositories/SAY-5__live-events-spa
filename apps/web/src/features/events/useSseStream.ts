import { useEffect } from 'react';
import { useAppDispatch } from '@/store/hooks';
import { connectionChanged, received } from './eventsSlice';
import { SseClient } from '@/lib/sse';

const STREAM_URL = '/api/events/stream';

export function useSseStream() {
  const dispatch = useAppDispatch();

  useEffect(() => {
    const client = new SseClient({
      url: STREAM_URL,
      onEvent: (ev) => dispatch(received(ev)),
      onStateChange: (s) => dispatch(connectionChanged(s)),
    });
    client.open();
    return () => client.close();
  }, [dispatch]);
}
