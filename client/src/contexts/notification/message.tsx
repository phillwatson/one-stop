import { useCallback, useEffect } from "react";
import Snackbar from "@mui/material/Snackbar";
import Alert from '@mui/material/Alert';

import { Notification, NotificationAction } from "./model";

interface MessageProps {
  notification: Notification;
  dispatch: React.Dispatch<NotificationAction>;
  index: number;
}

export default function Message(props: MessageProps) {
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
    <Snackbar anchorOrigin={{ vertical: 'top', horizontal: 'right' }} sx={{ marginTop: top }} open={ true }>
      <Alert elevation={24} onClose={ handleCloseAlert } severity={ notification.level }>
        { notification.message }
      </Alert>
    </Snackbar>
  );
}