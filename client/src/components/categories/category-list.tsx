import Table from '@mui/material/Table';
import TableBody from '@mui/material/TableBody';
import TableCell from '@mui/material/TableCell';
import TableContainer from '@mui/material/TableContainer';
import TableHead from '@mui/material/TableHead';
import TableRow from '@mui/material/TableRow';
import Tooltip from '@mui/material/Tooltip';
import { SxProps } from '@mui/material/styles';
import Fab from '@mui/material/Fab';
import DeleteIcon from '@mui/icons-material/DeleteOutline';
import AddIcon from '@mui/icons-material/Add';

import CategoryService from '../../services/category.service';
import { Category } from "../../model/category.model";
import { useEffect, useState } from 'react';
import { useMessageDispatch } from '../../contexts/messages/context';
import ConfirmationDialog from '../dialogs/confirm-dialog';
import EditCategory from './edit-category';
import { Avatar } from '@mui/material';

const colhead: SxProps = {
  fontWeight: 'bold'
};

const bottomFabStyle: SxProps = {
  position: 'fixed',
  bottom: 16,
  right: 16,
};

export default function CategoryEditor() {
  const showMessage = useMessageDispatch();
  const [ categories, setCategories ] = useState<Array<Category>>([]);
  const [ selectedCategory, setSelectedCategory ] = useState<Category|undefined>(undefined);
  const [ deleteDialogOpen, setDeleteDialogOpen ] = useState<boolean>(false);
  const [ editDialogOpen, setEditDialogOpen ] = useState<boolean>(false);
  useEffect(() => { refresh(); }, []);

  function refresh() {
    CategoryService.fetchAllCategories().then( response => setCategories(response));
  }

  function confirmDelete(category: Category) {
    setSelectedCategory(category);
    setDeleteDialogOpen(true);
  }

  function onDeleteConfirmed() {
    setDeleteDialogOpen(false);

    var category = selectedCategory!
    CategoryService.deleteCategory(category.id!!)
      .then(() => setCategories(categories.filter(c => c.id !== category.id)))
      .catch(err => showMessage(err))
  }

  function updateCategory(category: Category) {
    setSelectedCategory(category);
    setEditDialogOpen(true);
  }

  function addCategory() {
    setSelectedCategory(undefined);
    setEditDialogOpen(true);
  }

  function editCategoryConfirmed(category: Category) {
    if (category.id) {
      CategoryService.updateCategory(category)
        .then(() => {
          showMessage({ type: 'add', level: 'success', text: `Category "${category.name}" updated successfully` });
          setEditDialogOpen(false);
          refresh();
        })
        .catch(err => showMessage(err))
    } else {
      CategoryService.createCategory(category)
        .then(() => {
          showMessage({ type: 'add', level: 'success', text: `Category "${category.name}" added successfully`});
          setEditDialogOpen(false);
          refresh();
        })
        .catch(err => showMessage(err))
    }
  }

  return (
    <>
      <TableContainer>
        <Table size='small'>
          <TableHead>
            <TableRow>
              <TableCell sx={colhead} width={"32px"}></TableCell>
              <TableCell sx={colhead}>Name</TableCell>
              <TableCell sx={colhead}>Description</TableCell>
              <TableCell sx={colhead} width={"32px"}></TableCell>
            </TableRow>
          </TableHead>

          <TableBody>
            { categories && categories
              .sort((a, b) => a.name < b.name ? -1 : 1)
              .map(category =>
                <TableRow key={ category.id } hover>
                  <TableCell onClick={() => updateCategory(category)}>
                    <Avatar sx={{ backgroundColor: category.colour, width: 24, height: 24 }}>&nbsp;</Avatar>
                  </TableCell>
                  <TableCell onClick={() => updateCategory(category)}>{ category.name }</TableCell>
                  <TableCell onClick={() => updateCategory(category)}>{ category.description }</TableCell>
                  <TableCell onClick={() => confirmDelete(category)}>
                    <Tooltip title="Delete category..."><DeleteIcon/></Tooltip>
                  </TableCell>
                </TableRow>
              )
            }
          </TableBody>
        </Table>
      </TableContainer>

      <Fab color="primary" aria-label="add" sx={bottomFabStyle} onClick={() => addCategory()}><AddIcon /></Fab>
      <EditCategory open={editDialogOpen}
        category={selectedCategory}
        onCancel={() => setEditDialogOpen(false)}
        onConfirm={editCategoryConfirmed}/>

      <ConfirmationDialog open={deleteDialogOpen}
        title={"Delete Category \""+ selectedCategory?.name + "\""}
        content="Are you sure you want to delete this category?"
        onConfirm={onDeleteConfirmed}
        onCancel={() => setDeleteDialogOpen(false)} />
    </>
  );
}