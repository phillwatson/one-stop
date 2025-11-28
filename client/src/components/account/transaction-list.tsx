import { useCallback, useEffect, useMemo, useState } from "react";

import { DataGrid, GridColDef, getGridNumericOperators, getGridStringOperators,
   getGridDateOperators, GridToolbar, GridFilterModel, GridRowParams, GridRowClassNameParams } from '@mui/x-data-grid';
import Table from "@mui/material/Table";
import TableRow from "@mui/material/TableRow";
import TableCell from "@mui/material/TableCell";
import { TableContainer } from "@mui/material";

import AccountService from '../../services/account.service';
import { AccountDetail, TransactionDetail, Currency, PaginatedTransactions } from "../../model/account.model";
import useMonetaryContext from '../../contexts/monetary/monetary-context';
import { useMessageDispatch } from "../../contexts/messages/context";
import { formatDate, toISODate } from "../../util/date-util";
import { EMPTY_PAGINATED_LIST } from "../../model/paginated-list.model";
import AddSelector from "../categories/add-selector";
import useReconcileTransactions from '../reconciliation/reconcile-transactions-context';
import ReconcilationButton from "../reconciliation/reconciliation-button";

interface Props {
  account: AccountDetail;
}

const DEFAULT_PAGE_SIZE: number = 30;
const DEFAULT_SLOTS = { toolbar: GridToolbar };

const dateFilterOperators = getGridDateOperators().filter((op) => op.value === 'before' || op.value === 'onOrAfter');
const stringFilterOperators = getGridStringOperators().filter((op) => op.value === 'contains');
const moneyFilterOperators = getGridNumericOperators().filter((op) => op.value === '>=');

function getMoneyValue(value: number, _row: TransactionDetail, column: GridColDef) {
  return column.field === 'credit' ? value : 0 - value;
};

function getDateValue(value: string) {
  return new Date(value);
};

export default function TransactionList(props: Props) {
  const showMessage = useMessageDispatch();
  const [ formatMoney ] = useMonetaryContext();
  const reconcilations = useReconcileTransactions();

  const renderReconciliationCell = useCallback((params: any) => {
    const row = params.row as TransactionDetail;
    return (
      <ReconcilationButton
        transaction={ row }
        onUpdate={ updated => { setTransactions(prev => ({ ...prev, items: prev.items.map(i => i.id === updated.id ? updated : i) }))} }/>
    )
  }, [ ] );

  const renderMoneyCell = useCallback((value: any, row: TransactionDetail, column: GridColDef) => {
    return column.field === 'credit'
      ? row.amount > 0 ? formatMoney(row.amount, row.currency) : ''
      : row.amount < 0 ? formatMoney(0 - row.amount, row.currency) : ''
  }, [ formatMoney ] );

  const columnDefs = useMemo(() => ([
    {
      field: 'bookingDateTime',
      type: 'date',
      headerName: 'Date',
      width: 110,
      filterOperators: dateFilterOperators,
      valueFormatter: formatDate,
      valueGetter: getDateValue
    },
    {
      field: 'additionalInformation',
      headerName: 'Additional Info',
      flex: 1,
      width: 370,
      filterOperators: stringFilterOperators
    },
    {
      field: 'creditorName',
      headerName: 'Creditor',
      flex: 0.5,
      width: 200,
      filterOperators: stringFilterOperators
    },
    {
      field: 'reference',
      headerName: 'Reference',
      flex: 0.5,
      width: 200,
      filterOperators: stringFilterOperators
    },
    {
      field: 'debit',
      type: 'number',
      headerName: 'Debit',
      headerClassName: 'colhead',
      headerAlign: 'right',
      align: 'right',
      width: 120,
      filterOperators: moneyFilterOperators,
      valueFormatter: renderMoneyCell,
      valueGetter: getMoneyValue
    },
    {
      field: 'credit',
      type: 'number',
      headerName: 'Credit',
      headerAlign: 'right',
      align: 'right',
      width: 120,
      filterOperators: moneyFilterOperators,
      valueFormatter: renderMoneyCell, 
      valueGetter: getMoneyValue
    },
    {
      field: 'actions',
      headerName: 'Reconciled',
      width: 86,
      sortable: false,
      filterable: false,
      renderCell: renderReconciliationCell
    }
  ] as GridColDef[]), [ renderMoneyCell, renderReconciliationCell ] );
  

  const [loading, setLoading] = useState(false);
  const [transactions, setTransactions] = useState<PaginatedTransactions>(EMPTY_PAGINATED_LIST);
  const [selectedTransaction, setSelectedTransaction] = useState<TransactionDetail>();
  const [showAddCategory, setShowAddCategory] = useState<boolean>(false);
  const [transactionFilter, setTransactionFilter] = useState<any | undefined>(undefined);
  const [paginationModel, setPaginationModel] = useState({ page: 0, pageSize: DEFAULT_PAGE_SIZE });

  const onFilterChange = useCallback((filterModel: GridFilterModel) => {
    var filter: any;
    filterModel.items
      .filter(item => item.value !== undefined)
      .forEach((item) => {
        if (item.field === 'additionalInformation') {
          filter = { "info": item.value as string, ...filter };
        }
        if (item.field === 'reference') {
          filter = { "reference": item.value as string, ...filter };
        }
        if (item.field === 'creditorName') {
          filter = { "creditor": item.value as string, ...filter };
        }
        if (item.field === 'bookingDateTime') {
          const dateStr = toISODate(item.value);
          if (item.operator === 'before')
            filter = { "to-date": dateStr, ...filter };
          else
            filter = { "from-date": dateStr, ...filter };
        }
        if (item.field === 'debit') {
          filter = { "max-amount": 0 - item.value as number, ...filter };
        }
        if (item.field === 'credit') {
          filter = { "min-amount": item.value as number, ...filter };
        }
      }
    );

    setTransactionFilter(filter);
  }, []);

  function onRowClick(rowParams: GridRowParams) {
    var transaction = rowParams.row as TransactionDetail;
    if (! transaction) return;

    setSelectedTransaction(transaction);
    if (transaction !== undefined) {
      setShowAddCategory(true);
    }
  }

  function getRowClassName(params: GridRowClassNameParams) {
    return reconcilations.rowClassname(params.row as TransactionDetail);
  }

  const refresh = useCallback(() => {
    setLoading(true);
    AccountService.getTransactions(props.account.id, paginationModel.page, paginationModel.pageSize, transactionFilter)
      .then( response => setTransactions(response))
      .catch(err => showMessage(err))
      .finally(() => setLoading(false));
  }, [props.account.id, paginationModel.page, paginationModel.pageSize, transactionFilter, showMessage]);

  useEffect(() => {
    refresh();
  }, [ refresh ]);

  useEffect(() => {
    reconcilations.onSubmit=refresh;
    return () => {
      reconcilations.onSubmit=undefined;
    }
  }, [ reconcilations, refresh ]);

  return (
    <>
      <DataGrid rows={ transactions.items } rowCount={ transactions.total } columns={ columnDefs } 
        density="compact" disableDensitySelector
        loading={ loading } slots={ DEFAULT_SLOTS }
        pagination paginationModel={ paginationModel }
        pageSizeOptions={[5, 15, DEFAULT_PAGE_SIZE, 50, 100]}
        paginationMode="server" onPaginationModelChange={ setPaginationModel }
        filterMode="server" filterDebounceMs={ 500 } onFilterModelChange={ onFilterChange }

        getRowClassName={ getRowClassName }
        onRowClick={ onRowClick }
      />

      { transactionFilter && transactions.currencyTotals &&
        <TableContainer>
          <Table style={{width: "250px"}}>
            {
              Object.keys(transactions.currencyTotals).map(currency =>
                <TableRow>
                  <TableCell align="right" width={"50px"}>{currency}</TableCell>
                  <TableCell align="right" style={{ whiteSpace: 'nowrap' }}>{formatMoney(transactions.currencyTotals![currency], currency as Currency)}</TableCell>
                </TableRow>
              )
            }
          </Table>
        </TableContainer>
      }
 
    <AddSelector open={ showAddCategory }
        groupId='{ props.account.groupId }'
        transaction={ selectedTransaction }
        onCancel={ () => setShowAddCategory(false) }
        onConfirm={() => setShowAddCategory(false) }/>
   </>
  )
}