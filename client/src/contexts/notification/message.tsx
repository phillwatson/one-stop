import { useCallback, useEffect } from "react";
import Alert from "@mui/material/Alert";

import { Notification, NotificationAction } from "./model";

interface MessageProps {
  notification: Notification;
  dispatch: React.Dispatch<NotificationAction>;
}

// those severities that should be auto-closed
const AUTO_CLOSE = ['success', 'info'];

export default function Message(props: MessageProps) {
  const notification = props.notification;
  const dispatch = props.dispatch;

  const handleCloseAlert = useCallback(() => {
    dispatch({ type: 'delete', id: notification.id });
  }, [notification, dispatch]);

  useEffect(() => {
    const t = (AUTO_CLOSE.includes(notification.level)) ? setTimeout(() => { handleCloseAlert(); }, 5000) : null;
    return () => { if (t !== null) clearTimeout(t); }
  }, [notification, handleCloseAlert]);

  return (
    <Alert elevation={24} onClose={ handleCloseAlert } severity={ notification.level }>
      {
        notification.message.split('\n').map((line) => <div>{ line }</div>)
      }
    </Alert>
  );
}