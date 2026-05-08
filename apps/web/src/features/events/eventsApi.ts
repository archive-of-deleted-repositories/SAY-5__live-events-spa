import { createApi, fetchBaseQuery } from '@reduxjs/toolkit/query/react';
import type { Filters, LiveEvent } from '@/types';

export interface HistoryResponse {
  items: LiveEvent[];
  nextCursor: string | null;
}

function filtersToParams(f: Partial<Filters>) {
  const params: Record<string, string> = {};
  if (f.type) params.type = f.type;
  if (f.severity) params.severity = f.severity;
  if (f.from) params.from = f.from;
  if (f.to) params.to = f.to;
  return params;
}

export const eventsApi = createApi({
  reducerPath: 'eventsApi',
  baseQuery: fetchBaseQuery({ baseUrl: '/api' }),
  endpoints: (build) => ({
    history: build.query<
      HistoryResponse,
      { filters: Partial<Filters>; cursor?: string; limit?: number }
    >({
      query: ({ filters, cursor, limit = 50 }) => ({
        url: '/events',
        params: { ...filtersToParams(filters), cursor, limit },
      }),
    }),
  }),
});

export const { useHistoryQuery } = eventsApi;

/** Build the URL for a CSV export with the current filters applied. */
export function buildCsvExportUrl(filters: Partial<Filters>): string {
  const search = new URLSearchParams(filtersToParams(filters));
  const qs = search.toString();
  return qs ? `/api/events/export.csv?${qs}` : '/api/events/export.csv';
}
