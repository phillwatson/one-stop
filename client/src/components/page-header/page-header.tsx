import { PropsWithChildren, ReactElement } from "react";

import Box from "@mui/material/Box";
import Paper from "@mui/material/Paper";
import Divider from "@mui/material/Divider";
import Typography from "@mui/material/Typography";
import { Badge } from "@mui/material";

interface Props extends PropsWithChildren {
  title: string;
  icon?: ReactElement;
}

export default function PageHeader(props: Props) {
  return (
    <Box paddingLeft={{ xs: 0, sm: 1 }} paddingRight={{ xs: 0, sm: 1 }} paddingTop={ 1 }>
      <Typography sx={{ typography: { xs: "h5", sm: "h4" } }} noWrap>
      { props.icon && 
        <Badge sx={{ scale: '1.5', margin: 0, paddingLeft: '9px', paddingRight: '12px', verticalAlign: 'baseline' }}>
          { props.icon }
        </Badge>
      }
        { props.title }
      </Typography>

      <Divider/>

      <Box paddingTop={{ xs: 1, sm: 2 }}
           paddingLeft={{ xs: 0, sm: 1, md: 5, lg: 15 }}
           paddingRight={{ xs: 0, sm: 1, md: 5, lg: 15 }}>
        <Paper elevation={ 3 } sx={{ padding: 1 }}>
          {props.children}
        </Paper>
      </Box>
    </Box>
  );
}