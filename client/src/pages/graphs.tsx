import { useEffect, useState } from "react";
import { useParams } from "react-router-dom";

import { useMessageDispatch } from "../contexts/messages/context";
import AccountService from '../services/account.service';
import { AccountDetail } from "../model/account.model";

import AccountHeader from "../components/account/account-header";
import BarGraph from "../components/graph/bar-graph";
import PageHeader from "../components/page-header/page-header";

export default function Graph() {
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
    <PageHeader title="Account Graphs" >
      { account &&
        <AccountHeader account={ account }>
          <BarGraph account={ account }/>
        </AccountHeader>
      }
    </PageHeader>
  );
}