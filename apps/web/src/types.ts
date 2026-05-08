export type EventType = 'order' | 'payment' | 'shipment' | 'alert';
export type Severity = 'info' | 'warning' | 'critical';

export interface LiveEvent {
  id: string;
  timestamp: string;
  type: EventType;
  severity: Severity;
  source: string;
  payload: unknown;
}

export interface Filters {
  type: EventType | '';
  severity: Severity | '';
  search: string;
  from: string;
  to: string;
}

export const EMPTY_FILTERS: Filters = {
  type: '',
  severity: '',
  search: '',
  from: '',
  to: '',
};
