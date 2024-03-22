import { useEffect, useState } from "react";
import { useParams } from "react-router-dom";

import { useNotificationDispatch } from "../contexts/notification/context";
import AccountService from '../services/account.service';
import { AccountDetail } from "../model/account.model";

import AccountHeader from "../components/account/account-header";
import BarGraph from "../components/graph/bar-graph";

export default function Graph() {
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

  // fetch transactions for the last 30 days
  const fromDate = new Date(); fromDate.setDate(fromDate.getDate() - 30);
  const toDate = new Date();

  return (
    <div>
      { account &&
        <AccountHeader title="Account Graphs" account={ account }>
          <BarGraph account={ account } fromDate={ fromDate } toDate={ toDate }/>
        </AccountHeader>
      }
    </div>
  );
}