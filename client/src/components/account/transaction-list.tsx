import { useEffect, useState } from "react";

import { Paper } from '@mui/material';
import Table from '@mui/material/Table';
import TableBody from '@mui/material/TableBody';
import TableCell from '@mui/material/TableCell';
import TableHead from '@mui/material/TableHead';
import TableRow from '@mui/material/TableRow';
import { SxProps } from '@mui/material/styles';

import AccountService from '../../services/account.service';
import { AccountDetail, TransactionSummary } from "../../model/account.model";
import ServiceError from '../../model/service-error';
import CurrencyService from '../../services/currency.service';
import { useNotificationDispatch } from "../../contexts/notification-context";

interface Props {
  account: AccountDetail;
}

const colhead: SxProps = {
  fontWeight: 'bold'
};

export default function TransactionList(props: Props) {
  const showNotification = useNotificationDispatch();
  const [transactions, setTransactions] = useState<Array<TransactionSummary>>([]);

  useEffect(() => {
    AccountService.getTransactions(props.account.id, 0, 30)
      .then( response => setTransactions(response.items))
      .catch(err => showNotification({ type: "add", level: "error", message: (err as ServiceError).message }));
    }, [props.account.id, showNotification]);

  return (
    <Table size="small" aria-label="transactions">
      <TableHead>
        <TableRow>
          <TableCell sx={colhead}>Date</TableCell>
          <TableCell sx={colhead}>Description</TableCell>
          <TableCell sx={colhead} align="right">Debit</TableCell>
          <TableCell sx={colhead} align="right">Credit</TableCell>
        </TableRow>
      </TableHead>
      <TableBody>
        { transactions.map(transaction => (
          <TableRow key={transaction.id}>
            <TableCell>{new Date(transaction.date).toLocaleDateString("en-GB")}</TableCell>
            <TableCell>{transaction.description}</TableCell>
            <TableCell align="right">{transaction.amount < 0 ? CurrencyService.format(0 - transaction.amount, transaction.currency) : ''}</TableCell>
            <TableCell align="right">{transaction.amount > 0 ? CurrencyService.format(transaction.amount, transaction.currency) : ''}</TableCell>
          </TableRow>
        ))}
      </TableBody>
    </Table>
  )
}