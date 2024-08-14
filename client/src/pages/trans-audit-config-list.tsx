import { useNavigate } from "react-router-dom";

import Paper from '@mui/material/Paper';

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
      <Paper elevation={ 3 } sx={{ padding: 1}}>
        <AuditReportConfigList
          onAdd={ handleAddConfig }
          onEdit={ handleEditConfig } />
      </Paper>
    </PageHeader>
  );
}
