import { useEffect, useState } from 'react';

import Button from '@mui/material/Button';
import TextField from '@mui/material/TextField';
import Dialog from '@mui/material/Dialog';
import DialogActions from '@mui/material/DialogActions';
import DialogContent from '@mui/material/DialogContent';
import DialogContentText from '@mui/material/DialogContentText';
import DialogTitle from '@mui/material/DialogTitle';
import FormControlLabel from '@mui/material/FormControlLabel';

import UserConsent from '../../model/user-consent.model';
import Checkbox from '@mui/material/Checkbox';

interface Props {
  open: boolean;
  userConsent: UserConsent | undefined;
  onConfirm: (userConsent: UserConsent, includeAccounts: boolean) => void;
  onCancel: () => void
}

export default function DeleteConsentDialog(props: Props) {
  function handleCancel() {
    props.onCancel();
  };

  function handleConfirm() {
    if (props.userConsent === undefined) return;
    props.onConfirm(props.userConsent, confirmation.deleteAccounts);
  };

  const [confirmation, setConfirmation] = useState({ institutionName: "", deleteAccounts: false });
  useEffect(() => { setConfirmation({ institutionName: "", deleteAccounts: false }); }, [ props ]);
  
  function validateForm(): boolean {
    if (props.userConsent === undefined) return false;
    return confirmation.institutionName === props.userConsent.institutionName;
  }

  return (
    <Dialog open={props.open} onClose={handleCancel}>
      <DialogTitle>Revoke Consent for {props.userConsent?.institutionName}</DialogTitle>
      <DialogContent>
        <DialogContentText>
          To confirm consent removal, please enter the name of the institution.
          <TextField
            id="name" label="Institution Name" autoFocus
            margin="dense" fullWidth variant="standard"
            value={confirmation.institutionName}
            onChange={(e) => setConfirmation({ ...confirmation, institutionName: e.target.value })}
          />
          <p/>
          <FormControlLabel control={ <Checkbox value="deleteAccounts" checked={confirmation.deleteAccounts}
              onChange={(e) => setConfirmation({ ...confirmation, deleteAccounts: e.target.checked })}/>
            } label="Delete Stored Account Data" />
        </DialogContentText>
      </DialogContent>

      <DialogActions>
        <Button onClick={handleCancel} variant="outlined">Cancel</Button>
        <Button onClick={handleConfirm} variant="contained" disabled={!validateForm()}>Revoke</Button>
      </DialogActions>
    </Dialog>
  );
}