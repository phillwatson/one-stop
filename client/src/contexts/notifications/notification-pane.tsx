import { useCallback } from "react";
import Alert from "@mui/material/Alert/Alert";
import AlertTitle from "@mui/material/AlertTitle/AlertTitle";

import { formatDateTime } from "../../util/date-util";
import Notification from "../../model/notification-model";
import NotificationService from "../../services/notification-service";
import { NotificationAction } from "./context";
import { MessageAction } from "../messages/model";

interface NotificationPaneProps {
  notification: Notification;
  dispatch: React.Dispatch<NotificationAction>;
  showError: React.Dispatch<MessageAction>;
}

export default function NotificationPane(props: NotificationPaneProps) {
  const handleCloseAlert = useCallback(() => {
    NotificationService.deleteNotification(props.notification.id)
      .then(() => props.dispatch({ type: 'delete', id: props.notification.id }))
      .catch(error => props.showError(error));
  }, [ props ]);

  return (
    <Alert key={ props.notification.id} onClose={ handleCloseAlert } severity={ props.notification.severity }>
      <AlertTitle>{ formatDateTime(props.notification.timestamp) }: { props.notification.topic }</AlertTitle>
      {
        props.notification.message.split('\n').map((line, index) => <div key={ index }>{ line }</div>)
      }
    </Alert>
  );
}
