import { useCallback, useEffect, useState } from "react";

import { DataGrid, GridColDef, getGridNumericOperators, getGridStringOperators, getGridDateOperators, GridToolbar, GridFilterModel, GridRowSelectionModel } from '@mui/x-data-grid';
import Table from "@mui/material/Table";
import TableRow from "@mui/material/TableRow";
import TableCell from "@mui/material/TableCell";
import { TableContainer } from "@mui/material";

import AccountService from '../../services/account.service';
import { AccountDetail, TransactionDetail } from "../../model/account.model";
import useMonetaryContext from '../../contexts/monetary/monetary-context';
import { useMessageDispatch } from "../../contexts/messages/context";
import { formatDate, toISODate } from "../../util/date-util";
import { PaginatedTransactions } from "../../model/account.model";
import { EMPTY_PAGINATED_LIST } from "../../model/paginated-list.model";
import AddSelector from "../categories/add-selector";
import { Currency } from "../../model/account.model";

interface Props {
  account: AccountDetail;
}

const DEFAULT_PAGE_SIZE: number = 30;

export default function TransactionList(props: Props) {
  const showMessage = useMessageDispatch();
  const [ formatMoney ] = useMonetaryContext();

  const getColumnDefs = useCallback(() => { return [
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
      valueFormatter: (value: any, row: TransactionDetail) => row.amount < 0 ? formatMoney(0 - row.amount, row.currency) : '',
      valueGetter: (value: any, row: TransactionDetail) => 0 - row.amount
    },
    {
      field: 'credit',
      type: 'number',
      headerName: 'Credit',
      headerAlign: 'right',
      align: 'right',
      width: 130,
      filterOperators: getGridNumericOperators().filter((op) => op.value === '>='),
      valueFormatter: (value: any, row: TransactionDetail) => row.amount >= 0 ? formatMoney(row.amount, row.currency) : '',
      valueGetter: (value: any, row: TransactionDetail) => row.amount
    },
  ] as GridColDef[]}, [ formatMoney ] );
  

  const [loading, setLoading] = useState(false);
  const [transactions, setTransactions] = useState<PaginatedTransactions>(EMPTY_PAGINATED_LIST);
  const [selectedTransaction, setSelectedTransaction] = useState<TransactionDetail>();
  const [showAddCategory, setShowAddCategory] = useState<boolean>(false);
  const [paginationModel, setPaginationModel] = useState({
    page: 0,
    pageSize: DEFAULT_PAGE_SIZE,
  });
  const [transactionFilter, setTransactionFilter] = useState<any | undefined>(undefined);

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

  function addToCategory(selection: GridRowSelectionModel) {
    if (selection.length === 0) return;

    const transactionId = selection[0] as string;
    const transaction = transactions.items.find(t => t.id === transactionId);
    setSelectedTransaction(transaction);
    if (transaction !== undefined) {
      setShowAddCategory(true);
    }
  }

  useEffect(() => {
    setLoading(true);
    AccountService.getTransactions(props.account.id, paginationModel.page, paginationModel.pageSize, transactionFilter)
      .then( response => setTransactions(response))
      .catch(err => showMessage(err))
      .finally(() => setLoading(false));
  }, [props.account.id, paginationModel.page, paginationModel.pageSize, transactionFilter, showMessage]);

  return (
    <>
      <DataGrid rows={transactions.items} rowCount={transactions.total} columns={ getColumnDefs() } 
        density="compact" disableDensitySelector
        loading={loading} slots={{ toolbar: GridToolbar }}
        pagination paginationModel={paginationModel}
        pageSizeOptions={[5, 15, DEFAULT_PAGE_SIZE, 50, 100]}
        paginationMode="server" onPaginationModelChange={ setPaginationModel }
        filterMode="server" filterDebounceMs={ 500 } onFilterModelChange={ onFilterChange }
        onRowSelectionModelChange={ newRowSelectionModel => addToCategory(newRowSelectionModel) }
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