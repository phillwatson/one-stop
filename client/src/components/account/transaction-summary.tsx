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
import { formatDate } from '../../util/date-util';
import AddSelector from '../categories/add-selector';

const colhead: SxProps = {
  fontWeight: 'bold'
};

interface Props {
  accountId: string;
}

interface TransactionsList {
  page: Array<TransactionDetail>,
  total: number
}

export default function TransactionSummaryList(props: Props) {
  const showMessage = useMessageDispatch();
  const [transactions, setTransactions] = useState<TransactionsList>({ page: [], total: 0});
  const [selectedTransaction, setSelectedTransaction] = useState<TransactionDetail>();
  const [showAddCategory, setShowAddCategory] = useState<boolean>(false);

  useEffect(() => {
    AccountService.getTransactions(props.accountId, 0, 10)
      .then(response => setTransactions({ page: response.items, total: response.total }))
      .catch(err => showMessage(err))
  }, [props.accountId, showMessage]);

  function addToCategory(transaction: TransactionDetail) {
    setSelectedTransaction(transaction);
    setShowAddCategory(true);
  }

  const noTransactions = (transactions.page.length === 0);
  return(
    <>
      <Paper sx={{ margin: 1 }} elevation={3}>
        <Table size="small" aria-label="transactions">
          <caption><i>
            { noTransactions
              ? 'there are no transactions available'
              : `most recent ${transactions.page.length} transactions of ${transactions.total}`
            }
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
            { transactions.page.map(transaction => (
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
        groupId='{ props.account.groupId }'
        transaction={ selectedTransaction }
        onCancel={ () => setShowAddCategory(false) }
        onConfirm={() => setShowAddCategory(false) }/>
    </>
  );
};
