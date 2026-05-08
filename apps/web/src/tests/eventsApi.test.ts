import { describe, expect, it } from 'vitest';
import { buildCsvExportUrl } from '@/features/events/eventsApi';

describe('buildCsvExportUrl', () => {
  it('returns the bare path when no filters are set', () => {
    expect(buildCsvExportUrl({})).toBe('/api/events/export.csv');
  });

  it('encodes filter parameters', () => {
    const url = buildCsvExportUrl({ type: 'order', severity: 'critical' });
    expect(url).toContain('type=order');
    expect(url).toContain('severity=critical');
  });

  it('omits empty fields', () => {
    const url = buildCsvExportUrl({ type: 'alert', search: '', from: '' });
    expect(url).toBe('/api/events/export.csv?type=alert');
  });
});
