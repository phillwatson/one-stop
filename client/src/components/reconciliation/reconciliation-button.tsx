import IconButton from '@mui/material/IconButton';
import Tooltip from '@mui/material/Tooltip';
import ReconciledIcon from '@mui/icons-material/RadioButtonChecked';
import UnReconciledIcon from '@mui/icons-material/RadioButtonUnchecked';
import NotesIcon from '@mui/icons-material/NotesRounded';

import { TransactionDetail } from "../../model/account.model";
import useReconcileTransactions from './reconcile-transactions-context';
import { useCallback } from 'react';

type OnUpdatedCallback = (transaction: TransactionDetail) => void;

interface Props {
  transaction: TransactionDetail;
  onUpdate: OnUpdatedCallback;
}

export default function ReconcilationButton(props: Props) {
  const reconcilations = useReconcileTransactions();

  const isReconciled = useCallback(() => {
    const pending = reconcilations.pendingState(props.transaction);
    return (pending !== undefined) ? pending : props.transaction.reconciled;
  }, [ props.transaction, reconcilations ]);

  return (
    <>
      <IconButton size="small" style={{ padding: 4, margin: 0 }}
        onClick={ event => {
          event.stopPropagation();
          reconcilations.add(props.transaction);
      }}>
        { isReconciled()
            ? <ReconciledIcon fontSize="small" />
            : <UnReconciledIcon fontSize="small" />
        }
      </IconButton>

      <Tooltip title={ props.transaction.notes }>
        <IconButton size="small" style={{ padding: 4, margin: 0, color: props.transaction.notes ? '#000000ff' : '#0000006e' }}
          onClick={ event => {
            event.stopPropagation();
            reconcilations.openDialog(props.transaction, props.onUpdate);
        }}>
          <NotesIcon fontSize="small" />
        </IconButton>
      </Tooltip>
    </>
  );
}