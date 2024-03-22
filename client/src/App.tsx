import {
  createBrowserRouter,
  redirect,
  RouterProvider,
} from "react-router-dom";

import MainPage from './pages/main';
import Accounts from "./pages/accounts";
import UpdateProfile from "./pages/profile";
import MessageProvider from "./contexts/messages/context";
import UserProfileProvider from "./contexts/user-context";
import NewUser from "./pages/new-user";
import OnboardUser from "./pages/onboard-user";
import Transactions from "./pages/transactions";
import Graph from "./pages/graphs";

const router = createBrowserRouter([
  {
    path: "new-user",
    element: <NewUser />
  },
  {
    path: "onboard-user",
    element: <OnboardUser />,
    loader: async ({request}) => {
      const url = new URL(request.url);
      const token = url.searchParams.get("token");
      if (!token) {
        alert("No token provided.")
        return redirect("/");
      }
      return null;
    }
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
        path: "accounts/:accountId/transactions",
        element: <Transactions />
      },
      {
        path: "accounts/:accountId/graph",
        element: <Graph />
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
    <MessageProvider>
      <UserProfileProvider>
        <RouterProvider router={router} />
      </UserProfileProvider>
    </MessageProvider>
  );
}
