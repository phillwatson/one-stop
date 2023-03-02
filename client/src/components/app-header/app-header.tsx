import { styled } from '@mui/material/styles';
import MuiAppBar, { AppBarProps as MuiAppBarProps } from '@mui/material/AppBar';
import Toolbar from '@mui/material/Toolbar';
import Typography from '@mui/material/Typography';
import IconButton from '@mui/material/IconButton';
import MenuIcon from '@mui/icons-material/Menu';

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
      </Toolbar>
    </AppBar>
  );
}
