import List from '@mui/material/List';
import { PropsWithChildren } from 'react';

interface AppMenuProps extends PropsWithChildren {
}

export default function AppMenu(props: AppMenuProps) {
  return (
    <>
      <List>
        { props.children }
      </List>
    </>
  );
}
