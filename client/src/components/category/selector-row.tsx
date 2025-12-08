import { useDraggable } from '@dnd-kit/core';

import { SxProps } from '@mui/material/styles';
import Table from '@mui/material/Table';
import TableBody from '@mui/material/TableBody';
import TableRow from '@mui/material/TableRow';
import TableCell from '@mui/material/TableCell';
import IconButton from "@mui/material/IconButton";

import DragIndicatorOutlinedIcon from '@mui/icons-material/DragIndicatorOutlined';
import { CategorySelector } from "../../model/category.model";

const compactCell: SxProps = {
  paddingLeft: 0.4,
  paddingRight: 0,
  paddingTop: 0.3,
  paddingBottom: 0.3,
  fontSize: '11pt'
};


export interface DraggedSelector {
  selector: CategorySelector;
  accountName?: String;
}

interface Props {
  selector: CategorySelector;
  accountName?: String;
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

  let style = {
    userSelect: 'none',
    backgroundColor: ( isDragging ? "#dddfe0ff" : "transparent" )
  } as React.CSSProperties;

  return (
    <TableRow ref={ setNodeRef } hover style={ style } >
      <TableCell sx={ compactCell }>
        <IconButton color="inherit" aria-label="move" edge="start" size="small"
            ref={ setActivatorNodeRef } {...listeners} {...attributes} >
          <DragIndicatorOutlinedIcon />
        </IconButton>
      </TableCell>
      <TableCell sx={ compactCell }>{ props.accountName }</TableCell>
      <TableCell sx={ compactCell }>{ props.selector.infoContains }</TableCell>
      <TableCell sx={ compactCell }>{ props.selector.refContains }</TableCell>
      <TableCell sx={ compactCell }>{ props.selector.creditorContains }</TableCell>
    </TableRow>
  );
}
