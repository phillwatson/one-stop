import {
  createBrowserRouter,
  RouterProvider,
} from "react-router-dom";

import MainPage from './pages/main';
import Accounts from "./pages/accounts";
import UpdateProfile from "./pages/profile";
import NotificationProvider from "./contexts/notification-context";
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
        path: "profile",
        element: <UpdateProfile />
      },
    ],
  },
]);

export default function App() {
  return (
    <div>
      <NotificationProvider>
        <UserProfileProvider>
          <RouterProvider router={router} />
        </UserProfileProvider>
      </NotificationProvider>
    </div>
  );
}
