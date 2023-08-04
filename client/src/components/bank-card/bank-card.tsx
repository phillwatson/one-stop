import Card from '@mui/material/Card';
import CardHeader from '@mui/material/CardHeader';
import Avatar from '@mui/material/Avatar';

import Institution from '../../model/institution.model';
import UserConsent from '../../model/user-consent.model';

import './bank-card.css';

interface Props {
  institution: Institution;
    consent?: UserConsent;
    onLinkSelect?: (institution: Institution) => void;
}

export default function BankCard(props: Props) {
  const enabled = props.consent === undefined || props.consent.status === "EXPIRED";
  const css = "card " + (enabled ? "enabled" : "disabled");
  const label = props.institution.bic + (props.consent === undefined ? "" : ": " + props.consent?.status);


  function handleConnectToBank(institution: Institution) {
    if ((enabled) && (props.onLinkSelect !== undefined)) {
      props.onLinkSelect(institution);
    }
  }

  return (
    <Card className={css} elevation={enabled ? 7 : 1}>
      <CardHeader 
        avatar={ <Avatar aria-label={ props.institution.name } src={ props.institution.logo } /> }
        title={ props.institution.name } subheader={ label }
        onClickCapture={ () => { if (enabled) { handleConnectToBank(props.institution) }} }
      />
    </Card>
  );
}
