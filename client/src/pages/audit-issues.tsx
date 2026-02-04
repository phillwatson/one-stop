import AuditIssuesIcon from '@mui/icons-material/GppMaybe';

import PageHeader from "../components/page-header/page-header";
import AuditReportResults from "../components/audit-report/audit-results";

export default function AuditIssues() {
  return (
    <PageHeader title="Audit Report Issues Found" icon={ <AuditIssuesIcon /> }>
      <AuditReportResults />
    </PageHeader>
  );
}
