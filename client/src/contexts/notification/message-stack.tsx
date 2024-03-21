import { Notification, NotificationAction } from "./model";
import Message from './message';

interface MessageBoardProps {
  notifications: Array<Notification>;
  dispatch: React.Dispatch<NotificationAction>;
}

export default function MessageStack(props: MessageBoardProps) {
  return (
      <>
        { props.notifications.map((notification, index) =>
          <Message key={ notification.id } notification={ notification } dispatch={ props.dispatch } index={ index }/>
        )}
      </>
  );
}
