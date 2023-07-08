import { styled } from '@mui/material/styles';
import MuiAppBar, { AppBarProps as MuiAppBarProps } from '@mui/material/AppBar';
import Toolbar from '@mui/material/Toolbar';
import Typography from '@mui/material/Typography';
import IconButton from '@mui/material/IconButton';
import MenuIcon from '@mui/icons-material/Menu';
import { useCurrentUser } from '../../contexts/user-context';

interface AppHeaderProps extends MuiAppBarProps {
  drawerWidth: number;
  open?: boolean;
  title?: string;
  onClick?: React.MouseEventHandler | undefined;
}

const AppBar = styled(MuiAppBar, { shouldForwardProp: (prop) => prop !== 'open' && prop !== 'drawerWidth' })
<AppHeaderProps>(({ theme, open, drawerWidth }) => ({
  transition: theme.transitions.create(['margin', 'width'], {
    easing: theme.transitions.easing.sharp,
    duration: theme.transitions.duration.leavingScreen,
  }),
  ...(open && {
    width: `calc(100% - ${drawerWidth}px)`,
    marginLeft: `${drawerWidth}px`,
    transition: theme.transitions.create(['margin', 'width'], {
      easing: theme.transitions.easing.easeOut,
      duration: theme.transitions.duration.enteringScreen,
    }),
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

  return (
    <AppBar position="fixed" open={ props.open } drawerWidth={ props.drawerWidth }>
      <Toolbar>
          <IconButton color="inherit" aria-label="open drawer" onClick={ props.onClick }
           edge="start" sx={{ mr: 2, ...(props.open && { display: 'none' }) }}>
            <MenuIcon />
          </IconButton>

          <Typography variant="h6" noWrap component="div">
            { props.title }
          </Typography>
          <span style={{ marginLeft: "auto" }}>{getName()}</span>
      </Toolbar>
    </AppBar>
  );
}
