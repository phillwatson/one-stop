import { PropsWithChildren, MouseEventHandler, MouseEvent } from "react";

import EditIcon from '@mui/icons-material/EditOutlined';
import DeleteIcon from '@mui/icons-material/DeleteOutlined';
import AddCategoryIcon from '@mui/icons-material/PieChartOutlined';
import AddGroupIcon from '@mui/icons-material/PostAddOutlined';
import { IconButton, Stack, Tooltip } from "@mui/material";

import { CategoryGroup } from "../../model/category.model";
import { CustomTreeItem, CustomTreeItemProps } from "./custom-tree-item";


interface GroupProps extends CustomTreeItemProps, PropsWithChildren {
  group: CategoryGroup;
  onEditClick?: MouseEventHandler | undefined;
  onDeleteClick?: MouseEventHandler | undefined;
  onAddGroupClick?: MouseEventHandler | undefined;
  onAddCategoryClick?: MouseEventHandler | undefined;
}

function HoverActions(props: GroupProps) {
  function action(event: MouseEvent, handler: MouseEventHandler | undefined) {
    event.stopPropagation();
    if (handler) {
      handler(event);
    }
  }

  return (
    <Stack direction="row" spacing={1} marginLeft="auto" zIndex={2} >
      { props.onAddGroupClick &&
        <Tooltip title="New Group">
          <IconButton onClick={ (e) => action(e, props.onAddGroupClick) }
            color="inherit" aria-label="add group" edge="start" size="small" style={{ padding: "2px" }}>
            <AddGroupIcon fontSize="small" color="info" sx={{ opacity: 0.6, cursor: 'pointer' }} />
          </IconButton>
        </Tooltip>
      }

      { props.onAddCategoryClick &&
        <Tooltip title="New Category">
          <IconButton onClick={ (e) => action(e, props.onAddCategoryClick) }
            color="inherit" aria-label="add category" edge="start" size="small" style={{ padding: "2px" }}>
            <AddCategoryIcon fontSize="small" color="info" sx={{ opacity: 0.6, cursor: 'pointer' }} />
          </IconButton>
        </Tooltip>
      }

      { props.onEditClick &&
        <Tooltip title="Edit Group">
          <IconButton onClick={ (e) => action(e, props.onEditClick) }
            color="inherit" aria-label="edit" edge="start" size="small" style={{ padding: "2px" }}>
            <EditIcon fontSize="small" color="info" sx={{ opacity: 0.6, cursor: 'pointer' }} />
          </IconButton>
        </Tooltip>
      }

      { props.onDeleteClick &&
        <Tooltip title="Delete Group">
          <IconButton onClick={ (e) => action(e, props.onDeleteClick) }
            color="inherit" aria-label="delete" edge="start" size="small" style={{ padding: "2px" }} >
            <DeleteIcon fontSize="small" color="warning" sx={{ opacity: 0.6, cursor: 'pointer' }} />
          </IconButton>
        </Tooltip>
      }
    </Stack>
  );
}

export default function GroupTreeItem(props: GroupProps) {

  function showOptions() {
    return HoverActions(props);
  }

  return (
    <CustomTreeItem
      itemId={ props.group.id! }
      label={ props.group.name }
      style={{ userSelect: 'none' } }
      hoverComponent={ showOptions }
      >
      { props.children }
   </CustomTreeItem>
  )
}