import { PropsWithChildren, MouseEventHandler } from "react";

import { CategoryGroup } from "../../model/category.model";
import { CustomTreeItem } from "./custom-tree-item";

interface GroupProps extends PropsWithChildren {
  group: CategoryGroup;
  onEditClick?: MouseEventHandler | undefined;
  onDeleteClick?: MouseEventHandler | undefined;
}
export default function GroupTreeItem(props: GroupProps) {
  return (
    <CustomTreeItem
      onEditClick={ props.onEditClick }
      onDeleteClick={ props.onDeleteClick }
      itemId={ props.group.id! }
      label={ props.group.name }
      style={{ userSelect: 'none' } }>
      { props.children }
   </CustomTreeItem>
  )
}