import { useEffect, useState } from "react";

import { SimpleTreeView } from '@mui/x-tree-view/SimpleTreeView';
import { Box, Divider } from "@mui/material";

import { useMessageDispatch } from '../../contexts/messages/context';
import CategoryService from '../../services/category.service';
import { CategoryGroup, Category } from '../../model/category.model';

import GroupTreeItem from './group-tree-item';
import CategoryTreeItem from './category-tree-item';
import EditCategoryGroup from './edit-category-group';
import EditCategory from './edit-category';
import ConfirmationDialog from '../dialogs/confirm-dialog';

export class CategoryGroupNode {
  group: CategoryGroup;
  categories: Array<Category> = [];

  constructor(group: CategoryGroup) {
    this.group = group;
  }

  setCategories(categories: Array<Category>) {
    this.categories = categories;
  }
};

interface Props {
  onSelectCategory?: (group?: CategoryGroup, category?: Category) => void;
}

export default function CategoryTree(props: Props) {
  const showMessage = useMessageDispatch();
  const [ nodes, setNodes ] = useState<Array<CategoryGroupNode>>([]);
  const [ focusedNode, setFocusedNode ] = useState<CategoryGroupNode | undefined>(undefined);
  const [ focusedCategory, setFocusedCategory ] = useState<Category | undefined>(undefined);
  const [ selectedCategory, setSelectedCategory ] = useState<Category | undefined>(undefined);

  const [ editGroupOpen, setEditGroupOpen ] = useState<boolean>(false);
  const [ editCategoryOpen, setEditCategoryOpen ] = useState<boolean>(false);
  const [ deleteGroupOpen, setDeleteGroupOpen ] = useState<boolean>(false);
  const [ deleteCategoryOpen, setDeleteCategoryOpen ] = useState<boolean>(false);

  useEffect(() => {
    CategoryService
      .fetchAllGroups()
      .then(response => response.map(group => new CategoryGroupNode(group)))
      .then(nodes =>
        Promise.all(nodes.map(node =>
          CategoryService
            .fetchAllCategories(node.group.id!)
            .then(categories => {
              node.setCategories(categories);
              return node;
            })
        ))
      ).then(result => setNodes(result))
  }, []);

  function selectCategory(node?: CategoryGroupNode, category?: Category) {
    setSelectedCategory(category);
    if (props.onSelectCategory) {
      props.onSelectCategory(node?.group, category);
    }
  }

  function startGroupEdit(node?: CategoryGroupNode) {
    setFocusedNode(node)
    setEditGroupOpen(true);
  }

  function confirmGroupEdit(group: CategoryGroup) {
    const creating = group.id === undefined;
    (creating
      ? CategoryService.createGroup(group)
      : CategoryService.updateGroup(group))
      .then(updated => {
        setEditGroupOpen(false);

        const message = `Group "${updated.name}" ${creating ? "added" : "updated"} successfully`
        showMessage({ type: 'add', level: 'success', text: message });
        
        // reflect change on local state
        const node = new CategoryGroupNode(updated);
        if (! creating) node.setCategories(focusedNode!.categories);

        setNodes(nodes
          .filter(n => n.group.id !== updated.id)
          .concat(node!)
          .sort((a, b) => a.group.name.localeCompare(b.group.name))
        );
      })
      .catch(err => showMessage(err))
  }

  function deleteGroup(node: CategoryGroupNode) {
    setFocusedNode(node);
    setDeleteGroupOpen(true);
  }

  function confirmDeleteGroup() {
    const group = focusedNode?.group
    if (group) {
      CategoryService.deleteGroup(group.id!)
        .then(() => {
          setDeleteGroupOpen(false);
          showMessage({ type: 'add', level: 'success', text: `Group "${group.name}" deleted` });

          // delete group node from local state
          setNodes(nodes
            .filter(n => n.group.id !== group.id)
          );

          // reflect deletion on containing element
          if (group.id === selectedCategory?.groupId) {
            selectCategory(undefined, undefined);
          }
        })
        .catch(err => showMessage(err))
    } else {
      setDeleteGroupOpen(false);
    }
  }

  function startCategoryEdit(node: CategoryGroupNode, category?: Category) {
    setFocusedNode(node)
    setFocusedCategory(category);
    setEditCategoryOpen(true);
  }

  function confirmCategoryEdit(category: Category) {
    const node = focusedNode;
    if (node && category) {
      const creating = category.id === undefined;
      (creating
        ? CategoryService.createCategory(node.group.id!!, category)
        : CategoryService.updateCategory(category))
        .then(updated => {
          setEditCategoryOpen(false);

          const message = `Category "${updated.name}" ${creating ? "added" : "updated"} successfully`
          showMessage({ type: 'add', level: 'success', text: message });

          // reflect change on group node's state
          node.setCategories(node.categories
              .filter(c => c.id !== updated.id)
              .concat(updated)
              .sort((a, b) => a.name.localeCompare(b.name))
          );
          
          // reflect change on local state
          setNodes(nodes
            .filter(n => n.group.id !== updated.groupId)
            .concat(node)
            .sort((a, b) => a.group.name.localeCompare(b.group.name))
          );
        })
        .catch(err => showMessage(err))
    } else {
      setEditCategoryOpen(false);
    }
  }

  function deleteCategory(node: CategoryGroupNode, category: Category) {
    setFocusedNode(node);
    setFocusedCategory(category);
    setDeleteCategoryOpen(true);
  }

  function confirmDeleteCategory() {
    const node = focusedNode;
    const category = focusedCategory;
    if (node && category) {
      CategoryService.deleteCategory(category.id!)
        .then(() => {
          setDeleteCategoryOpen(false);
          showMessage({ type: 'add', level: 'success', text: `Category "${category.name}" deleted` });

          // delete catagory from local state
          node.setCategories(node.categories
            .filter(c => c.id !== category.id)
          );

          // reflect deletion on containing element
          if (category.id === selectedCategory?.groupId) {
            selectCategory(undefined, undefined);
          }
        })
        .catch(err => showMessage(err))
    } else {
      setDeleteCategoryOpen(false);
    }
  }

  return (
    <div style={{ height: '700px', overflow: 'scroll' }}>
      <Box style={{ padding: "8px", fontWeight: 'bold' }}>
          Category Groups
        <Divider style={{ paddingTop: "10px"}}/>
      </Box>

      <SimpleTreeView style={{ height: '100%' }} >
        { nodes && nodes.map(node =>
          <GroupTreeItem group={ node.group } key={ node.group.id! } itemId={ node.group.id! }
            onAddCategoryClick={() => startCategoryEdit(node, undefined) }
            onAddGroupClick={ () => startGroupEdit(undefined) }
            onEditClick={() => startGroupEdit(node) }
            onDeleteClick={ () => deleteGroup(node) }
          >

            { node.categories.map(category =>
              <CategoryTreeItem category={ category } key={ category.id! } itemId={ category.id! }
                onClick={ () => selectCategory(node, category) }
                onEditClick={ () => startCategoryEdit(node, category) }
                onDeleteClick={ () => deleteCategory(node, category) }
              />
            )}
          </GroupTreeItem>
        )}
      </SimpleTreeView>
  
      { editCategoryOpen && focusedNode?.group &&
        <EditCategory open={ editCategoryOpen }
          group={ focusedNode.group }
          category={ focusedCategory! }
          onConfirm={ confirmCategoryEdit }
          onCancel={() => setEditCategoryOpen(false)}/>
      }

      { editGroupOpen &&
        <EditCategoryGroup open={ editGroupOpen }
          group={ focusedNode?.group }
          onConfirm={ confirmGroupEdit }
          onCancel={() => setEditGroupOpen(false)}/>
      }
 
      { deleteCategoryOpen &&
        <ConfirmationDialog open={deleteCategoryOpen}
          title={"Delete Category \""+ focusedCategory?.name + "\""}
          content={ [
            "Are you sure you want to delete this category, and it's transaction selectors?",
            "This action cannot be undone." ] }
          onConfirm={ confirmDeleteCategory }
          onCancel={() => setDeleteCategoryOpen(false)} />
      }
 
      { deleteGroupOpen &&
        <ConfirmationDialog open={deleteGroupOpen}
          title={"Delete Category Group \""+ focusedNode?.group.name + "\""}
           content={ [
            "Are you sure you want to delete this group?",
            "This will delete the categories in the group and their transaction selectors.",
            "This action cannot be undone." ] }
          onConfirm={ confirmDeleteGroup }
          onCancel={() => setDeleteGroupOpen(false)} />
      }
    </div>
  )
}