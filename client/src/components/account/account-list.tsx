import Table from '@mui/material/Table';
import TableBody from '@mui/material/TableBody';
import TableCell from '@mui/material/TableCell';
import TableContainer from '@mui/material/TableContainer';
import TableHead from '@mui/material/TableHead';
import TableRow from '@mui/material/TableRow';
import Collapse from '@mui/material/Collapse';
import Box from '@mui/material/Box';
import { SxProps } from '@mui/material/styles';

import './account-list.css';

import CurrencyService from '../../services/currency.service';
import { AccountDetail } from '../../model/account.model';
import AccountRow from './account-row';
import TransactionSummaryList from './transaction-summary';
import { useState } from 'react';

interface Props {
  accounts: Array<AccountDetail>;
  onSelect: (accountId: string) => void;
}

const colhead: SxProps = {
  fontWeight: 'bold'
};

export default function AccountList(props: Props) {
  const [selectedAccounts, setSelectedAccounts] = useState<string[]>([]);

  function isSelected(accountId: string) {
    return selectedAccounts.find(id => id === accountId) !== undefined;
  }

  function handleSelectAccount(accountId: string) {
    if (isSelected(accountId)) {
      setSelectedAccounts(selectedAccounts.filter(id => id !== accountId))
    } else {
      setSelectedAccounts([...selectedAccounts, accountId])
    }
    //props.onSelect(accountId);
  }



  return(
    <TableContainer>
      <Table>
        <TableHead>
          <TableRow>
            <TableCell align="center" colSpan={5} sx={colhead}>Details</TableCell>
            <TableCell align="center" colSpan={2} sx={colhead}>Balance</TableCell>
          </TableRow>
          <TableRow>
            <TableCell></TableCell>
            <TableCell sx={colhead}>Institution</TableCell>
            <TableCell sx={colhead}>Owner</TableCell>
            <TableCell sx={colhead}>Name</TableCell>
            <TableCell sx={colhead}>IBAN</TableCell>
            <TableCell sx={colhead}>Type</TableCell>
            <TableCell sx={colhead}>Amount</TableCell>
          </TableRow>
        </TableHead>

        <TableBody>
          { props.accounts && props.accounts
            .sort((a, b) => a.name < b.name ? -1 : 1 )
            .map(account =>
              <AccountRow account={account} onSelect={() => handleSelectAccount(account.id)}>
                <Collapse in={isSelected(account.id)} timeout="auto" unmountOnExit>
                  <Box sx={{ margin: 1 }}>
                    <TransactionSummaryList accountId={account.id} showTransactions={isSelected(account.id)}/>
                  </Box>
                </Collapse>
              </AccountRow>
            )
          }
          <TableRow>
            <TableCell colSpan={6} align="right" sx={colhead}>Total</TableCell>
            <TableCell colSpan={1} align="right">{CurrencyService.format(32.2, 'EUR')}</TableCell>
          </TableRow>
        </TableBody>
      </Table>
    </TableContainer>
  );
}
