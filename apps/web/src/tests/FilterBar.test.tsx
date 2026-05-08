import { describe, expect, it } from 'vitest';
import { render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { Provider } from 'react-redux';
import { configureStore } from '@reduxjs/toolkit';
import { eventsReducer } from '@/features/events/eventsSlice';
import { eventsApi } from '@/features/events/eventsApi';
import { FilterBar } from '@/features/events/FilterBar';

function renderWithStore() {
  const store = configureStore({
    reducer: { events: eventsReducer, [eventsApi.reducerPath]: eventsApi.reducer },
    middleware: (g) => g().concat(eventsApi.middleware),
  });
  render(
    <Provider store={store}>
      <FilterBar />
    </Provider>,
  );
  return store;
}

describe('FilterBar', () => {
  it('renders type and severity controls', () => {
    renderWithStore();
    expect(screen.getByLabelText('Type')).toBeInTheDocument();
    expect(screen.getByLabelText('Severity')).toBeInTheDocument();
    expect(screen.getByLabelText('Search')).toBeInTheDocument();
  });

  it('updates redux state on change', async () => {
    const store = renderWithStore();
    const user = userEvent.setup();
    await user.selectOptions(screen.getByLabelText('Type'), 'alert');
    expect(store.getState().events.filters.type).toBe('alert');
  });

  it('reset button clears filters', async () => {
    const store = renderWithStore();
    const user = userEvent.setup();
    await user.selectOptions(screen.getByLabelText('Type'), 'order');
    await user.click(screen.getByRole('button', { name: /reset/i }));
    expect(store.getState().events.filters.type).toBe('');
  });

  it('export link reflects current filters', async () => {
    renderWithStore();
    const user = userEvent.setup();
    await user.selectOptions(screen.getByLabelText('Type'), 'payment');
    const link = screen.getByTestId('export-csv') as HTMLAnchorElement;
    expect(link.getAttribute('href')).toContain('type=payment');
  });
});
