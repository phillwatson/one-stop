/**
 * Identifies a notification's level of severity.
 */
export type Severity = 'error' | 'warning' | 'info';

export default interface Notification {
  id: string;
  message: string;
  topic: string;
  severity: Severity;
  timestamp: string;
}