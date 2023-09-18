import { useEffect, useState } from "react";
import { Fab, SxProps } from "@mui/material";
import AddIcon from '@mui/icons-material/Add';

import AccountService from '../services/account.service';
import { AccountDetail } from "../model/account.model";
import AccountList from "../components/account/account-list";
import Institutions from "../components/add-institution/add-institution";
import { useSearchParams } from "react-router-dom";
import { useNotificationDispatch } from "../contexts/notification-context";

const bottomFabStyle: SxProps = {
  position: 'fixed',
  bottom: 16,
  right: 16,
};

export default function Accounts() {
  const [ queryParams ] = useSearchParams();
  const [ accounts, setAccounts ] = useState<Array<AccountDetail>>([]);
  const [ showInstitutions, setShowInstitutions ] = useState<boolean>(false);
  const showNotification = useNotificationDispatch();

  useEffect(() => {
    const error = queryParams.get("error");
    if (error) {
      const details = queryParams.get("details")
      showNotification({ type: 'add', level: 'error', message: details ? details : error!!});
    }
  }, [ queryParams, showNotification ]);

  useEffect(() => {
    AccountService.getAll().then( response => setAccounts(response.items));
  }, []);

  function handleSelectAccount(account: AccountDetail) {
    alert(`selected account ${account.iban}`);
  }

  function handleDeleteAccount(account: AccountDetail) {
    alert(`deleted account ${account.iban}`);
  }

  return (
    <div>
      <h2>Accounts</h2>
      <hr></hr>
      <AccountList accounts={accounts} onSelect={handleSelectAccount} onDelete={handleDeleteAccount}/>

      <Fab color="primary" aria-label="add" sx={bottomFabStyle} onClick={() => setShowInstitutions(true)}><AddIcon /></Fab>
      <Institutions open={showInstitutions} onClose={() => setShowInstitutions(false)}></Institutions>
    </div>
  );
}
