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

import Institution from '../../model/institution.model';
import UserConsent from '../../model/user-consent.model';

interface Props {
  institution: Institution;
    consent?: UserConsent;
    onLinkSelect?: (institution: Institution, link: boolean) => void;
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

  function handleConnectToBank(institution: Institution, link: boolean) {
    if (props.onLinkSelect !== undefined) {
      props.onLinkSelect(institution, link);
    }
  }

  return (
    <Card>
      <CardHeader
        avatar={ <Avatar aria-label={ props.institution.name } src={ props.institution.logo } /> }
        title={ props.institution.name } subheader={ props.institution.bic }
        onClickCapture={ handleExpandClick }
      />
        { (!props.consent && isActionAvailable()) &&
          <CardActions disableSpacing>
            <IconButton aria-label="connect to institution" onClick={ () => handleConnectToBank(props.institution, false) }>
              <LinkIcon />
            </IconButton>
          </CardActions>
        }
        { props.consent &&
          <CardActions disableSpacing>
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
            <IconButton aria-label="close connection" onClick={ () => handleConnectToBank(props.institution, true) }>
              <LinkOffIcon />
            </IconButton>
          </CardActions>
        }
      </Collapse>
    </Card>
  );
}
