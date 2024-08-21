import { useState } from 'react';

import Avatar from '@mui/material/Avatar';
import Box from '@mui/material/Box';

import UserProfile from '../../model/user-profile.model';
import AppMenuItem from '../app-menu/app-menu-item';
import MenuItemDef from '../app-menu/menu-item-def';
import Menu from '@mui/material/Menu';
import Paper from '@mui/material/Paper';
import { SlotComponentProps } from '@mui/material/utils/types';

function getName(user?: UserProfile): string | undefined {
  if (!user) {
    return undefined;
  }

  if (user.preferredName) {
    return user.preferredName;
  }

  if (user.givenName) {
    return user.givenName;
  }

  return user.username;
}

function stringToColor(value: string) {
  let hash = 0;

  /* eslint-disable no-bitwise */
  for (let i = 0; i < value.length; i += 1) {
    hash = value.charCodeAt(i) + ((hash << 5) - hash);
  }

  let color = '#';
  for (let i = 0; i < 3; i += 1) {
    const value = (hash >> (i * 8)) & 0xff;
    color += `00${value.toString(16)}`.slice(-2);
  }
  /* eslint-enable no-bitwise */

  return color;
}

const MenuContainer: SlotComponentProps<typeof Paper, {}, {}> = {
  elevation: 0,
  sx: {
    overflow: 'visible',
    filter: 'drop-shadow(0px 2px 8px rgba(0,0,0,0.32))',
    mt: 1.5,
    '& .MuiAvatar-root': { width: 32, height: 32, ml: -0.5, mr: 1, },
    '&::before': {
      content: '""',
      display: 'block',
      position: 'absolute',
      top: 0,
      right: 14,
      width: 10,
      height: 10,
      bgcolor: 'background.paper',
      transform: 'translateY(-50%) rotate(45deg)',
      zIndex: 0
    }
  }
}

function avaterProps(name?: string) {
  if (! name) {
    return {};
  }
  return {
    sx: { bgcolor: stringToColor(name), width: 38, height: 38 },
    children: `${ name[0] }`
  };
}

interface Props {
  user?: UserProfile;
  menuItems?: MenuItemDef[];
}

export default function UserAvatar(props: Props) {
  const [anchorEl, setAnchorEl] = useState<null | HTMLElement>(null);
  const open = Boolean(anchorEl);

  function openMenu(event: React.MouseEvent<HTMLElement>) {
    setAnchorEl(event.currentTarget);
  };

  function closeMenu() {
    setAnchorEl(null);
  }

  return (
    <Box>
      <Avatar {...avaterProps(getName(props.user))} role={ getName(props.user) } onClick={ openMenu }/>
 
      { props.menuItems && props.menuItems.length > 0 &&
        <Menu id="profile-menu"
          anchorEl={anchorEl} open={open} onClose={ closeMenu } onClick={ closeMenu }
          transformOrigin={{ horizontal: 'right', vertical: 'top' }}
          anchorOrigin={{ horizontal: 'right', vertical: 'bottom' }}
          slotProps={{ paper: MenuContainer }}
        >
        { props.menuItems && props.menuItems.map((item, index) => 
          <AppMenuItem key={ index } menuDef={ item } onClick={ closeMenu }/>
        )}
      </Menu>
      }
    </Box>
  );
}