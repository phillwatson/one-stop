import { createContext, useCallback, useContext, useEffect, useReducer, useRef } from "react";

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

// the interval at which the notifications are refreshed = 20 seconds
const NOTIFICATION_REFRESH_INTERVAL = 20 * 1000;

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
    case 'add':    {
      if (state.notifications.find(n => n.id === action.notification.id)) {
        return state;
      }
      return { ...state, notifications: [ ...state.notifications, action.notification ] };
    }
    case 'addAll': {
      const newEntries = action.notifications.filter(e => !state.notifications.find(n => n.id === e.id));
      return { ...state, notifications: [ ...state.notifications, ...newEntries ] };
    }
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

  const showError = useMessageDispatch();
  const [state, dispatch] = useReducer(notificationActionReducer, { show: false, notifications: []});

  // records the timestamp of the last notification received
  // so that we can retrieve only new notifications
  const notificationMarker = useRef(new Date(0).toISOString());

  // records whether the window has focus
  const fucused = useRef(true);
  function onFocus() {
    fucused.current = true;
  }
  function onBlur() {
    fucused.current = false;
  }

  const fetchNotifications = useCallback(async () => {
    // only poll for notifications if the window is focused
    if (fucused.current) {
      try {
        let page = 0;
        while (true) {
          let response = await NotificationService.getNotifications(notificationMarker.current, page++, 100);
          if (response.count > 0) {
            dispatch({ type: 'addAll', notifications: response.items });
            notificationMarker.current = response.items[response.count - 1].timestamp;
          }
          if (! response.links.next) {
            break; // no more pages
          }
        }
      } catch (error) {
        showError(error as AxiosError);
      }
    }
  }, [showError]);


  useEffect(() => {
    // we only want to poll when window is focused
    // otherwise the access-token will be constantly refreshed
    window.addEventListener('focus', onFocus);
    window.addEventListener('blur', onBlur);

    // clear the listeners when the component is unmounted
    return () => { 
      window.removeEventListener('focus', onFocus);
      window.removeEventListener('blur', onBlur);
    }
  }, []);

  useEffect(() => {
    var timer: any = undefined;
    if (currentUser) {
      fetchNotifications();
      timer = setInterval(fetchNotifications, NOTIFICATION_REFRESH_INTERVAL);
    }

    // clear the timer when the component is unmounted
    return () => { if (timer) clearInterval(timer); }
  }, [currentUser, fetchNotifications]);

  return (
    <NotificationContext.Provider value={ state.notifications }>
      <NotificationActionDispatchContext.Provider value={ dispatch }>
        { props.children }

        <NotificationDrawer state={ state } dispatch={ dispatch } showError={ showError } />
      </NotificationActionDispatchContext.Provider>
    </NotificationContext.Provider>
  );
}
