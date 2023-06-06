import React from "react";

import Country from '../model/country.model';
import CountryService from '../services/country.service';
import CountrySelector from '../components/country-selector/country-selector'
import Institution from '../model/institution.model';
import InstitutionService from '../services/institution.service';
import BankList from '../components/bank-list/bank-list'
import UserConsentService from '../services/consent.service';
import UserConsent from "../model/user-consent.model";

export default function Institutions() {
  const [userConsents, setUserConsents] = React.useState<Array<UserConsent> | undefined>(undefined);

  const [countries, setCountries] = React.useState<Array<Country> | undefined>(undefined);
  const [activeCountry, setActiveCountry] = React.useState<Country | undefined>(undefined);

  const [institutions, setInstitutions] = React.useState<Array<Institution> | undefined>(undefined);

  React.useEffect(() => {
    CountryService.getAll().then(data => {
      setCountries(data);
    });

    UserConsentService.getConsents(0, 3000).then(data => {
      setUserConsents(data.items);
    });
  }, []);

  React.useEffect(() => {
    if (activeCountry === undefined) {
      setInstitutions(undefined);
    } else {
      InstitutionService.getAll(activeCountry.id, 0, 3000).then(data => {
          setInstitutions(data.items);
        });
    }
  }, [ activeCountry, userConsents ]);
  
  function handleLinkSelect(institution: Institution, link: boolean) {
    if (link) {
      UserConsentService.cancelConsent(institution.id).then(() => {
        if (userConsents !== undefined) {
          const update = userConsents.filter(consent => consent.institutionId !== institution.id);
          setUserConsents(update);
        }
      });
    } else {
      UserConsentService.registerConsent(institution.id).then(registerUri => {
        UserConsentService.getConsent(institution.id).then(data => {
          const update = (userConsents === undefined)
            ? [ data ]
            : userConsents.concat([ data ]);
          setUserConsents(update);
        });

        console.log(`Redirecting to ${registerUri}`);
        window.location = registerUri;
      });
    }
  }

  return (
    <div>
      <h2>Institutions</h2>
      <div>
        Select Country: <CountrySelector countries={ countries }
            activeCountryId={ (activeCountry === undefined) ? undefined : activeCountry.id }
            onSelectCountry={ setActiveCountry }/>
      </div>
      <div>
        <BankList institutions={ institutions } userConsents={ userConsents } onLinkSelect={ handleLinkSelect }/>
      </div>
    </div>
  );
}
