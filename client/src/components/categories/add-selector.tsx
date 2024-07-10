import { useEffect, useState } from "react";
import { Avatar, Box, Button, Dialog, DialogActions, DialogContent, DialogTitle, Divider, MenuItem, Select, SxProps, TextField, Tooltip } from "@mui/material";
import Table from '@mui/material/Table';
import TableBody from '@mui/material/TableBody';
import TableCell from '@mui/material/TableCell';
import TableContainer from '@mui/material/TableContainer';
import TableHead from '@mui/material/TableHead';
import TableRow from '@mui/material/TableRow';
import DeleteIcon from '@mui/icons-material/Clear';

import CategoryService from "../../services/category.service";
import { Category, CategorySelector } from "../../model/category.model";
import { TransactionDetail } from "../../model/account.model";
import { useMessageDispatch } from "../../contexts/messages/context";

const colhead: SxProps = {
  fontWeight: 'bold'
};

const NULL_CATEGORY: Category = { name: '', description: '', colour: '' };
const NULL_SELECTOR: CategorySelector = { infoContains: '', refContains: '', creditorContains: '' };

interface Props {
  open: boolean;
  transaction?: TransactionDetail;
  onConfirm: (category: Category, selector: CategorySelector) => void;
  onCancel: () => void;
}

export default function AddSelector(props: Props) {
  const showMessage = useMessageDispatch();
  const [ category, setCategory ] = useState<Category>(NULL_CATEGORY);
  const [ categories, setCategories ] = useState<Array<Category>>([]);
  const [ selector, setSelector ] = useState<CategorySelector>(NULL_SELECTOR);
  const [ selectors, setSelectors ] = useState<Array<CategorySelector>>([]);

  const accountId = props.transaction?.accountId;

  useEffect(() => {
    if (props.open) {
      CategoryService.fetchAllCategories().then( response => setCategories(response));
    }
  }, [ props.open ]);

  useEffect(() => {
    if (props.open && props.transaction !== undefined) {
      setSelector({
        infoContains: props.transaction.additionalInformation,
        refContains: props.transaction.reference,
        creditorContains: props.transaction.creditorName
      });
    } else {
      setSelector(NULL_SELECTOR);
    }
  }, [ props.open, props.transaction ]);

  useEffect(() => {
    if (props.open && accountId && category.id) {
      CategoryService.getCategorySelectors(category.id!!, accountId)
        .then(response => { setSelectors(response); })
    } else {
      setSelectors([]);
    }
  }, [ props.open, accountId, category.id ]);

  function selectCategory(categoryId: string) {
    if (categories !== undefined && categoryId !== undefined && categoryId.length > 0) {
      setCategory(categories.find(cat => cat.id === categoryId) || NULL_CATEGORY);
    } else {
      setCategory(NULL_CATEGORY);
    }
  }

  function removeSelector(selector: CategorySelector) {
    setSelectors(selectors.filter(sel => sel !== selector));
  }

  const handleCancel = () => {
    props.onCancel();
  };

  function handleConfirm() {
    if (accountId && category.id) {
      CategoryService.setCategorySelectors(category.id!!, accountId, [ ...selectors, selector ])
      .then(() => {
        showMessage({ type: 'add', level: 'success', text: `Successfully added to Category "${category.name}"` });
        props.onConfirm(category, selector);
      });
    }
  };

  function validateForm(): boolean {
    return category.id !== undefined && selector !== undefined && 
      ((selector.infoContains !== undefined && selector.infoContains.trim().length > 0) ||
       (selector.refContains !== undefined && selector.refContains.trim().length > 0) ||
       (selector.creditorContains !== undefined && selector.creditorContains.trim().length > 0));
  }

  return (
    <Dialog open={ props.open } onClose={ handleCancel } fullWidth>
      <DialogTitle sx={{ bgcolor: 'primary.main', color: 'white'}}>Add To Category</DialogTitle>
      <DialogContent>
        <p></p>
        <Select fullWidth value={ category.id || ''}
          onChange={(e) => selectCategory(e.target.value as string)}
          renderValue={() => (
            <Box sx={{ display: 'flex', flexWrap: 'wrap', gap: 0.5, alignItems: 'baseline'}}>
              <Avatar sx={{ backgroundColor: category?.colour, width: 24, height: 24 }}>&nbsp;</Avatar>&nbsp;{ category?.name }
            </Box>
          )}>
          { categories && categories.map(cat =>
            <MenuItem value={ cat.id } key={ cat.id }>
              <Avatar sx={{ backgroundColor: cat.colour, width: 24, height: 24 }}>&nbsp;</Avatar>&nbsp;{ cat.name }
            </MenuItem>
          )}
        </Select>
      </DialogContent>

      <DialogContent>
        <Divider textAlign="left" sx={{fontWeight: 'light'}}>Enter selector criteria (case-sensitive):</Divider>
        <TextField
          id="name" label="Additional Info Contains" margin="dense" fullWidth variant="standard" autoFocus 
          value={selector.infoContains || ''}
          onChange={(e) => setSelector({ ...selector, infoContains: e.target.value })}
        />

        <TextField
          id="ref" label="Reference Contains" margin="dense" fullWidth variant="standard"
          value={selector.refContains || ''}
          onChange={(e) => setSelector({ ...selector, refContains: e.target.value })}
        />

        <TextField
          id="creditor" label="Creditor Contains" margin="dense" fullWidth variant="standard"
          value={selector.creditorContains || ''}
          onChange={(e) => setSelector({ ...selector, creditorContains: e.target.value })}
        />
      </DialogContent>

      <DialogContent>
        <Divider textAlign="left" sx={{fontWeight: 'light'}}>Or choose from existing selectors:</Divider>
        <TableContainer>
          <Table size='small'>
            <TableHead>
              <TableRow>
                <TableCell sx={colhead}>Info</TableCell>
                <TableCell sx={colhead}>Reference</TableCell>
                <TableCell sx={colhead}>Creditor</TableCell>
                <TableCell sx={colhead}></TableCell>
              </TableRow>
            </TableHead>

            <TableBody>
              { selectors && selectors.map((entry, index) =>
                  <TableRow key={ index } hover onClick={() => setSelector(entry)}>
                    <TableCell>{ entry.infoContains }</TableCell>
                    <TableCell>{ entry.refContains }</TableCell>
                    <TableCell>{ entry.creditorContains }</TableCell>
                    <TableCell width={"22px"} onClick={(e) => { removeSelector(entry); e.stopPropagation();}}>
                      <Tooltip title="Delete selector"><DeleteIcon/></Tooltip>
                  </TableCell>
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
