import { createContext, useContext, useState, PropsWithChildren } from 'react';
import AccountService from '../../services/account.service';
import { TransactionDetail } from '../../model/account.model';
import UpdateTransactionDialog from './update-transaction-dialog';
import { useMessageDispatch } from '../../contexts/messages/context';

type OnUpdatedCallback = (transaction: TransactionDetail) => void;
type OpenUpdateTransaction = (transaction: TransactionDetail, onUpdated?: OnUpdatedCallback) => void;
type AddTransaction = (transaction: TransactionDetail) => void;
type IsPending = (transaction: TransactionDetail) => boolean | undefined;

/**
 * An interface to describe the state that we will pass in the provider.
*/
interface ReconciliationContext {
  pendingCount: number,
  pendingState: IsPending,
  add: AddTransaction,
  clear: () => void,
  submit: () => void,
  openDialog: OpenUpdateTransaction
}

/**
 * Creates a context in which to pass the state of the reconcilations.
 * Contains no-op default implementations.
 */
const reconcileTransactionsContext = createContext<ReconciliationContext>({
  pendingCount: 0,
  pendingState(transaction: TransactionDetail): boolean | undefined { return undefined; },
  add: (transaction: TransactionDetail) => {},
  clear: () => {},
  submit: () => {},
  openDialog: () => (transaction: TransactionDetail, onUpdated?: OnUpdatedCallback) => {}
});

/**
 * The function that allows callers to access the context's state; and functions
 * to add, and remove, transactions to, and from, the reconcilation list.
 */
export default function useReconcileTransactions(): ReconciliationContext {
  return useContext(reconcileTransactionsContext);
}

export function ReconcileTransactionsProvider(props: PropsWithChildren) {
  const [ open, setOpen ] = useState<boolean>(false);
  const [ transaction, setTransaction ] = useState<TransactionDetail|undefined>(undefined);
  const [ onUpdated, setOnUpdated ] = useState<OnUpdatedCallback|undefined>(undefined);
  const [ reconciledTransactions, setReconciledTransactions ] = useState<Map<string, boolean>>(new Map());
  const showMessage = useMessageDispatch();

  const context: ReconciliationContext = {
    pendingCount: reconciledTransactions.size,

    pendingState(transaction: TransactionDetail): boolean | undefined {
      return reconciledTransactions.get(transaction.id);
    },

    add: (transaction: TransactionDetail) => {
      var pending = reconciledTransactions.get(transaction.id);
      if (pending !== undefined) {
        setReconciledTransactions(prev => {
          const newMap = new Map(prev);
          newMap.delete(transaction.id);
          return newMap;
        });
      } else {
        setReconciledTransactions(prev => {
          const newMap = new Map(prev);
          newMap.set(transaction.id, !transaction.reconciled);
          return newMap;
        });
      }
    },

    clear: () => {
      setReconciledTransactions(new Map());
    },

    submit: () =>{
      if (reconciledTransactions.size > 0) {
        AccountService.batchReconciliationUpdate(reconciledTransactions)
          .then(() => {
            // clear the list
            setReconciledTransactions(new Map());
          })
          .catch(err => showMessage(err));
      }
    },

    openDialog: (t: TransactionDetail, updatedCb?: OnUpdatedCallback) => {
        setTransaction(t);
        setOnUpdated(() => updatedCb);
        setOpen(true);
    }
  };

  function closeDialog() {
    setOpen(false);
    setTransaction(undefined);
    setOnUpdated(undefined);
  }

  return (
    <reconcileTransactionsContext.Provider value={ context }>
      { props.children }
      { transaction && (
        <UpdateTransactionDialog open={ open } transaction={ transaction } onClose={ closeDialog } onUpdated={ onUpdated } />
      ) }
    </reconcileTransactionsContext.Provider>
  );
}
