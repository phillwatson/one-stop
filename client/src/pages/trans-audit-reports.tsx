import { useEffect, useState } from 'react';

import Paper from '@mui/material/Paper';

import PageHeader from "../components/page-header/page-header";

import { useMessageDispatch } from "../contexts/messages/context";
import AuditReportConfigList from "../components/audit-report/audit-config-list";
import EditAuditReportConfig from "../components/audit-report/audit-config";
import ConfirmationDialog from "../components/dialogs/confirm-dialog";

import AuditReportService from "../services/audit-report.service";
import { AuditReportConfig } from '../model/audit-report.model';

export default function TransactionAuditReports() {
  const showMessage = useMessageDispatch();
  const [ reportConfigs, setReportConfigs ] = useState<Array<AuditReportConfig>>([]);
  const [ selectedConfig, setSelectedConfig ] = useState<AuditReportConfig|undefined>(undefined);
  const [ editDialogOpen, setEditDialogOpen ] = useState<boolean>(false);
  const [ deleteDialogOpen, setDeleteDialogOpen ] = useState<boolean>(false);

  useEffect(() => {
    AuditReportService.fetchAllConfigs()
      .then(response => setReportConfigs(response))
      .catch(err => showMessage(err));
  }, [ showMessage ]);

  function handleEditClick(config: AuditReportConfig) {
    setSelectedConfig(config);
    setEditDialogOpen(true);
  }
  
  function handleAddClick() {
    setSelectedConfig(undefined);
    setEditDialogOpen(true);
  }

  function handleDeleteClick(config: AuditReportConfig) {
    setSelectedConfig(config);
    setDeleteDialogOpen(true);
  }

  function onUpdateConfirmed(config: AuditReportConfig) {
    const callService = (config.id)
      ? AuditReportService.updateAuditConfig(config)
      : AuditReportService.createAuditConfig(config);

    callService.then(updatedConfig => {
      setEditDialogOpen(false);
      setReportConfigs(reportConfigs
        .filter(c => c.id !== updatedConfig.id)
        .concat(updatedConfig)
        .sort((a, b) => a.name.localeCompare(b.name))
      );
    })
    .catch(err => showMessage(err))
  }

  function onDeleteConfirmed() {
    setDeleteDialogOpen(false);

    var config: AuditReportConfig = selectedConfig!
    AuditReportService.deleteAuditConfig(config.id!!)
      .then(() => {
        setDeleteDialogOpen(false);
        setReportConfigs(reportConfigs.filter(c => c.id !== config.id));
      })
      .catch(err => showMessage(err))
  }

  return (
    <PageHeader title="Transaction Audit Reports">
      <Paper elevation={ 3 } sx={{ padding: 1}}>
      {! editDialogOpen &&
        <>
          <AuditReportConfigList
            reportConfigs={ reportConfigs }
            onAdd={ handleAddClick }
            onEdit={ handleEditClick }
            onDelete={ handleDeleteClick } />
    
          <ConfirmationDialog open={deleteDialogOpen}
            title={"Delete Report Configuration \""+ selectedConfig?.name + "\""}
            content="Are you sure you want to delete this report configuration?"
            onConfirm={ onDeleteConfirmed }
            onCancel={() => setDeleteDialogOpen(false)} />
        </>
      }
      { editDialogOpen &&
        <EditAuditReportConfig
          reportConfig={ selectedConfig }
          onSave={ onUpdateConfirmed } />
      }
      </Paper>
    </PageHeader>
  );
}
