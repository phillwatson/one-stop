import MenuList from '@mui/material/MenuList';

import AppMenuItem from './app-menu-item';
import MenuItemDef from './menu-item-def';

interface Props  {
  menuItems?: MenuItemDef[]
}

export default function AppMenu(props: Props) {
  return (
    <MenuList>
      { props.menuItems && props.menuItems.map((item, index) => 
        <AppMenuItem key={ index } sx={{ padding: 2 }} menuDef={ item }/>
      )}
    </MenuList>
  );
}
