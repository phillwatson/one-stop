import { createBrowserRouter, redirect, RouterProvider } from "react-router-dom";

import { MonetaryFormatProvider } from "./contexts/monetary/monetary-context";
import MessageProvider from "./contexts/messages/context";
import NotificationProvider from "./contexts/notifications/context";
import UserProfileProvider from "./contexts/user-context";
import { ReconcileTransactionsProvider } from "./components/reconciliation/reconcile-transactions-context";

import MainPage from './pages/main';
import Accounts from "./pages/accounts";
import UpdateProfile from "./pages/profile";
import NewUser from "./pages/new-user";
import OnboardUser from "./pages/onboard-user";
import Transactions from "./pages/transactions";
import Graph from "./pages/graphs";
import Categories from "./pages/categories";
import StatisticsPage from "./pages/statistics";
import TransactionAuditReports from "./pages/trans-audit-config-list";
import TransactionAuditReportConfig from "./pages/trans-audit-config";
import AuditIssues from "./pages/audit-issues";
import SharePrices from "./pages/share-prices";
import SharePortfolios from "./pages/share-portfolios";

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
        path: "categories",
        element: <Categories />
      },
      {
        path: "statistics",
        element: <StatisticsPage />
      },
      {
        path: "reports/audit/configs",
        element: <TransactionAuditReports />
      },
      {
        path: "reports/audit/configs/:reportConfigId",
        element: <TransactionAuditReportConfig />
      },
      {
        path: "reports/audit/configs/add",
        element: <TransactionAuditReportConfig />
      },
      {
        path: "reports/audit/issues",
        element: <AuditIssues />
      },
      {
        path: 'shares/prices',
        element: <SharePrices />
      },
      {
        path: 'shares/portfolios',
        element: <SharePortfolios />
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
    <MonetaryFormatProvider>
      <MessageProvider>
        <UserProfileProvider>
          <NotificationProvider>
            <ReconcileTransactionsProvider>
              <RouterProvider router={router} />
            </ReconcileTransactionsProvider>
          </NotificationProvider>
        </UserProfileProvider>
      </MessageProvider>
    </MonetaryFormatProvider>
  );
}
