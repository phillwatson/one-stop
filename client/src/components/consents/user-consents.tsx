import { useEffect, useState } from 'react';

import { SxProps } from '@mui/material/styles';
import Table from '@mui/material/Table';
import TableHead from '@mui/material/TableHead';
import TableBody from '@mui/material/TableBody';
import TableCell from '@mui/material/TableCell';
import TableRow from '@mui/material/TableRow';
import DeleteIcon from '@mui/icons-material/DeleteOutline';

import "./user-consents.css";
import { useNotificationDispatch } from "../../contexts/notification/context";
import UserConsentService from "../../services/consent.service"
import InstitutionService from "../../services/institution.service";
import Institution from '../../model/institution.model';
import UserConsent from '../../model/user-consent.model';
import DeleteConsentDialog from './delete-consent-dialog';
import Tooltip from '@mui/material/Tooltip';

interface Props {
}

const colhead: SxProps = {
  fontWeight: 'bold'
};

export default function UserConsentList(props: Props) {
  const showNotification = useNotificationDispatch();
  const [userConsents, setUserConsents] = useState<Array<UserConsent>>([]);
  const [institutions, setInstitutions] = useState<Array<Institution>>([]);
  const [selectedUserConsent, setSelectedUserConsent] = useState<UserConsent|undefined>(undefined);
  const [deleteDialogOpen, setDeleteDialogOpen] = useState<boolean>(false);

  useEffect(() => {
    UserConsentService.getConsents().then(response => setUserConsents(response.items));
  }, []);
  
  useEffect(() => {
    Promise.all(
      userConsents.map(consent => InstitutionService.get(consent.institutionId).then(response => response))
    ).then(result => setInstitutions(result))
  }, [ userConsents ])

  function confirmDelete(userConsent: UserConsent) {
    setSelectedUserConsent(userConsent);
    setDeleteDialogOpen(true);
  }

  function onDeleteConfirmed(userConsent: UserConsent, includeAccounts: boolean) {
    setDeleteDialogOpen(false);

    console.log("Deleting consent: " + userConsent.id)
    userConsents.forEach(consent => console.log("   >> " + consent.id))
    UserConsentService.cancelConsent(userConsent.institutionId, includeAccounts)
      .then(() => setUserConsents(userConsents.filter(consent => consent.id !== userConsent.id)))
      .catch(err => showNotification(err))
  }

  function formatDateTime(dateStr?: string): string {
    if (dateStr == null) return "";

    const date = new Date(dateStr);
    return date.toLocaleDateString("en-GB") + " " + date.toLocaleTimeString("en-GB");
  }

  function getLogo(consentInfo: UserConsent): string | undefined {
    return institutions.find(institution => institution.id === consentInfo.institutionId)?.logo
  }

  if (userConsents.length === 0) {
    return <></>
  }

  return (
    <div>
      <Table size="small" aria-label="userConsents">
        <caption><i>You're registered with the above institutions</i></caption>
        <TableHead>
          <TableRow>
            <TableCell sx={colhead} colSpan={2}>Financial Institution Consents</TableCell>
            <TableCell sx={colhead}>Status</TableCell>
            <TableCell sx={colhead}>Given</TableCell>
            <TableCell sx={colhead}>Expires</TableCell>
            <TableCell sx={colhead}></TableCell>
          </TableRow>
        </TableHead>
        <TableBody>
          { userConsents.map(consent => (
            <TableRow key={consent.id}>
              <TableCell><img src={ getLogo(consent) } alt={consent.institutionName + " logo"} width="32px" height="32px"/></TableCell>
              <TableCell width={"90%"}>{consent.institutionName}</TableCell>
              <TableCell>{consent.status}</TableCell>
              <TableCell>{formatDateTime(consent.dateGiven)}</TableCell>
              <TableCell>{formatDateTime(consent.agreementExpires)}</TableCell>
              <TableCell onClick={() => confirmDelete(consent)}>
                <Tooltip title="Revoke Consent..."><DeleteIcon/></Tooltip>
              </TableCell>
            </TableRow>
          ))}
        </TableBody>
      </Table>

      <DeleteConsentDialog userConsent={selectedUserConsent!} open={deleteDialogOpen}
         onConfirm={onDeleteConfirmed}
         onCancel={() => setDeleteDialogOpen(false)} />
    </div>
  );
}
