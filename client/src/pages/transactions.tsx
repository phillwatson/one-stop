import { useEffect, useState } from "react";

import Table from '@mui/material/Table';
import TableBody from '@mui/material/TableBody';
import TableCell from '@mui/material/TableCell';
import TableContainer from '@mui/material/TableContainer';
import TableHead from '@mui/material/TableHead';
import TableRow from '@mui/material/TableRow';

import AccountService from '../services/account.service';
import { AccountDetail } from "../model/account.model";
import ServiceError from '../model/service-error';
import { useParams } from "react-router-dom";
import { useNotificationDispatch } from "../contexts/notification-context";
import TransactionList from "../components/account/transaction-list";

export default function Transactions() {
  const { accountId } = useParams();
  const showNotification = useNotificationDispatch();

  const [account, setAccount] = useState<AccountDetail>();

  useEffect(() => {
    if (accountId) {
      AccountService.get(accountId)
        .then( response => setAccount(response))
        .catch(err => showNotification({ type: "add", level: "error", message: (err as ServiceError).message }));
    }
  }, [accountId, showNotification]);

  return (
    <div>
      <h2>Account Transactions</h2>
      <hr></hr>
      { account &&
        <TableContainer>
          <Table>
            <TableHead>
              <TableRow key={account.id}>
                <TableCell size="small" padding='none'>
                  <img src={ account.institution.logo } alt="{ props.bank.name } logo" width="68px" height="68px"/>
                </TableCell>
                <TableCell size="small">{account.institution.name}</TableCell>
                <TableCell size="small">{account.ownerName}</TableCell>
                <TableCell size="small">{account.name}</TableCell>
                <TableCell size="small">{account.iban}</TableCell>
              </TableRow>
            </TableHead>
            <TableBody>
              <TableRow>
                <TableCell colSpan={5}>
                  <TransactionList account={account} />
                </TableCell>
              </TableRow>
            </TableBody>
          </Table>
        </TableContainer>
      }
    </div>
  );
}
