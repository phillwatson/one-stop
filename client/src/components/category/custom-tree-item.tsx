import { forwardRef, useState, MouseEvent, MouseEventHandler } from "react";

import EditOutlinedIcon from '@mui/icons-material/EditOutlined';
import DeleteOutlinedIcon from '@mui/icons-material/DeleteOutlined';

import { useTreeItem } from '@mui/x-tree-view/useTreeItem';
import { 
  TreeItemProvider,
  TreeItemContent,
  TreeItemLabel,
  TreeItemRoot,
  TreeItemIcon,
  TreeItemIconContainer,
  TreeItemGroupTransition,
  UseTreeItemParameters
 } from '@mui/x-tree-view';
import { IconButton, Stack } from "@mui/material";

export interface CustomTreeItemProps
  extends Omit<UseTreeItemParameters, 'rootRef'>,
      Omit<React.HTMLAttributes<HTMLLIElement>, 'onFocus'> { 
  labelIcon?: any;
  onEditClick?: MouseEventHandler | undefined;
  onDeleteClick?: MouseEventHandler | undefined;
 }

export const CustomTreeItem = forwardRef((
  props: CustomTreeItemProps,
  ref: React.Ref<HTMLLIElement>
) => { 

  const { id, itemId, label, disabled, children, labelIcon: LabelIcon, ...other } = props;
  const { 
    getContextProviderProps,
    getRootProps,
    getContentProps,
    getLabelProps,
    getGroupTransitionProps,
    getIconContainerProps,
    status,
  } = useTreeItem({ id, itemId, label, disabled, children, rootRef: ref });

  const [ isMouseOver, setMouseOver ] = useState<boolean>(false);

  function action(event: MouseEvent, handler: MouseEventHandler | undefined) {
    event.stopPropagation();
    if (handler) {
      handler(event);
    }
  }

  return (
    <TreeItemProvider { ...getContextProviderProps() }>
      <TreeItemRoot
        onMouseEnter={ () => setMouseOver(true) }
        onMouseLeave={ () => setMouseOver(false) }
        { ...getRootProps(other) } >

        <TreeItemContent { ...getContentProps() }>
          <TreeItemIconContainer { ...getIconContainerProps() }>
            <TreeItemIcon status={ status } />
          </TreeItemIconContainer>

          { LabelIcon }

          <TreeItemLabel { ...getLabelProps() } />

          { isMouseOver && (props.onEditClick || props.onDeleteClick) &&
            <Stack direction="row" spacing={1} marginLeft="auto" >
              { props.onEditClick &&
                <IconButton
                  color="inherit" aria-label="edit" edge="start" size="small"
                  style={{ padding: "2px" }}
                  onClick={ (e) => action(e, props.onEditClick) }>
                  <EditOutlinedIcon fontSize="small" color="info" 
                    sx={{ opacity: 0.6, cursor: 'pointer' }}
                  />
                </IconButton>
              }
              { props.onDeleteClick &&
                <IconButton
                  color="inherit" aria-label="delete" edge="start" size="small"
                  style={{ padding: "2px" }}
                  onClick={ (e) => action(e, props.onDeleteClick) }>
                  <DeleteOutlinedIcon fontSize="small" color="warning"
                    sx={{ opacity: 0.6, cursor: 'pointer' }}
                  />
                </IconButton>
              }
            </Stack>
          }
        </TreeItemContent>

        { children && <TreeItemGroupTransition { ...getGroupTransitionProps() } /> }

      </TreeItemRoot>
    </TreeItemProvider>
  );
 });
