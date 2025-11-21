import IconButton from '@mui/material/IconButton';
import Tooltip from '@mui/material/Tooltip';
import ReconciledIcon from '@mui/icons-material/PlaylistAddCheckCircleRounded';
import UnReconciledIcon from '@mui/icons-material/AddTaskRounded';
import NotesIcon from '@mui/icons-material/NotesRounded';

import { TransactionDetail } from "../../model/account.model";
import useUpdateTransaction from '../../contexts/update-transaction/update-transaction-context';

type OnUpdatedCallback = (transaction: TransactionDetail) => void;

interface Props {
  transaction: TransactionDetail;
  onUpdate: OnUpdatedCallback;
}

export default function ReconcilationButton(props: Props) {
  const openUpdate = useUpdateTransaction();

  return (
    <Tooltip title={ props.transaction.notes }>
      <IconButton size="small" style={{ padding: 6, margin: 0 }}
        onClick={ event => {
          event.stopPropagation();
          openUpdate(props.transaction, props.onUpdate);
      }}>
        { props.transaction.reconciled ?
          <ReconciledIcon fontSize="small" /> :
          props.transaction.notes ?
            <NotesIcon fontSize="small" /> :
          <UnReconciledIcon fontSize="small" />
        }
      </IconButton>
    </Tooltip>
  );
}