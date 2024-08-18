import PageHeader from "../components/page-header/page-header";
import AuditReportResults from "../components/audit-report/audit-results";

export default function AuditIssues() {
  return (
    <PageHeader title="Audit Report Issues Found">
      <AuditReportResults />
    </PageHeader>
  );
}
