import { useEffect, useState } from "react";

import { SimpleTreeView } from '@mui/x-tree-view/SimpleTreeView';

import { useMessageDispatch } from '../../contexts/messages/context';
import CategoryService from '../../services/category.service';
import { CategoryGroup, Category } from '../../model/category.model';

import GroupTreeItem from './group-tree-item';
import { CategoryTreeItem } from './category-tree-item';
import EditCategoryGroup from './edit-category-group';
import EditCategory from './edit-category';

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
  onSelectCategory?: (group: CategoryGroup, category: Category) => void;
}

export default function CategoryTree(props: Props) {
  const showMessage = useMessageDispatch();
  const [ nodes, setNodes ] = useState<Array<CategoryGroupNode>>([]);
  const [ selectedNode, setSelectedNode ] = useState<CategoryGroupNode | undefined>(undefined);
  const [ selectedCategory, setSelectedCategory ] = useState<Category | undefined>(undefined);
  const [ editGroupOpen, setEditGroupOpen ] = useState<boolean>(false);
  const [ editCategoryOpen, setEditCategoryOpen ] = useState<boolean>(false);

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


  function startGroupEdit(node: CategoryGroupNode) {
    setSelectedNode(node)
    setEditGroupOpen(true);
  }

  function confirmGroupEdit(group: CategoryGroup) {
  }

  function deleteGroup(node: CategoryGroupNode) {
  }

  function startCategoryEdit(node: CategoryGroupNode, category: Category) {
    setSelectedNode(node)
    setSelectedCategory(category);
    setEditCategoryOpen(true);
  }

  function confirmCategoryEdit(category: Category) {
    const node = selectedNode;
    if (node) {
      if (category.id) {
        CategoryService.updateCategory(category)
          .then(category => {
            setEditCategoryOpen(false);
            showMessage({ type: 'add', level: 'success', text: `Category "${category.name}" updated successfully` });

            node.setCategories(node.categories
                .filter(c => c.id !== category.id)
                .concat(category)
                .sort((a, b) => a.name.localeCompare(b.name))
            );
            
            setNodes(nodes
              .filter(n => n.group.id !== category.groupId)
              .concat(node)
              .sort((a, b) => a.group.name.localeCompare(b.group.name))
            );
          })
          .catch(err => showMessage(err))
      } else {
        CategoryService.createCategory(node.group.id!!, category)
          .then(category => {
            setEditCategoryOpen(false);
            showMessage({ type: 'add', level: 'success', text: `Category "${category.name}" added successfully`});
          })
          .catch(err => showMessage(err))
      }
    }
  }

  function clickCategory(node: CategoryGroupNode, category: Category) {
    if (props.onSelectCategory) props.onSelectCategory(node.group, category)
  }

  function deleteCategory(node: CategoryGroupNode, category: Category) {
  }

  return (
    <div style={{ height: '700px', overflow: 'scroll' }}>
      <SimpleTreeView style={{ height: '100%' }} >
        { nodes && nodes.map(node =>
          <GroupTreeItem group={ node.group } key={ node.group.id! }
            onEditClick={() => startGroupEdit(node) }
            onDeleteClick={ () => deleteGroup(node) }
          >

            { node.categories.map(category =>
              <CategoryTreeItem key={ category.id! } itemId={ category.id! }
                category={ category }
                onClick={ () => clickCategory(node, category) }
                onEditClick={ () => startCategoryEdit(node, category) }
                onDeleteClick={ () => deleteCategory(node, category) }
              />
            )}
          </GroupTreeItem>
        )}
      </SimpleTreeView>
  
      { editCategoryOpen &&
        <EditCategory open={ editCategoryOpen }
          group={ selectedNode!.group }
          category={ selectedCategory! }
          onConfirm={ confirmCategoryEdit }
          onCancel={() => setEditCategoryOpen(false)}/>
      }

      { editGroupOpen &&
        <EditCategoryGroup open={ editGroupOpen }
          group={ selectedNode!.group }
          onConfirm={ confirmGroupEdit }
          onCancel={() => setEditGroupOpen(false)}/>
      }
    </div>
  )
}