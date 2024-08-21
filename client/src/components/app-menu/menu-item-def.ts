import { ReactElement } from 'react';

export default interface MenuItemDef {
  label: string;
  icon?: ReactElement
  route: string;
  action?: () => void;
}
