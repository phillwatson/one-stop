import { useNavigate } from "react-router-dom";

import PageHeader from "../components/page-header/page-header";
import AuditIssuesList from "../components/audit-report/audit-config-list";

export default function TransactionAuditReports() {
  const navigate = useNavigate();

  return (
    <PageHeader title="Transaction Audit Results">
      <AuditIssuesList />
    </PageHeader>
  );
}
