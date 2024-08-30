import { useEffect, useState } from 'react';

import { SxProps } from '@mui/material/styles';
import Table from '@mui/material/Table';
import TableHead from '@mui/material/TableHead';
import TableBody from '@mui/material/TableBody';
import TableCell from '@mui/material/TableCell';
import TableRow from '@mui/material/TableRow';
import DeleteIcon from '@mui/icons-material/DeleteOutline';

import { useMessageDispatch } from "../../contexts/messages/context";
import UserConsentService from "../../services/consent.service"
import InstitutionService from "../../services/institution.service";
import Institution from '../../model/institution.model';
import UserConsent from '../../model/user-consent.model';
import DeleteConsentDialog from './delete-consent-dialog';
import Tooltip from '@mui/material/Tooltip';
import { formatDateTime } from '../../util/date-util';
import Avatar from '@mui/material/Avatar/Avatar';

interface Props {
}

const colhead: SxProps = {
  fontWeight: 'bold'
};

const VISIBLE_CONSENTS = ["GIVEN", "DENIED", "SUSPENDED", "EXPIRED", "TIMEOUT"];

export default function UserConsentList(props: Props) {
  const showMessage = useMessageDispatch();
  const [userConsents, setUserConsents] = useState<Array<UserConsent>>([]);
  const [institutions, setInstitutions] = useState<Array<Institution>>([]);
  const [selectedUserConsent, setSelectedUserConsent] = useState<UserConsent|undefined>(undefined);
  const [deleteDialogOpen, setDeleteDialogOpen] = useState<boolean>(false);

  useEffect(() => {
    UserConsentService.getConsents().then(response => 
      setUserConsents(response.items.filter(consent => VISIBLE_CONSENTS.includes(consent.status))));
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
    UserConsentService.cancelConsent(userConsent.institutionId, includeAccounts)
      .then(() => {
        showMessage({ type: "add", text: `Revoked consent for ${userConsent.institutionName}.`, level: "success"})
        setUserConsents(userConsents.filter(consent => consent.id !== userConsent.id));
      })
      .catch(err => showMessage(err))
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
              <TableCell>
                <Avatar src={ getLogo(consent) } alt={ consent.institutionName + " logo" } sx={{ width: "32px", height: "32px" }}></Avatar>
              </TableCell>
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
