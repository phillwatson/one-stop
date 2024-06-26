import { useEffect, useState } from "react";
import { Fab, SxProps } from "@mui/material";
import AddIcon from '@mui/icons-material/Add';

import AccountService from '../services/account.service';
import { AccountDetail } from "../model/account.model";
import AccountList from "../components/account/account-list";
import Institutions from "../components/add-institution/add-institution";
import { useSearchParams } from "react-router-dom";
import { useMessageDispatch } from "../contexts/messages/context";
import PageHeader from "../components/page-header/page-header";

const bottomFabStyle: SxProps = {
  position: 'fixed',
  bottom: 16,
  right: 16,
};

export default function Accounts() {
  const [ queryParams ] = useSearchParams();
  const [ accounts, setAccounts ] = useState<Array<AccountDetail>>([]);
  const [ showInstitutions, setShowInstitutions ] = useState<boolean>(false);
  const showMessage = useMessageDispatch();

  useEffect(() => {
    const error = queryParams.get("error");
    if (error) {
      const details = queryParams.get("details")
      showMessage({ type: 'add', level: 'error', text: details ? details : error!!});
    }
  }, [ queryParams, showMessage ]);

  useEffect(() => {
    AccountService.getAll().then( response => setAccounts(response.items));
  }, []);

  return (
    <PageHeader title="Accounts">
      <AccountList accounts={accounts}/>

      <Fab color="primary" aria-label="add" sx={bottomFabStyle} onClick={() => setShowInstitutions(true)}><AddIcon /></Fab>
      <Institutions open={showInstitutions} onClose={() => setShowInstitutions(false)}></Institutions>
    </PageHeader>
  );
}
