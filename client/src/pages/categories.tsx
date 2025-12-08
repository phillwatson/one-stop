import { useState } from "react";
import { DndContext, DragOverlay, DragEndEvent, DragStartEvent } from '@dnd-kit/core';

import Grid from "@mui/material/Grid";
import Item from "@mui/material/Grid";

import { CategoryGroup, Category } from "../model/category.model";
import PageHeader from "../components/page-header/page-header";
import CategoryTree from "../components/category/category-tree";
import Selectors from "../components/category/selector-list";
import { DraggableSectorRow, DraggedSelector } from "../components/category/selector-row";

interface Selection {
  group?: CategoryGroup;
  category?: Category;
};

export default function Categories() {
  const [ selected, setSelected ] = useState<Selection | undefined>(undefined);
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
    console.log(`Dropped selector ${draggedSelector.selector.infoContains} on category ${category.name}`);
  }

  function selectCategory(group: CategoryGroup, category: Category) {
    setSelected({ group, category });
  }

  return (
    <PageHeader title="Transaction Categories">
      <DndContext onDragStart={ handleDragStart } onDragEnd={ handleDragEnd }>
        <Grid container direction="row" wrap="nowrap" width="100%" columnGap={2}>
          <Grid width="29%">
            <Item>
              <CategoryTree onSelectCategory={ selectCategory } />
            </Item>
          </Grid>
          <Grid width="69%">
            <Item>
              <Selectors
                group={ selected?.group }
                category={ selected?.category }
              />
            </Item>
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
