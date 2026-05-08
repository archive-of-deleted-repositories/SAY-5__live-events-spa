import { describe, expect, it } from 'vitest';
import { render, screen } from '@testing-library/react';
import { Provider } from 'react-redux';
import { configureStore } from '@reduxjs/toolkit';
import { eventsReducer, received } from '@/features/events/eventsSlice';
import { eventsApi } from '@/features/events/eventsApi';
import { EventList } from '@/features/events/EventList';
import { EventRow } from '@/features/events/EventRow';
import { LiveIndicator } from '@/features/events/LiveIndicator';
import { connectionChanged } from '@/features/events/eventsSlice';
import type { LiveEvent } from '@/types';

function makeStore() {
  return configureStore({
    reducer: { events: eventsReducer, [eventsApi.reducerPath]: eventsApi.reducer },
    middleware: (g) => g().concat(eventsApi.middleware),
  });
}

const sample: LiveEvent = {
  id: 'a',
  timestamp: '2026-01-01T00:00:00Z',
  type: 'order',
  severity: 'critical',
  source: 'sample-source',
  payload: { sku: 'A1' },
};

describe('EventRow', () => {
  it('renders all metadata columns', () => {
    render(<EventRow event={sample} />);
    expect(screen.getByTestId('event-row')).toBeInTheDocument();
    expect(screen.getByText('order')).toBeInTheDocument();
    expect(screen.getByText('critical')).toBeInTheDocument();
    expect(screen.getByText('sample-source')).toBeInTheDocument();
  });
});

describe('EventList', () => {
  it('shows the empty state when there are no events', () => {
    const store = makeStore();
    render(
      <Provider store={store}>
        <EventList />
      </Provider>,
    );
    expect(screen.getByTestId('empty-state')).toBeInTheDocument();
  });

  it('renders the virtual list (not the empty state) when events exist', () => {
    const store = makeStore();
    store.dispatch(received(sample));
    const { container } = render(
      <Provider store={store}>
        <EventList />
      </Provider>,
    );
    expect(screen.queryByTestId('empty-state')).not.toBeInTheDocument();
    // react-virtuoso always renders an item-list container when fed data.
    expect(container.querySelector('[data-testid="virtuoso-item-list"]')).not.toBeNull();
  });
});

describe('LiveIndicator', () => {
  it('reflects the connection state', () => {
    const store = makeStore();
    store.dispatch(connectionChanged('open'));
    render(
      <Provider store={store}>
        <LiveIndicator />
      </Provider>,
    );
    expect(screen.getByText('live')).toBeInTheDocument();
  });

  it('shows reconnecting on error', () => {
    const store = makeStore();
    store.dispatch(connectionChanged('error'));
    render(
      <Provider store={store}>
        <LiveIndicator />
      </Provider>,
    );
    expect(screen.getByText('reconnecting')).toBeInTheDocument();
  });
});
