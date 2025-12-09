import { useState, MouseEventHandler, MouseEvent } from "react";
import { useDraggable } from '@dnd-kit/core';

import { SxProps } from '@mui/material/styles';
import Table from '@mui/material/Table';
import TableBody from '@mui/material/TableBody';
import TableRow from '@mui/material/TableRow';
import TableCell from '@mui/material/TableCell';
import IconButton from "@mui/material/IconButton";

import { CategorySelector } from "../../model/category.model";

import DragIndicatorOutlinedIcon from '@mui/icons-material/DragIndicatorOutlined';
import DeleteOutlinedIcon from '@mui/icons-material/DeleteOutlined';

const compactCell: SxProps = {
  paddingLeft: 0.4,
  paddingRight: 0,
  paddingTop: 0.3,
  paddingBottom: 0.3,
  fontSize: '11pt',
  whiteSpace: 'nowrap',
  overflow: 'hidden',
  textOverflow: 'ellipsis'
};

export interface DraggedSelector {
  selector: CategorySelector;
  accountName?: String;
}

interface Props {
  selector: CategorySelector;
  accountName?: String;
  onDeleteClick?: MouseEventHandler | undefined;
}

export function DraggableSectorRow(props: Props) {
  return (
    <Table size='small'>
      <TableBody style={{ backgroundColor: "#d9e3e9" }}>
        <SelectorRow
          key={ props.selector.id! }
          selector={ props.selector }
          accountName={ props.accountName } />
      </TableBody>
    </Table>
  )
}; 

export default function SelectorRow(props: Props) {
  const { attributes, listeners, isDragging, setNodeRef, setActivatorNodeRef } = useDraggable({
    id: props.selector.id!,
    data: {
      selector: props.selector,
      accountName: props.accountName
    } as DraggedSelector
  });

  const rowStyle = {
    userSelect: 'none',
    backgroundColor: ( isDragging ? "#dddfe0ff" : "transparent" )    
  } as React.CSSProperties;

  const [ isMouseOver, setMouseOver ] = useState<boolean>(false);

  function action(event: MouseEvent, handler: MouseEventHandler | undefined) {
    event.stopPropagation();
    if (handler) {
      handler(event);
    }
  }

  return (
    <TableRow ref={ setNodeRef } hover style={ rowStyle }
        onMouseEnter={ () => setMouseOver(true) }
        onMouseLeave={ () => setMouseOver(false) }
     >
      <TableCell sx={ compactCell } width={"22px"}>
        <IconButton color="inherit" aria-label="move" edge="start" size="small"
          ref={ setActivatorNodeRef } {...listeners} {...attributes} >
          <DragIndicatorOutlinedIcon />
        </IconButton>
      </TableCell>
      <TableCell sx={ compactCell } width={"22px"}>
        { props.onDeleteClick && isMouseOver &&
          <IconButton aria-label="delete" edge="start" size="small"
            onClick={ (e) => action(e, props.onDeleteClick) }>
            <DeleteOutlinedIcon fontSize="small" color="warning"
              sx={{ opacity: 0.6, cursor: 'pointer' }}
            />
          </IconButton>
        }
      </TableCell>
      <TableCell sx={ compactCell }>{ props.accountName }</TableCell>
      <TableCell sx={ compactCell }>{ props.selector.infoContains }</TableCell>
      <TableCell sx={ compactCell }>{ props.selector.refContains }</TableCell>
      <TableCell sx={ compactCell }>{ props.selector.creditorContains }</TableCell>
    </TableRow>
  );
}
