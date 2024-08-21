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
    onClose?: React.MouseEventHandler | undefined;
}

export default function SizeBar(props: SizeBarProps) {
  return (
    <Drawer
      sx={{
          width: 240,
          flexShrink: 0,
          '& .MuiDrawer-paper': {
            width: 240,
            boxSizing: 'border-box',
          }
      }}
      anchor="left"
      open={ props.open }
      onClose={ props.onClose }
      >

      <DrawerHeader onClick={ props.onClose }>
        <IconButton>
          <ChevronLeftIcon />
        </IconButton>
      </DrawerHeader>

      <Divider />
      { props.children }
    </Drawer>
  );
}
