import { useEffect, useState } from 'react';

import "./auth-provider-list.css";
import DeleteIcon from '@mui/icons-material/LinkOffOutlined';
import { SxProps } from '@mui/material/styles';
import Table from '@mui/material/Table';
import TableHead from '@mui/material/TableHead';
import TableBody from '@mui/material/TableBody';
import TableCell from '@mui/material/TableCell';
import TableRow from '@mui/material/TableRow';

import { UserAuthProvider } from "../../model/user-profile.model";
import ProfileService from "../../services/profile.service"
import Tooltip from '@mui/material/Tooltip/Tooltip';
import { useMessageDispatch } from '../../contexts/messages/context';
import ConfirmationDialog from '../dialogs/confirm-dialog';

const colhead: SxProps = {
  fontWeight: 'bold'
};

function formatDateTime(dateStr?: string): string {
  if (dateStr == null) return "";

  const date = new Date(dateStr);
  return date.toLocaleDateString("en-GB") + " " + date.toLocaleTimeString("en-GB");
}

export default function AuthProviderList() {
  const showMessage = useMessageDispatch();
  const [ authProviders, setAuthProviders ] = useState<Array<UserAuthProvider>>([]);
  const [ deleteDialogOpen, setDeleteDialogOpen ] = useState<boolean>(false);
  const [ selectedAuthProvider, setSelectedAuthProvider ] = useState<UserAuthProvider|undefined>(undefined);

  useEffect(() => {
    ProfileService.getAuthProviders().then( response => setAuthProviders(response));
  }, []);

  function confirmDelete(authProvider: UserAuthProvider) {
    setSelectedAuthProvider(authProvider);
    setDeleteDialogOpen(true);
  }

  function onDeleteConfirmed() {
    setDeleteDialogOpen(false);

    var authProvider = selectedAuthProvider!
    ProfileService.deleteAuthProvider(authProvider.id)
      .then(() => setAuthProviders(authProviders.filter(ap => ap.id !== authProvider.id)))
      .catch(err => showMessage(err))
  }

  return (
    <div>
      <Table size="small" aria-label="authproviders">
        <caption><i>You're registered with the above auth providers</i></caption>
        <TableHead>
          <TableRow>
            <TableCell sx={colhead} colSpan={2}>Auth Providers</TableCell>
            <TableCell sx={colhead}>Created</TableCell>
            <TableCell sx={colhead}>Last Used</TableCell>
            <TableCell sx={colhead}></TableCell>
          </TableRow>
        </TableHead>
        <TableBody>
          { authProviders.map(authProvider => (
            <TableRow key={authProvider.id}>
              <TableCell><img src={ authProvider.logo } alt={authProvider.name + " logo"} width="32px" height="32px"/></TableCell>
              <TableCell width={"70%"}>{authProvider.name}</TableCell>
              <TableCell>{formatDateTime(authProvider.dateCreated)}</TableCell>
              <TableCell>{formatDateTime(authProvider.dateLastUsed)}</TableCell>
              <TableCell onClick={() => confirmDelete(authProvider)}>
                <Tooltip title="Unlink Auth Provider..."><DeleteIcon/></Tooltip>
              </TableCell>
            </TableRow>
          ))}
        </TableBody>
      </Table>

      <ConfirmationDialog open={deleteDialogOpen}
                  title={"Unlink Auth Provider "+ selectedAuthProvider?.name}
                  content="Are you sure you want to unlink this auth provider?"
                  onConfirm={onDeleteConfirmed}
                  onCancel={() => setDeleteDialogOpen(false)} />
    </div>
  );
}