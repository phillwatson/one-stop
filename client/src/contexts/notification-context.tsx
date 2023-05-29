import { createContext, forwardRef, useCallback, useContext, useEffect, useReducer } from "react";
import Snackbar from "@mui/material/Snackbar";
import Collapse from '@mui/material/Collapse';
import MuiAlert, { AlertProps } from '@mui/material/Alert';
import { TransitionGroup } from 'react-transition-group';

/**
 * See: https://react.dev/learn/scaling-up-with-reducer-and-context
 */


/**
 * Identifies an notification's level of severity.
 */
type Severity = 'error' | 'warning' | 'info' | 'success';

/**
 * The record of an notification that is to be reported.
 */
type Notification = {
  id: number;
  message: string;
  level: Severity;
}

/**
 * An action to be performed on the list of Notifications.
 */
type NotificationAction = 
  | { type: 'add', level: Severity, message: string }
  | { type: 'delete', id: number };


/**
 * Accepts actions and updates the list of Notifications accordingly.
 * 
 * @param notifications the list of Notifications to be updated (the state)
 * @param action the action to be performed on the Notifications.
 * @returns the modified copy of the given Notifications.
 */
function notificationReducer(notifications: Notification[], action: NotificationAction): Notification[] {
  switch (action.type) {
    case 'add': {
      return [ { id: Date.now(), message: action.message, level: action.level }, ...notifications ];
    }

    case 'delete': {
      return notifications.filter(e => e.id !== action.id);
    }

    default: {
      throw Error('Unknown notification action: ' + action);
    }
  }
}

/**
 * A context that allows components to access the list of Notifications, and respond
 * to changes to the list.
 */
const NotificationContext = createContext(Array<Notification>());
export function useNotifications(): Notification[] {
  return useContext(NotificationContext);
}

/**
 * A context that allows components to dispatch NotificationAction to update the list of
 * Notifications.
 */
const NotificationDispatchContext = createContext(function(action: NotificationAction) {});
export function useNotificationDispatch() {
  return useContext(NotificationDispatchContext);
}

/**
 * A component that wraps child components within the context of a list of Notifications.
 * 
 * @param props the child components to be wrapped.
 * @returns the NotificationProvider component.
 */
export default function NotificationProvider(props: React.PropsWithChildren) {
  const [notifications, dispatch] = useReducer(notificationReducer, []);

  return (
    <NotificationContext.Provider value={notifications}>
      <NotificationDispatchContext.Provider value={dispatch}>
        { props.children }

        <MessageBoard notifications={notifications} dispatch={dispatch} />
      </NotificationDispatchContext.Provider>
    </NotificationContext.Provider>
  );
}

const Alert = forwardRef<HTMLDivElement, AlertProps>(function Alert(props, ref) {
  return <MuiAlert elevation={24} ref={ref} variant="filled" {...props} />;
});


interface MessageBoardProps {
  notifications: Array<Notification>;
  dispatch: React.Dispatch<NotificationAction>;
}

function MessageBoard(props: MessageBoardProps) {
  return (
    <TransitionGroup>
    { props.notifications.map((notification, index) =>
      <Collapse key={notification.id}>
        <Message notification={notification} dispatch={props.dispatch} index={index}/>
      </Collapse>
    )}
    </TransitionGroup>
  )
}

interface MessageProps {
    notification: Notification;
    dispatch: React.Dispatch<NotificationAction>;
    index: number;
}

function Message(props: MessageProps) {
  const notification = props.notification;
  const dispatch = props.dispatch;

  const handleCloseAlert = useCallback(() => {
    dispatch({ type: 'delete', id: notification.id });
  }, [notification, dispatch]);

  useEffect(() => {
    // set a timeout for "success" messages
    const t = (notification.level === 'success') ? setTimeout(() => { handleCloseAlert(); }, 5000) : null;
    return () => { if (t !== null) clearTimeout(t); }
  }, [notification, handleCloseAlert]);

  const top = (props.index * 60) + "px";
  return (
    <Snackbar key={props.notification.id} anchorOrigin={{ vertical: 'top', horizontal: 'right' }} sx={{ marginTop: top }} open={true}>
      <Alert onClose={handleCloseAlert} severity={props.notification.level}>
        {props.notification.message}
      </Alert>
    </Snackbar>
  );
}