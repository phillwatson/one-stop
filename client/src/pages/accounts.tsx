import { useEffect, useState } from "react";
import { Fab, SxProps } from "@mui/material";
import AddIcon from '@mui/icons-material/Add';

import AccountService from '../services/account.service';
import { AccountDetail } from "../model/account.model";
import AccountList from "../components/account/account-list";
import Institutions from "../components/add-institution/add-institution";

const bottomFabStyle: SxProps = {
  position: 'fixed',
  bottom: 16,
  right: 16,
};

export default function Accounts() {
  const [ accounts, setAccounts ] = useState<Array<AccountDetail>>([]);
  const [ showInstitutions, setShowInstitutions ] = useState<boolean>(false);

  useEffect(() => {
    AccountService.getAll().then( response => setAccounts(response.items));
  }, []);

  function handleSelectAccount(accountId: string) {
    alert(`selected account ${accountId}`);
  }
  return (
    <div>
      <h2>Accounts</h2>
      <hr></hr>
      <AccountList accounts={accounts} onSelect={handleSelectAccount}/>

      <Fab color="primary" aria-label="add" sx={bottomFabStyle} onClick={() => setShowInstitutions(true)}><AddIcon /></Fab>
      <Institutions open={showInstitutions} onClose={() => setShowInstitutions(false)}></Institutions>
    </div>
  );
}
