import { useNavigate } from "react-router-dom";

import Paper from '@mui/material/Paper';

import PageHeader from "../components/page-header/page-header";
import AuditIssuesList from "../components/audit-report/audit-config-list";

export default function TransactionAuditReports() {
  const navigate = useNavigate();

  return (
    <PageHeader title="Transaction Audit Results">
      <Paper elevation={ 3 } sx={{ padding: 1}}>
        <AuditIssuesList />
      </Paper>
    </PageHeader>
  );
}
