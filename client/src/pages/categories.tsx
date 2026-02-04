import { useState } from "react";
import { DndContext, DragOverlay, DragEndEvent, DragStartEvent } from '@dnd-kit/core';

import Grid from "@mui/material/Grid";
import CategoriesIcon from '@mui/icons-material/Category';

import { useMessageDispatch } from '../contexts/messages/context';
import CategoryService from "../services/category.service";
import { CategoryGroup, Category, CategorySelector, selectorName } from "../model/category.model";
import PageHeader from "../components/page-header/page-header";
import CategoryTree from "../components/category/category-tree";
import Selectors from "../components/category/selector-list";
import { DraggableSectorRow, DraggedSelector } from "../components/category/selector-row";

export default function Categories() {
  const showMessage = useMessageDispatch();
  const [ currentCategory, setCurrentCategory ] = useState<Category | undefined>(undefined);
  const [ selectors, setSelectors ] = useState<Array<CategorySelector>>([]);
  const [ draggingSelector, setDraggingSelector ] = useState<DraggedSelector | null>(null);

  function handleDragStart(event: DragStartEvent) {
    const draggedSelector = event.active.data.current as unknown as DraggedSelector;
    setDraggingSelector(draggedSelector);
  }

  function handleDragEnd(event: DragEndEvent) {
    const category = event.over?.data.current as unknown as Category;
    const draggedSelector = event.active.data.current as unknown as DraggedSelector;
    if (category === undefined || draggedSelector.selector === undefined) {
      return;
    }

    if (draggedSelector.selector.categoryId === category.id) {
      return;
    }
    
    const selector = draggedSelector.selector;
    CategoryService.moveCategorySelector(selector, category.id!)
      .then(() => {
        showMessage({ type: 'add', level: 'success', text: `Selector "${selectorName(selector)}" moved successfully` });

        // delete selector node from local state
        setSelectors(selectors.filter(s => s.id !== selector.id));
      })
      .catch(err => showMessage(err))
  }

  function deleteSelector(selector: CategorySelector) {
    if (selector) {
      CategoryService.deleteCategorySelector(selector)
        .then(() => {
          showMessage({ type: 'add', level: 'success', text: `Selector "${selectorName(selector)}" deleted` });

          // delete selector from local state
          setSelectors(selectors.filter(s => s.id !== selector.id));
        })
        .catch(err => showMessage(err));
    }
  }

  function selectCategory(group?: CategoryGroup, category?: Category) {
    setCurrentCategory(category)
    if (category) {
      CategoryService
        .getAllCategorySelectors(category?.id!)
        .then(response => setSelectors(response))
        .catch(err => showMessage(err));
    } else {
      setSelectors([]);
    }
  }

  return (
    <PageHeader title="Transaction Categories" icon={ <CategoriesIcon /> }>
      <DndContext onDragStart={ handleDragStart } onDragEnd={ handleDragEnd }>
        <Grid container direction="row" wrap="nowrap" width="100%" columnGap={2}>
          <Grid width="29%">
              <CategoryTree onSelectCategory={ selectCategory } />
          </Grid>
          <Grid width="69%">
              <Selectors category={ currentCategory } selectors={ selectors } onDeleteSelector={ deleteSelector } />
          </Grid>
        </Grid>


        <DragOverlay dropAnimation={ null } zIndex={ 2 }>
          { draggingSelector &&
            <DraggableSectorRow
              key={ draggingSelector.selector.id! }
              selector={ draggingSelector.selector }
              accountName={ draggingSelector.accountName } />
          }
        </DragOverlay>
      </DndContext>
    </PageHeader>
  );
}
