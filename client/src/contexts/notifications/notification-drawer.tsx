import { Drawer } from '@mui/material';

import './notification-drawer.css';
import { NotificationState, NotificationAction, useNotificationDispatch } from "./context";
import NotificationPane from "./notification-pane";

interface NotificationDrawerProps {
  state: NotificationState;
  dispatch: React.Dispatch<NotificationAction>;
}

export default function NotiticationDrawer(props: NotificationDrawerProps) {
  const addNotification = useNotificationDispatch();

  function handleClose() {
    addNotification({ type: 'show', value: false });
  }
  
  return (
    <Drawer className='notification-drawer' anchor='right' open={ props.state.show } onClose={ handleClose }
      PaperProps={{ sx: { maxWidth: "60%" }, }}>
      <div className='notification-panel'>
        { props.state.notifications.map((notification) =>
          <NotificationPane key={ notification.id } notification={ notification } dispatch={ props.dispatch }/>
        )}
      </div>
    </Drawer>
  );
}