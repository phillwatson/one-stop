import React from "react";
import { Button, Dialog, DialogActions, DialogContent, DialogContentText, DialogTitle, Slide } from "@mui/material";

import Country from '../../model/country.model';
import CountryService from '../../services/country.service';
import CountrySelector from '../country-selector/country-selector'
import Institution from '../../model/institution.model';
import InstitutionService from '../../services/institution.service';
import BankList from '../bank-list/bank-list'
import UserConsentService from '../../services/consent.service';
import UserConsent from "../../model/user-consent.model";
import { useNotificationDispatch } from '../../contexts/notification-context';
import ServiceError from "../../model/service-error";
import { TransitionProps } from "@mui/material/transitions";

interface Props {
  open: boolean;
  onClose: () => void;
}

const dialogPaper = {
  minHeight: '60vh',
  maxHeight: '60vh',
};

const Transition = React.forwardRef(function Transition(
  props: TransitionProps & {
    children: React.ReactElement<any, any>;
  },
  ref: React.Ref<unknown>,
) {
  return <Slide direction="up" ref={ref} {...props} />;
});

export default function Institutions(props: Props) {
  const showNotification = useNotificationDispatch();

  const handleClose = () => {
    props.onClose();
  };

  const [userConsents, setUserConsents] = React.useState<Array<UserConsent> | undefined>(undefined);
  const [countries, setCountries] = React.useState<Array<Country> | undefined>(undefined);
  const [activeCountry, setActiveCountry] = React.useState<Country | undefined>(undefined);
  const [institutions, setInstitutions] = React.useState<Array<Institution> | undefined>(undefined);

  React.useEffect(() => {
    if (props.open) {
      CountryService.getAll().then(data => {
        setCountries(data);
      });

      // get user's consents that have not been cancelled or denied
      UserConsentService.getConsents(0, 3000).then(data => {
        setUserConsents(data.items.filter(consent => (consent.status !== 'CANCELLED' && consent.status !== 'DENIED')));
      });
    }
  }, [ props.open ]);

  React.useEffect(() => {
    if (activeCountry === undefined) {
      setInstitutions(undefined);
    } else {
      InstitutionService.getAll(activeCountry.id, 0, 3000).then(data => {
          setInstitutions(data.items);
        });
    }
  }, [ activeCountry, userConsents ]);
  
  function handleLinkSelect(institution: Institution) {
    UserConsentService.registerConsent(institution.id).then(registerUri => {
      console.log(`Redirecting to ${registerUri}`);
      window.location = registerUri;
    })
    .catch(err => showNotification({ type: "add", level: "error", message: (err as ServiceError).message }))
  }

  return (
    <Dialog open={props.open} onClose={handleClose} scroll="paper" fullWidth={true} maxWidth="lg"
      TransitionComponent={Transition} 
        aria-labelledby="scroll-dialog-title">
      <DialogTitle id="scroll-dialog-title">Add Institution</DialogTitle>
      <DialogContent dividers={true}>
        Select Country: <CountrySelector countries={ countries }
            activeCountryId={ (activeCountry === undefined) ? undefined : activeCountry.id }
            onSelectCountry={ setActiveCountry }/>
      </DialogContent>
      <DialogContent>
        <DialogContentText style={dialogPaper}>
          <BankList institutions={ institutions } userConsents={ userConsents } onLinkSelect={ handleLinkSelect }/>
        </DialogContentText>

      </DialogContent>
      <DialogActions>
          <Button onClick={handleClose}>Cancel</Button>
      </DialogActions>
    </Dialog>
  );
}
