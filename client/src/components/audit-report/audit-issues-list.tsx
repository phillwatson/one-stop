import { useCallback, useMemo, useEffect, useState } from 'react';

import { useMessageDispatch } from '../../contexts/messages/context';
import useMonetaryContext from '../../contexts/monetary/monetary-context';
import AuditReportService from '../../services/audit-report.service';
import { AuditIssue, AuditReportConfig } from '../../model/audit-report.model';
import { formatDate } from "../../util/date-util";

import Switch from '@mui/material/Switch';
import PaginatedList, { EMPTY_PAGINATED_LIST } from '../../model/paginated-list.model';
import { DataGrid, GridColDef, GridRenderCellParams } from '@mui/x-data-grid';

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
  const [ formatMoney ] = useMonetaryContext();

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

  const renderMoneyCell = useCallback((value: any, row: AuditIssue, column: GridColDef) => {
    return column.field === 'credit'
      ? row.amount > 0 ? formatMoney(row.amount, row.currency) : ''
      : row.amount < 0 ? formatMoney(0 - row.amount, row.currency) : ''
  }, [ formatMoney ] );

  function getMoneyValue(value: number, _row: AuditIssue, column: GridColDef) {
    return column.field === 'credit' ? value : 0 - value;
  };

  function getDateValue(value: string) {
    return new Date(value);
  };

  const columnDefs = useMemo(() => ([
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
      valueFormatter: formatDate,
      valueGetter: getDateValue
    },
    {
      field: 'additionalInformation',
      headerName: 'Additional Info',
      flex: 1,
      width: 380
    },
    {
      field: 'creditorName',
      headerName: 'Creditor',
      flex: 0.5,
      width: 200
    },
    {
      field: 'reference',
      headerName: 'Reference',
      flex: 0.5,
      width: 200
    },
    {
      field: 'debit',
      type: 'number',
      headerName: 'Debit',
      headerClassName: 'colhead',
      headerAlign: 'right',
      align: 'right',
      width: 130,
      valueFormatter: renderMoneyCell,
      valueGetter: getMoneyValue
    },
    {
      field: 'credit',
      type: 'number',
      headerName: 'Credit',
      headerAlign: 'right',
      align: 'right',
      width: 130,
      valueFormatter: renderMoneyCell,
      valueGetter: getMoneyValue
    },
  ] as GridColDef<AuditIssue>[]), [ renderMoneyCell ]);

  return (
    <DataGrid rows={ issues.items } rowCount={ issues.total } columns={ columnDefs } 
      density="compact" showToolbar loading={ loading }
      disableDensitySelector disableRowSelectionOnClick disableColumnFilter
      pagination paginationModel={ paginationModel }
      pageSizeOptions={[ 5, 15, DEFAULT_PAGE_SIZE, 50, 100 ]}
      paginationMode="server" onPaginationModelChange={ setPaginationModel }
    />
  );
};