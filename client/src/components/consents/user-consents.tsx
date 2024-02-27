import { useEffect, useState } from 'react';

import { SxProps } from '@mui/material/styles';
import Table from '@mui/material/Table';
import TableHead from '@mui/material/TableHead';
import TableBody from '@mui/material/TableBody';
import TableCell from '@mui/material/TableCell';
import TableRow from '@mui/material/TableRow';

import "./user-consents.css";
import UserConsentService from "../../services/consent.service"
import InstitutionService from "../../services/institution.service";
import Institution from '../../model/institution.model';
import UserConsent from '../../model/user-consent.model';

interface Props {
}

const colhead: SxProps = {
  fontWeight: 'bold'
};

export default function UserConsentList(props: Props) {
  const [userConsents, setUserConsents] = useState<Array<UserConsent>>([]);
  const [institutions, setInstitutions] = useState<Array<Institution>>([]);

  useEffect(() => {
    UserConsentService.getConsents().then(response => setUserConsents(response.items));
  }, []);
  
  useEffect(() => {
    Promise.all(
      userConsents.map(consent => InstitutionService.get(consent.institutionId).then(response => response))
    ).then(result => setInstitutions(result))
  }, [ userConsents ])

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
    <div className="panel">
      <Table size="small" aria-label="userConsents">
        <caption><i>You're registered with the above institutions</i></caption>
        <TableHead>
          <TableRow>
            <TableCell sx={colhead} colSpan={2}>Financial Institution Consents</TableCell>
            <TableCell sx={colhead}>Given</TableCell>
            <TableCell sx={colhead}>Expires</TableCell>
          </TableRow>
        </TableHead>
        <TableBody>
          { userConsents.map(consent => (
            <TableRow key={consent.id}>
              <TableCell><img src={ getLogo(consent) } alt={consent.institutionName + " logo"} width="32px" height="32px"/></TableCell>
              <TableCell width={"90%"}>{consent.institutionName}</TableCell>
              <TableCell>{formatDateTime(consent.dateGiven)}</TableCell>
              <TableCell>{formatDateTime(consent.agreementExpires)}</TableCell>
            </TableRow>
          ))}
        </TableBody>
      </Table>
    </div>
  );
}
