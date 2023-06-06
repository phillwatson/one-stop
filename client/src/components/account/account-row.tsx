import Table from '@mui/material/Table';
import TableBody from '@mui/material/TableBody';
import TableCell from '@mui/material/TableCell';
import TableHead from '@mui/material/TableHead';
import TableRow from '@mui/material/TableRow';
import Collapse from '@mui/material/Collapse';
import Box from '@mui/material/Box/Box';

import { useEffect, useState } from 'react';
import { useNotificationDispatch } from '../../contexts/notification-context';

import './account-list.css';
import ServiceError from '../../model/service-error';
import CurrencyService from '../../services/currency.service';
import AccountService from '../../services/account.service';
import { AccountDetail, TransactionSummary } from '../../model/account.model';

interface Props {
  accountId: string;
  onSelect: (accountId: string) => void;
}

export default function AccountList(props: Props) {
  const showNotification = useNotificationDispatch();

  const [account, setAccount] = useState<AccountDetail | undefined>(undefined);

  useEffect(() => {
    AccountService.get(props.accountId)
      .then(response => setAccount(response))
      .catch(err =>
        showNotification({ type: "add", level: "error", message: (err as ServiceError).message })
      )
  }, [props.accountId, showNotification]);

  const [showTransactions, setShowTransactions] = useState<boolean>(false);
  const [transactions, setTransactions] = useState<Array<TransactionSummary> | undefined>(undefined);

  useEffect(() => {
    if (showTransactions) {
      AccountService.getTransactions(props.accountId)
        .then(response => setTransactions(response.items))
        .catch(err =>
          showNotification({ type: "add", level: "error", message: (err as ServiceError).message })
        )
    } else {
      setTransactions(undefined);
    }
  }, [props.accountId, showTransactions, showNotification]);

  if (account === undefined) {
    return null;
  }

  function handleSelectAccount() {
    setShowTransactions(!showTransactions);
    //props.onSelect(props.accountId);
  }

  return(
    <>
      <TableRow key={props.accountId}>
        <TableCell rowSpan={account.balance.length}><img src={ account.institution.logo } alt="{ props.bank.name } logo" width="60px" height="60px"/></TableCell>
        <TableCell rowSpan={account.balance.length} onClick={handleSelectAccount}>{account.institution.name}</TableCell>
        <TableCell rowSpan={account.balance.length} onClick={handleSelectAccount}>{account.ownerName}</TableCell>
        <TableCell rowSpan={account.balance.length} onClick={handleSelectAccount}>{account.name}</TableCell>
        <TableCell rowSpan={account.balance.length} onClick={handleSelectAccount}>{account.iban}</TableCell>
        <TableCell >{account.balance[0].type}</TableCell>
        <TableCell >{CurrencyService.format(account.balance[0].amount, account.balance[0].currency)}</TableCell>
      </TableRow>
      { account.balance.length > 1 && account.balance.slice(1).map( balance =>
        <TableRow key={balance.id}>
          <TableCell>{balance.type}</TableCell>
          <TableCell>{CurrencyService.format(balance.amount, balance.currency)}</TableCell>
        </TableRow>
      )}
        <TableRow>
        <TableCell style={{ paddingBottom: 0, paddingTop: 0 }} colSpan={7}>
          <Collapse in={showTransactions} timeout="auto" unmountOnExit>
            <Box sx={{ margin: 1 }}>
              <Table size="small" aria-label="transactions">
                <TableHead>
                  <TableRow>
                    <TableCell>Date</TableCell>
                    <TableCell>Description</TableCell>
                    <TableCell align="right">Amount</TableCell>
                  </TableRow>
                </TableHead>
                <TableBody>
                  { transactions !== undefined && transactions.map(transaction => (
                    <TableRow key={transaction.id}>
                      <TableCell component="th" scope="row">
                        {new Date(transaction.date).toLocaleString("en-GB")}
                      </TableCell>
                      <TableCell>{transaction.description}</TableCell>
                      <TableCell align="right">{CurrencyService.format(transaction.amount, account.balance[0].currency)}</TableCell>
                    </TableRow>
                  ))}
                </TableBody>
              </Table>
            </Box>
          </Collapse>
        </TableCell>
      </TableRow>      
    </>
  );
};
