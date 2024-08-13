import Table from '@mui/material/Table';
import TableBody from '@mui/material/TableBody';
import TableCell from '@mui/material/TableCell';
import TableContainer from '@mui/material/TableContainer';
import TableHead from '@mui/material/TableHead';
import TableRow from '@mui/material/TableRow';
import Tooltip from '@mui/material/Tooltip';
import DeleteIcon from '@mui/icons-material/DeleteOutline';
import { SxProps } from '@mui/material/styles';

import { AuditReportConfig } from '../../model/audit-report.model';

interface Props {
  reportConfigs: Array<AuditReportConfig>;
  onDelete?: (config: AuditReportConfig) => void;
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
    if (props.onDelete) {
      props.onDelete(config);
    }
  }

  return (
    <TableContainer>
      <Table size='small'>
        <TableHead>
          <TableRow>
            <TableCell sx={{...colhead, ...compactCell}} width="24px"></TableCell>
            <TableCell sx={colhead}>Name</TableCell>
            <TableCell sx={colhead}>Description</TableCell>
            { props.onDelete &&
              <TableCell sx={{...colhead, ...compactCell}} width="24px"></TableCell>
            }
          </TableRow>
        </TableHead>
        <TableBody>
          { props.reportConfigs && props.reportConfigs
            .sort((a, b) => a.name < b.name ? -1 : 1)
            .map(config =>
              <TableRow key={ config.id } hover>
                <TableCell onClick={() => handleEditClick(config)}>{ config.name }</TableCell>
                <TableCell onClick={() => handleEditClick(config)}>{ config.description }</TableCell>
                { props.onDelete &&
                  <TableCell onClick={() => handleDeleteClick(config)} sx={ compactCell } width="24px">
                    <Tooltip title="Delete report..."><DeleteIcon/></Tooltip>
                  </TableCell>
                }
              </TableRow>
            )
          }

          { props.onAdd &&
          <>
            <TableRow key={ "add1" }>
              <TableCell align="center" colSpan={ 3 + (props.onDelete ? 1 : 0) } />
            </TableRow>
            <TableRow key="add2" hover>
              <TableCell align="center" colSpan={ 3 + (props.onDelete ? 1 : 0) }
                onClick={() => handleAddClick()}><b>Add Report...</b></TableCell>
            </TableRow>
          </>
          }
        </TableBody>
      </Table>
    </TableContainer>
  );
}