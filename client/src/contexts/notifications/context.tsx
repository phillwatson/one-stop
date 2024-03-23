import { createContext, useContext, useEffect, useReducer, useRef } from "react";

import Notification from "../../model/notification-model";
import NotificationDrawer from "./notification-drawer";
import NotificationService from "../../services/notification-service";
import { useMessageDispatch } from "../messages/context";
import { AxiosError } from "axios";
import { useCurrentUser } from "../user-context";

export interface NotificationState {
  show: boolean;
  notifications: Notification[];
}

/**
 * An action to be performed on the list of Notifications.
 */
export type NotificationAction = 
  | { type: 'show', value: boolean }
  | { type: 'add', notification: Notification }
  | { type: 'addAll', notifications: Notification[] }
  | { type: 'delete', id: string };

// the interval at which the notifications are refreshed = 60 seconds
const NOTIFICATION_REFRESH_INTERVAL = 10 * 1000;

/**
 * Accepts actions and updates the list of Notifications accordingly.
 * 
 * @param state the NotificationState containing the list of Notifications to be updated.
 * @param action the action to be performed on the Notifications.
 * @returns the modified copy of the given Notifications.
 */
function notificationActionReducer(state: NotificationState, action: NotificationAction): NotificationState {
  switch (action.type) {
    case "show":   return { ...state, show: action.value };
    case 'add':    return { ...state, notifications: [ action.notification, ...state.notifications ] };
    case 'addAll': return { ...state, notifications: [ ...action.notifications, ...state.notifications ] };
    case 'delete': return { ...state, notifications: state.notifications.filter(e => e.id !== action.id), show: state.show && state.notifications.length > 1 };
    default: throw Error('Unknown message action: ' + action);
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
 * A context that allows components to dispatch NotificationAction to update the state
 * of the Notifications.
 */
const NotificationActionDispatchContext = createContext(function(action: NotificationAction) {});
export function useNotificationDispatch() {
  return useContext(NotificationActionDispatchContext);
}

/**
 * A component that wraps child components within the context of a list of Notifications.
 * 
 * @param props the child components to be wrapped.
 * @returns the NotificationProvider component.
 */
export default function NotificationProvider(props: React.PropsWithChildren) {
  const [currentUser] = useCurrentUser();

  const showMessage = useMessageDispatch();
  const [state, dispatch] = useReducer(notificationActionReducer, { show: false, notifications: []});

  // records the timestamp of the last notification received
  // so that we can retrieve only new notifications
  const notificationMarker = useRef(new Date(0).toISOString());

  useEffect(() => {
    var timer: NodeJS.Timer | undefined = undefined;
    if (currentUser) {
      timer = setInterval(async () => {
        try {
          let page = 0;
          while (true) {
            let response = await NotificationService.getNotifications(notificationMarker.current, page++, 100);
            if (response.count > 0) {
              dispatch({ type: 'addAll', notifications: response.items });
              notificationMarker.current = response.items[response.count - 1].timestamp;
            }
            // if response contained less than a page size
            if (response.count < 100) {
              break; // no more pages
            }
          }
        } catch (error) {
          showMessage(error as AxiosError);
        }
      }, NOTIFICATION_REFRESH_INTERVAL);
    }
    return () => { if (timer) clearInterval(timer); }
  }, [currentUser, showMessage]);

  return (
    <NotificationContext.Provider value={ state.notifications }>
      <NotificationActionDispatchContext.Provider value={ dispatch }>
        { props.children }

        <NotificationDrawer state={ state } dispatch={ dispatch } />
      </NotificationActionDispatchContext.Provider>
    </NotificationContext.Provider>
  );
}
