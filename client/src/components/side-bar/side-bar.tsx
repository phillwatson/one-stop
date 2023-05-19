import { styled } from '@mui/material/styles';
import Drawer from '@mui/material/Drawer';
import Divider from '@mui/material/Divider';
import IconButton from '@mui/material/IconButton';
import ChevronLeftIcon from '@mui/icons-material/ChevronLeft';


const DrawerHeader = styled('div')(({ theme }) => ({
  display: 'flex',
  alignItems: 'center',
  padding: theme.spacing(0, 1),
  // necessary for content to be below app bar
  ...theme.mixins.toolbar,
  justifyContent: 'flex-end',
}));

interface SizeBarProps extends React.PropsWithChildren {
    open?: boolean;
    drawerWidth: number;
    onClose?: React.MouseEventHandler | undefined;
}

export default function SizeBar(props: SizeBarProps) {
  return (
    <Drawer
        sx={{
            width: props.drawerWidth,
            flexShrink: 0,
            '& .MuiDrawer-paper': {
              width: props.drawerWidth,
              boxSizing: 'border-box',
            },
        }}
        variant="persistent"
        anchor="left"
        open={ props.open }
        >

        <DrawerHeader>
          <IconButton onClick={ props.onClose }>
            <ChevronLeftIcon />
          </IconButton>
        </DrawerHeader>

        <Divider />
        { props.children }
    </Drawer>
  );
}
