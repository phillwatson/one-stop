import { useCallback, useEffect, useState } from "react";

import "./transaction-list.css";
import { DataGrid, GridColDef, getGridNumericOperators, getGridStringOperators, getGridDateOperators, GridToolbar, GridFilterModel } from '@mui/x-data-grid';

import AccountService from '../../services/account.service';
import { AccountDetail, TransactionDetail } from "../../model/account.model";
import CurrencyService from '../../services/currency.service';
import { useMessageDispatch } from "../../contexts/messages/context";
import { formatDate, toISODate } from "../../util/date-util";
import PaginatedList, { EMPTY_PAGINATED_LIST } from "../../model/paginated-list.model";
import Box from "@mui/material/Box/Box";

interface Props {
  account: AccountDetail;
}

const columns: GridColDef[] = [
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
    valueFormatter: (value: any, row: TransactionDetail) => row.amount < 0 ? CurrencyService.format(0 - row.amount, row.currency) : '',
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
    valueFormatter: (value: any, row: TransactionDetail) => row.amount >= 0 ? CurrencyService.format(row.amount, row.currency) : '',
    valueGetter: (value: any, row: TransactionDetail) => row.amount
  },
];

const DEFAULT_PAGE_SIZE: number = 30;

export default function TransactionList(props: Props) {
  const showMessage = useMessageDispatch();
  const [loading, setLoading] = useState(false);
  const [transactions, setTransactions] = useState<PaginatedList<TransactionDetail>>(EMPTY_PAGINATED_LIST);
  const [paginationModel, setPaginationModel] = useState({
    page: 0,
    pageSize: DEFAULT_PAGE_SIZE,
  });
  const [queryOptions, setQueryOptions] = useState<GridFilterModel>({ items: [] });

  const onFilterChange = useCallback((filterModel: GridFilterModel) => {
    // Here you save the data you need from the filter model
    setQueryOptions(filterModel);
  }, []);

  useEffect(() => {
    setLoading(true);
    var filter = {};
    queryOptions.items.forEach((item) => {
      if (item.value === undefined) {
        return
      }

      if (item.field === 'additionalInformation') {
        filter = { "info": item.value as string };
      }
      if (item.field === 'reference') {
        filter = { "reference": item.value as string, ...filter };
      }
      if (item.field === 'creditorName') {
        filter = { "creditorName": item.value as string, ...filter };
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
    });

    AccountService.getTransactions(props.account.id, paginationModel.page, paginationModel.pageSize, filter)
      .then( response => setTransactions(response))
      .catch(err => showMessage(err))
      .finally(() => setLoading(false));
    }, [props.account.id, paginationModel.page, paginationModel.pageSize, queryOptions, showMessage]);

  return (
    <Box className="grid">
      <DataGrid rows={transactions.items} rowCount={transactions.total} columns={columns} 
        autoHeight density="compact" disableDensitySelector
        loading={loading} slots={{ toolbar: GridToolbar }}
        pagination paginationModel={paginationModel}
        pageSizeOptions={[5, 15, DEFAULT_PAGE_SIZE, 50, 100]}
        paginationMode="server" onPaginationModelChange={setPaginationModel}
        filterMode="server" filterDebounceMs={500} onFilterModelChange={onFilterChange}/>
    </Box>
  )
}