import { useEffect, useState } from "react";
import { useParams } from "react-router-dom";

import AccountService from '../services/account.service';
import { AccountDetail } from "../model/account.model";
import { useMessageDispatch } from "../contexts/messages/context";
import TransactionList from "../components/account/transaction-list";
import AccountHeader from "../components/account/account-header";
import PageHeader from "../components/page-header/page-header";
import Balances from "../components/account/balances";

export default function Transactions() {
  const { accountId } = useParams();
  const showMessage = useMessageDispatch();

  const [account, setAccount] = useState<AccountDetail>();

  useEffect(() => {
    if (accountId) {
      AccountService.get(accountId)
        .then( response => setAccount(response))
        .catch(err => showMessage(err));
    }
  }, [accountId, showMessage]);

  return (
    <PageHeader title="Account Transactions" >
      { account &&
        <AccountHeader account={ account }>
          <Balances account={ account } />
          <TransactionList account={account} />
        </AccountHeader>
      }
    </PageHeader>
  );
}
