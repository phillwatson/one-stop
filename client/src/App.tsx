import {
  createBrowserRouter,
  RouterProvider,
} from "react-router-dom";

import MainPage from './pages/main';
import Institutions from "./pages/institutions";
import Accounts from "./pages/accounts";
import UpdateProfile from "./pages/profile";
import ErrorsProvider from "./contexts/error-context";
import UserProfileProvider from "./contexts/user-context";

const router = createBrowserRouter([
  {
    path: "/",
    element: <MainPage />,
    children: [
      {
        path: "/",
        element: <div />
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
        <UserProfileProvider>
          <RouterProvider router={router} />
        </UserProfileProvider>
      </ErrorsProvider>
    </div>
  );
}
