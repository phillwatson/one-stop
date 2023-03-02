import { NavLink } from "react-router-dom";
import './tool-bar.css';

export default function ToolBar() {
  return (
    <nav className="toolbar">
      <NavLink className="item" to="sign-in">Sign In</NavLink>
      <NavLink className="item" to="about">About</NavLink>
      <NavLink className="item" to="accounts">Accounts</NavLink>
    </nav>
  );
};
