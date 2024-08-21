import { PropsWithChildren } from "react";

import Box from "@mui/material/Box/Box";
import Paper from "@mui/material/Paper";
import { Divider } from "@mui/material";

interface Props extends PropsWithChildren {
  title: string;
}

export default function PageHeader(props: Props) {
  return (
    <Box paddingLeft={{ xs: 0, sm: 1 }} paddingRight={{ xs: 0, sm: 1 }}>
      <h2>{ props.title }</h2>
      <Divider/>
      <Box paddingTop={{ xs: 1, sm: 2 }} paddingLeft={{ xs: 0, sm: 1, md: 5, lg: 21 }} paddingRight={{ xs: 0, sm: 1, md: 5, lg: 21 }}>
        <Paper elevation={ 3 } sx={{ padding: 1}}>
          {props.children}
        </Paper>
      </Box>
    </Box>
  );
}