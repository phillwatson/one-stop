import { useEffect, useState } from "react";
import { Button, Dialog, DialogActions, DialogContent, DialogTitle, MenuItem, Select, SxProps, TextField } from "@mui/material";
import Table from '@mui/material/Table';
import TableBody from '@mui/material/TableBody';
import TableCell from '@mui/material/TableCell';
import TableContainer from '@mui/material/TableContainer';
import TableHead from '@mui/material/TableHead';
import TableRow from '@mui/material/TableRow';

import AccountService from "../../services/account.service";
import CategoryService from "../../services/category.service";
import { Category, CategorySelector } from "../../model/category.model";
import { AccountDetail, TransactionDetail } from "../../model/account.model";
import { useMessageDispatch } from "../../contexts/messages/context";

const colhead: SxProps = {
  fontWeight: 'bold'
};

interface Props {
  open: boolean;
  transaction?: TransactionDetail;
  onConfirm: (category: Category, selector: CategorySelector) => void;
  onCancel: () => void;
}

export default function AddSelector(props: Props) {
  const showMessage = useMessageDispatch();
  const [ account, setAccount ] = useState<AccountDetail>();
  const [ category, setCategory ] = useState<Category>();
  const [ categories, setCategories ] = useState<Array<Category>>([]);
  const [ selectors, setSelectors ] = useState<Array<CategorySelector>>([]);
  const [ selector, setSelector ] = useState<CategorySelector>({ });

  useEffect(() => {
    if (props.open && props.transaction !== undefined) {
      AccountService.get(props.transaction.accountId).then(response => setAccount(response));
    } else {
      setAccount(undefined);
    }
  }, [ props.open, props.transaction?.accountId ]);

  useEffect(() => {
    if (props.open && props.transaction !== undefined) {
      setSelector({
        infoContains: props.transaction.additionalInformation,
        refContains: props.transaction.reference,
        creditorContains: props.transaction.creditorName
      });
    }
  }, [ props.open, props.transaction ]);

  useEffect(() => {
    if (account && category) {
      CategoryService.getCategorySelectors(category.id!!, account.id)
        .then( response => { setSelectors(response); })
    } else {
      setSelectors([]);
    }
  }, [ account, category ]);

  useEffect(() => {
    if (props.open) {
      CategoryService.getAll().then( response => setCategories(response.items));
    }
  }, [ props.open ]);

  function select(s: CategorySelector) {
    setSelector(s);
  }

  function selectCategory(categoryId: string) {
    if (categories !== undefined && categoryId !== undefined && categoryId.length > 0) {
      setCategory(categories.find(cat => cat.id === categoryId));
    } else {
      setCategory(undefined);
    }
  }

  const handleCancel = () => {
    props.onCancel();
  };

  function handleConfirm() {
    if (category) {
      CategoryService.setCategorySelectors(category.id!!, account!.id, [ ...selectors, selector ]);
      showMessage({ type: 'add', level: 'success', text: `Successfully added to Category "${category.name}"` });
      props.onConfirm(category, selector);
    }
  };

  function validateForm(): boolean {
    return category !== undefined && selector !== undefined && 
      ((selector.infoContains !== undefined && selector.infoContains.trim().length > 0) ||
       (selector.refContains !== undefined && selector.refContains.trim().length > 0) ||
       (selector.creditorContains !== undefined && selector.creditorContains.trim().length > 0));
  }

  return (
    <Dialog open={ props.open } onClose={ handleCancel } fullWidth>
      <DialogTitle>Add To Category</DialogTitle>
      <DialogContent>
        <Select fullWidth value={ category ? category?.id : ""}
          onChange={(e) => selectCategory(e.target.value as string)}>
          { categories && categories.map(cat =>
            <MenuItem value={ cat.id }>{ cat.name }</MenuItem>
          )}
        </Select>
      </DialogContent>
      <DialogContent dividers>
        <TextField
          id="name" label="Info Contains" autoFocus
          margin="dense" fullWidth variant="standard"
          value={selector.infoContains}
          onChange={(e) => setSelector({ ...selector, infoContains: e.target.value })}
        />

        <TextField
          id="ref" label="Reference Contains"
          margin="dense" fullWidth variant="standard"
          value={selector.refContains}
          onChange={(e) => setSelector({ ...selector, refContains: e.target.value })}
        />

        <TextField
          id="creditor" label="Creditor Contains"
          margin="dense" fullWidth variant="standard"
          value={selector.creditorContains}
          onChange={(e) => setSelector({ ...selector, creditorContains: e.target.value })}
        />
      </DialogContent>

      <DialogContent>
        <TableContainer>
          <Table size='small'>
            <TableHead>
              <TableRow>
                <TableCell sx={colhead}>Info</TableCell>
                <TableCell sx={colhead}>Reference</TableCell>
                <TableCell sx={colhead}>Creditor</TableCell>
              </TableRow>
            </TableHead>

            <TableBody>
              { selectors && selectors.map((entry, index) =>
                  <TableRow key={ index } hover onClick={() => select(entry)}>
                    <TableCell>{ entry.infoContains }</TableCell>
                    <TableCell>{ entry.refContains }</TableCell>
                    <TableCell>{ entry.creditorContains }</TableCell>
                  </TableRow>
                )
              }
            </TableBody>
          </Table>
        </TableContainer>

      </DialogContent>

      <DialogActions>
        <Button onClick={handleCancel} variant="outlined">Cancel</Button>
        <Button onClick={handleConfirm} variant="contained" disabled={!validateForm()}>
          Add
        </Button>
      </DialogActions>
    </Dialog>
  );
}
