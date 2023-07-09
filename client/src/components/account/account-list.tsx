import { useMemo, useState } from 'react';
import Table from '@mui/material/Table';
import TableBody from '@mui/material/TableBody';
import TableCell from '@mui/material/TableCell';
import TableContainer from '@mui/material/TableContainer';
import TableHead from '@mui/material/TableHead';
import TableRow from '@mui/material/TableRow';
import Collapse from '@mui/material/Collapse';
import { SxProps } from '@mui/material/styles';

import './account-list.css';

import CurrencyService from '../../services/currency.service';
import { AccountDetail, AccountBalance } from '../../model/account.model';
import AccountRow from './account-row';
import TransactionSummaryList from './transaction-summary';

interface Props {
  accounts: Array<AccountDetail>;
  onSelect: (account: AccountDetail) => void;
  onDelete: (account: AccountDetail) => void;
}

const colhead: SxProps = {
  fontWeight: 'bold'
};

const balanceRow: SxProps = {
  border: 0
};

export default function AccountList(props: Props) {
  const [selectedAccounts, setSelectedAccounts] = useState<string[]>([]);

  function isSelected(accountId: string) {
    return selectedAccounts.find(id => id === accountId) !== undefined;
  }

  function handleSelectAccount(account: AccountDetail) {
    if (isSelected(account.id)) {
      setSelectedAccounts(selectedAccounts.filter(id => id !== account.id))
    } else {
      setSelectedAccounts([...selectedAccounts, account.id])
    }
    //props.onSelect(accountId);
  }

  function handleDeleteAccount(account: AccountDetail) {
    props.onDelete(account);
  }

  const balanceTotals: Array<AccountBalance> = useMemo(() => {
    const result = Array<AccountBalance>();
    props.accounts.forEach(account => {
      account.balance.forEach(balance => {
        if (result.length === 0) {
          result.push({...balance});
        } else {
          const type = result.find(i => i.type === balance.type)
          if (type == null) {
            result.push({...balance})
          } else {
            type.amount += balance.amount
          }
        }
      })
    });

    return result;
  }, [props.accounts]);

  return (
    <TableContainer>
      <Table>
        <TableHead>
          <TableRow>
            <TableCell align="center" colSpan={6} sx={colhead}>Details</TableCell>
            <TableCell align="center" colSpan={2} sx={colhead}>Balance</TableCell>
          </TableRow>
          <TableRow>
            <TableCell sx={colhead} colSpan={3} align='center'>Institution</TableCell>
            <TableCell sx={colhead}>Owner</TableCell>
            <TableCell sx={colhead}>Name</TableCell>
            <TableCell sx={colhead}>IBAN</TableCell>
            <TableCell sx={colhead}>Type</TableCell>
            <TableCell sx={colhead}>Amount</TableCell>
          </TableRow>
        </TableHead>

        <TableBody>
          { props.accounts && props.accounts
            .sort((a, b) => (a.name === b.name) ? a.iban < b.iban ? -1 : 1 : a.name < b.name ? -1 : 1 )
            .map(account =>
              <AccountRow key={account.id} account={account} onSelect={handleSelectAccount} onDelete={handleDeleteAccount}>
                <Collapse in={isSelected(account.id)} timeout="auto" unmountOnExit>
                  <TransactionSummaryList accountId={account.id}/>
                </Collapse>
              </AccountRow>
            )
          }
          <TableRow>
            <TableCell colSpan={6} sx={balanceRow}></TableCell>
            <TableCell colSpan={2} align="center" sx={colhead}>Totals</TableCell>
          </TableRow>
          { balanceTotals.length > 1 && balanceTotals.map( balance =>
            <TableRow key={balance.id}>
              <TableCell colSpan={6} sx={balanceRow}></TableCell>
              <TableCell>{balance.type}</TableCell>
              <TableCell>{CurrencyService.format(balance.amount, balance.currency)}</TableCell>
            </TableRow>
          )}
        </TableBody>
      </Table>
    </TableContainer>
  );
}
