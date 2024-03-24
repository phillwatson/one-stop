import { Drawer } from '@mui/material';

import './notification-drawer.css';
import { NotificationState, NotificationAction } from "./context";
import NotificationPane from "./notification-pane";
import { MessageAction } from '../messages/model';
import { useCallback } from 'react';

interface NotificationDrawerProps {
  state: NotificationState;
  dispatch: React.Dispatch<NotificationAction>;
  showError: React.Dispatch<MessageAction>;
}

export default function NotiticationDrawer(props: NotificationDrawerProps) {
  const handleClose = useCallback(() => {
    props.dispatch({ type: 'show', value: false });
  }, [props]);
  
  return (
    <Drawer className='notification-drawer' anchor='right' open={ props.state.show } onClose={ handleClose }
      PaperProps={{ sx: { maxWidth: "60%" }, }}>
      <div className='notification-panel'>
        { props.state.notifications.map((notification) =>
          <NotificationPane key={ notification.id } notification={ notification } dispatch={ props.dispatch } showError={ props.showError }/>
        )}
      </div>
    </Drawer>
  );
}