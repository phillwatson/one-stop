import { useState, useEffect, MouseEventHandler, MouseEvent } from "react";
import { useDroppable } from '@dnd-kit/core';

import EditIcon from '@mui/icons-material/EditOutlined';
import DeleteIcon from '@mui/icons-material/DeleteOutlined';
import PieChartIcon from '@mui/icons-material/PieChart';
import { IconButton, Stack, Tooltip } from "@mui/material";

import Box from "@mui/material/Box";

import { Category, CategorySelector } from "../../model/category.model";
import { CustomTreeItem, CustomTreeItemProps } from "./custom-tree-item";

interface Props extends CustomTreeItemProps {
  category: Category;
  onEditClick?: MouseEventHandler | undefined;
  onDeleteClick?: MouseEventHandler | undefined;
};

function HoverActions(props: Props) {
  function action(event: MouseEvent, handler: MouseEventHandler | undefined) {
    event.stopPropagation();
    if (handler) {
      handler(event);
    }
  }

  return (
    <Stack direction="row" spacing={1} marginLeft="auto" zIndex={2} >
      { props.onEditClick &&
        <Tooltip title="Edit Category">
          <IconButton onClick={ (e) => action(e, props.onEditClick) }
            color="inherit" aria-label="edit" edge="start" size="small" style={{ padding: "2px" }}>
            <EditIcon fontSize="small" color="info" sx={{ opacity: 0.6, cursor: 'pointer' }} />
          </IconButton>
        </Tooltip>
      }

      { props.onDeleteClick &&
        <Tooltip title="Delete Category">
          <IconButton onClick={ (e) => action(e, props.onDeleteClick) }
            color="inherit" aria-label="delete" edge="start" size="small" style={{ padding: "2px" }} >
            <DeleteIcon fontSize="small" color="warning" sx={{ opacity: 0.6, cursor: 'pointer' }} />
          </IconButton>
        </Tooltip>
      }
    </Stack>
  );
}

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


  function showOptions() {
    return HoverActions(props);
  }

  return (
    <CustomTreeItem ref={ setNodeRef }
      label={ props.category.name }
      labelIcon={ <Box component={ PieChartIcon } color={ props.category.colour } /> }
      style={{ userSelect: 'none', backgroundColor: isDroppable ? '#c2e6ab6e' : undefined }}
      hoverComponent={ showOptions }
      { ...props }
    />
  )
}
