import { PropsWithChildren } from 'react';
import TableCell from '@mui/material/TableCell';
import TableRow from '@mui/material/TableRow';

import './account-list.css';
import CurrencyService from '../../services/currency.service';
import { AccountDetail } from '../../model/account.model';

interface Props extends PropsWithChildren {
  account: AccountDetail;
  onSelect: (accountId: string) => void;
}

export default function AccountList(props: Props) {
  function handleSelectAccount() {
    props.onSelect(props.account.id);
  }

  return(
    <>
      <TableRow key={props.account.id}>
        <TableCell size="small" rowSpan={props.account.balance.length}><img src={ props.account.institution.logo } alt="{ props.bank.name } logo" width="60px" height="60px"/></TableCell>
        <TableCell size="small" rowSpan={props.account.balance.length} onClick={handleSelectAccount}>{props.account.institution.name}</TableCell>
        <TableCell size="small" rowSpan={props.account.balance.length} onClick={handleSelectAccount}>{props.account.ownerName}</TableCell>
        <TableCell size="small" rowSpan={props.account.balance.length} onClick={handleSelectAccount}>{props.account.name}</TableCell>
        <TableCell size="small" rowSpan={props.account.balance.length} onClick={handleSelectAccount}>{props.account.iban}</TableCell>
        <TableCell size="small">{props.account.balance[0].type}</TableCell>
        <TableCell size="small" >{CurrencyService.format(props.account.balance[0].amount, props.account.balance[0].currency)}</TableCell>
      </TableRow>
      { props.account.balance.length > 1 && props.account.balance.slice(1).map( balance =>
        <TableRow key={balance.id}>
          <TableCell size="small">{balance.type}</TableCell>
          <TableCell size="small">{CurrencyService.format(balance.amount, balance.currency)}</TableCell>
        </TableRow>
      )}
        <TableRow>
        <TableCell style={{ paddingBottom: 0, paddingTop: 0 }} colSpan={7}>
          { props.children }
        </TableCell>
      </TableRow>      
    </>
  );
};
