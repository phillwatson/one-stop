import React from "react";
import {
  Nav,
  NavLink,
  Bars,
  NavMenu,
  NavBtn,
  NavBtnLink
} from "./NavbarElements";

export default function Navbar() {
  return (
    <Nav>
    <Bars />
    <NavMenu>
      <NavLink to="/about">About</NavLink>
      <NavLink to="/institutions">Institutions</NavLink>
      <NavLink to="/sign-up">Sign Up</NavLink>
    </NavMenu>
    <NavBtn>
      <NavBtnLink to="/sign-in">Sign In</NavBtnLink>
    </NavBtn>
    </Nav>
  );
};

export default Navbar;
