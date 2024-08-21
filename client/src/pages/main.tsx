import React, { useCallback, useMemo } from "react";
import { Outlet } from "react-router-dom";

import Box from '@mui/material/Box';
import Logout from '@mui/icons-material/Logout';
import Person from '@mui/icons-material/Person';
import Savings from '@mui/icons-material/Savings';
import CategoryIcon from '@mui/icons-material/Category';
import PieChartIcon from '@mui/icons-material/PieChart';
import AuditReportIcon from '@mui/icons-material/VerifiedUser';
import AuditIssuesIcon from '@mui/icons-material/GppMaybe';

import { useCurrentUser } from "../contexts/user-context";
import AppHeader from "../components/app-header/app-header";
import SideBar from '../components/side-bar/side-bar';
import { AppMenu } from "../components/app-menu";
import MenuItemDef from "../components/app-menu/menu-item-def";
import ProfileService from "../services/profile.service";
import SignIn from "./sign-in";

const appTitle = "One Stop";

export default function MainPage() {
  const [user, setUser] = useCurrentUser();
  const [menuOpen, setMenuOpen] = React.useState(false);

  const handleDrawerOpen = () => {
    setMenuOpen(true);
  };

  const handleDrawerClose = () => {
    setMenuOpen(false);
  };

  const logout = useCallback(() => {
    handleDrawerClose();
    ProfileService.logout()
      .finally(() => setUser(undefined) );
  }, [ setUser ]);

  const mainMenuItems: MenuItemDef[] = useMemo(() => {
    return [
      { label: 'Accounts', route: 'accounts', icon: <Savings/>, action: handleDrawerClose },
      { label: 'Categories', route: 'categories', icon: <CategoryIcon/>, action: handleDrawerClose },
      { label: 'Statistics', route: 'statistics', icon: <PieChartIcon/>, action: handleDrawerClose },
      { label: 'Audit Reports', route: 'reports/audit/configs', icon: <AuditReportIcon/>, action: handleDrawerClose },
      { label: 'Audit Issues', route: 'reports/audit/issues', icon: <AuditIssuesIcon/>, action: handleDrawerClose },
      { label: 'Logout', route: '/', icon: <Logout/>, action: logout }
    ];
  }, [ logout ]);

  const profileMenuItems: MenuItemDef[] = useMemo(() => {
    return [
      { label: 'Profile', route: 'profile', icon: <Person/> },
      { label: 'Logout', route: '/', icon: <Logout/>, action: logout }
    ];
  }, [ logout ]);

  if (!user) {
    return (<SignIn/>);
  }

  return (
    <Box padding={{ xs: 1, sm: 2 }}>
      <AppHeader onClick={ handleDrawerOpen } title={ appTitle } menuItems={ profileMenuItems }/>
      <SideBar open={ menuOpen } onClose={ handleDrawerClose }>
        <AppMenu menuItems={ mainMenuItems } />
      </SideBar>
      <Box style={{ paddingTop: "60px" }}>
        <Outlet />
      </Box>
    </Box>
  );
}
