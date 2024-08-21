import { useNavigate } from "react-router-dom";

import PageHeader from "../components/page-header/page-header";
import AuditReportConfigList from "../components/audit-report/audit-config-list";
import { AuditReportConfig } from '../model/audit-report.model';

export default function TransactionAuditReports() {
  const navigate = useNavigate();

  function handleEditConfig(config: AuditReportConfig) {
    navigate(`/reports/audit/configs/${config.id}`);
  }
  
  function handleAddConfig() {
    navigate("/reports/audit/configs/add");
  }

  return (
    <PageHeader title="Transaction Audit Reports">
      <AuditReportConfigList
        onAdd={ handleAddConfig }
        onEdit={ handleEditConfig } />
    </PageHeader>
  );
}
