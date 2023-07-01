import { PropsWithChildren, useRef, useState } from 'react';
import TableCell from '@mui/material/TableCell';
import TableRow from '@mui/material/TableRow';
import Grow from '@mui/material/Grow';
import Popper from '@mui/material/Popper';
import MenuItem from '@mui/material/MenuItem';
import MenuList from '@mui/material/MenuList';

import './account-list.css';
import CurrencyService from '../../services/currency.service';
import { AccountDetail } from '../../model/account.model';
import Paper from '@mui/material/Paper';
import ClickAwayListener from '@mui/material/ClickAwayListener';

interface Props extends PropsWithChildren {
  account: AccountDetail;
  onSelect: (accountId: string) => void;
}

export default function AccountList(props: Props) {
  const [ showMenu, setShowMenu ] = useState<boolean>(false);
  const anchorRef = useRef<HTMLButtonElement>(null);

  const handleToggle = () => {
    setShowMenu((prevOpen) => !prevOpen);
  };

  const handleClose = (event: Event | React.SyntheticEvent) => {
    if (
      anchorRef.current &&
      anchorRef.current.contains(event.target as HTMLElement)
    ) {
      return;
    }

    setShowMenu(false);
  };

  function handleListKeyDown(event: React.KeyboardEvent) {
    if (event.key === 'Tab') {
      event.preventDefault();
      setShowMenu(false);
    } else if (event.key === 'Escape') {
      setShowMenu(false);
    }
  }
  
  function Menu() {
    return(
      <Popper
        open={showMenu}
        anchorEl={anchorRef.current}
        role={undefined}
        placement="bottom"
        transition
        disablePortal
        modifiers={[
          {
            name: 'arrow',
            enabled: true,
            options: {
              element: anchorRef.current,
            },
          }
        ]}
      >
        {({ TransitionProps, placement }) => (
          <Grow
            {...TransitionProps}
            style={{ transformOrigin: placement === 'bottom-start' ? 'left top' : 'left bottom', }}
          >
            <Paper>
              <ClickAwayListener onClickAway={handleClose}>
                <MenuList
                  autoFocusItem={showMenu}
                  id="composition-menu"
                  aria-labelledby="composition-button"
                  onKeyDown={handleListKeyDown}
                >
                  <MenuItem onClick={handleClose}>Export</MenuItem>
                  <MenuItem onClick={handleClose}>Remove</MenuItem>
                </MenuList>
              </ClickAwayListener>
            </Paper>
          </Grow>
        )}
      </Popper>
    );
  }


  function handleSelectAccount() {
    props.onSelect(props.account.id);
  }

  return(
    <>
      <TableRow key={props.account.id}>
        <TableCell size="small" rowSpan={props.account.balance.length} onClick={handleToggle} ref={anchorRef} id="composition-button">
          <img src={ props.account.institution.logo } alt="{ props.bank.name } logo" width="60px" height="60px"/>
          <Menu/>
        </TableCell>
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
