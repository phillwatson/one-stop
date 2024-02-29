import { Button, Dialog, DialogActions, DialogContent, DialogContentText, DialogTitle } from '@mui/material';

type Props = {
  title: string,
  content: string,
  onConfirm: () => any,
  onCancel: () => any,
  open: boolean
}

export default function ConfirmationDialog(props: Props) {
  return (
      <Dialog
          open={props.open}
          onClose={props.onCancel}
          aria-labelledby="alert-dialog-title"
          aria-describedby="alert-dialog-description"
      >
          <DialogTitle id="alert-dialog-title">
              {props.title}
          </DialogTitle>
          <DialogContent>
              <DialogContentText id="alert-dialog-description">
                  {props.content}
              </DialogContentText>
          </DialogContent>
          <DialogActions>
              <Button variant="contained" autoFocus onClick={props.onConfirm}>OK</Button>
              <Button variant="outlined" onClick={props.onCancel}>Cancel</Button>
          </DialogActions>
      </Dialog>
  );
}