import { styled } from '@mui/material/styles';

import MuiAppBar, { AppBarProps as MuiAppBarProps } from '@mui/material/AppBar';
import Toolbar from '@mui/material/Toolbar';
import Grid from '@mui/material/Grid';
import Typography from '@mui/material/Typography';
import IconButton from '@mui/material/IconButton';
import MenuIcon from '@mui/icons-material/Menu';

import { useCurrentUser } from '../../contexts/user-context';
import MonetarySwitch from '../../contexts/monetary/monetary-switch';

import UserAvatar from '../user-profile/user-avatar';
import MenuItemDef from '../app-menu/menu-item-def';
import NotificationBell from '../../contexts/notifications/notification-bell';
import SubmitReconcilationButton from '../reconciliation/submit-button';

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

  return (
    <AppBar position="fixed">
      <Toolbar>
        <Grid container direction="row" alignItems="center" wrap='nowrap'>

          <Grid item>
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
              <SubmitReconcilationButton />
            </Grid>
            <Grid item>
              <NotificationBell />
            </Grid>
            <Grid item>
              <MonetarySwitch />
            </Grid>
            <Grid item>
              <UserAvatar user={ currentUser } menuItems={ props.menuItems } />
            </Grid>
          </Grid>
        </Grid>
      </Toolbar>
    </AppBar>
  );
}
