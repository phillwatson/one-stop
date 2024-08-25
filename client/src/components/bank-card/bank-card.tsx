import Card from '@mui/material/Card';
import CardHeader from '@mui/material/CardHeader';
import Avatar from '@mui/material/Avatar';

import Institution from '../../model/institution.model';
import UserConsent from '../../model/user-consent.model';

import styles from './bank-card.module.css';

interface Props {
  institution: Institution;
    consent?: UserConsent;
    onLinkSelect?: (institution: Institution) => void;
}

// those statuses in which an existing consent can be renewed/registered
const RENEWABLE_STATUSES = ["EXPIRED", "SUSPENDED", "DENIED", "TIMEOUT", "CANCELLED"];

export default function BankCard(props: Props) {
  const enabled = props.consent === undefined || RENEWABLE_STATUSES.includes(props.consent.status);
  const label = props.institution.bic + (props.consent === undefined ? "" : ": " + props.consent?.status);

  function handleConnectToBank(institution: Institution) {
    if ((enabled) && (props.onLinkSelect !== undefined)) {
      props.onLinkSelect(institution);
    }
  }

  return (
    <Card className={`${styles.card} ${ (enabled ? styles.enabled : styles.disabled ) }`} elevation={ enabled ? 7 : 1} >
      <CardHeader 
        avatar={ <Avatar aria-label={ props.institution.name } src={ props.institution.logo } /> }
        title={ props.institution.name } subheader={ label }
        onClickCapture={ () => { if (enabled) { handleConnectToBank(props.institution) }} }
      />
    </Card>
  );
}
