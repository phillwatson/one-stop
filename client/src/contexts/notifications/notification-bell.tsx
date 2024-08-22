
import IconButton from '@mui/material/IconButton';
import NotificationsOnIcon from '@mui/icons-material/NotificationsActive';
import NotificationsOffIcon from '@mui/icons-material/Notifications';

import { useNotificationDispatch, useNotifications } from './context';
import { Severity } from '../../model/notification-model';

export default function NotificationBell() {
  const userNotifications = useNotifications();

  const openNotifications = useNotificationDispatch();
  function showNotifications() {
    openNotifications({ type: 'show', value: true });
  }

  function notificationsOff() {
    return userNotifications.length === 0
  }

  function bellColour(): Severity | 'inherit' {
    const nonInfoNotification = userNotifications
      .find(notication => notication.severity !== 'info');
      
    return nonInfoNotification === undefined ? 'inherit' : nonInfoNotification.severity;
  }

  return (
    <IconButton color={ bellColour() } disabled={ notificationsOff() } onClick={ showNotifications }>
      { notificationsOff()
        ? <NotificationsOffIcon/>
        : <NotificationsOnIcon/>
      }
    </IconButton>
  );
}