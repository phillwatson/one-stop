import "./message-stack.css";
import { Notification, NotificationAction } from "./model";
import Message from './message';

interface MessageBoardProps {
  notifications: Array<Notification>;
  dispatch: React.Dispatch<NotificationAction>;
}

export default function MessageStack(props: MessageBoardProps) {
  return (
    <div className="stack" >
      { props.notifications.map((notification) =>
        <Message key={ notification.id } notification={ notification } dispatch={ props.dispatch }/>
      )}
    </div>
  );
}
