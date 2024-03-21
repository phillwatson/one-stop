import { createContext, useContext, useReducer } from "react";

import { Notification, NotificationAction } from "./model";
import MessageStack from "./message-stack";
import { AxiosError } from "axios";
import { ServiceError } from "../../model/service-error";

function extractMessages(error: AxiosError): Notification[] {
  const response: any = error.response;
  if ((! response) || (!response.data)) {
    return [ { id: Date.now(), message: error.message, level: 'error' } ];
  }

  return response.data.errors.map((error: ServiceError) => {
    return { id: Date.now(), message: error.message, level: error.severity };
  });
}

/**
 * Accepts actions and updates the list of Notifications accordingly.
 * 
 * @param notifications the list of Notifications to be updated (the state)
 * @param action the action to be performed on the Notifications.
 * @returns the modified copy of the given Notifications.
 */
function notificationReducer(notifications: Notification[], action: NotificationAction | AxiosError): Notification[] {
  if (action instanceof AxiosError) {
    const errors = extractMessages(action);
    return [ ...errors, ...notifications ];
  }

  switch (action.type) {
    case 'add': {
      return [ { id: Date.now(), message: (action.message as string), level: action.level }, ...notifications ];
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

        <MessageStack notifications={notifications} dispatch={dispatch} />
      </NotificationDispatchContext.Provider>
    </NotificationContext.Provider>
  );
}
