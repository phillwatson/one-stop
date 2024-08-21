import { useRef, useState } from 'react';

import ClickAwayListener from '@mui/material/ClickAwayListener';
import Avatar from '@mui/material/Avatar';
import MenuList from '@mui/material/MenuList';
import Popper from '@mui/material/Popper';
import Grow from '@mui/material/Grow';
import Paper from '@mui/material/Paper';
import Box from '@mui/material/Box';

import UserProfile from '../../model/user-profile.model';
import AppMenuItem from '../app-menu/app-menu-item';
import MenuItemDef from '../app-menu/menu-item-def';

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

function stringAvatar(name?: string) {
  if (! name) {
    return {};
  }
  const nameParts = name.split(' ', 2);
  return {
    sx: { bgcolor: stringToColor(name), width: 38, height: 38 },
    children: `${ nameParts.map(p => p[0]) }`
  };
}

interface Props {
  user?: UserProfile;
  menuItems?: MenuItemDef[];
}

export default function UserAvatar(props: Props) {
  const anchorRef = useRef<null | HTMLElement>(null);
  const [open, setOpen] = useState(false);

  function toggleMenu() {
    setOpen(! open);
  }

  function closeMenu() {
    setOpen(false);
  }

  return (
    <Box ref={ anchorRef }>
      <Avatar {...stringAvatar(getName(props.user))} role={ getName(props.user) } onClick={ toggleMenu }/>
 
      { props.menuItems && props.menuItems.length > 0 &&
        <Popper placement="bottom-end" transition disablePortal
          open={open} anchorEl={anchorRef.current}>
          {({ TransitionProps }) => (
            <Grow {...TransitionProps} style={{ transformOrigin: 'left top' }} >
              <Paper>
                <ClickAwayListener onClickAway={ closeMenu }>
                  <MenuList id="user-menu" autoFocusItem={ open }>
                    { props.menuItems && props.menuItems.map((item, index) => 
                      <AppMenuItem key={ index } menuDef={ item } onClick={ closeMenu }/>
                    )}
                  </MenuList>
                </ClickAwayListener>
              </Paper>
            </Grow>
          )}
        </Popper>
      }
    </Box>
  );
}