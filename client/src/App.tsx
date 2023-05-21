import {
  createBrowserRouter,
  RouterProvider,
} from "react-router-dom";

import MainPage from './pages/main';
import SignIn from "./pages/sign-in";
import Institutions from "./pages/institutions";
import Accounts from "./pages/accounts";
import UpdateProfile from "./pages/profile";
import ErrorsProvider from "./contexts/error-context";

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
    <div>
      <ErrorsProvider>
        <RouterProvider router={router} />
      </ErrorsProvider>
    </div>
  );
}
