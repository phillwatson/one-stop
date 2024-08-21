import { styled } from '@mui/material/styles';
import MuiAppBar, { AppBarProps as MuiAppBarProps } from '@mui/material/AppBar';
import Toolbar from '@mui/material/Toolbar';
import Typography from '@mui/material/Typography';
import IconButton from '@mui/material/IconButton';
import MenuIcon from '@mui/icons-material/Menu';

import { useCurrentUser } from '../../contexts/user-context';
import MonetarySwitch from '../../contexts/monetary/monetary-switch';
import { useNotificationDispatch, useNotifications } from '../../contexts/notifications/context';
import NotificationsOnIcon from '@mui/icons-material/NotificationsActive';
import NotificationsOffIcon from '@mui/icons-material/Notifications';

interface AppHeaderProps extends MuiAppBarProps {
  title?: string;
  onClick?: React.MouseEventHandler | undefined;
}

const AppBar = styled(MuiAppBar)
<AppHeaderProps>(({ theme }) => ({
  transition: theme.transitions.create(['margin', 'width'], {
    easing: theme.transitions.easing.sharp,
    duration: theme.transitions.duration.leavingScreen,
  }),
}));

export default function AppHeader(props: AppHeaderProps) {
  const [currentUser] = useCurrentUser();
  function getName() {
    if (!currentUser) {
      return null;
    }

    if (currentUser.preferredName) {
      return currentUser.preferredName;
    }

    if (currentUser.givenName) {
      return currentUser.givenName;
    }

    return currentUser.username;
  }

  const userNotifications = useNotifications();
  const openNotifications = useNotificationDispatch();
  function showNotifications() {
    openNotifications({ type: 'show', value: true });
  }

  function notificationsBell(off: boolean) {
    return (
      <IconButton color="inherit" disabled={ off } onClick={ showNotifications }>
        { off ? <NotificationsOffIcon/> : <NotificationsOnIcon/> }
      </IconButton>
    );
  }

  return (
    <AppBar position="fixed">
      <Toolbar>
          <IconButton color="inherit" aria-label="open drawer" onClick={ props.onClick }
           edge="start">
            <MenuIcon />
          </IconButton>

          <Typography variant="h6" noWrap component="div">
            { props.title }
          </Typography>
          <span style={{ marginLeft: "auto" }}>
            { getName() }
          </span>
          <MonetarySwitch></MonetarySwitch>
          { notificationsBell(userNotifications.length === 0) }
      </Toolbar>
    </AppBar>
  );
}
