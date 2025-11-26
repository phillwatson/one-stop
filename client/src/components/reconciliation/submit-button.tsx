import Button from '@mui/material/Button';
import CloudUploadIcon from '@mui/icons-material/CloudUpload';
import CancelIcon from '@mui/icons-material/Clear';
import Tooltip from '@mui/material/Tooltip';

import useReconcileTransactions from './reconcile-transactions-context';

export default function SubmitReconcilationButton() {
  const reconcilations = useReconcileTransactions();
  return (
    <> { reconcilations.pendingCount > 0 &&
      <>
        <Tooltip title={`Cancel reconciliation of ${reconcilations.pendingCount} transactions`}>
          <Button
            size='small' style={{ marginRight: "4px", backgroundColor: '#d8e0c1ff', color: '#000000' }}
            component="label"
            role={undefined}
            variant="contained"
            tabIndex={-1}
            startIcon={<CancelIcon />}
            onClick={() => { reconcilations.clear() }}>
            Cancel
          </Button>
        </Tooltip>
              
        <Tooltip title={`${reconcilations.pendingCount} transactions to submit`}>
          <Button
            size='small' style={{ marginLeft: "4px", backgroundColor: '#b8d5f0ff', color: '#000000' }}
            component="label"
            role={undefined}
            variant="contained"
            tabIndex={-1}
            startIcon={<CloudUploadIcon />}
            onClick={() => { reconcilations.submit() }}>
            Submit
          </Button>
        </Tooltip>
      </>
    }</>
  );
}