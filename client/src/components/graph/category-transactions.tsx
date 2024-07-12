import { useEffect, useState } from 'react';

import { Paper } from '@mui/material';
import Table from '@mui/material/Table';
import TableBody from '@mui/material/TableBody';
import TableCell from '@mui/material/TableCell';
import TableHead from '@mui/material/TableHead';
import TableRow from '@mui/material/TableRow';
import { SxProps } from '@mui/material/styles';

import { useMessageDispatch } from '../../contexts/messages/context';
import AccountService from '../../services/account.service';
import CurrencyService from '../../services/currency.service';
import { TransactionDetail } from '../../model/account.model';
import { toLocaleDate, formatDate } from '../../util/date-util';
import AddSelector from '../categories/add-selector';
import { CategoryStatistics } from '../../model/category.model';

const colhead: SxProps = {
  fontWeight: 'bold'
};

export interface Props {
  category: CategoryStatistics;
  fromDate: Date;
  toDate: Date;
}

export default function CategoryTransactions(props: Props) {
  const showMessage = useMessageDispatch();
  const [transactions, setTransactions] = useState<Array<TransactionDetail>>([]);
  const [selectedTransaction, setSelectedTransaction] = useState<TransactionDetail>();
  const [showAddCategory, setShowAddCategory] = useState<boolean>(false);

  useEffect(() => {
    AccountService.getTransactionsByCategory(props.category.categoryId!!, props.fromDate, props.toDate)
      .then(response => setTransactions(response))
      .catch(err => showMessage(err))
  }, [props, showMessage]);

  function addToCategory(transaction: TransactionDetail) {
    setSelectedTransaction(transaction);
    setShowAddCategory(true);
  }

  const noTransactions = (transactions.length === 0);
  return(
    <>
      <Paper sx={{ margin: 1, padding: 2 }} elevation={3}>
        <Table size="small" aria-label="transactions">
          <caption><i>
            { noTransactions ? 'there are no' : transactions.length } {' '}
             transaction(s) matching "{props.category.category}"
             from {toLocaleDate(props.fromDate)} (inclusive)
             to {toLocaleDate(props.toDate)} (exclusive)
          </i></caption>
          <TableHead>
            <TableRow>
              <TableCell sx={colhead}>Date</TableCell>
              <TableCell sx={colhead}>Additional Info</TableCell>
              <TableCell sx={colhead} align="right">Debit</TableCell>
              <TableCell sx={colhead} align="right">Credit</TableCell>
            </TableRow>
          </TableHead>
          <TableBody>
            { transactions.map(transaction => (
              <TableRow key={transaction.id} onClick={() => addToCategory(transaction)}>
                <TableCell>{formatDate(transaction.bookingDateTime)}</TableCell>
                <TableCell>{transaction.additionalInformation}</TableCell>
                <TableCell align="right">{transaction.amount < 0 ? CurrencyService.format(0 - transaction.amount, transaction.currency) : ''}</TableCell>
                <TableCell align="right">{transaction.amount > 0 ? CurrencyService.format(transaction.amount, transaction.currency) : ''}</TableCell>
              </TableRow>
            ))}
          </TableBody>
        </Table>
      </Paper>

      <AddSelector open={ showAddCategory }
          catagoryId={ props.category.categoryId }
          transaction={ selectedTransaction }
          onCancel={ () => setShowAddCategory(false) }
          onConfirm={() => setShowAddCategory(false) }/>
    </>
  );
};
