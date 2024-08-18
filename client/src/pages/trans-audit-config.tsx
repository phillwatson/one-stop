import { useEffect, useState } from 'react';
import { useNavigate, useParams } from 'react-router-dom';

import Paper from '@mui/material/Paper';

import { useMessageDispatch } from '../contexts/messages/context';
import AuditReportService from "../services/audit-report.service";
import { AuditReportConfig } from '../model/audit-report.model';
import PageHeader from '../components/page-header/page-header';
import EditAuditReportConfig from "../components/audit-report/edit-audit-config";


export default function TransactionAuditReportConfig() {
  const { reportConfigId } = useParams();
  const navigate = useNavigate();

  const showMessage = useMessageDispatch();
  const [ reportConfig, setReportConfig ] = useState<AuditReportConfig|undefined>();

  useEffect(() => {
    if (reportConfigId) {
      AuditReportService.getAuditConfig(reportConfigId)
        .then(response => setReportConfig(response))
        .catch(err => showMessage(err));
    }
  }, [ showMessage, reportConfigId ]);

  function handleSubmit(reportConfig: AuditReportConfig) {
    navigate('/reports/audit/configs');
  }

  function handleCancel() {
    navigate('/reports/audit/configs');
  }

  return (
    <PageHeader title="Transaction Audit Report Configuration" >
      <Paper elevation={ 3 } sx={{ padding: 1}}>
        <EditAuditReportConfig reportConfig={ reportConfig } onSubmit={ handleSubmit } onCancel={ handleCancel } />
      </Paper>
    </PageHeader>
  )
}