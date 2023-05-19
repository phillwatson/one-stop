import React from "react";
import {
  createBrowserRouter,
  RouterProvider,
  Outlet
} from "react-router-dom";

import Box from '@mui/material/Box';
import AppHeader from "./components/app-header/app-header";
import SideBar from './components/side-bar/side-bar';

import SignIn from "./pages/sign-in";
import Institutions from "./pages/institutions";
import Accounts from "./pages/accounts";
import UpdateProfile from "./pages/profile";
import { AppMenu, AppMenuItem } from "./components/app-menu";
import { MenuItem } from "./components/app-menu/app-menu-item";
import Logout from '@mui/icons-material/Logout';
import AccountBalance from '@mui/icons-material/AccountBalance';
import Person from '@mui/icons-material/Person';
import Savings from '@mui/icons-material/Savings';
import MainPanel from "./components/main-panel/main-panel";

const appTitle = "One Stop";
const drawerWidth = 240;

const menuItems: MenuItem[] = [
 { label: 'Accounts', route: 'accounts', icon: <Savings/> },
 { label: 'Institutions', route: 'institutions', icon: <AccountBalance/> },
 { label: 'Profile', route: 'profiles', icon: <Person/> },
 { label: 'Logout', route: 'sign-in', icon: <Logout/> }
];

function MainPage() {
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
      <MainPanel drawerWidth={ drawerWidth } open={ open }>
        <Outlet />
      </MainPanel>
    </Box>
  );
}

const router = createBrowserRouter([
  {
    path: "/",
    element: <MainPage />,
    children: [
      {
        path: "/",
        element: <SignIn />
      },
      {
        path: "sign-in",
        element: <SignIn />
      },
      {
        path: "accounts",
        element: <Accounts />
      },
      {
        path: "institutions",
        element: <Institutions />
      },
      {
        path: "profiles",
        element: <UpdateProfile />
      },
    ],
  },
]);

export default function App() {
  return (
    <RouterProvider router={router} />
  );
}
