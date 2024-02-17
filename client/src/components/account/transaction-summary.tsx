import { useEffect, useState } from 'react';

import { Paper } from '@mui/material';
import Table from '@mui/material/Table';
import TableBody from '@mui/material/TableBody';
import TableCell from '@mui/material/TableCell';
import TableHead from '@mui/material/TableHead';
import TableRow from '@mui/material/TableRow';
import { SxProps } from '@mui/material/styles';

import { useNotificationDispatch } from '../../contexts/notification-context';
import AccountService from '../../services/account.service';
import ServiceError from '../../model/service-error';
import CurrencyService from '../../services/currency.service';
import { TransactionSummary } from '../../model/account.model';

const colhead: SxProps = {
  fontWeight: 'bold'
};

interface Props {
  accountId: string;
}

export default function TransactionSummaryList(props: Props) {
  const showNotification = useNotificationDispatch();
  const [transactions, setTransactions] = useState<Array<TransactionSummary> | undefined>(undefined);

  useEffect(() => {
    if (transactions === undefined) {
      AccountService.getTransactions(props.accountId, 0, 10)
        .then(response => setTransactions(response.items))
        .catch(err => showNotification({ type: "add", level: "error", message: (err as ServiceError).message }))
    }
  }, [props.accountId, transactions, showNotification]);

  const noTransactions = (transactions === undefined || transactions.length === 0);
  return(
    <Paper sx={{ margin: 1 }} elevation={3}>
      <Table size="small" aria-label="transactions">
        <caption><i>
          { noTransactions
            ? 'there are no transactions available'
            : `most recent ${transactions.length} transactions`
          }
        </i></caption>
        <TableHead>
          <TableRow>
            <TableCell sx={colhead}>Date</TableCell>
            <TableCell sx={colhead}>Description</TableCell>
            <TableCell sx={colhead} align="right">Debit</TableCell>
            <TableCell sx={colhead} align="right">Credit</TableCell>
          </TableRow>
        </TableHead>
        <TableBody>
          { transactions && transactions.map(transaction => (
            <TableRow key={transaction.id}>
              <TableCell>{new Date(transaction.date).toLocaleDateString("en-GB")}</TableCell>
              <TableCell>{transaction.description}</TableCell>
              <TableCell align="right">{transaction.amount < 0 ? CurrencyService.format(0 - transaction.amount, transaction.currency) : ''}</TableCell>
              <TableCell align="right">{transaction.amount > 0 ? CurrencyService.format(transaction.amount, transaction.currency) : ''}</TableCell>
            </TableRow>
          ))}
        </TableBody>
      </Table>
    </Paper>
  );
};
