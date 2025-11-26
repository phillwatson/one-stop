import { useCallback, useState } from 'react';
import { Button, Checkbox, Dialog, DialogActions, DialogContent, DialogTitle, FormControlLabel, TextField } from '@mui/material';
import Table from '@mui/material/Table';
import TableCell from '@mui/material/TableCell';
import TableHead from '@mui/material/TableHead';
import TableRow from '@mui/material/TableRow';

import AccountService from '../../services/account.service';
import { TransactionDetail } from '../../model/account.model';
import { useMessageDispatch } from '../../contexts/messages/context';
import useMonetaryContext from '../../contexts/monetary/monetary-context';
import { formatDate } from "../../util/date-util";
import useReconcileTransactions from './reconcile-transactions-context';

type Props = {
  open: boolean;
  transaction: TransactionDetail;
  onClose: () => void;
  onUpdated?: (transaction: TransactionDetail) => void;
}

export default function UpdateTransactionDialog(props: Props) {
  const showMessage = useMessageDispatch();
  const [ formatMoney ] = useMonetaryContext();
  const reconcilations = useReconcileTransactions();

  // determine initial reconciled state - either from pending changes, or from transaction itself
  const isTransactionReconciled = useCallback(() => {
    var result = reconcilations.pendingState(props.transaction);
    return (result === undefined) ? props.transaction.reconciled : result;
  }, [ reconcilations, props.transaction ]);

  const [ reconciled, setReconciled ] = useState<boolean>(isTransactionReconciled());
  const [ notes, setNotes ] = useState<string>(props.transaction.notes || '');
  const [ saving, setSaving ] = useState<boolean>(false);

  function isSaveDisabled(): boolean {
    return saving || (reconciled === isTransactionReconciled() && notes === (props.transaction.notes || ''));
  }

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
        props.onClose();
      })
      .catch(err => {
        showMessage(err);
      })
      .finally(() => {
        setSaving(false);
      });
  }

  return (
    <Dialog open={props.open} onClose={handleCancel} fullWidth maxWidth="sm">
      <DialogTitle>Reconcile Transaction</DialogTitle>
      <DialogContent>
        <Table size="small">
          <TableHead>
            <TableRow>
              <TableCell>{formatDate(props.transaction.bookingDateTime)}</TableCell>
              <TableCell>{props.transaction.additionalInformation || props.transaction.reference}</TableCell>
              <TableCell>{formatMoney(props.transaction.amount, props.transaction.currency)}</TableCell>
            </TableRow>
          </TableHead>
        </Table>

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
        <Button onClick={handleSave} disabled={isSaveDisabled()} variant="contained">Save</Button>
      </DialogActions>
    </Dialog>
  );
}
