import { useEffect, useState } from "react";
import Grid from "@mui/material/Grid";
import Item from "@mui/material/Grid";

import PageHeader from "../components/page-header/page-header";
import CategoryGroupList from "../components/categories/category-group-list";
import CategoryList from "../components/categories/category-list";
import { CategoryGroup, Category } from "../model/category.model";
import CategoryService from "../services/category.service";

export default function Categories() {
  const [ groups, setGroups ] = useState<Array<CategoryGroup>>([]);
  const [ selectedGroup, selectGroup ] = useState<CategoryGroup|undefined>(undefined);
  const [ categories, setCategories ] = useState<Array<Category>>([]);

  useEffect(() => {
    CategoryService.fetchAllGroups().then(response => setGroups(response));
  }, [ ]);

  useEffect(() => {
    if (selectedGroup) {
      CategoryService.fetchAllCategories(selectedGroup.id!!)
        .then( response => setCategories(response));
    }
  }, [ selectedGroup ]);

  function deleteGroup(group: CategoryGroup) {
    selectGroup(undefined);
    setGroups(groups.filter(c => c.id !== group.id));
  }

  function editGroup(group: CategoryGroup) {
    setGroups(groups
      .filter(g => g.id !== group.id)
      .concat(group)
      .sort((a, b) => a.name.localeCompare(b.name))
    );
    selectGroup(group);
  }

  function addGroup(group: CategoryGroup) {
    setGroups(groups
      .concat(group)
      .sort((a, b) => a.name.localeCompare(b.name))
    );
    selectGroup(group);
  }

  function deleteCategory(category: Category) {
    setCategories(categories.filter(c => c.id !== category.id));
  }

  function editCategory(category: Category) {
    setCategories(categories
      .filter(c => c.id !== category.id)
      .concat(category)
      .sort((a, b) => a.name.localeCompare(b.name))
    );
  }

  function addCategory(category: Category) {
    setCategories(categories
      .concat(category)
      .sort((a, b) => a.name.localeCompare(b.name))
    );
  }

  return (
    <PageHeader title="Transaction Category Groups">
      <Grid container direction="row" wrap="nowrap" width="100%" columnGap={2}>
        <Grid width="49%">
          <Item>
            <CategoryGroupList elevation={ 1 }
              groups={ groups }
              onSelected={ selectGroup }
              onAdd={ addGroup }
              onEdit={ editGroup }
              onDelete={ deleteGroup }/>
          </Item>
        </Grid>
        <Grid width="49%">
          <Item>
            <CategoryList elevation={ 1 } group={ selectedGroup } categories={ categories }
              onAdd={ addCategory }
              onEdit={ editCategory }
              onDelete={ deleteCategory }/>
          </Item>
        </Grid>
      </Grid>
    </PageHeader>
  );
}
