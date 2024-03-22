import { AxiosError } from "axios";

/**
 * Identifies an notification's level of severity.
 */
export type Severity = 'error' | 'warning' | 'info' | 'success';

export type Notification = {
  id: number;
  message: string;
  level: Severity;
}


/**
 * An action to be performed on the list of Notifications.
 */
export type NotificationAction = 
  | { type: 'add', level: Severity, message: string }
  | { type: 'delete', id: number };
