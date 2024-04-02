import { useEffect, useState } from "react";

import Table from '@mui/material/Table';
import TableHead from '@mui/material/TableHead';
import TableBody from '@mui/material/TableBody';
import TableCell from '@mui/material/TableCell';
import TableRow from '@mui/material/TableRow';
import { SxProps } from '@mui/material/styles';

import AccountService from '../../services/account.service';
import { AccountDetail, TransactionDetail } from "../../model/account.model";
import CurrencyService from '../../services/currency.service';
import { useMessageDispatch } from "../../contexts/messages/context";
import { formatDate } from "../../util/date-util";

interface Props {
  account: AccountDetail;
}

const colhead: SxProps = {
  fontWeight: 'bold'
};

export default function TransactionList(props: Props) {
  const showMessage = useMessageDispatch();
  const [transactions, setTransactions] = useState<Array<TransactionDetail>>([]);

  useEffect(() => {
    AccountService.getTransactions(props.account.id, 0, 30)
      .then( response => setTransactions(response.items))
      .catch(err => showMessage(err));
    }, [props.account.id, showMessage]);

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
            <TableCell>{ formatDate(transaction.bookingDateTime) }</TableCell>
            <TableCell>{ transaction.reference }</TableCell>
            <TableCell align="right">{transaction.amount < 0 ? CurrencyService.format(0 - transaction.amount, transaction.currency) : ''}</TableCell>
            <TableCell align="right">{transaction.amount > 0 ? CurrencyService.format(transaction.amount, transaction.currency) : ''}</TableCell>
          </TableRow>
        ))}
      </TableBody>
    </Table>
  )
}