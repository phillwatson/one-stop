import { PropsWithChildren, useState } from 'react';
import { useNavigate } from 'react-router-dom';

import TableCell from '@mui/material/TableCell';
import TableRow from '@mui/material/TableRow';
import Menu from '@mui/material/Menu';
import MenuItem from '@mui/material/MenuItem';
import ListItemText from '@mui/material/ListItemText';
import ListItemIcon from '@mui/material/ListItemIcon';
import FileDownloadIcon from '@mui/icons-material/FileDownload';
import GraphIcon from '@mui/icons-material/Equalizer';
import MoreVertIcon from '@mui/icons-material/MoreVert';
import ReadMoreIcon from '@mui/icons-material/ReadMore';

import useMonetaryContext from '../../contexts/monetary/monetary-context';
import { AccountDetail } from '../../model/account.model';
import Avatar from '@mui/material/Avatar';

interface Props extends PropsWithChildren {
  account: AccountDetail;
  onSelect: (account: AccountDetail) => void;
}

export default function AccountList(props: Props) {
  const navigation = useNavigate();
  const [ formatMoney ] = useMonetaryContext();

  const [anchorEl, setAnchorEl] = useState<null | HTMLElement>(null);
  const menuOpen = Boolean(anchorEl);

  function toggleMenu(event: React.MouseEvent<HTMLElement>) {
    setAnchorEl(event.currentTarget);
  };

  function closeMenu() {
    setAnchorEl(null);
  };


  function handleSelectAccount() {
    props.onSelect(props.account);
  }

  function exportAccount() {
    closeMenu();
  }

  function showAccount(accountId: string) {
    closeMenu();
    navigation(`/accounts/${accountId}/transactions`);
  }

  function showGraph(accountId: string) { 
    closeMenu();
    navigation(`/accounts/${accountId}/graph`);
  }

  function AccountMenu(props: { accountId: string }) {
    return(
        <Menu
          id="account-menu" open={menuOpen} onClose={closeMenu}
          aria-labelledby="account-button" anchorEl={anchorEl}
          anchorOrigin={{ vertical: 'center', horizontal: 'left' }}
          transformOrigin={{ vertical: 'top', horizontal: 'left' }}
        >
          <MenuItem onClick={() => showAccount(props.accountId)} sx={{ width: 190, maxWidth: '100%' }}>
            <ListItemIcon><ReadMoreIcon fontSize="small"/></ListItemIcon>
            <ListItemText>Show More...</ListItemText>
          </MenuItem>
          <MenuItem onClick={() => showGraph(props.accountId)} >
            <ListItemIcon><GraphIcon fontSize="small"/></ListItemIcon>
            <ListItemText>Show Graph...</ListItemText>
          </MenuItem>
          <MenuItem onClick={exportAccount}>
            <ListItemIcon><FileDownloadIcon fontSize="small"/></ListItemIcon>
            <ListItemText>Export...</ListItemText>
          </MenuItem>
        </Menu>
    );
  }

  return(
    <>
      <TableRow key={props.account.id} >
        <TableCell size="small" padding='none' rowSpan={props.account.balance.length}
          id="account-button" onClick={toggleMenu}
          aria-haspopup="true"
          aria-controls={menuOpen ? 'account-menu' : undefined}
          aria-expanded={menuOpen ? 'true' : undefined}>
          <MoreVertIcon fontSize="small"/>
        </TableCell>

        <TableCell size="small" padding='none' rowSpan={props.account.balance.length} onClick={handleSelectAccount}>
          <Avatar src={ props.account.institution.logo } alt="{ props.bank.name } logo"
            sx={{ margin: "3px", width: "38px", height: "38px" }}></Avatar>
        </TableCell>

        <TableCell size="small" rowSpan={props.account.balance.length} onClick={handleSelectAccount}>{props.account.institution.name}</TableCell>
        <TableCell size="small" rowSpan={props.account.balance.length} onClick={handleSelectAccount}>{props.account.ownerName}</TableCell>
        <TableCell size="small" rowSpan={props.account.balance.length} onClick={handleSelectAccount}>{props.account.name}</TableCell>
        <TableCell size="small" rowSpan={props.account.balance.length} onClick={handleSelectAccount}>{props.account.iban}</TableCell>
        <TableCell size="small">{props.account.balance[0].type}</TableCell>
        <TableCell size="small" >{formatMoney(props.account.balance[0].amount, props.account.balance[0].currency)}</TableCell>
      </TableRow>

      { props.account.balance.length > 1 && props.account.balance.slice(1).map( balance =>
        <TableRow key={balance.id}>
          <TableCell size="small">{balance.type}</TableCell>
          <TableCell size="small">{formatMoney(balance.amount, balance.currency)}</TableCell>
        </TableRow>
      )}

      <TableRow>
        <TableCell style={{ paddingBottom: 0, paddingTop: 0 }} colSpan={8}>
          { props.children }
        </TableCell>
      </TableRow>

      <AccountMenu accountId={props.account.id}/>
    </>
  );
};
