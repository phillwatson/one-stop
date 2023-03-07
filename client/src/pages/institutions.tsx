import React from "react";

import Country from '../model/country.model';
import CountryService from '../services/country.service';
import CountrySelector from '../components/country-selector/country-selector'
import Bank from '../model/bank.model';
import BankService from '../services/bank.service';
import BankList from '../components/bank-list/bank-list'
import UserConsentService from '../services/consent.service';
import UserConsent from "../model/user-consent.model";

export default function Institutions() {
  const [userConsents, setUserConsents] = React.useState<Array<UserConsent> | undefined>(undefined);

  const [countries, setCountries] = React.useState<Array<Country> | undefined>(undefined);
  const [activeCountry, setActiveCountry] = React.useState<Country | undefined>(undefined);

  const [banks, setBanks] = React.useState<Array<Bank> | undefined>(undefined);

  React.useEffect(() => {
    CountryService.getAll().then((response) => {
      setCountries(response.data);
    });

    UserConsentService.getConsents().then((response) => {
      setUserConsents(response.data);
    });
  }, []);

  React.useEffect(() => {
    if (activeCountry === undefined) {
        setBanks(undefined);
    } else {
        BankService.getAll(activeCountry.id).then((response) => {
          setBanks(response.data);
        });
    }
  }, [ activeCountry, userConsents ]);
  
  function handleLinkSelect(bank: Bank, link: boolean) {
    if (link) {
      UserConsentService.cancelConsent(bank.id).then(() => {
        if (userConsents !== undefined) {
          const update = userConsents.filter(consent => consent.institutionId !== bank.id);
          setUserConsents(update);
        }
      });
    } else {
      UserConsentService.registerConsent(bank.id).then(response => {
        UserConsentService.getConsent(bank.id).then(response => {
          const update = (userConsents === undefined)
            ? [ response.data ]
            : userConsents.concat([ response.data]);
          setUserConsents(update);
        });

        console.log(`Redirecting to ${response.data}`);
        window.open(response.data, '_blank');
      });
    }
  }

  return (
    <div>
      <div>
        Select Country: <CountrySelector countries={ countries }
            activeCountryId={ (activeCountry === undefined) ? undefined : activeCountry.id }
            onSelectCountry={ setActiveCountry }/>
      </div>
      <div>
        <BankList banks={ banks } userConsents={ userConsents } onLinkSelect={ handleLinkSelect }/>
      </div>
    </div>
  );
}
