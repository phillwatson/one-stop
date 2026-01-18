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
import SharePricesIcon from '@mui/icons-material/QueryStats';

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

  function openMenu() {
    setMenuOpen(true);
  };

  function closeMenu() {
    setMenuOpen(false);
  };

  const logout = useCallback(() => {
    closeMenu();
    ProfileService.logout()
      .finally(() => setUser(undefined) );
  }, [ setUser ]);

  const mainMenuItems: MenuItemDef[] = useMemo(() => ([
      { label: 'Accounts', route: 'accounts', icon: <Savings/>, action: closeMenu },
      { label: 'Categories', route: 'categories', icon: <CategoryIcon/>, action: closeMenu },
      { label: 'Statistics', route: 'statistics', icon: <PieChartIcon/>, action: closeMenu },
      { label: 'Audit Reports', route: 'reports/audit/configs', icon: <AuditReportIcon/>, action: closeMenu },
      { label: 'Audit Issues', route: 'reports/audit/issues', icon: <AuditIssuesIcon/>, action: closeMenu },
      { label: 'Share Prices', route: 'shares/prices', icon: <SharePricesIcon />, action: closeMenu },
      { label: 'Logout', route: '/', icon: <Logout/>, action: logout }
    ]), [ logout ]);

  const profileMenuItems: MenuItemDef[] = useMemo(() => ([
      { label: 'Profile', route: 'profile', icon: <Person/> },
      { label: 'Logout', route: '/', icon: <Logout/>, action: logout }
    ]), [ logout ]);

  return (!user)
    ? (<SignIn/>)
    : (
      <Box padding={{ xs: 1, sm: 2 }}>
        <AppHeader onClick={ openMenu } title={ appTitle } menuItems={ profileMenuItems }/>
        <SideBar open={ menuOpen } onClose={ closeMenu }>
          <AppMenu menuItems={ mainMenuItems } />
        </SideBar>
        <Box style={{ paddingTop: "60px" }}>
          <Outlet />
        </Box>
      </Box>
    );
}
