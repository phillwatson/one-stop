import { useEffect, useState } from 'react';
import Button from '@mui/material/Button';
import TextField from '@mui/material/TextField';
import Dialog from '@mui/material/Dialog';
import DialogActions from '@mui/material/DialogActions';
import DialogContent from '@mui/material/DialogContent';
import DialogContentText from '@mui/material/DialogContentText';
import DialogTitle from '@mui/material/DialogTitle';

import { AccountDetail } from '../../model/account.model';

interface Props {
  open: boolean;
  account: AccountDetail;
  onConfirm: (account: AccountDetail) => void;
  onCancel: (account: AccountDetail) => void
}

export default function DeleteAccountDialog(props: Props) {
  function handleCancel() {
    props.onCancel(props.account);
  };

  function handleConfirm() {
    props.onConfirm(props.account);
  };

  const [accountName, setAccountName] = useState<string>("");
  useEffect(() => setAccountName(""), [ props.open ]);

  function validateForm(): boolean {
    return accountName === props.account.name;
  }

  return (
    <Dialog open={props.open} onClose={handleCancel}>
      <DialogTitle>Remove Account</DialogTitle>
      <DialogContent>
        <DialogContentText>
          To confirm account removal, please enter the name of the account.
        </DialogContentText>
        <TextField
          id="name"
          label="Account name"
          autoFocus
          margin="dense"
          fullWidth
          variant="standard"
          value={accountName} onChange={(e) => setAccountName(e.target.value)}
        />
      </DialogContent>
      <DialogActions>
        <Button onClick={handleCancel}>Cancel</Button>
        <Button onClick={handleConfirm} disabled={!validateForm()}>Remove</Button>
      </DialogActions>
    </Dialog>
  );
}