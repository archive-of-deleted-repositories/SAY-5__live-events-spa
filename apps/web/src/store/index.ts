import { configureStore } from '@reduxjs/toolkit';
import { eventsReducer } from '@/features/events/eventsSlice';
import { eventsApi } from '@/features/events/eventsApi';

export const store = configureStore({
  reducer: {
    events: eventsReducer,
    [eventsApi.reducerPath]: eventsApi.reducer,
  },
  middleware: (getDefault) => getDefault().concat(eventsApi.middleware),
});

export type RootState = ReturnType<typeof store.getState>;
export type AppDispatch = typeof store.dispatch;
