import * as React from 'react';
import { styled } from '@mui/material/styles';
import Card from '@mui/material/Card';
import CardHeader from '@mui/material/CardHeader';
import CardContent from '@mui/material/CardContent';
import CardActions from '@mui/material/CardActions';
import Collapse from '@mui/material/Collapse';
import Avatar from '@mui/material/Avatar';
import IconButton, { IconButtonProps } from '@mui/material/IconButton';
import Typography from '@mui/material/Typography';
import ExpandMoreIcon from '@mui/icons-material/ExpandMore';
import LinkIcon from '@mui/icons-material/Link';
import LinkOffIcon from '@mui/icons-material/LinkOff';
import CurrencyPoundIcon from '@mui/icons-material/CurrencyPound';

import Bank from '../../model/bank.model';
import UserConsent from '../../model/user-consent.model';

interface Props {
    bank: Bank;
    consent?: UserConsent;
    onLinkSelect?: (bank: Bank, link: boolean) => void;
}

interface ExpandMoreProps extends IconButtonProps {
  expand: boolean;
}

const ExpandMore = styled((props: ExpandMoreProps) => {
  const { expand, ...other } = props;
  return <IconButton {...other} />;
})(({ theme, expand }) => ({
  transform: !expand ? 'rotate(0deg)' : 'rotate(180deg)',
  marginLeft: 'auto',
  transition: theme.transitions.create('transform', {
    duration: theme.transitions.duration.shortest,
  }),
}));

export default function BankCard(props: Props) {
  const [expanded, setExpanded] = React.useState(false);

  function handleExpandClick() {
    if (props.consent) {
      setExpanded(!expanded);
    }
  };

  function isActionAvailable() {
    return props.onLinkSelect !== undefined
  }

  function handleConnectToBank(bank: Bank, link: boolean) {
    if (props.onLinkSelect !== undefined) {
      props.onLinkSelect(bank, link);
    }
  }

  return (
    <Card>
      <CardHeader
        avatar={ <Avatar aria-label={ props.bank.name } src={ props.bank.logo } /> }
        title={ props.bank.name } subheader={ props.bank.bic }
        onClickCapture={ handleExpandClick }
      />
        { (!props.consent && isActionAvailable()) &&
          <CardActions disableSpacing>
            <IconButton aria-label="connect to bank" onClick={ () => handleConnectToBank(props.bank, false) }>
              <LinkIcon />
            </IconButton>
            { props.bank.paymentsEnabled ? <CurrencyPoundIcon /> : null}
          </CardActions>
        }
        { props.consent &&
          <CardActions disableSpacing>
            { props.bank.paymentsEnabled ? <CurrencyPoundIcon /> : null}
            <ExpandMore expand={expanded} onClick={handleExpandClick} aria-expanded={expanded} aria-label="show accounts">
              <ExpandMoreIcon />
            </ExpandMore>
          </CardActions>
        }
      <Collapse in={expanded} timeout="auto" unmountOnExit>
        { props.consent &&
          <CardContent>
            <Typography paragraph>Consent Status: { props.consent!.status }</Typography>
            <Typography>Show accounts and balances.</Typography>
          </CardContent>
        }
        { props.consent && props.consent.status !== "CANCELLED" && props.consent.status !== "DENIED" && isActionAvailable() &&
          <CardActions disableSpacing>
            <IconButton aria-label="close connection" onClick={ () => handleConnectToBank(props.bank, true) }>
              <LinkOffIcon />
            </IconButton>
          </CardActions>
        }
      </Collapse>
    </Card>
  );
}
