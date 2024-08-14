import { useState, useEffect } from 'react';

import Table from '@mui/material/Table';
import TableBody from '@mui/material/TableBody';
import TableCell from '@mui/material/TableCell';
import TableContainer from '@mui/material/TableContainer';
import TableHead from '@mui/material/TableHead';
import TableRow from '@mui/material/TableRow';
import Tooltip from '@mui/material/Tooltip';
import DeleteIcon from '@mui/icons-material/DeleteOutline';
import { SxProps } from '@mui/material/styles';

import { useMessageDispatch } from '../../contexts/messages/context';
import AuditReportService from "../../services/audit-report.service";
import { AuditReportConfig } from '../../model/audit-report.model';
import ConfirmationDialog from "../../components/dialogs/confirm-dialog";

interface Props {
  onEdit?: (config: AuditReportConfig) => void;
  onAdd?: () => void;
}

const colhead: SxProps = {
  fontWeight: 'bold'
};

const compactCell = {
  paddingLeft: 0.5,
  paddingRight: 0
};

export default function AuditReportConfigList(props: Props) {
  const showMessage = useMessageDispatch();

  const [ reportConfigs, setReportConfigs ] = useState<Array<AuditReportConfig>>([]);

  useEffect(() => {
    AuditReportService.fetchAllConfigs()
      .then(response => setReportConfigs(response))
      .catch(err => showMessage(err));
  }, [ showMessage ]);

  const [ selectedConfig, setSelectedConfig ] = useState<AuditReportConfig|undefined>(undefined);
  const [ deleteDialogOpen, setDeleteDialogOpen ] = useState<boolean>(false);

  function handleEditClick(config: AuditReportConfig) {
    if (props.onEdit) {
      props.onEdit(config);
    }
  }
  
  function handleAddClick() {
    if (props.onAdd) {
      props.onAdd();
    }
  }
  
  function handleDeleteClick(config: AuditReportConfig) {
    setSelectedConfig(config);
    setDeleteDialogOpen(true);
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
    <>
    <TableContainer>
      <Table size='small'>
        <TableHead>
          <TableRow>
            <TableCell sx={colhead}>Name</TableCell>
            <TableCell sx={colhead}>Description</TableCell>
            <TableCell sx={{...colhead, ...compactCell}} width="24px"></TableCell>
          </TableRow>
        </TableHead>
        <TableBody>
          { reportConfigs && reportConfigs
            .sort((a, b) => a.name < b.name ? -1 : 1)
            .map(config =>
              <TableRow key={ config.id } hover>
                <TableCell onClick={() => handleEditClick(config)}>{ config.name }</TableCell>
                <TableCell onClick={() => handleEditClick(config)}>{ config.description }</TableCell>
                <TableCell onClick={() => handleDeleteClick(config)} sx={ compactCell } width="24px">
                  <Tooltip title="Delete report..."><DeleteIcon/></Tooltip>
                </TableCell>
              </TableRow>
            )
          }

          { props.onAdd &&
          <>
            <TableRow key={ "add1" }>
              <TableCell align="center" colSpan={ 3 } />
            </TableRow>
            <TableRow key="add2" hover>
              <TableCell align="center" colSpan={ 3 }
                onClick={() => handleAddClick()}><b>Add Report...</b></TableCell>
            </TableRow>
          </>
          }
        </TableBody>
      </Table>
    </TableContainer>

    <ConfirmationDialog open={deleteDialogOpen}
      title={"Delete Report Configuration \""+ selectedConfig?.name + "\""}
      content="Are you sure you want to delete this report configuration?"
      onConfirm={ onDeleteConfirmed }
      onCancel={() => setDeleteDialogOpen(false)} />

    </>
  );
}