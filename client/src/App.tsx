import React from "react";
import {
  createBrowserRouter,
  RouterProvider,
  Outlet
} from "react-router-dom";

import ToolBar from "./components/tool-bar";
import Home from "./pages";
import About from "./pages/about";
import Institutions from "./pages/institutions";
import SignIn from "./pages/sign-in";

function HeaderLayout() {
  return (
    <>
      <header>
        <ToolBar />
      </header>
      <Outlet />
    </>
  );
}

const router = createBrowserRouter([
  {
    path: "/",
    element: <HeaderLayout />,
    children: [
      {
        path: "/",
        element: <Home />
      },
      {
        path: "about",
        element: <About />
      },
      {
        path: "institutions",
        element: <Institutions />
      },
      {
        path: "sign-in",
        element: <SignIn />
      },
    ],
  },
]);

export default function App() {
  return (
    <RouterProvider router={router} />
  );
}
