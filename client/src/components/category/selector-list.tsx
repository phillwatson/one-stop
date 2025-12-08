import { useEffect, useState } from "react";

import { SxProps } from '@mui/material/styles';
import TableContainer from '@mui/material/TableContainer';
import Table from '@mui/material/Table';
import TableHead from '@mui/material/TableHead';
import TableBody from '@mui/material/TableBody';
import TableRow from '@mui/material/TableRow';
import TableCell from '@mui/material/TableCell';

import { CategoryGroup, Category, CategorySelector } from "../../model/category.model";
import CategoryService from '../../services/category.service';
import AccountService from '../../services/account.service';
import { AccountDetail } from "../../model/account.model";

import SelectorRow from './selector-row';

const colhead: SxProps = {
  fontWeight: 'bold'
};

interface Props {
  group?: CategoryGroup;
  category?: Category;
}

export default function Selectors(props: Props) {
  const [ selectors, setSelectors ] = useState<Array<CategorySelector>>([]);
  const [ accounts, setAccounts ] = useState<Array<AccountDetail>>([]);

  useEffect(() => {
    AccountService.fetchAll().then(response => setAccounts(response))
  }, []);

  function getAccountName(accountId: String): String {
    const name = accounts.find(account => account.id === accountId)?.name;
    return name ? name : "";
  }

  useEffect(() => {
    if (props.category !== undefined) {
      CategoryService
        .getAllCategorySelectors(props.category.id!)
        .then(response => setSelectors(response));
    } else {
      setSelectors([]);
    }
  }, [ props.category ]);

  return (
    <div style={{ height: '700px', overflow: 'scroll' }}>
      <TableContainer>
        <Table size='small'>
          <TableHead>
            <TableRow>
              <TableCell sx={ colhead }></TableCell>
              <TableCell sx={ colhead }>Account</TableCell>
              <TableCell sx={ colhead }>Info</TableCell>
              <TableCell sx={ colhead }>Reference</TableCell>
              <TableCell sx={ colhead }>Creditor</TableCell>
            </TableRow>
          </TableHead>
          <TableBody>
            { selectors.map(selector =>
              <SelectorRow
                key={ selector.id! }
                selector={ selector }
                accountName={ getAccountName(selector.accountId) } />
            )}
          </TableBody>
        </Table>
      </TableContainer>
    </div>
  );
}