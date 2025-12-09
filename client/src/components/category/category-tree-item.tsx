import { useState, useEffect, MouseEventHandler, MouseEvent } from "react";
import { useDroppable } from '@dnd-kit/core';

import PieChartIcon from '@mui/icons-material/PieChart';
import Box from "@mui/material/Box";

import { Category, CategorySelector } from "../../model/category.model";
import { CustomTreeItem, CustomTreeItemProps } from "./custom-tree-item";

interface Props extends CustomTreeItemProps {
  category: Category;
  onEditClick?: MouseEventHandler | undefined;
  onDeleteClick?: MouseEventHandler | undefined;
};

export default function CategoryTreeItem(props: Props) {
  const { isOver, active, setNodeRef } = useDroppable({
    id: props.category.id!,
    data: props.category
  });

  const [ isDroppable, setDroppable ] = useState<Boolean>(false);

  useEffect(() => {
    let result = false;
    if (isOver && active?.data.current?.selector) {
      const selector = active.data.current.selector as CategorySelector;
      result = selector.categoryId !== props.category.id;
    }
    setDroppable(result);
  }, [ props.category, isOver, active ])


  function handleEditClick(event: MouseEvent) {
    if (props.onEditClick)
      props.onEditClick(event);
  }

  function handleDeleteClick(event: MouseEvent) {
    if (props.onDeleteClick)
      props.onDeleteClick(event);
  }

  return (
    <CustomTreeItem ref={ setNodeRef }
      onEditClick={ handleEditClick }
      onDeleteClick={ handleDeleteClick }
      label={ props.category.name }
      labelIcon={ <Box component={ PieChartIcon } color={ props.category.colour } /> }
      style={{ userSelect: 'none', backgroundColor: isDroppable ? '#c2e6ab6e' : undefined }}
      { ...props }
    />
  )
}
