import {
  createHashRouter,
  RouterProvider,
} from "react-router-dom";

import MainPage from './pages/main';
import Accounts from "./pages/accounts";
import UpdateProfile from "./pages/profile";
import NotificationProvider from "./contexts/notification-context";
import UserProfileProvider from "./contexts/user-context";
import OnboardUser from "./pages/onboard-user";

const router = createHashRouter([
  {
    path: "onboard-user",
    element: <OnboardUser />
  },
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
        path: "profile",
        element: <UpdateProfile />
      },
    ],
  },
]);

export default function App() {
  return (
    <NotificationProvider>
      <UserProfileProvider>
        <RouterProvider router={router} />
      </UserProfileProvider>
    </NotificationProvider>
  );
}
