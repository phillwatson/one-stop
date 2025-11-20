import { useEffect, useState } from 'react';
import { Button, Checkbox, Dialog, DialogActions, DialogContent, DialogTitle, FormControlLabel, TextField } from '@mui/material';
import AccountService from '../../services/account.service';
import { TransactionDetail } from '../../model/account.model';
import { useMessageDispatch } from '../../contexts/messages/context';

type Props = {
  open: boolean;
  transaction: TransactionDetail;
  onClose: () => void;
  onUpdated?: (transaction: TransactionDetail) => void;
}

export default function UpdateTransactionDialog(props: Props) {
  const showMessage = useMessageDispatch();

  const [ reconciled, setReconciled ] = useState<boolean>(props.transaction.reconciled || false);
  const [ notes, setNotes ] = useState<string>(props.transaction.note || props.transaction.additionalInformation || '');
  const [ saving, setSaving ] = useState<boolean>(false);

  useEffect(() => {
    setReconciled(props.transaction.reconciled || false);
    setNotes(props.transaction.note || props.transaction.additionalInformation || '');
  }, [ props.transaction ]);

  function handleCancel() {
    props.onClose();
  }

  function handleSave() {
    setSaving(true);
    AccountService.updateTransaction(props.transaction.id, reconciled, notes)
      .then(response => {
        showMessage({ type: 'add', level: 'success', text: 'Transaction updated' });
        // inform caller so they can update local state optimistically
        if (props.onUpdated) props.onUpdated(response);
        setSaving(false);
        props.onClose();
      })
      .catch(err => {
        setSaving(false);
        showMessage(err);
      });
  }

  return (
    <Dialog open={props.open} onClose={handleCancel} fullWidth maxWidth="sm">
      <DialogTitle>Update Transaction</DialogTitle>
      <DialogContent>
        <FormControlLabel
          control={<Checkbox checked={reconciled} onChange={e => setReconciled(e.target.checked)} />}
          label="Reconciled"
        />

        <TextField
          label="Notes"
          multiline
          minRows={3}
          fullWidth
          margin="normal"
          value={notes}
          onChange={e => setNotes(e.target.value)}
        />
      </DialogContent>
      <DialogActions>
        <Button onClick={handleCancel} disabled={saving} variant="outlined">Cancel</Button>
        <Button onClick={handleSave} disabled={saving} variant="contained">Save</Button>
      </DialogActions>
    </Dialog>
  );
}
