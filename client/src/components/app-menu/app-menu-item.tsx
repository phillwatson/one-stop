import ListItem from '@mui/material/ListItem';
import ListItemButton from '@mui/material/ListItemButton';
import ListItemIcon from '@mui/material/ListItemIcon';
import ListItemText from '@mui/material/ListItemText';
import { ReactElement } from 'react';
import { useNavigate } from 'react-router-dom';

export interface MenuItem {
  label: string;
  icon?: ReactElement
  route: string;
  action?: () => void;
}

export interface AppMenuItemProps extends MenuItem {
  index?: number;
}

export default function AppMenuItem(props: AppMenuItemProps) {
  const navigation = useNavigate();

  return (
    <>
      <ListItem disablePadding onClick={ () => { if (props.action) { props.action(); } navigation(props.route); }}>
        <ListItemButton>
          <ListItemIcon>
            { props.icon }
          </ListItemIcon>
          <ListItemText primary={ props.label} />
        </ListItemButton>
      </ListItem>
    </>
  );
}
