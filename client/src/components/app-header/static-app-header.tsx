import { PropsWithChildren } from 'react';
import AppBar from '@mui/material/AppBar';
import Toolbar from '@mui/material/Toolbar';
import Typography from '@mui/material/Typography';
import Box from '@mui/material/Box';

interface Props extends PropsWithChildren {
  title: string,
  header: string
}

export default function StaticAppHeader(props: Props) {
  return (
    <Box sx={{ display: 'flex' }}>
      <AppBar position="fixed">
        <Toolbar>
          <Typography variant="h6" noWrap component="div">
            { props.title }
          </Typography>
        </Toolbar>
      </AppBar>
      <div style={{ padding: "90px", flexGrow: 1 }}>
        <h2>{ props.header }</h2>
        <hr></hr>
        { props.children }
      </div>
    </Box>
  );
}
