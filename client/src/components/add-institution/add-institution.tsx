import { forwardRef, useEffect, useState } from "react";

import Button from "@mui/material/Button";
import Dialog from "@mui/material/Dialog"
import DialogActions from "@mui/material/DialogActions";
import DialogContent from "@mui/material/DialogContent";
import DialogTitle from "@mui/material/DialogTitle";
import Slide from "@mui/material/Slide";
import { TransitionProps } from "@mui/material/transitions";
import { SxProps } from "@mui/material";

import CountryService from '../../services/country.service';
import InstitutionService from '../../services/institution.service';
import UserConsentService from '../../services/consent.service';

import UserConsent from "../../model/user-consent.model";
import Country from '../../model/country.model';
import Institution from '../../model/institution.model';

import CountrySelector from '../country-selector/country-selector'
import BankList from '../bank-list/bank-list'
import { useMessageDispatch } from '../../contexts/messages/context';
import { getDefaultLocaleRegion } from "../../util/date-util";

interface Props {
  open: boolean;
  onClose: () => void;
}

const dialogTitle: SxProps = {
  backgroundColor: 'primary.main',
  color: 'common.white',
};

const Transition = forwardRef(function Transition(
  props: TransitionProps & { children: React.ReactElement<any, any> },
  ref: React.Ref<unknown>,
) {
  return <Slide direction="up" ref={ref} {...props} />;
});

function defaultCountry(countries: Array<Country>): Country | undefined {
  const defaultRegion = getDefaultLocaleRegion()
  if (defaultRegion) {
    return countries.find(c =>
      c.id.localeCompare(defaultRegion, undefined, { sensitivity: 'base' }) === 0
    );
  }
}

export default function Institutions(props: Props) {
  const showMessage = useMessageDispatch();

  const handleClose = () => {
    props.onClose();
  };

  const [userConsents, setUserConsents] = useState<Array<UserConsent>>([]);
  const [countries, setCountries] = useState<Array<Country>>([]);
  const [activeCountry, setActiveCountry] = useState<Country | undefined>();
  const [institutions, setInstitutions] = useState<Array<Institution>>([]);

  useEffect(() => {
    if (props.open) {
      CountryService.getAll()
        .then(data => setCountries(data.items) );

      // get user's consents that have not been cancelled or denied
      UserConsentService.getConsents(0, 3000).then(data => {
        setUserConsents(data.items.filter(consent => (consent.status !== 'CANCELLED' && consent.status !== 'DENIED')));
      });
    }
  }, [ props.open ]);

  useEffect(() => {
    if (activeCountry === undefined) {
      setActiveCountry(defaultCountry(countries));
    } else {
      InstitutionService.getAll(activeCountry.id, 0, 3000).then(data => setInstitutions(data.items));
    }
  }, [ countries, activeCountry, userConsents ]);

  function handleLinkSelect(institution: Institution) {
    UserConsentService.registerConsent(institution.id).then(registerUri => {
      window.location = registerUri;
    })
    .catch(err => showMessage(err))
  }

  return (
    <Dialog open={ props.open } onClose={ handleClose }
      TransitionComponent={Transition} aria-labelledby="scroll-dialog-title">
      <DialogTitle id="scroll-dialog-title" sx={ dialogTitle }>Add Institution</DialogTitle>

      <DialogContent dividers={ true }>
        <CountrySelector
          countries={ countries }
          activeCountry={ activeCountry }
          onSelectCountry={ setActiveCountry }/>
      </DialogContent>

      <DialogContent>
        <BankList institutions={ institutions } userConsents={ userConsents } onLinkSelect={ handleLinkSelect }/>
      </DialogContent>

      <DialogActions>
        <Button onClick={ handleClose }>Cancel</Button>
      </DialogActions>
    </Dialog>
  );
}
