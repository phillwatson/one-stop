import { useEffect, useState } from 'react';

import { Box, Paper } from '@mui/material';
import Table from '@mui/material/Table';
import TableBody from '@mui/material/TableBody';
import TableCell from '@mui/material/TableCell';
import TableHead from '@mui/material/TableHead';
import TableRow from '@mui/material/TableRow';
import { SxProps } from '@mui/material/styles';

import { useMessageDispatch } from '../../contexts/messages/context';
import AccountService from '../../services/account.service';
import useMonetaryContext from '../../contexts/monetary/monetary-context';
import { CategoryStatistics } from '../../model/category.model';
import { TransactionDetail } from '../../model/account.model';
import { toLocaleDate, formatDate } from '../../util/date-util';
import AddSelector from './add-selector';

const colhead: SxProps = {
  fontWeight: 'bold'
};

export interface Props {
  elevation?: number;
  category: CategoryStatistics;
  fromDate: Date;
  toDate: Date;
}

export default function CategoryTransactions(props: Props) {
  const showMessage = useMessageDispatch();
  const [ formatMoney ] = useMonetaryContext();

  const [transactions, setTransactions] = useState<Array<TransactionDetail>>([]);
  const [selectedTransaction, setSelectedTransaction] = useState<TransactionDetail>();
  const [showAddCategory, setShowAddCategory] = useState<boolean>(false);

  useEffect(() => {
    AccountService.getTransactionsByCategory(props.category.groupId, props.category.categoryId!!, props.fromDate, props.toDate)
      .then(response => setTransactions(response))
      .catch(err => {
        setTransactions([]);
        showMessage(err);
      })
  }, [props, showMessage]);

  function addToCategory(transaction: TransactionDetail) {
    setSelectedTransaction(transaction);
    setShowAddCategory(true);
  }

  function closeAddToCategory() {
    setShowAddCategory(false);
    setSelectedTransaction(undefined);
  }

  const noTransactions = (transactions.length === 0);
  return(
    <>
      <Paper sx={{ marginTop: 1, padding: 2 }} elevation={ props.elevation || 3 }>
        <Box sx={{ textAlign: 'center', fontSize: '1.2em', fontWeight: 'bold' }}>{ props.category.categoryName }</Box>
        <Table size="small" aria-label="transactions">
          <caption><i>
            { noTransactions ? 'there are no' : transactions.length } {' '}
             transaction(s) matching "{props.category.categoryName}"
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
                <TableCell align="right">{transaction.amount < 0 ? formatMoney(0 - transaction.amount, transaction.currency) : ''}</TableCell>
                <TableCell align="right">{transaction.amount > 0 ? formatMoney(transaction.amount, transaction.currency) : ''}</TableCell>
              </TableRow>
            ))}
          </TableBody>
        </Table>
      </Paper>

      <AddSelector open={ showAddCategory }
          groupId={ props.category.groupId }
          catagoryId={ props.category.categoryId }
          transaction={ selectedTransaction }
          onCancel={ () => closeAddToCategory() }
          onConfirm={() => closeAddToCategory() }/>
    </>
  );
};
