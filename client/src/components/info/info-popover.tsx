import { useState } from "react";

import IconButton from "@mui/material/IconButton";
import Popover from "@mui/material/Popover";
import Typography from "@mui/material/Typography";
import InfoIcon from '@mui/icons-material/Info';

interface Props {
  content: string;
  width?: string;
}

export function InfoPopover(props: Props) {
  const [ anchorEl, setAnchorEl ] = useState<null | HTMLElement>(null);

  const handleClick = (event: React.MouseEvent<HTMLElement>) => {
    setAnchorEl(event.currentTarget);
  };

  const handleClose = () => {
    setAnchorEl(null);
  };

  return (
    <>
      <IconButton size="small" onClick={handleClick}>
        <InfoIcon fontSize="small" />
      </IconButton>

      <Popover
        open={Boolean(anchorEl)}
        anchorEl={anchorEl}
        onClose={handleClose}
        anchorOrigin={{
          vertical: 'bottom',
          horizontal: 'center',
        }}
        transformOrigin={{
          vertical: 'top',
          horizontal: 'center',
        }}
      >
        <Typography sx={{ p: 2, width: props.width || '200px' }}>
          { props.content }
        </Typography>
      </Popover>
    </>
  );
}