import { Button, Dialog, DialogActions, DialogContent, DialogTitle, Typography } from '@mui/material';

type Props = {
  title: string,
  content: string | Array<string>,
  onConfirm: () => any,
  onCancel: () => any,
  open: boolean
}

export default function ConfirmationDialog(props: Props) {
  const message = Array.isArray(props.content) ? props.content : Array.of(props.content);

  return (
    <Dialog
      open={props.open}
      onClose={props.onCancel}
      aria-labelledby="alert-dialog-title"
      aria-describedby="alert-dialog-description"
    >
      <DialogTitle id="alert-dialog-title"  sx={{ bgcolor: 'primary.main', color: 'white'}}>
        {props.title}
      </DialogTitle>
      <DialogContent dividers>
        { message.map((line, index) =>
          <Typography key={ index } gutterBottom>
            { line }
          </Typography>
        )}
      </DialogContent>
      <DialogActions>
        <Button variant="outlined" onClick={props.onCancel}>Cancel</Button>
        <Button variant="contained" autoFocus onClick={props.onConfirm}>OK</Button>
      </DialogActions>
    </Dialog>
  );
}