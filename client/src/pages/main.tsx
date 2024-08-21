import React, { useMemo } from "react";
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
import { AppMenu, AppMenuItem } from "../components/app-menu";
import { MenuItem } from "../components/app-menu/app-menu-item";
import ProfileService from "../services/profile.service";
import SignIn from "./sign-in";

const appTitle = "One Stop";

export default function MainPage() {
  const [menuOpen, setMenuOpen] = React.useState(true);

  const handleDrawerOpen = () => {
    setMenuOpen(true);
  };

  const handleDrawerClose = () => {
    setMenuOpen(false);
  };

  const [user, setUser] = useCurrentUser();

  const menuItems: MenuItem[] = useMemo(() => {
    function logout() {
      ProfileService.logout().finally(() => setUser(undefined) );
    };

    return [
      { label: 'Accounts', route: 'accounts', icon: <Savings/>, action: handleDrawerClose },
      { label: 'Categories', route: 'categories', icon: <CategoryIcon/>, action: handleDrawerClose },
      { label: 'Statistics', route: 'statistics', icon: <PieChartIcon/>, action: handleDrawerClose },
      { label: 'Audit Reports', route: 'reports/audit/configs', icon: <AuditReportIcon/>, action: handleDrawerClose },
      { label: 'Audit Issues', route: 'reports/audit/issues', icon: <AuditIssuesIcon/>, action: handleDrawerClose },
      { label: 'Profile', route: 'profile', icon: <Person/>, action: handleDrawerClose },
      { label: 'Logout', route: '', icon: <Logout/>, action: logout }
    ];
  }, [setUser]);
   

  if (!user) {
    return (<SignIn/>);
  }

  return (
    <Box padding={{ xs: 1, sm: 2 }}>
      <AppHeader onClick={ handleDrawerOpen } title={ appTitle }/>
      <SideBar open={ menuOpen } onClose={ handleDrawerClose }>
        <AppMenu>
          {menuItems && menuItems.map((item, index) => 
            <AppMenuItem key={ index } { ...item }/>
          )}
        </AppMenu>
      </SideBar>
      <Box style={{ paddingTop: "60px" }}>
        <Outlet />
      </Box>
    </Box>
  );
}
