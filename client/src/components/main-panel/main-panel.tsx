import { styled } from '@mui/material/styles';

const drawerWidth = 240;

const Main = styled('main', { shouldForwardProp: (prop) => prop !== 'open' && prop !== 'drawerWidth' })<{
  open?: boolean, drawerWidth: number;
}>(({ theme, open }) => ({
  flexGrow: 1,
  padding: theme.spacing(3),
  transition: theme.transitions.create('margin', {
    easing: theme.transitions.easing.sharp,
    duration: theme.transitions.duration.leavingScreen,
  }),
  marginLeft: `-${drawerWidth}px`,
  ...(open && {
    transition: theme.transitions.create('margin', {
      easing: theme.transitions.easing.easeOut,
      duration: theme.transitions.duration.enteringScreen,
    }),
    marginLeft: 0,
  }),
}));

interface MainPanelProps extends React.PropsWithChildren {
    drawerWidth: number;
    open?: boolean;
}

export default function MainPanel(props: MainPanelProps) {
  return (
    <Main open={ props.open } drawerWidth={ props.drawerWidth } style={{ paddingTop: "90px" }}>
      { props.children }
    </Main>
  );
}
