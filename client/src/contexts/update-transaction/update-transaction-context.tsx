import { createContext, useContext, useState, PropsWithChildren } from 'react';
import { TransactionDetail } from '../../model/account.model';
import UpdateTransactionDialog from '../../components/dialogs/update-transaction-dialog';

type OnUpdatedCallback = (transaction: TransactionDetail) => void;
type OpenUpdateTransaction = (transaction: TransactionDetail, onUpdated?: OnUpdatedCallback) => void;

const UpdateTransactionContext = createContext<OpenUpdateTransaction>(() => {});

export default function useUpdateTransaction(): OpenUpdateTransaction {
  return useContext(UpdateTransactionContext);
}

export function UpdateTransactionProvider(props: PropsWithChildren) {
  const [ open, setOpen ] = useState<boolean>(false);
  const [ transaction, setTransaction ] = useState<TransactionDetail|undefined>(undefined);
  const [ onUpdated, setOnUpdated ] = useState<OnUpdatedCallback|undefined>(undefined);

  function openDialog(t: TransactionDetail, updatedCb?: OnUpdatedCallback) {
    setTransaction(t);
    setOnUpdated(() => updatedCb);
    setOpen(true);
  }

  function closeDialog() {
    setOpen(false);
    setTransaction(undefined);
    setOnUpdated(undefined);
  }

  return (
    <UpdateTransactionContext.Provider value={ openDialog }>
      { props.children }
      { transaction && (
        <UpdateTransactionDialog open={ open } transaction={ transaction } onClose={ closeDialog } onUpdated={ onUpdated } />
      ) }
    </UpdateTransactionContext.Provider>
  );
}
