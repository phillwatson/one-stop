import { useEffect, useState } from "react";

import { SxProps } from '@mui/material/styles';
import TableContainer from '@mui/material/TableContainer';
import Table from '@mui/material/Table';
import TableHead from '@mui/material/TableHead';
import TableBody from '@mui/material/TableBody';
import TableRow from '@mui/material/TableRow';
import TableCell from '@mui/material/TableCell';

import { useMessageDispatch } from '../../contexts/messages/context';
import { CategorySelector, selectorName } from "../../model/category.model";
import AccountService from '../../services/account.service';
import { AccountDetail } from "../../model/account.model";
import ConfirmationDialog from '../dialogs/confirm-dialog';

import SelectorRow from './selector-row';

const colhead: SxProps = {
  fontWeight: 'bold'
};

interface Props {
  selectors: Array<CategorySelector>;
  onDeleteSelector?: (selector: CategorySelector) => void;  
}

export default function Selectors(props: Props) {
  const showMessage = useMessageDispatch();
  const [ accounts, setAccounts ] = useState<Array<AccountDetail>>([]);
  const [ deleteSelector, setDeleteSelector ] = useState<CategorySelector | undefined>(undefined);

  useEffect(() => {
    AccountService.fetchAll()
      .then(response => setAccounts(response))
      .catch(err => showMessage(err));
  }, [ showMessage ]);

  function getAccountName(accountId: String): String {
    const name = accounts.find(account => account.id === accountId)?.name;
    return name ? name : "";
  }

  function onDeleteSelector(selector: CategorySelector) {
    setDeleteSelector(selector);
  }

  function confirmDeleteSelector() {
    const selector = deleteSelector;
    if (selector) {
      if (props.onDeleteSelector) {
        props.onDeleteSelector(selector);
      }
    } else {
      setDeleteSelector(undefined);
    }
  }

  return (
    <div style={{ height: '700px', overflow: 'scroll' }}>
      <TableContainer>
        <Table size='small' stickyHeader>
          <TableHead>
            <TableRow>
              <TableCell sx={ colhead }></TableCell>
              <TableCell sx={ colhead }></TableCell>
              <TableCell sx={ colhead }>Account</TableCell>
              <TableCell sx={ colhead }>Info</TableCell>
              <TableCell sx={ colhead }>Reference</TableCell>
              <TableCell sx={ colhead }>Creditor</TableCell>
            </TableRow>
          </TableHead>
          <TableBody>
            { props.selectors.map(selector =>
              <SelectorRow
                key={ selector.id! }
                selector={ selector }
                accountName={ getAccountName(selector.accountId) }
                onDeleteClick={ () => onDeleteSelector(selector) } />
            )}
          </TableBody>
        </Table>
      </TableContainer>
 
      { deleteSelector &&
        <ConfirmationDialog open={ true }
          title={"Delete Selector \""+ selectorName(deleteSelector) + "\""}
           content={ [
            "Are you sure you want to delete this transaction selector?",
            "This action cannot be undone." ] }
          onConfirm={ confirmDeleteSelector }
          onCancel={() => setDeleteSelector(undefined)} />
      }
    </div>
  );
}