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
import { Avatar, Paper } from '@mui/material';

import CategoryService from '../../services/category.service';
import { CategoryGroup, Category } from "../../model/category.model";
import { useMessageDispatch } from '../../contexts/messages/context';
import ConfirmationDialog from '../dialogs/confirm-dialog';
import EditCategory from './edit-category';

const colhead: SxProps = {
  fontWeight: 'bold'
};

const compactCell = {
  paddingLeft: 0.5,
  paddingRight: 0
};

interface Props {
  elevation?: number;
  group?: CategoryGroup;
  categories: Array<Category>;
  onSelected?: (category: Category) => void;
  onDelete?: (category: Category) => void;
  onEdit?: (category: Category) => void;
  onAdd?: (category: Category) => void;
}

export default function CategoryList(props: Props) {
  const showMessage = useMessageDispatch();
  const [ selectedCategory, setSelectedCategory ] = useState<Category|undefined>(undefined);
  const [ deleteDialogOpen, setDeleteDialogOpen ] = useState<boolean>(false);
  const [ editDialogOpen, setEditDialogOpen ] = useState<boolean>(false);

  function handleSelectClick(category: Category) {
    if (props.onSelected) {
      props.onSelected(category);
    } else {
      handleEditClick(category);
    }
  }

  function handleEditClick(category: Category) {
    setSelectedCategory(category);
    setEditDialogOpen(true);
  }

  function handleAddClick() {
    setSelectedCategory(undefined);
    setEditDialogOpen(true);
  }

  function handleDeleteClick(category: Category) {
    setSelectedCategory(category);
    setDeleteDialogOpen(true);
  }

  function onDeleteConfirmed() {
    setDeleteDialogOpen(false);

    var category: Category = selectedCategory!
    CategoryService.deleteCategory(category.id!!)
      .then(() => props.onDelete && props.onDelete(category))
      .catch(err => showMessage(err))
  }

  function editCategoryConfirmed(category: Category) {
    if (category.id) {
      CategoryService.updateCategory(category)
        .then(category => {
          showMessage({ type: 'add', level: 'success', text: `Category "${category.name}" updated successfully` });
          setEditDialogOpen(false);

          props.onEdit && props.onEdit(category);
        })
        .catch(err => showMessage(err))
    } else {
      CategoryService.createCategory(props.group!.id!!, category)
        .then(category => {
          showMessage({ type: 'add', level: 'success', text: `Category "${category.name}" added successfully`});
          setEditDialogOpen(false);

          props.onAdd && props.onAdd(category);
        })
        .catch(err => showMessage(err))
    }
  }

  return (
    <Paper elevation={props.elevation || 0} sx={{ padding: 1}}>
      <TableContainer>
        <Table size='small'>
          <TableHead>
            <TableRow>
              <TableCell sx={{...colhead, ...compactCell}} width="24px"></TableCell>
              <TableCell sx={colhead}>Category</TableCell>
              <TableCell sx={colhead}>Description</TableCell>
              { props.onDelete &&
                <TableCell sx={{...colhead, ...compactCell}} width="24px"></TableCell>
              }
            </TableRow>
          </TableHead>

          <TableBody>
            { props.categories && props.categories
              .sort((a, b) => a.name.localeCompare(b.name))
              .map(category =>
                <TableRow key={ category.id } hover>
                  <TableCell sx={ compactCell } onClick={() => handleSelectClick(category)}>
                    <Avatar sx={{ backgroundColor: category.colour, width: 24, height: 24 }}>&nbsp;</Avatar>
                  </TableCell>
                  <TableCell onClick={() => handleSelectClick(category)}>{ category.name }</TableCell>
                  <TableCell onClick={() => handleSelectClick(category)}>{ category.description }</TableCell>
                  { props.onDelete &&
                    <TableCell onClick={() => handleDeleteClick(category)} sx={ compactCell } width="24px">
                      <Tooltip title="Delete category..."><DeleteIcon/></Tooltip>
                    </TableCell>
                  }
                </TableRow>
              )
            }
            { props.onAdd && props.group &&
            <>
              <TableRow key={ "add1" }>
                <TableCell align="center" colSpan={ 3 + (props.onDelete ? 1 : 0) } />
              </TableRow>
              <TableRow key="add2" hover>
                <TableCell align="center" colSpan={ 3 + (props.onDelete ? 1 : 0) }
                  onClick={() => handleAddClick()}><b>Add Category...</b></TableCell>
              </TableRow>
            </>
            }
          </TableBody>
        </Table>
      </TableContainer>

      { props.group &&
        <EditCategory open={editDialogOpen}
        group={props.group!!}
        category={selectedCategory}
        onConfirm={ editCategoryConfirmed }
        onCancel={() => setEditDialogOpen(false)}/>
      }

      <ConfirmationDialog open={deleteDialogOpen}
        title={"Delete Category \""+ selectedCategory?.name + "\""}
        content="Are you sure you want to delete this category?"
        onConfirm={ onDeleteConfirmed }
        onCancel={() => setDeleteDialogOpen(false)} />
    </Paper>
  );
}