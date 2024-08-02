import { useState } from 'react';

import Table from '@mui/material/Table';
import TableBody from '@mui/material/TableBody';
import TableCell from '@mui/material/TableCell';
import TableContainer from '@mui/material/TableContainer';
import TableHead from '@mui/material/TableHead';
import TableRow from '@mui/material/TableRow';
import Tooltip from '@mui/material/Tooltip';
import { SxProps } from '@mui/material/styles';
import DeleteIcon from '@mui/icons-material/DeleteOutline';
import EditIcon from '@mui/icons-material/Edit';
import Paper from '@mui/material/Paper';

import { useMessageDispatch } from '../../contexts/messages/context';
import { CategoryGroup } from "../../model/category.model";
import CategoryService from '../../services/category.service';
import ConfirmationDialog from '../dialogs/confirm-dialog';
import EditCategoryGroup from './edit-category-group';

const colhead: SxProps = {
  fontWeight: 'bold'
};

const compactCell = {
  paddingLeft: 0,
  paddingRight: 0
};

interface Props {
  elevation?: number;
  groups: Array<CategoryGroup>;
  onSelected?: (group: CategoryGroup) => void;
  onDelete?: (group: CategoryGroup) => void;
  onEdit?: (group: CategoryGroup) => void;
  onAdd?: (group: CategoryGroup) => void;
}

export default function CategoryGroupList(props: Props) {
  const showMessage = useMessageDispatch();

  const [ selectedGroup, setSelectedGroup ] = useState<CategoryGroup|undefined>(undefined);
  const [ deleteGroupDialogOpen, setDeleteGroupDialogOpen ] = useState<boolean>(false);
  const [ editGroupDialogOpen, setEditGroupDialogOpen ] = useState<boolean>(false);

  function editGroupConfirmed(group: CategoryGroup) {
    if (group.id) {
      CategoryService.updateGroup(group)
        .then(group => {
          showMessage({ type: 'add', level: 'success', text: `Category Group "${group.name}" updated successfully` });
          setEditGroupDialogOpen(false);

          props.onEdit && props.onEdit(group);
          setSelectedGroup(group);
        })
        .catch(err => showMessage(err))
    } else {
      CategoryService.createGroup(group)
        .then(group => {
          showMessage({ type: 'add', level: 'success', text: `Category Group "${group.name}" added successfully`});
          setEditGroupDialogOpen(false);

          props.onAdd && props.onAdd(group);
          setSelectedGroup(group);
        })
        .catch(err => showMessage(err))
    }
  }

  function handleSelectClick(group: CategoryGroup) {
    setSelectedGroup(group);
    props.onSelected && props.onSelected(group);
  }

  function handleEditClick(group: CategoryGroup) {
    if (props.onEdit) {
      handleSelectClick(group);
      setEditGroupDialogOpen(true);
    }
  }

  function handleAddClick() {
    if (props.onAdd) {
      setSelectedGroup(undefined);
      setEditGroupDialogOpen(true);
    }
  }

  function handleDeleteClick(group: CategoryGroup) {
    if (props.onDelete) {
      setSelectedGroup(group);
      setDeleteGroupDialogOpen(true);
    }
  }

  function onDeleteConfirmed() {
    setDeleteGroupDialogOpen(false);

    var group = selectedGroup
    if (group !== undefined) {
      CategoryService.deleteGroup(group.id!!)
        .then(() => props.onDelete && props.onDelete(group!!))
        .catch(err => showMessage(err))
    }
  }

  return (
    <Paper elevation={props.elevation || 0} sx={{ padding: 1}}>
      <TableContainer>
        <Table size='small'>
          <TableHead>
            <TableRow>
              <TableCell sx={colhead}>Group</TableCell>
              <TableCell sx={colhead}>Description</TableCell>
              { props.onEdit &&
                <TableCell sx={{ ...colhead, ...compactCell}} width="24px"></TableCell>
              }
              { props.onDelete &&
                <TableCell sx={{ ...colhead, ...compactCell}} width="24px"></TableCell>
              }
            </TableRow>
          </TableHead>

          <TableBody>
            { props.groups && props.groups
              .sort((a, b) => a.name < b.name ? -1 : 1)
              .map(group =>
                <TableRow key={ group.id } hover selected={ selectedGroup && group.id === selectedGroup.id }>
                  <TableCell onClick={() => handleSelectClick(group)}>{ group.name }</TableCell>
                  <TableCell onClick={() => handleSelectClick(group)}>{ group.description }</TableCell>
                  { props.onEdit &&
                    <TableCell onClick={() => handleEditClick(group)} sx={ compactCell } width="24px">
                      <Tooltip title="Edit category group..."><EditIcon/></Tooltip>
                    </TableCell>
                  }
                  { props.onDelete &&
                    <TableCell onClick={() => handleDeleteClick(group)} sx={ compactCell } width="24px">
                      <Tooltip title="Delete category group..."><DeleteIcon/></Tooltip>
                    </TableCell>
                  }
                </TableRow>
              )
            }
            { props.onAdd &&
            <>
              <TableRow key={ "add1" }>
                <TableCell align="center" colSpan={ 3 + (props.onDelete ? 1 : 0) + (props.onEdit ? 1 : 0) } />
              </TableRow>
              <TableRow key={ "add2" } hover>
                <TableCell align="center" colSpan={ 3 + (props.onDelete ? 1 : 0) + (props.onEdit ? 1 : 0) }
                  onClick={() => handleAddClick()}><b>Add Category Group...</b></TableCell>
              </TableRow>
              </>
            }
         </TableBody>
        </Table>
      </TableContainer>

      <EditCategoryGroup open={ editGroupDialogOpen }
        group={ selectedGroup }
        onConfirm={ editGroupConfirmed }
        onCancel={() => setEditGroupDialogOpen(false)}/>

      <ConfirmationDialog open={ deleteGroupDialogOpen }
        title={"Delete Category Group \""+ selectedGroup?.name + "\""}
        content="Are you sure you want to delete this category?"
        onConfirm={ onDeleteConfirmed }
        onCancel={() => setDeleteGroupDialogOpen(false)} />
   </Paper>
  );
}