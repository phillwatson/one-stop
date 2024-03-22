/**
 * Identifies a message's level of severity.
 */
export type Severity = 'error' | 'warning' | 'info' | 'success';

export type Message = {
  id: number;
  text: string;
  level: Severity;
}


/**
 * An action to be performed on the list of Messages.
 */
export type MessageAction = 
  | { type: 'add', level: Severity, text: string }
  | { type: 'delete', id: number };
