import React from "react";
import {
  Outlet
} from "react-router-dom";

import { styled } from '@mui/material/styles';
import Box from '@mui/material/Box';
import Logout from '@mui/icons-material/Logout';
import AccountBalance from '@mui/icons-material/AccountBalance';
import Person from '@mui/icons-material/Person';
import Savings from '@mui/icons-material/Savings';

import AppHeader from "../components/app-header/app-header";
import SideBar from '../components/side-bar/side-bar';
import { AppMenu, AppMenuItem } from "../components/app-menu";
import { MenuItem } from "../components/app-menu/app-menu-item";

const appTitle = "One Stop";
const drawerWidth = 240;

const menuItems: MenuItem[] = [
 { label: 'Accounts', route: 'accounts', icon: <Savings/> },
 { label: 'Institutions', route: 'institutions', icon: <AccountBalance/> },
 { label: 'Profile', route: 'profiles', icon: <Person/> },
 { label: 'Logout', route: 'sign-in', icon: <Logout/> }
];

const Main = styled('main', { shouldForwardProp: (prop) => prop !== 'open' && prop !== 'drawerWidth' })
  <{ open?: boolean, drawerWidth: number; }>
(({ theme, open }) => ({
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

export default function MainPage() {
  const [open, setOpen] = React.useState(true);

  const handleDrawerOpen = () => {
    setOpen(true);
  };

  const handleDrawerClose = () => {
    setOpen(false);
  };

  return (
    <Box sx={{ display: 'flex' }}>
      <AppHeader drawerWidth={ drawerWidth } open={ open } onClick={ handleDrawerOpen } title={ appTitle }/>
      <SideBar drawerWidth={ drawerWidth } open={open} onClose={ handleDrawerClose }>
        <AppMenu>
          {menuItems && menuItems.map((item, index) => 
            <AppMenuItem key={ index } { ...item }/>
          )}
        </AppMenu>
      </SideBar>
      <Main open={ open } drawerWidth={ drawerWidth } style={{ paddingTop: "90px" }}>
        <Outlet />
      </Main>
    </Box>
  );
}
