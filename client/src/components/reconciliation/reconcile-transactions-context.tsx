import { createContext, useContext, useState, PropsWithChildren } from 'react';
import AccountService from '../../services/account.service';
import { TransactionDetail } from '../../model/account.model';
import UpdateTransactionDialog from './update-transaction-dialog';
import { useMessageDispatch } from '../../contexts/messages/context';
import styles from './reconciliations.module.css';

type OnSubmittedCallback = (reconciliations: Map<string, boolean>) => void;
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
  rowClassname: (transaction: TransactionDetail) => string,
  add: AddTransaction,
  clear: () => void,
  submit: () => void,
  onSubmit: OnSubmittedCallback | undefined,
  openDialog: OpenUpdateTransaction
}

/**
 * Creates a context in which to pass the state of the reconcilations.
 * Contains no-op default implementations.
 */
const reconcileTransactionsContext = createContext<ReconciliationContext>({
  pendingCount: 0,
  onSubmit: undefined,

  pendingState(transaction: TransactionDetail): boolean | undefined { return undefined; },
  rowClassname(transaction: TransactionDetail): string { return ""; },
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

    // callback to allow client to update their view of the reconciliations after submission
    onSubmit: undefined,

    pendingState(transaction: TransactionDetail): boolean | undefined {
      return reconciledTransactions.get(transaction.id);
    },

    rowClassname(transaction: TransactionDetail): string {
      var pendingState = reconciledTransactions.get(transaction.id);
      if (pendingState === undefined) {
        return transaction.reconciled
          ? styles.reconciled
          : styles.unreconciled;
      } else {
        return pendingState
          ? styles.reconciled + ' ' + styles.pending
          : styles.unreconciled + ' ' + styles.pending;
      }
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
      if (reconciledTransactions.size > 0) {
        setReconciledTransactions(new Map());
      }
    },

    submit: () =>{
      if (reconciledTransactions.size > 0) {
        AccountService.batchReconciliationUpdate(reconciledTransactions)
          .then(() => {
          if (context.onSubmit !== undefined) {
            var reconciliations = reconciledTransactions
            context.onSubmit(reconciliations);
          }
            // clear the list
            setReconciledTransactions(new Map());
          })
          .catch(err => showMessage(err));
      }
    },

    openDialog: (t: TransactionDetail, updatedCallback?: OnUpdatedCallback) => {
        setTransaction(t);
        setOnUpdated(() => updatedCallback);
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
