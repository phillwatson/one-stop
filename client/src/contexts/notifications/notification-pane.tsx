import { useCallback } from "react";
import Alert from "@mui/material/Alert/Alert";
import AlertTitle from "@mui/material/AlertTitle/AlertTitle";

import Notification from "../../model/notification-model";
import { NotificationAction } from "./context";
import { formatDateTime } from "../../util/date-util";

interface NotificationPaneProps {
  notification: Notification;
  dispatch: React.Dispatch<NotificationAction>;
}

export default function NotificationPane(props: NotificationPaneProps) {

  const handleCloseAlert = useCallback(() => {
    props.dispatch({ type: 'delete', id: props.notification.id });
  }, [props]);

  return (
    <Alert key={ props.notification.id } onClose={ handleCloseAlert } severity={ props.notification.severity }>
      <AlertTitle>{ formatDateTime(props.notification.timestamp) }: { props.notification.topic }</AlertTitle>
      {
        props.notification.message.split('\n').map((line, index) => <div key={ index }>{ line }</div>)
      }
    </Alert>
  );
}
