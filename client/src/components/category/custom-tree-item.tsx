import { forwardRef, ReactElement, useState } from "react";

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

export interface CustomTreeItemProps extends
      Omit<UseTreeItemParameters, 'rootRef'>,
      Omit<React.HTMLAttributes<HTMLLIElement>, 'onFocus'> { 
  labelIcon?: any;
  hoverComponent?: () => ReactElement;
 }

export const CustomTreeItem = forwardRef((
  props: CustomTreeItemProps,
  ref: React.Ref<HTMLLIElement>
) => { 

  const { id, itemId, label, disabled, children, labelIcon: LabelIcon, hoverComponent, ...other } = props;
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

          <TreeItemLabel { ...getLabelProps() } >
            <span style={{ whiteSpace: "nowrap", overflow: "hidden", textOverflow: "ellipsis" }}>
              { label }
            </span>
          </TreeItemLabel>

          { isMouseOver && props.hoverComponent &&
            props.hoverComponent()
          }
        </TreeItemContent>

        { children && <TreeItemGroupTransition { ...getGroupTransitionProps() } /> }

      </TreeItemRoot>
    </TreeItemProvider>
  );
 });
