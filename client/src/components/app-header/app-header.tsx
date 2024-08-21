import { styled } from '@mui/material/styles';
import MuiAppBar, { AppBarProps as MuiAppBarProps } from '@mui/material/AppBar';
import Toolbar from '@mui/material/Toolbar';
import Grid from '@mui/material/Grid';
import Typography from '@mui/material/Typography';
import IconButton from '@mui/material/IconButton';

import MenuIcon from '@mui/icons-material/Menu';
import NotificationsOnIcon from '@mui/icons-material/NotificationsActive';
import NotificationsOffIcon from '@mui/icons-material/Notifications';

import { useCurrentUser } from '../../contexts/user-context';
import MonetarySwitch from '../../contexts/monetary/monetary-switch';
import { useNotificationDispatch, useNotifications } from '../../contexts/notifications/context';
import UserAvatar from '../user-profile/user-avatar';
import MenuItemDef from '../app-menu/menu-item-def';

interface Props extends MuiAppBarProps {
  title?: string;
  menuItems?: MenuItemDef[];
  onClick?: React.MouseEventHandler | undefined;
}

const AppBar = styled(MuiAppBar)
<Props>(({ theme }) => ({
  transition: theme.transitions.create(['margin', 'width'], {
    easing: theme.transitions.easing.sharp,
    duration: theme.transitions.duration.leavingScreen,
  }),
}));

export default function AppHeader(props: Props) {
  const [ currentUser ] = useCurrentUser();

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
        <Grid container direction="row" alignItems="flex-end" wrap='nowrap'>

          <Grid item alignSelf="flex-start">
            <IconButton color="inherit" aria-label="open drawer" onClick={ props.onClick } edge="start">
              <MenuIcon />
            </IconButton>
          </Grid>

          <Grid item>
            <Typography variant="h6" noWrap>
              { props.title }
            </Typography>
          </Grid>

          <Grid container direction="row" justifyContent="flex-end" alignItems="flex-end" spacing={ 1 }>
            <Grid item>
                <UserAvatar user={ currentUser } menuItems={ props.menuItems } />
            </Grid>
            <Grid item>
              <MonetarySwitch></MonetarySwitch>
            </Grid>
            <Grid item>
              { notificationsBell(userNotifications.length === 0) }
            </Grid>
          </Grid>
        </Grid>
      </Toolbar>
    </AppBar>
  );
}
