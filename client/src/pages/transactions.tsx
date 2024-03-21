import { useEffect, useState } from "react";
import { useParams } from "react-router-dom";

import AccountService from '../services/account.service';
import { AccountDetail } from "../model/account.model";
import { useNotificationDispatch } from "../contexts/notification/context";
import TransactionList from "../components/account/transaction-list";
import AccountHeader from "../components/account/account-header";

export default function Transactions() {
  const { accountId } = useParams();
  const showNotification = useNotificationDispatch();

  const [account, setAccount] = useState<AccountDetail>();

  useEffect(() => {
    if (accountId) {
      AccountService.get(accountId)
        .then( response => setAccount(response))
        .catch(err => showNotification(err));
    }
  }, [accountId, showNotification]);

  return (
    <div>
      { account &&
        <AccountHeader title="Account Transactions" account={ account }>
            <TransactionList account={account} />
        </AccountHeader>
      }
    </div>
  );
}
