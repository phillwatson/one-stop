import { useEffect, useState } from "react";
import { Box, Button, Dialog, DialogActions, DialogContent, DialogTitle, Divider, FormControl, Grid, InputLabel, MenuItem, Select, SxProps, TextField, Tooltip } from "@mui/material";
import Item from "@mui/material/Grid";
import Table from '@mui/material/Table';
import TableBody from '@mui/material/TableBody';
import TableCell from '@mui/material/TableCell';
import TableContainer from '@mui/material/TableContainer';
import TableHead from '@mui/material/TableHead';
import TableRow from '@mui/material/TableRow';
import DeleteIcon from '@mui/icons-material/DeleteOutlined';
import PieChartIcon from '@mui/icons-material/PieChart';

import CategoryService from "../../services/category.service";
import { CategoryGroup, Category, CategorySelector } from "../../model/category.model";
import { TransactionDetail } from "../../model/account.model";
import { useMessageDispatch } from "../../contexts/messages/context";

const colhead: SxProps = {
  fontWeight: 'bold'
};

const NULL_GROUP: CategoryGroup = { name: '', description: '' };
const NULL_CATEGORY: Category = { name: '', description: '', colour: '' };
const NULL_SELECTOR: CategorySelector = { categoryId: '', accountId: '', infoContains: '', refContains: '', creditorContains: '' };

interface Props {
  open: boolean;
  groupId: string;
  catagoryId?: string; // optional current category
  transaction?: TransactionDetail;
  onConfirm: (category: Category, selector: CategorySelector) => void;
  onCancel: () => void;
}

export default function AddSelector(props: Props) {
  const showMessage = useMessageDispatch();
  const [ groups, setGroups ] = useState<Array<CategoryGroup>>([]);
  const [ group, setGroup ] = useState<CategoryGroup>(NULL_GROUP);
  const [ categories, setCategories ] = useState<Array<Category>>([]);
  const [ category, setCategory ] = useState<Category>(NULL_CATEGORY);
  const [ selectors, setSelectors ] = useState<Array<CategorySelector>>([]);
  const [ selector, setSelector ] = useState<CategorySelector>(NULL_SELECTOR);

  const accountId = props.transaction?.accountId;

  useEffect(() => {
    if (props.open) {
      CategoryService.fetchAllGroups().then( response => setGroups(response) );
    }
  }, [ props.open ]);

  useEffect(() => {
    if (props.groupId) {
      setGroup(groups.find(group => group.id === props.groupId) || NULL_GROUP);
    } else {
      setGroup(NULL_GROUP);
    }
  }, [ groups, props.groupId ]);

  useEffect(() => {
    if (group && group.id) {
      CategoryService.fetchAllCategories(group.id).then( response => setCategories(response) );
    } else {
      setCategories([]);
    }
  }, [ group, props.catagoryId ]);

  useEffect(() => {
    if (props.catagoryId) {
      setCategory(categories.find(category => category.id === props.catagoryId) || NULL_CATEGORY);
    } else {
      setCategory(NULL_CATEGORY);
    }
  }, [ categories, props.catagoryId ]);

  useEffect(() => {
    if (props.open && props.transaction !== undefined) {
      setSelector({
        categoryId: props.catagoryId!,
        accountId: props.transaction.accountId,
        infoContains: props.transaction.additionalInformation,
        refContains: props.transaction.reference,
        creditorContains: props.transaction.creditorName
      });
    } else {
      setSelector(NULL_SELECTOR);
    }
  }, [ props.open, props.catagoryId, props.transaction ]);

  useEffect(() => {
    if (accountId && category.id) {
      CategoryService.getAllAccountCategorySelectors(category.id!!, accountId)
        .then(response => { setSelectors(response); })
    } else {
      setSelectors([]);
    }
  }, [ accountId, category.id ]);

  function selectGroup(groupId: string) {
    if (groups !== undefined && groupId !== undefined && groupId.length > 0) {
      setGroup(groups.find(grp => grp.id === groupId) || NULL_GROUP);
    } else {
      setGroup(NULL_GROUP);
    }

    setCategory(NULL_CATEGORY);
  }

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
      CategoryService.setAccountCategorySelectors(category.id!!, accountId, [ ...selectors, selector ])
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
    <Dialog open={ props.open } onClose={ handleCancel }>
      <DialogTitle sx={{ bgcolor: 'primary.main', color: 'white'}}>Add To Category</DialogTitle>
      <br/>
      <DialogContent>
        <Grid container direction="column" spacing={ 2 }>
          <Grid>
            <Item>
              <FormControl fullWidth>
                <InputLabel id="select-group">Category Group</InputLabel>
                <Select labelId="select-group" label="Category Group" value={ group.id || ''}
                  onChange={(e) => selectGroup(e.target.value as string)} >
                  { groups && groups.map(group =>
                    <MenuItem value={ group.id } key={ group.id }>{ group.name }</MenuItem>
                  )}
                </Select>
              </FormControl>
            </Item>
          </Grid>
          <Grid>
            <Item>
              <FormControl fullWidth>
                <InputLabel id="select-category">Category</InputLabel>
                <Select labelId="select-category" label="Category" value={ category.id || ''}
                  onChange={(e) => selectCategory(e.target.value as string)} renderValue={() => (
                    <Box sx={{ display: 'flex', flexWrap: 'wrap', gap: 0.5, alignItems: 'baseline'}}>
                      <Box component={ PieChartIcon } color={ category.colour } width={ 24 } height={ 24 } alignSelf="center"/>
                      &nbsp;{ category?.name }
                    </Box>
                  )}>
                  { categories && categories.map(cat =>
                    <MenuItem value={ cat.id } key={ cat.id }>
                      <Box component={ PieChartIcon } color={ cat.colour } />
                      &nbsp;{ cat.name }
                    </MenuItem>
                  )}
                </Select>
              </FormControl>
            </Item>
          </Grid>
        </Grid>
      </DialogContent>

      <DialogContent>
        <Divider textAlign="left" sx={{fontWeight: 'light'}}>Enter selector criteria (case-sensitive):</Divider>
        <TextField
          id="name" label="Additional Info Contains (optional)" margin="dense" fullWidth variant="standard" autoFocus 
          value={selector.infoContains || ''}
          onChange={(e) => setSelector({ ...selector, infoContains: e.target.value })}
        />

        <TextField
          id="ref" label="Reference Contains (optional)" margin="dense" fullWidth variant="standard"
          value={selector.refContains || ''}
          onChange={(e) => setSelector({ ...selector, refContains: e.target.value })}
        />

        <TextField
          id="creditor" label="Creditor Contains (optional)" margin="dense" fullWidth variant="standard"
          value={selector.creditorContains || ''}
          onChange={(e) => setSelector({ ...selector, creditorContains: e.target.value })}
        />
      </DialogContent>

      <DialogContent>
        <Divider textAlign="left" sx={{fontWeight: 'light'}}>Or choose from existing selectors:</Divider>
        <TableContainer style={{ maxHeight: "280px" }}>
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
