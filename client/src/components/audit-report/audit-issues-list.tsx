import { useEffect, useRef, useState } from 'react';

import { useMessageDispatch } from '../../contexts/messages/context';
import AuditReportService from '../../services/audit-report.service';
import { AuditIssue, AuditReportConfig } from '../../model/audit-report.model';
import CurrencyService from '../../services/currency.service';
import { formatDate } from "../../util/date-util";

import Switch from '@mui/material/Switch';
import PaginatedList, { EMPTY_PAGINATED_LIST } from '../../model/paginated-list.model';
import { DataGrid, GridColDef, getGridNumericOperators, getGridStringOperators, getGridDateOperators, GridToolbar, GridRenderCellParams } from '@mui/x-data-grid';

interface Props {
  reportConfig: AuditReportConfig;
}

const DEFAULT_PAGE_SIZE = 20;

interface IssueUpdate {
  issue: AuditIssue;
  acknowledged: boolean;
}


export default function AuditIssuesList(props: Props) {
  const showMessage = useMessageDispatch();
  const [loading, setLoading] = useState(false);
  const [ paginationModel, setPaginationModel ] = useState({ page: 0, pageSize: DEFAULT_PAGE_SIZE });
  const [ issues, setIssues ] = useState<PaginatedList<AuditIssue>>(EMPTY_PAGINATED_LIST);
  useEffect(() => {
    setLoading(true);
    AuditReportService.getAuditIssues(props.reportConfig.id!, undefined, paginationModel.page, paginationModel.pageSize)
      .then(page => setIssues(page))
      .catch(err => showMessage(err))
      .finally(() => setLoading(false));
  }, [ showMessage, props.reportConfig.id, paginationModel ]);

  const [ issueUpdate, setIssueUpdate ] = useState<IssueUpdate>();
  useEffect(() => {
    if (issueUpdate) {
      AuditReportService.updateAuditIssue(issueUpdate.issue.issueId, issueUpdate.acknowledged)
        .then(update => {
          setIssueUpdate(undefined);

          showMessage({ type: 'add', level: 'success', text: `Issue ${update.additionalInformation} ${update.acknowledged ? "acknowledged" : "reverted"}`});
          const newList = issues.items.map(entry => { return (entry.issueId === update.issueId) ? update : entry; });
          const newPage = ({ ...issues, items: newList });
          setIssues(newPage);
        })
        .catch(err => showMessage(err));
    }
  }, [ showMessage, issueUpdate, issues ]);

  function updateAcknowledged(issue: AuditIssue, value: boolean) {
    setIssueUpdate({ issue: issue, acknowledged: value});
  };

  const columns = useRef<GridColDef<AuditIssue>[]>([
    { field: 'acknowledged',
      type: 'boolean',
      headerName: 'Ack',
      renderCell: (params: GridRenderCellParams<any, boolean>) => (
        <Switch checked={ params.value || false } onChange={ e => {
          updateAcknowledged(params.row, e.target.checked);
        }}/>
      )
    },
    {
      field: 'bookingDateTime',
      type: 'date',
      headerName: 'Date',
      width: 110,
      filterOperators: getGridDateOperators().filter((op) => op.value === 'before' || op.value === 'onOrAfter'),
      valueFormatter: (value: string) => formatDate(value),
      valueGetter: (value: string) => new Date(value)
    },
    {
      field: 'additionalInformation',
      headerName: 'Additional Info',
      flex: 1,
      width: 380,
      filterOperators: getGridStringOperators().filter((op) => op.value === 'contains')
    },
    {
      field: 'creditorName',
      headerName: 'Creditor',
      flex: 0.5,
      width: 200,
      filterOperators: getGridStringOperators().filter((op) => op.value === 'contains')
    },
    {
      field: 'reference',
      headerName: 'Reference',
      flex: 0.5,
      width: 200,
      filterOperators: getGridStringOperators().filter((op) => op.value === 'contains')
    },
    {
      field: 'debit',
      type: 'number',
      headerName: 'Debit',
      headerClassName: 'colhead',
      headerAlign: 'right',
      align: 'right',
      width: 130,
      filterOperators: getGridNumericOperators().filter((op) => op.value === '>='),
      valueFormatter: (value: any, row: AuditIssue) => row.amount < 0 ? CurrencyService.format(0 - row.amount, row.currency) : '',
      valueGetter: (value: any, row: AuditIssue) => 0 - row.amount
    },
    {
      field: 'credit',
      type: 'number',
      headerName: 'Credit',
      headerAlign: 'right',
      align: 'right',
      width: 130,
      filterOperators: getGridNumericOperators().filter((op) => op.value === '>='),
      valueFormatter: (value: any, row: AuditIssue) => row.amount >= 0 ? CurrencyService.format(row.amount, row.currency) : '',
      valueGetter: (value: any, row: AuditIssue) => row.amount
    },
  ]);

  return (
    <DataGrid rows={ issues.items } rowCount={ issues.total } columns={ columns.current } 
      density="compact" disableDensitySelector disableRowSelectionOnClick
      loading={ loading } slots={{ toolbar: GridToolbar }}
      pagination paginationModel={ paginationModel }
      pageSizeOptions={[ 5, 15, DEFAULT_PAGE_SIZE, 50, 100 ]}
      paginationMode="server" onPaginationModelChange={ setPaginationModel }
    />
  );
};