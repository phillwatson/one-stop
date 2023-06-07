import { useEffect, useState } from 'react';

import Table from '@mui/material/Table';
import TableBody from '@mui/material/TableBody';
import TableCell from '@mui/material/TableCell';
import TableHead from '@mui/material/TableHead';
import TableRow from '@mui/material/TableRow';

import { useNotificationDispatch } from '../../contexts/notification-context';
import AccountService from '../../services/account.service';
import ServiceError from '../../model/service-error';
import CurrencyService from '../../services/currency.service';
import { TransactionSummary } from '../../model/account.model';

interface Props {
  accountId: string;
  showTransactions: boolean;
}

export default function TransactionSummaryList(props: Props) {
  const showNotification = useNotificationDispatch();
  const [transactions, setTransactions] = useState<Array<TransactionSummary>>([]);

  useEffect(() => {
    if ((props.showTransactions) && (transactions.length === 0)) {
      AccountService.getTransactions(props.accountId)
        .then(response => setTransactions(response.items))
        .catch(err =>
          showNotification({ type: "add", level: "error", message: (err as ServiceError).message })
        )
    }
  }, [props.accountId, props.showTransactions, transactions.length, showNotification]);

  if (! props.showTransactions) {
    return null;
  }

  return(
    <>
      <Table size="small" aria-label="transactions">
        <TableHead>
          <TableRow>
            <TableCell>Date</TableCell>
            <TableCell>Description</TableCell>
            <TableCell align="right">Amount</TableCell>
          </TableRow>
        </TableHead>
        <TableBody>
          { transactions.map(transaction => (
            <TableRow key={transaction.id}>
              <TableCell component="th" scope="row">
                {new Date(transaction.date).toLocaleString("en-GB")}
              </TableCell>
              <TableCell>{transaction.description}</TableCell>
              <TableCell align="right">{CurrencyService.format(transaction.amount, transaction.currency)}</TableCell>
            </TableRow>
          ))}
        </TableBody>
      </Table>
    </>
  );
};
