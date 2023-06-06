import Table from '@mui/material/Table';
import TableBody from '@mui/material/TableBody';
import TableCell from '@mui/material/TableCell';
import TableContainer from '@mui/material/TableContainer';
import TableHead from '@mui/material/TableHead';
import TableRow from '@mui/material/TableRow';

import CurrencyService from '../../services/currency.service';
import Account from '../../model/account.model';
import './account-list.css';
import AccountRow from './account-row';
import { SxProps } from '@mui/material/styles';

interface Props {
  accounts: Array<Account>;
  onSelect: (accountId: string) => void;
}

const colhead: SxProps = {
  fontWeight: 'bold'
};

export default function AccountList(props: Props) {
  if (props.accounts === undefined) {
    return null;
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
              <AccountRow accountId={account.id} onSelect={props.onSelect}/>
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
