import { useNavigate } from 'react-router-dom';

import MenuItem from '@mui/material/MenuItem/MenuItem';
import ListItemIcon from '@mui/material/ListItemIcon';
import ListItemText from '@mui/material/ListItemText';
import { SxProps, Theme } from '@mui/material/styles';
import MenuItemDef from './menu-item-def';

interface Props {
  menuDef: MenuItemDef;
  onClick?: (menuDef: MenuItemDef) => void;

  /**
   * The system prop that allows defining system overrides as well as additional CSS styles.
   */
  sx?: SxProps<Theme>;
}

export default function AppMenuItem(props: Props) {
  const navigation = useNavigate();

  function handleClick() {
    if (props.onClick) {
      props.onClick(props.menuDef);
    }

    if (props.menuDef.action) {
      props.menuDef.action();
    }

    if (props.menuDef.route) {
      navigation(props.menuDef.route);
    }
  }

  return (
    <MenuItem onClick={ handleClick } sx={ props.sx || {} }>
      { props.menuDef.icon &&
        <ListItemIcon>
          { props.menuDef.icon }
        </ListItemIcon>
      }
      <ListItemText primary={ props.menuDef.label} />
    </MenuItem>
  );
}
